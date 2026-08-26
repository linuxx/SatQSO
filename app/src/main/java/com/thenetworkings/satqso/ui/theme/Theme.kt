package com.thenetworkings.satqso.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = SpaceBlack,
    secondary = CyanSecondary,
    onSecondary = SpaceBlack,
    tertiary = SignalGreen,
    background = SpaceBlack,
    onBackground = TextPrimary,
    surface = SpaceBlack,
    onSurface = TextPrimary,
    surfaceContainer = SpaceSurface,
    surfaceContainerHigh = SpaceSurfaceHigh,
    onSurfaceVariant = TextSecondary,
    outline = SpaceBorder,
)

private val LightColorScheme = lightColorScheme(
    primary = CyanPrimary,
    onPrimary = SpaceBlack,
    secondary = OrbitBlue,
    onSecondary = SpaceBlack,
    tertiary = SignalGreen,
    background = SpaceBlack,
    onBackground = TextPrimary,
    surface = SpaceBlack,
    onSurface = TextPrimary,
    surfaceContainer = SpaceSurface,
    surfaceContainerHigh = SpaceSurfaceHigh,
    onSurfaceVariant = TextSecondary,
    outline = SpaceBorder,
)

@Composable
fun SatQSOTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme || !dynamicColor) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
