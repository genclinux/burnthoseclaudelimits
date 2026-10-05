package com.noor.wallpapers.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

@Composable
fun NoorTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Typography(), content = content)
}
