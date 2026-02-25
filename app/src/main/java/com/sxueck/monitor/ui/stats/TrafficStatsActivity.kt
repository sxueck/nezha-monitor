package com.sxueck.monitor.ui.stats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sxueck.monitor.data.traffic.*
import com.sxueck.monitor.ui.components.charts.*
import com.sxueck.monitor.data.store.AppPreferences
import com.sxueck.monitor.ui.theme.NezhaMonitorTheme
import com.sxueck.monitor.ui.theme.isDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow

class TrafficStatsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val trafficStore = TrafficStore(applicationContext)
        val preferences = AppPreferences(applicationContext)
        
        setContent {
            NezhaMonitorTheme(preferences = preferences) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TrafficStatsScreen(
                        preferences = preferences,
                        trafficStore = trafficStore,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrafficStatsScreen(
    preferences: AppPreferences,
    trafficStore: TrafficStore,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isDark = isDarkTheme(preferences)
    var selectedPeriod by remember { mutableStateOf("hour") }
    var isLoading by remember { mutableStateOf(false) }
    var aggregatedStats by remember { mutableStateOf<AggregatedTrafficStats?>(null) }
    
    // 加载数据
    fun loadStats() {
        scope.launch {
            isLoading = true
            try {
                val (startTime, endTime) = when (selectedPeriod) {
                    "hour" -> TrafficPeriodHelper.getLastHourRange()
                    "day" -> TrafficPeriodHelper.getLastDayRange()
                    "week" -> TrafficPeriodHelper.getLastWeekRange()
                    else -> TrafficPeriodHelper.getLastHourRange()
                }
                
                aggregatedStats = trafficStore.calculateAggregatedStats(
                    period = selectedPeriod,
                    startTime = startTime,
                    endTime = endTime
                )
            } finally {
                isLoading = false
            }
        }
    }
    
    // 初始加载
    LaunchedEffect(selectedPeriod) {
        loadStats()
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NETWORK TRAFFIC",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp
                            )
                        )
                        Text(
                            text = "Monitor",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 4.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { loadStats() },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier
                                .size(24.dp)
                                .alpha(if (isLoading) 0.7f else 1f),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 背景装饰
            if (isDark) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            // 顶部渐变光晕
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF00D9FF).copy(alpha = 0.08f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width / 2, 0f),
                                    radius = size.width * 0.8f
                                ),
                                radius = size.width * 0.8f,
                                center = Offset(size.width / 2, 0f)
                            )
                        }
                )
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                }
            } else if (aggregatedStats != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 时间段选择器
                    item {
                        PeriodSelector(
                            preferences = preferences,
                            selectedPeriod = selectedPeriod,
                            onPeriodSelected = { selectedPeriod = it }
                        )
                    }
                    
                    // 总体统计卡片
                    item {
                        OverallTrafficCard(
                            stats = aggregatedStats!!,
                            isDark = isDark
                        )
                    }
                    
                    // 流量趋势图
                    item {
                        val chartData = generateChartData(aggregatedStats!!)
                        if (chartData.isNotEmpty()) {
                            TrafficChart(
                                dataPoints = chartData,
                                modifier = Modifier.fillMaxWidth(),
                                showGrid = true,
                                animate = true
                            )
                        }
                    }
                    
                    // 服务器对比图
                    if (aggregatedStats!!.serverStats.size > 1) {
                        item {
                            ServerTrafficBarChart(
                                stats = aggregatedStats!!.serverStats,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    

                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No traffic data available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    preferences: AppPreferences,
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit
) {
    val isDark = isDarkTheme(preferences)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PeriodButton(
            text = "1H",
            description = "Last Hour",
            period = "hour",
            isSelected = selectedPeriod == "hour",
            onClick = { onPeriodSelected("hour") },
            modifier = Modifier.weight(1f),
            isDark = isDark
        )
        PeriodButton(
            text = "24H",
            description = "Last Day",
            period = "day",
            isSelected = selectedPeriod == "day",
            onClick = { onPeriodSelected("day") },
            modifier = Modifier.weight(1f),
            isDark = isDark
        )
        PeriodButton(
            text = "7D",
            description = "Last Week",
            period = "week",
            isSelected = selectedPeriod == "week",
            onClick = { onPeriodSelected("week") },
            modifier = Modifier.weight(1f),
            isDark = isDark
        )
    }
}

@Composable
private fun PeriodButton(
    text: String,
    description: String,
    period: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "period_button_scale"
    )
    
    Card(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun OverallTrafficCard(
    stats: AggregatedTrafficStats,
    isDark: Boolean
) {
    val animatedTotal by animateFloatAsState(
        targetValue = stats.totalNet.toFloat(),
        animationSpec = tween(durationMillis = 800),
        label = "total_animation"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Column {
                // 标题行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Traffic",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = TrafficPeriodHelper.formatPeriodDescription(stats.period),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // 服务器数量指示器
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stats.serverCount} Servers",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 总流量大数字
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val formattedTotal = formatBytes(stats.totalNet)
                        val parts = formattedTotal.split(" ")
                        
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = parts[0],
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 48.sp,
                                    brush = if (isDark) {
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF00D9FF),
                                                Color(0xFF8B5CF6)
                                            )
                                        )
                                    } else null
                                ),
                                color = if (isDark) Color.Unspecified else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = parts.getOrElse(1) { "" },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 下载和上传统计
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatisticItem(
                        icon = Icons.Default.TrendingDown,
                        label = "Download",
                        value = formatBytes(stats.totalNetIn),
                        color = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
                    )
                    
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )
                    
                    StatisticItem(
                        icon = Icons.Default.TrendingUp,
                        label = "Upload",
                        value = formatBytes(stats.totalNetOut),
                        color = if (isDark) Color(0xFFF59E0B) else Color(0xFFEA580C)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ServerTrafficCard(
    stats: TrafficStats,
    isDark: Boolean,
    index: Int
) {
    val delayMillis = index * 50
    
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }
    
    val downloadPercent = if (stats.netTotalDelta > 0) {
        stats.netInDelta.toFloat() / stats.netTotalDelta.toFloat()
    } else 0.5f
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) + 
                slideInVertically(
                    animationSpec = tween(300),
                    initialOffsetY = { it / 2 }
                )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 服务器名称和总流量
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isDark) Color(0xFF2A2A4A)
                                    else Color(0xFFF1F5F9)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = stats.serverName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Text(
                        text = stats.formatNetTotal(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 进度条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(downloadPercent.coerceIn(0.01f, 0.99f))
                                .background(
                                    if (isDark) Color(0xFF4ADE80)
                                    else Color(0xFF16A34A),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight((1 - downloadPercent).coerceIn(0.01f, 0.99f))
                                .background(
                                    if (isDark) Color(0xFFF59E0B)
                                    else Color(0xFFEA580C),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 下载和上传详情
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TrafficDetailItem(
                        label = "Download",
                        value = stats.formatNetIn(),
                        color = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
                    )
                    TrafficDetailItem(
                        label = "Upload",
                        value = stats.formatNetOut(),
                        color = if (isDark) Color(0xFFF59E0B) else Color(0xFFEA580C)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrafficDetailItem(
    label: String,
    value: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = color
            )
        }
    }
}

/**
 * 生成图表数据
 */
private fun generateChartData(stats: AggregatedTrafficStats): List<TrafficDataPoint> {
    // 这里简化处理，实际应该从 TrafficStore 获取历史数据
    // 模拟生成一些数据点用于展示
    return when (stats.period) {
        "hour" -> generateMockDataPoints(12, stats.totalNetIn / 12, stats.totalNetOut / 12)
        "day" -> generateMockDataPoints(24, stats.totalNetIn / 24, stats.totalNetOut / 24)
        "week" -> generateMockDataPoints(7, stats.totalNetIn / 7, stats.totalNetOut / 7)
        else -> emptyList()
    }
}

private fun generateMockDataPoints(
    count: Int,
    avgDownload: Long,
    avgUpload: Long
): List<TrafficDataPoint> {
    return List(count) { index ->
        val variation = (0.5f + Math.random().toFloat())
        TrafficDataPoint(
            timestamp = System.currentTimeMillis() / 1000 - (count - index) * 3600,
            download = (avgDownload * variation / 1024f / 1024f).coerceAtLeast(0.1f),  // MB
            upload = (avgUpload * variation / 1024f / 1024f).coerceAtLeast(0.1f),      // MB
            label = "$index"
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    val digitGroups = (kotlin.math.log10(bytes.toDouble()) / kotlin.math.log10(1024.0)).toInt()
    val adjustedDigitGroups = digitGroups.coerceIn(0, units.size - 1)
    return String.format("%.2f %s", bytes / Math.pow(1024.0, adjustedDigitGroups.toDouble()), units[adjustedDigitGroups])
}
