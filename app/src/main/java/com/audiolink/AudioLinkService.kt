package com.audiolink

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.audiolink.ui.AudioLinkPill
import com.audiolink.ui.AudioLinkTheme
import com.audiolink.ui.CardState

/**
 * Etapa 2: detecta "/audio" en campos de texto de otras apps y muestra la tarjeta
 * inmediatamente encima del campo.
 *
 * Privacidad: solo se lee el texto del campo que está en foco para comprobar si es exactamente
 * el comando. No se guarda, no se registra y no se envía a ningún sitio. Los campos de contraseña
 * se ignoran. Los eventos de la propia app se ignoran.
 */
class AudioLinkService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var wm: WindowManager

    private var overlay: ComposeView? = null
    private var owner: OverlayOwner? = null
    private var params: WindowManager.LayoutParams? = null

    private var tracked: AccessibilityNodeInfo? = null
    private var trackedPkg: String? = null
    private var lastHeight = 0
    private var interacting = false   // true mientras la tarjeta necesita teclado propio (Escuchar)

    private val cardState = mutableStateOf(CardState.MENU)
    private val repositionRunnable = Runnable { reposition() }

    private val baseFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

    override fun onServiceConnected() {
        super.onServiceConnected()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    // ---------- Eventos ----------

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.packageName == packageName) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> onTextChanged(event)

            AccessibilityEvent.TYPE_VIEW_FOCUSED -> if (overlay != null && !interacting) {
                val source = event.source
                if (source == null || source != tracked) hideCard() else scheduleReposition()
            }

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> if (overlay != null && !interacting) {
                val active = rootInActiveWindow?.packageName
                if (active != null && active != trackedPkg) hideCard() else scheduleReposition()
            }

            else -> if (overlay != null) scheduleReposition()
        }
    }

    private fun onTextChanged(event: AccessibilityEvent) {
        val node = event.source ?: return
        if (node.isPassword || !node.isEditable) return
        val pkg = event.packageName?.toString() ?: return

        if (CommandMatcher.isCommand(textOf(node))) {
            if (overlay != null && node == tracked) {
                scheduleReposition()          // ya visible: sin activaciones duplicadas
            } else {
                hideCard()
                showCard(node, pkg)
            }
        } else if (overlay != null && node == tracked && !interacting) {
            hideCard()                         // el comando ya no está completo o fue borrado
        }
    }

    private fun textOf(node: AccessibilityNodeInfo): String =
        if (node.isShowingHintText) "" else node.text?.toString().orEmpty()

    // ---------- Tarjeta ----------

    private fun showCard(node: AccessibilityNodeInfo, pkg: String) {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.isEmpty) return

        tracked = node
        trackedPkg = pkg
        cardState.value = CardState.MENU
        interacting = false
        lastHeight = 0

        val themed = ContextThemeWrapper(this, R.style.Theme_AudioLink)
        val newOwner = OverlayOwner().also { it.start() }
        val view = ComposeView(themed).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(newOwner)
            setViewTreeViewModelStoreOwner(newOwner)
            setViewTreeSavedStateRegistryOwner(newOwner)
            setContent {
                AudioLinkTheme {
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { visible = true }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(120)) + slideInVertically(tween(120)) { it / 3 },
                    ) {
                        AudioLinkPill(
                            state = cardState.value,
                            onStateChange = { setState(it) },
                            onClose = { clearCommandAndHide() },
                        )
                    }
                }
            }
            addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
                val h = bottom - top
                if (h != lastHeight) { lastHeight = h; scheduleReposition() }
            }
        }

        val lp = WindowManager.LayoutParams(
            dp(320), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            baseFlags, PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        }
        placeAbove(rect, lp, dp(56))

        try {
            wm.addView(view, lp)
            overlay = view
            owner = newOwner
            params = lp
        } catch (e: Exception) {
            newOwner.stop()
            tracked = null
            trackedPkg = null
        }
    }

    private fun hideCard() {
        handler.removeCallbacks(repositionRunnable)
        overlay?.let { try { wm.removeView(it) } catch (e: Exception) { } }
        owner?.stop()
        overlay = null
        owner = null
        params = null
        tracked = null
        trackedPkg = null
        interacting = false
    }

    /** X: cierra la tarjeta y, si el campo sigue conteniendo solo el comando, lo borra. */
    private fun clearCommandAndHide() {
        val node = tracked
        if (node != null && node.refresh() && CommandMatcher.isCommand(textOf(node))) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
        hideCard()
    }

    private fun setState(newState: CardState) {
        cardState.value = newState
        setInteractive(newState == CardState.LISTEN)
    }

    /** Solo en Escuchar la ventana necesita foco para poder escribir el identificador. */
    private fun setInteractive(on: Boolean) {
        interacting = on
        val view = overlay ?: return
        val lp = params ?: return
        lp.flags = if (on) baseFlags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv() else baseFlags
        try { wm.updateViewLayout(view, lp) } catch (e: Exception) { }
    }

    // ---------- Posición ----------

    private fun scheduleReposition() {
        handler.removeCallbacks(repositionRunnable)
        handler.postDelayed(repositionRunnable, 16)
    }

    private fun reposition() {
        val view = overlay ?: return
        val lp = params ?: return
        val node = tracked ?: return
        if (!node.refresh() || !node.isVisibleToUser) { hideCard(); return }
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.isEmpty) { hideCard(); return }
        placeAbove(rect, lp, if (view.height > 0) view.height else dp(56))
        try { wm.updateViewLayout(view, lp) } catch (e: Exception) { }
    }

    /** Coloca la tarjeta con su borde inferior justo encima del campo. */
    private fun placeAbove(field: Rect, lp: WindowManager.LayoutParams, cardHeight: Int) {
        val screenW = resources.displayMetrics.widthPixels
        val w = field.width().coerceAtLeast(dp(280)).coerceAtMost(screenW)
        lp.width = w
        lp.x = field.left.coerceIn(0, (screenW - w).coerceAtLeast(0))
        lp.y = (field.top - cardHeight - dp(6)).coerceAtLeast(0)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    // ---------- Ciclo de vida ----------

    override fun onInterrupt() = hideCard()

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        hideCard()
        Toast.makeText(this, "Audio Link: la detección de /audio se ha desactivado.", Toast.LENGTH_LONG).show()
        return super.onUnbind(intent)
    }
}
