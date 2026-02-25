package com.sxueck.monitor.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.sxueck.monitor.data.store.AppPreferences

enum class ThemeMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2);

    companion object {
        fun fromInt(value: Int): ThemeMode = values().find { it.value == value } ?: SYSTEM
    }
}

// 深色主题配色方案
private val DarkColorScheme = darkColorScheme(
    primary = NezhaColors.AccentPrimaryDark,
    onPrimary = Color.White,
    secondary = NezhaColors.AccentSecondaryDark,
    onSecondary = Color.White,
    tertiary = NezhaColors.AccentTertiaryDark,
    onTertiary = Color.White,
    background = NezhaColors.BackgroundDark,
    onBackground = NezhaColors.TextPrimaryDark,
    surface = NezhaColors.SurfaceDark,
    onSurface = NezhaColors.TextPrimaryDark,
    surfaceVariant = NezhaColors.SurfaceVariantDark,
    onSurfaceVariant = NezhaColors.TextSecondaryDark,
    error = NezhaColors.ErrorDark,
    onError = Color.White,
    outline = NezhaColors.BorderDark,
    primaryContainer = NezhaColors.SurfaceVariantDark,
    onPrimaryContainer = NezhaColors.TextPrimaryDark,
    secondaryContainer = NezhaColors.CardDark,
    onSecondaryContainer = NezhaColors.TextPrimaryDark
)

// 浅色主题配色方案
private val LightColorScheme = lightColorScheme(
    primary = NezhaColors.AccentPrimaryLight,
    onPrimary = Color.White,
    secondary = NezhaColors.AccentSecondaryLight,
    onSecondary = Color.White,
    tertiary = NezhaColors.AccentTertiaryLight,
    onTertiary = Color.White,
    background = NezhaColors.BackgroundLight,
    onBackground = NezhaColors.TextPrimaryLight,
    surface = NezhaColors.SurfaceLight,
    onSurface = NezhaColors.TextPrimaryLight,
    surfaceVariant = NezhaColors.SurfaceVariantLight,
    onSurfaceVariant = NezhaColors.TextSecondaryLight,
    error = NezhaColors.ErrorLight,
    onError = Color.White,
    outline = NezhaColors.BorderLight,
    primaryContainer = NezhaColors.SurfaceVariantLight,
    onPrimaryContainer = NezhaColors.TextPrimaryLight,
    secondaryContainer = NezhaColors.CardLight,
    onSecondaryContainer = NezhaColors.TextPrimaryLight
)

/**
 * Nezha 监控应用主题
 *
 * @param darkTheme 是否使用深色主题，默认跟随系统设置
 * @param dynamicColor 是否使用动态颜色（Android 12+），默认启用
 * @param content 主题内容
 */
@Composable
fun NezhaMonitorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        // Android 12+ 支持动态颜色
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // 设置状态栏颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()

            // 根据主题设置状态栏图标颜色
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NezhaTypography,
        content = content
    )
}

/**
 * Nezha 监控应用主题（支持主题模式）
 *
 * @param preferences AppPreferences 实例
 * @param dynamicColor 是否使用动态颜色（Android 12+），默认启用
 * @param content 主题内容
 */
@Composable
fun NezhaMonitorTheme(
    preferences: AppPreferences,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val themeMode by preferences.themeModeFlow.collectAsState(initial = 0)
    val themeModeEnum = ThemeMode.fromInt(themeMode)

    val isSystemDark = isSystemInDarkTheme()
    val darkTheme = when (themeModeEnum) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemDark
    }

    val context = LocalContext.current
    val colorScheme = when {
        // Android 12+ 支持动态颜色
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    // 设置状态栏颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            
            // 根据主题设置状态栏图标颜色
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NezhaTypography,
        content = content
    )
}

/**
 * 获取当前是否是深色主题的便捷函数（考虑主题模式设置）
 */
@Composable
fun isDarkTheme(preferences: AppPreferences? = null): Boolean {
    return if (preferences != null) {
        val themeMode by preferences.themeModeFlow.collectAsState(initial = 0)
        when (ThemeMode.fromInt(themeMode)) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
    } else {
        isSystemInDarkTheme()
    }
}

/**
 * 获取当前主题模式
 */
@Composable
fun getThemeMode(preferences: AppPreferences): ThemeMode {
    val themeMode by preferences.themeModeFlow.collectAsState(initial = 0)
    return ThemeMode.fromInt(themeMode)
}
