package com.audiolink.ui

import android.os.SystemClock
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class CardState { MENU, RECORDING, PREVIEW, LISTEN }

/**
 * Barra compacta de Audio Link (una sola fila de 52 dp).
 *
 * Etapa 1: solo interfaz y navegación entre estados.
 * No es arrastrable ni desplazable: no tiene ningún gesto asociado.
 * La grabación, reproducción, envío y validación reales se conectan en etapas posteriores
 * mediante onPlay / onSend / onValidate. Mientras sean null, se avisa de que aún no existen;
 * no se simula que funcionen.
 */
@Composable
fun AudioLinkPill(
    state: CardState,
    onStateChange: (CardState) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    onPlay: (() -> Unit)? = null,
    onSend: ((durationMs: Long) -> Unit)? = null,
    onValidate: ((id: String) -> Unit)? = null,
    validating: Boolean = false,
    error: String? = null,
) {
    var recordedMs by remember { mutableLongStateOf(0L) }
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    fun pending(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    fun tap() = haptics.performHapticFeedback(HapticFeedbackType.LongPress)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = shape,
        color = AL.colors.card,
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (fadeIn(tween(140)) + scaleIn(tween(140), initialScale = 0.96f)) togetherWith
                    fadeOut(tween(90))
            },
            label = "audio-link-card",
        ) { s ->
            when (s) {
                CardState.MENU -> MenuRow(
                    onRecord = { tap(); onStateChange(CardState.RECORDING) },
                    onListen = { tap(); onStateChange(CardState.LISTEN) },
                    onClose = onClose,
                )
                CardState.RECORDING -> RecordingRow(
                    onCancel = { onStateChange(CardState.MENU) },
                    onFinish = { ms -> tap(); recordedMs = ms; onStateChange(CardState.PREVIEW) },
                )
                CardState.PREVIEW -> PreviewRow(
                    durationMs = recordedMs,
                    onPlay = { onPlay?.invoke() ?: pending("La reproducción real llega en la Etapa 3") },
                    onRepeat = { onStateChange(CardState.RECORDING) },
                    onDiscard = { onStateChange(CardState.MENU) },
                    onSend = { tap(); onSend?.invoke(recordedMs) ?: pending("El envío con identificador llega en la Etapa 4") },
                )
                CardState.LISTEN -> ListenRow(
                    validating = validating,
                    externalError = error,
                    onBack = { onStateChange(CardState.MENU) },
                    onValidate = { id -> onValidate?.invoke(id) ?: pending("La validación llega en la Etapa 4") },
                )
            }
        }
    }
}

// ---------- Filas por estado ----------

@Composable
private fun MenuRow(onRecord: () -> Unit, onListen: () -> Unit, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).padding(start = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlAction("Grabar", onRecord, Modifier.weight(1f), filled = true, dot = AL.colors.danger)
        Spacer(Modifier.width(6.dp))
        AlAction("Escuchar", onListen, Modifier.weight(1f), icon = Icons.Rounded.PlayArrow)
        CloseButton(onClose)
    }
}

@Composable
private fun RecordingRow(onCancel: () -> Unit, onFinish: (Long) -> Unit) {
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = SystemClock.elapsedRealtime()
        while (true) {
            elapsed = SystemClock.elapsedRealtime() - start
            delay(100)
        }
    }
    val pulse by rememberInfiniteTransition(label = "rec").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "rec-dot",
    )
    Row(
        Modifier.fillMaxWidth().height(52.dp).padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).alpha(pulse).background(AL.colors.danger, CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(formatTime(elapsed), color = AL.colors.text, fontSize = 15.sp)
        Spacer(Modifier.weight(1f))
        AlAction("Cancelar", onCancel, tonalBg = Color.Transparent, textColor = AL.colors.muted)
        Spacer(Modifier.width(4.dp))
        AlAction("Finalizar", { onFinish(elapsed) }, filled = true, icon = Icons.Rounded.Check)
    }
    // Etapa 3: aquí se inicia la captura real tras comprobar el permiso de micrófono.
}

@Composable
private fun PreviewRow(
    durationMs: Long,
    onPlay: () -> Unit,
    onRepeat: () -> Unit,
    onDiscard: () -> Unit,
    onSend: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).padding(start = 10.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(AL.colors.tonal).clickable(onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.PlayArrow, "Reproducir", tint = AL.colors.text, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(formatTime(durationMs), color = AL.colors.text, fontSize = 15.sp)
        Spacer(Modifier.weight(1f))
        SmallIcon(Icons.Rounded.Refresh, "Repetir grabación", onRepeat)
        SmallIcon(Icons.Rounded.Delete, "Descartar", onDiscard)
        Spacer(Modifier.width(4.dp))
        AlAction("Enviar", onSend, filled = true, icon = Icons.Rounded.Send)
    }
}

@Composable
private fun ListenRow(
    validating: Boolean,
    externalError: String?,
    onBack: () -> Unit,
    onValidate: (String) -> Unit,
) {
    var id by rememberSaveable { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val shownError = externalError ?: localError

    Column {
        Row(
            Modifier.fillMaxWidth().height(52.dp).padding(start = 6.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SmallIcon(Icons.Rounded.ArrowBack, "Volver", onBack)
            Box(Modifier.weight(1f).padding(horizontal = 6.dp)) {
                BasicTextField(
                    value = id,
                    onValueChange = { id = it.filter { c -> !c.isWhitespace() }; localError = null },
                    singleLine = true,
                    textStyle = TextStyle(color = AL.colors.text, fontSize = 15.sp),
                    cursorBrush = SolidColor(AL.colors.accent),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (id.isEmpty()) Text("Identificador", color = AL.colors.muted, fontSize = 15.sp)
                        inner()
                    },
                )
            }
            AlAction(
                "Validar y escuchar",
                {
                    if (id.isBlank()) localError = "Introduce un identificador." else onValidate(id.trim())
                },
                filled = true,
                busy = validating,
            )
        }
        if (shownError != null) {
            Text(
                shownError,
                color = AL.colors.danger,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 50.dp, bottom = 10.dp),
            )
        }
    }
}

// ---------- Componentes ----------

@Composable
fun AlAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    icon: ImageVector? = null,
    dot: Color? = null,
    busy: Boolean = false,
    tonalBg: Color = AL.colors.tonal,
    textColor: Color? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (filled) AL.colors.accent else tonalBg,
        contentColor = textColor ?: if (filled) AL.colors.onAccent else AL.colors.text,
    ) {
        Row(
            Modifier.fillMaxHeight().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            when {
                busy -> CircularProgressIndicator(
                    Modifier.size(14.dp), strokeWidth = 2.dp, color = LocalContentColor.current,
                )
                dot != null -> Box(Modifier.size(8.dp).background(dot, CircleShape))
                icon != null -> Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun SmallIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(icon, contentDescription = description, tint = AL.colors.muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit) = SmallIcon(Icons.Rounded.Close, "Cerrar", onClick)

private fun formatTime(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
