package com.sxueck.monitor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NezhaColors.AccentPrimary,
    onPrimary = Color.White,
    secondary = NezhaColors.AccentSecondary,
    onSecondary = Color.White,
    tertiary = NezhaColors.AccentTertiary,
    onTertiary = Color.White,
    background = NezhaColors.Background,
    onBackground = NezhaColors.TextPrimary,
    surface = NezhaColors.Surface,
    onSurface = NezhaColors.TextPrimary,
    surfaceVariant = NezhaColors.SurfaceVariant,
    onSurfaceVariant = NezhaColors.TextSecondary,
    error = NezhaColors.Error,
    onError = Color.White,
    outline = NezhaColors.Border
)

@Composable
fun NezhaMonitorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NezhaTypography,
        content = content
    )
}
