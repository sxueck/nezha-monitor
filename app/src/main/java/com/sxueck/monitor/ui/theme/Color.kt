package com.sxueck.monitor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object NezhaColors {
    // ============== 深色主题颜色 ==============
    // Primary Colors - Cyan accent for tech feel
    val AccentPrimaryDark = Color(0xFF00D9FF)
    val AccentSecondaryDark = Color(0xFF8B5CF6)
    val AccentTertiaryDark = Color(0xFF10B981)
    
    // Status Colors - Dark theme
    val SuccessDark = Color(0xFF10B981)
    val ErrorDark = Color(0xFFEF4444)
    val WarningDark = Color(0xFFF59E0B)
    val InfoDark = Color(0xFF3B82F6)
    
    // Background Colors - Dark tech theme
    val BackgroundDark = Color(0xFF0F0F1A)
    val SurfaceDark = Color(0xFF1A1A2E)
    val SurfaceVariantDark = Color(0xFF252542)
    val CardDark = Color(0xFF2A2A4A)
    
    // Text Colors - Dark theme
    val TextPrimaryDark = Color(0xFFFFFFFF)
    val TextSecondaryDark = Color(0xFFB0B0C0)
    val TextMutedDark = Color(0xFF808090)
    
    // Border - Dark theme
    val BorderDark = Color(0xFF3D3D5C)
    
    // Gradient Colors - Dark theme
    val GradientStartDark = Color(0xFF2D2D5A)
    val GradientEndDark = Color(0xFF1A1A3A)
    
    // Chart Colors - Dark theme
    val ChartLineDownload = Color(0xFF4ADE80)
    val ChartLineUpload = Color(0xFFF59E0B)
    val ChartGridDark = Color(0xFF2A2A4A)
    val ChartFillDownload = Color(0x1A4ADE80)
    val ChartFillUpload = Color(0x1AF59E0B)
    
    // Widget specific - Dark theme
    val WidgetBackgroundDark = Color(0xFF1E1E3E)
    val WidgetCardBackgroundDark = Color(0xFF2A2A4A)
    
    // ============== 浅色主题颜色 ==============
    // Primary Colors - Vibrant blue for light theme
    val AccentPrimaryLight = Color(0xFF0066FF)
    val AccentSecondaryLight = Color(0xFF7C3AED)
    val AccentTertiaryLight = Color(0xFF059669)
    
    // Status Colors - Light theme
    val SuccessLight = Color(0xFF10B981)
    val ErrorLight = Color(0xFFDC2626)
    val WarningLight = Color(0xFFD97706)
    val InfoLight = Color(0xFF2563EB)
    
    // Background Colors - Clean light theme
    val BackgroundLight = Color(0xFFF8FAFC)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceVariantLight = Color(0xFFF1F5F9)
    val CardLight = Color(0xFFFFFFFF)
    
    // Text Colors - Light theme
    val TextPrimaryLight = Color(0xFF0F172A)
    val TextSecondaryLight = Color(0xFF475569)
    val TextMutedLight = Color(0xFF94A3B8)
    
    // Border - Light theme
    val BorderLight = Color(0xFFE2E8F0)
    
    // Gradient Colors - Light theme
    val GradientStartLight = Color(0xFFEEF2FF)
    val GradientEndLight = Color(0xFFE0E7FF)
    
    // Chart Colors - Light theme
    val ChartLineDownloadLight = Color(0xFF16A34A)
    val ChartLineUploadLight = Color(0xFFEA580C)
    val ChartGridLight = Color(0xFFE2E8F0)
    val ChartFillDownloadLight = Color(0x1A16A34A)
    val ChartFillUploadLight = Color(0x1AEA580C)
    
    // Widget specific - Light theme
    val WidgetBackgroundLight = Color(0xFFFFFFFF)
    val WidgetCardBackgroundLight = Color(0xFFF8FAFC)
    
    // ============== 通用强调色 ==============
    val NeonCyan = Color(0xFF00FFFF)
    val NeonPink = Color(0xFFFF00FF)
    val NeonPurple = Color(0xFFBF00FF)
    val NeonGreen = Color(0xFF39FF14)
    val NeonOrange = Color(0xFFFF6B35)
    
    // 动态颜色函数 - 需要在Composable中使用
    @Composable
    fun accentPrimary() = if (isSystemInDarkTheme()) AccentPrimaryDark else AccentPrimaryLight
    
    @Composable
    fun surface() = if (isSystemInDarkTheme()) SurfaceDark else SurfaceLight
    
    @Composable
    fun surfaceVariant() = if (isSystemInDarkTheme()) SurfaceVariantDark else SurfaceVariantLight
    
    @Composable
    fun success() = if (isSystemInDarkTheme()) SuccessDark else SuccessLight
    
    @Composable
    fun textMuted() = if (isSystemInDarkTheme()) TextMutedDark else TextMutedLight
    
    @Composable
    fun textPrimary() = if (isSystemInDarkTheme()) TextPrimaryDark else TextPrimaryLight
    
    @Composable
    fun textSecondary() = if (isSystemInDarkTheme()) TextSecondaryDark else TextSecondaryLight
    
    // 动态获取颜色函数（在Composable中使用）
    fun getChartColors(isDark: Boolean) = if (isDark) {
        ChartColors(
            download = ChartLineDownload,
            upload = ChartLineUpload,
            grid = ChartGridDark,
            fillDownload = ChartFillDownload,
            fillUpload = ChartFillUpload
        )
    } else {
        ChartColors(
            download = ChartLineDownloadLight,
            upload = ChartLineUploadLight,
            grid = ChartGridLight,
            fillDownload = ChartFillDownloadLight,
            fillUpload = ChartFillUploadLight
        )
    }
}

data class ChartColors(
    val download: Color,
    val upload: Color,
    val grid: Color,
    val fillDownload: Color,
    val fillUpload: Color
)
