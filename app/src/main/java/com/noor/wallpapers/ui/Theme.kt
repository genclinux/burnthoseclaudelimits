package com.noor.wallpapers.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.noor.wallpapers.art.Colors
import com.noor.wallpapers.art.Palette

val Gold = Color(0xFFD9B54A)
val GoldLight = Color(0xFFF2D27A)
val Emerald = Color(0xFF0D4633)
val EmeraldDeep = Color(0xFF02140E)
val EmeraldSurface = Color(0xFF072A20)

private val Scheme = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF2A1F00),
    primaryContainer = Color(0xFF3D3000),
    onPrimaryContainer = GoldLight,
    secondary = Color(0xFF7FCBA8),
    onSecondary = Color(0xFF00382A),
    secondaryContainer = Emerald,
    onSecondaryContainer = Color(0xFFCDEEDD),
    background = EmeraldDeep,
    onBackground = Color(0xFFE9E3D3),
    surface = EmeraldDeep,
    onSurface = Color(0xFFE9E3D3),
    surfaceVariant = EmeraldSurface,
    onSurfaceVariant = Color(0xFFBFC9C2),
    surfaceContainer = EmeraldSurface,
    surfaceContainerHigh = Color(0xFF0B3528),
    surfaceContainerLow = Color(0xFF041D16),
    outline = Color(0xFF5E7A6E),
)

/**
 * The app's colours: the original emerald and gold, or any palette (hers, a
 * built-in one, or one she made), always kept dark so the artwork stays the star.
 */
fun schemeFor(p: Palette?): ColorScheme {
    if (p == null || p.id == "emerald") return Scheme
    fun c(argb: Int) = Color(argb)
    val base = Colors.darken(p.bgBottom, if (Colors.luminance(p.bgBottom) > 0.2) 0.7f else 0.25f)
    val surface = Colors.mix(base, p.bgTop, 0.22f)
    val high = Colors.mix(base, p.bgTop, 0.38f)
    val low = Colors.mix(base, p.bgTop, 0.1f)
    val text = Colors.mix(Colors.WHITE, p.line, 0.12f)
    return darkColorScheme(
        primary = c(p.line),
        onPrimary = c(Colors.darken(p.line, 0.82f)),
        primaryContainer = c(Colors.mix(base, p.line, 0.3f)),
        onPrimaryContainer = c(Colors.lighten(p.line, 0.4f)),
        secondary = c(Colors.lighten(p.accentA, 0.25f)),
        onSecondary = c(Colors.darken(p.accentA, 0.8f)),
        secondaryContainer = c(Colors.mix(base, p.accentA, 0.45f)),
        onSecondaryContainer = c(Colors.lighten(p.accentA, 0.7f)),
        background = c(base),
        onBackground = c(text),
        surface = c(base),
        onSurface = c(text),
        surfaceVariant = c(surface),
        onSurfaceVariant = c(Colors.mix(text, base, 0.25f)),
        surfaceContainer = c(surface),
        surfaceContainerHigh = c(high),
        surfaceContainerLow = c(low),
        outline = c(Colors.mix(p.line, base, 0.55f)),
    )
}


@Composable
fun NoorTheme(palette: Palette? = null, content: @Composable () -> Unit) {
    val scheme = remember(palette) { schemeFor(palette) }
    MaterialTheme(colorScheme = scheme, typography = NoorType, shapes = NoorShapes, content = content)
}
