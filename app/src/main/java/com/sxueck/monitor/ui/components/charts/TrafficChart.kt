package com.sxueck.monitor.ui.components.charts

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sxueck.monitor.data.traffic.TrafficStats
import com.sxueck.monitor.ui.theme.ChartColors
import com.sxueck.monitor.ui.theme.NezhaColors
import com.sxueck.monitor.ui.theme.isDarkTheme
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 流量趋势图表数据点
 */
data class TrafficDataPoint(
    val timestamp: Long,
    val download: Float,  // MB
    val upload: Float,    // MB
    val label: String
)

/**
 * 流量趋势图表组件
 * 
 * 大胆风格设计，包含：
 * - 渐变填充区域
 * - 发光效果线条
 * - 网格背景
 * - 动画效果
 * - 悬停提示
 */
@Composable
fun TrafficChart(
    dataPoints: List<TrafficDataPoint>,
    modifier: Modifier = Modifier,
    showGrid: Boolean = true,
    animate: Boolean = true
) {
    val isDark = isDarkTheme()
    val chartColors = remember(isDark) { NezhaColors.getChartColors(isDark) }
    
    // 动画进度
    var animationProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animate) 1f else 1f,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutQuart),
        label = "chart_animation"
    )
    
    LaunchedEffect(dataPoints) {
        animationProgress = 0f
        delay(100)
        animationProgress = 1f
    }
    
    // 计算最大值用于Y轴缩放
    val maxValue = remember(dataPoints) {
        if (dataPoints.isEmpty()) 1f
        else {
            val maxDownload = dataPoints.maxOfOrNull { it.download } ?: 0f
            val maxUpload = dataPoints.maxOfOrNull { it.upload } ?: 0f
            max(maxDownload, maxUpload) * 1.2f
        }
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Column {
            // 图例
            ChartLegend(
                downloadColor = chartColors.download,
                uploadColor = chartColors.upload,
                isDark = isDark
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 图表主体
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (dataPoints.size >= 2) {
                    ChartCanvas(
                        dataPoints = dataPoints,
                        maxValue = maxValue,
                        chartColors = chartColors,
                        showGrid = showGrid,
                        animationProgress = animatedProgress,
                        isDark = isDark
                    )
                } else {
                    // 数据不足时显示提示
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Insufficient data",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegend(
    downloadColor: Color,
    uploadColor: Color,
    isDark: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Download legend
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(downloadColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Download",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Upload legend
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(uploadColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Upload",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChartCanvas(
    dataPoints: List<TrafficDataPoint>,
    maxValue: Float,
    chartColors: ChartColors,
    showGrid: Boolean,
    animationProgress: Float,
    isDark: Boolean
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val paddingLeft = 40f
        val paddingBottom = 24f
        val paddingTop = 16f
        val paddingRight = 16f
        
        val chartWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingTop - paddingBottom
        
        val xStep = chartWidth / (dataPoints.size - 1).coerceAtLeast(1)
        
        // 绘制网格
        if (showGrid) {
            drawGrid(
                chartWidth = chartWidth,
                chartHeight = chartHeight,
                paddingLeft = paddingLeft,
                paddingTop = paddingTop,
                gridColor = chartColors.grid,
                maxValue = maxValue,
                textMeasurer = textMeasurer,
                textStyle = textStyle
            )
        }
        
        // 计算点位置
        val downloadPoints = dataPoints.mapIndexed { index, point ->
            Offset(
                x = paddingLeft + index * xStep,
                y = paddingTop + chartHeight - (point.download / maxValue * chartHeight * animationProgress)
            )
        }
        
        val uploadPoints = dataPoints.mapIndexed { index, point ->
            Offset(
                x = paddingLeft + index * xStep,
                y = paddingTop + chartHeight - (point.upload / maxValue * chartHeight * animationProgress)
            )
        }
        
        // 绘制渐变填充区域
        drawAreaFill(
            points = downloadPoints,
            baseY = paddingTop + chartHeight,
            color = chartColors.fillDownload
        )
        
        drawAreaFill(
            points = uploadPoints,
            baseY = paddingTop + chartHeight,
            color = chartColors.fillUpload
        )
        
        // 绘制线条
        drawLinePath(
            points = downloadPoints,
            color = chartColors.download,
            strokeWidth = 3f,
            isDark = isDark
        )
        
        drawLinePath(
            points = uploadPoints,
            color = chartColors.upload,
            strokeWidth = 3f,
            isDark = isDark
        )
        
        // 绘制数据点
        downloadPoints.forEach { point ->
            drawCircle(
                color = chartColors.download,
                radius = 4f,
                center = point
            )
            drawCircle(
                color = Color.White,
                radius = 2f,
                center = point
            )
        }
        
        uploadPoints.forEach { point ->
            drawCircle(
                color = chartColors.upload,
                radius = 4f,
                center = point
            )
            drawCircle(
                color = Color.White,
                radius = 2f,
                center = point
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid(
    chartWidth: Float,
    chartHeight: Float,
    paddingLeft: Float,
    paddingTop: Float,
    gridColor: Color,
    maxValue: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textStyle: TextStyle
) {
    // 水平网格线
    val gridLines = 5
    for (i in 0..gridLines) {
        val y = paddingTop + chartHeight * i / gridLines
        val value = maxValue * (1 - i.toFloat() / gridLines)
        
        drawLine(
            color = gridColor,
            start = Offset(paddingLeft, y),
            end = Offset(paddingLeft + chartWidth, y),
            strokeWidth = 1f
        )
        
        // Y轴标签
        val label = String.format("%.1f", value)
        val textLayoutResult = textMeasurer.measure(label, textStyle)
        drawText(
            textMeasurer = textMeasurer,
            text = label,
            style = textStyle,
            topLeft = Offset(
                paddingLeft - textLayoutResult.size.width - 8f,
                y - textLayoutResult.size.height / 2
            )
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAreaFill(
    points: List<Offset>,
    baseY: Float,
    color: Color
) {
    if (points.size < 2) return
    
    val path = Path().apply {
        moveTo(points.first().x, baseY)
        lineTo(points.first().x, points.first().y)
        
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
        
        lineTo(points.last().x, baseY)
        close()
    }
    
    drawPath(
        path = path,
        color = color
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLinePath(
    points: List<Offset>,
    color: Color,
    strokeWidth: Float,
    isDark: Boolean
) {
    if (points.size < 2) return
    
    // 如果是深色主题，添加发光效果
    if (isDark) {
        // 绘制发光效果的线条（使用较宽、透明度较低的线条）
        val glowPath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }
        
        drawPath(
            path = glowPath,
            color = color.copy(alpha = 0.3f),
            style = Stroke(width = strokeWidth * 3)
        )
    }
    
    // 主线条
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
    }
    
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth)
    )
}

/**
 * 服务器流量条形图
 * 
 * 用于展示各服务器流量对比
 */
@Composable
fun ServerTrafficBarChart(
    stats: List<TrafficStats>,
    modifier: Modifier = Modifier
) {
    val isDark = isDarkTheme()
    val chartColors = remember(isDark) { NezhaColors.getChartColors(isDark) }
    
    if (stats.isEmpty()) return
    
    // 找出最大值
    val maxValue = remember(stats) {
        stats.maxOfOrNull { maxOf(it.netInDelta, it.netOutDelta).toFloat() } ?: 1f
    }
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = "Server Traffic Comparison",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        stats.take(5).forEach { stat ->
            ServerBarItem(
                serverName = stat.serverName,
                download = stat.netInDelta.toFloat(),
                upload = stat.netOutDelta.toFloat(),
                maxValue = maxValue,
                downloadColor = chartColors.download,
                uploadColor = chartColors.upload,
                isDark = isDark
            )
            
            if (stat != stats.take(5).last()) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ServerBarItem(
    serverName: String,
    download: Float,
    upload: Float,
    maxValue: Float,
    downloadColor: Color,
    uploadColor: Color,
    isDark: Boolean
) {
    Column {
        Text(
            text = serverName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 下载条
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.weight(1f)
            ) {
                // 背景条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(
                            if (isDark) Color(0xFF2A2A4A) else Color(0xFFE2E8F0),
                            RoundedCornerShape(3.dp)
                        )
                )
                
                // 下载数据条
                val downloadProgress = (download / maxValue).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(downloadProgress)
                        .height(6.dp)
                        .background(downloadColor, RoundedCornerShape(3.dp))
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = formatBytesShort(download.toLong()),
                style = MaterialTheme.typography.labelSmall,
                color = downloadColor,
                modifier = Modifier.width(50.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 上传条
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.weight(1f)
            ) {
                // 背景条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(
                            if (isDark) Color(0xFF2A2A4A) else Color(0xFFE2E8F0),
                            RoundedCornerShape(3.dp)
                        )
                )
                
                // 上传数据条
                val uploadProgress = (upload / maxValue).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(uploadProgress)
                        .height(6.dp)
                        .background(uploadColor, RoundedCornerShape(3.dp))
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = formatBytesShort(upload.toLong()),
                style = MaterialTheme.typography.labelSmall,
                color = uploadColor,
                modifier = Modifier.width(50.dp)
            )
        }
    }
}

/**
 * 简化的字节格式化
 */
private fun formatBytesShort(bytes: Long): String {
    if (bytes <= 0) return "0B"
    val units = arrayOf("B", "K", "M", "G", "T")
    val digitGroups = (kotlin.math.log10(bytes.toDouble()) / kotlin.math.log10(1024.0)).toInt()
    val adjustedDigitGroups = digitGroups.coerceIn(0, units.size - 1)
    return String.format("%.1f%s", bytes / Math.pow(1024.0, adjustedDigitGroups.toDouble()), units[adjustedDigitGroups])
}

// 缓动函数
private val EaseOutQuart = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
