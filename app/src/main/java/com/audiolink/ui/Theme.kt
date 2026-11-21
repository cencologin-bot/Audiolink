package com.audiolink.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Colores de Audio Link. La barra toma el tono del teclado:
 * - Android 12+: colores dinámicos del sistema (los mismos que usan los teclados con Material You).
 * - Android 8 a 11: gris claro u oscuro estándar de teclado, según el modo del sistema.
 *
 * Android no permite leer el color real del teclado, por eso es una aproximación
 * que depende del teclado instalado y de su tema.
 */
data class ALColors(
    val screen: Color,
    val card: Color,     // fondo de la barra: igual que el teclado
    val tonal: Color,    // botones secundarios (como las teclas)
    val accent: Color,
    val onAccent: Color,
    val danger: Color,
    val text: Color,
    val muted: Color,
)

private val DarkAL = ALColors(
    screen = Color(0xFF121212), card = Color(0xFF202124), tonal = Color(0xFF35363A),
    accent = Color(0xFF8AB4F8), onAccent = Color(0xFF062E6F), danger = Color(0xFFF28B82),
    text = Color(0xFFE8EAED), muted = Color(0xFF9AA0A6),
)

private val LightAL = ALColors(
    screen = Color(0xFFF8F9FA), card = Color(0xFFE9ECEF), tonal = Color(0xFFFFFFFF),
    accent = Color(0xFF1A73E8), onAccent = Color(0xFFFFFFFF), danger = Color(0xFFD93025),
    text = Color(0xFF202124), muted = Color(0xFF5F6368),
)

private val LocalAL = staticCompositionLocalOf { DarkAL }

object AL {
    val colors: ALColors
        @Composable get() = LocalAL.current
}

@Composable
fun AudioLinkTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val dynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val scheme = when {
        dynamic && dark -> dynamicDarkColorScheme(context)
        dynamic -> dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    val colors = when {
        dynamic -> ALColors(
            screen = scheme.surface, card = scheme.surfaceContainer, tonal = scheme.surfaceContainerHighest,
            accent = scheme.primary, onAccent = scheme.onPrimary, danger = scheme.error,
            text = scheme.onSurface, muted = scheme.onSurfaceVariant,
        )
        dark -> DarkAL
        else -> LightAL
    }
    CompositionLocalProvider(LocalAL provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
