package com.sxueck.monitor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxueck.monitor.data.model.MonitorConfig
import com.sxueck.monitor.data.model.ServerDisplayData
import com.sxueck.monitor.data.model.WidgetSnapshot
import com.sxueck.monitor.data.store.AppPreferences
import com.sxueck.monitor.ui.settings.SettingsActivity
import com.sxueck.monitor.ui.stats.TrafficStatsActivity
import com.sxueck.monitor.ui.theme.NezhaMonitorTheme
import com.sxueck.monitor.ui.theme.ThemeMode
import com.sxueck.monitor.worker.MonitorWorkScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferences = AppPreferences(applicationContext)

        setContent {
            NezhaMonitorTheme(preferences = preferences) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        preferences = preferences,
                        onNavigateToTrafficStats = {
                            startActivity(Intent(this, TrafficStatsActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    preferences: AppPreferences,
    onNavigateToTrafficStats: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val snapshot by preferences.snapshotFlow.collectAsStateWithLifecycle(initialValue = WidgetSnapshot())
    val scope = rememberCoroutineScope()
    
    // 使用remember记住主题模式状态，确保切换立即生效
    var currentThemeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    
    // 从preferences读取主题模式
    LaunchedEffect(Unit) {
        preferences.themeModeFlow.collect { modeValue ->
            currentThemeMode = ThemeMode.fromInt(modeValue)
        }
    }
    
    var dragOffsetX by remember { mutableStateOf(0f) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var selectedServerId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(snapshot.status) {
        if (snapshot.status == "ok") {
            showSuccess = true
            delay(2000)
            showSuccess = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "NEZHA MONITOR",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    // 主题切换按钮 - 使用本地状态，立即响应
                    IconButton(
                        onClick = {
                            val nextMode = when (currentThemeMode) {
                                ThemeMode.SYSTEM -> ThemeMode.LIGHT
                                ThemeMode.LIGHT -> ThemeMode.DARK
                                ThemeMode.DARK -> ThemeMode.SYSTEM
                            }
                            currentThemeMode = nextMode
                            scope.launch {
                                preferences.saveThemeMode(nextMode.value)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = when (currentThemeMode) {
                                ThemeMode.SYSTEM -> Icons.Outlined.DarkMode
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                            },
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 刷新按钮
                    IconButton(
                        onClick = {
                            scope.launch {
                                isRefreshing = true
                                MonitorWorkScheduler.scheduleNow(preferences.context)
                                delay(1000)
                                isRefreshing = false
                            }
                        },
                        enabled = !isRefreshing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier
                                .size(24.dp)
                                .alpha(if (isRefreshing) 0.7f else 1f),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 设置按钮 - 跳转到设置页面
                    IconButton(onClick = { 
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragOffsetX < -200f) {
                                onNavigateToTrafficStats()
                            }
                            dragOffsetX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffsetX += dragAmount
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Overall Stats Card - 使用新的平面设计
                OverallStatsCardFlat(snapshot = snapshot, isRefreshing = isRefreshing)

                // Server List
                Text(
                    text = "Servers (${snapshot.servers.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )

                if (snapshot.servers.isEmpty()) {
                    EmptyServerState(message = snapshot.message)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = snapshot.servers,
                            key = { it.id }
                        ) { server ->
                            ServerCard(
                                server = server,
                                isExpanded = selectedServerId == server.id,
                                onClick = {
                                    selectedServerId = if (selectedServerId == server.id) null else server.id
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showSuccess,
                modifier = Modifier.align(Alignment.TopCenter),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                SuccessToast()
            }
        }
    }
}

@Composable
private fun OverallStatsCardFlat(snapshot: WidgetSnapshot, isRefreshing: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isRefreshing) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    // 无边框的平面设计 - 使用Surface代替Card，移除所有边框和阴影
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column {
                // Header with status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Dashboard",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = snapshot.lastActiveText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    val statusColor = when (snapshot.status) {
                        "ok" -> Color(0xFF4ADE80)
                        "error" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outline
                    }
                    
                    // 平面状态标签 - 无描边
                    Surface(
                        color = statusColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = snapshot.status.uppercase(),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = statusColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 平面统计卡片 - 无边框设计
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total 卡片 - 平面设计
                    FlatStatCard(
                        label = "Total",
                        value = snapshot.tagTotal.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Online 卡片 - 平面设计
                    FlatStatCard(
                        label = "Online",
                        value = snapshot.tagOnline.toString(),
                        color = Color(0xFF22C55E),
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Offline 卡片 - 平面设计
                    FlatStatCard(
                        label = "Offline",
                        value = snapshot.tagOffline.toString(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (snapshot.status == "ok") {
                    Spacer(modifier = Modifier.height(20.dp))
                    // 细线分隔
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        CompactMetric(label = "CPU", value = snapshot.cpuText)
                        CompactMetric(label = "Memory", value = snapshot.memoryText)
                    }
                }
            }
        }
    }
}

@Composable
private fun FlatStatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ServerCard(
    server: ServerDisplayData,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isExpanded) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    val cardColor = if (server.isOffline) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isExpanded) 8.dp else 2.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 6.dp,
            focusedElevation = 6.dp
        ),
        border = if (server.isOffline) {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
        } else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Status indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (server.isOffline) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    Color(0xFF4ADE80),
                                shape = RoundedCornerShape(6.dp)
                            )
                    )
                    
                    Column {
                        Text(
                            text = server.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (server.platform.isNotEmpty()) {
                            val uptimeText = if (!server.isOffline && server.uptime.isNotEmpty()) " · up ${server.uptime}" else ""
                            Text(
                                text = "${server.platform} ${server.platformVersion} · ${server.arch}${uptimeText}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Compact metrics (always visible)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CompactProgressBar(
                    label = "CPU",
                    percentage = server.cpuPercent.toFloat(),
                    color = MaterialTheme.colorScheme.primary
                )
                CompactProgressBar(
                    label = "MEM",
                    percentage = server.memoryPercent.toFloat(),
                    color = Color(0xFF8B5CF6)
                )
                CompactProgressBar(
                    label = "DISK",
                    percentage = server.diskPercent.toFloat(),
                    color = Color(0xFFF59E0B)
                )
            }

            // Expanded details
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Detailed metrics
                    DetailRow("CPU Load", "${server.load1}/${server.load5}/${server.load15}")
                    DetailRow("Memory", "${server.memoryUsed} / ${server.memoryTotal} (${"%.1f".format(server.memoryPercent)}%)")
                    DetailRow("Disk", "${server.diskUsed} / ${server.diskTotal} (${"%.1f".format(server.diskPercent)}%)")
                    DetailRow("Network Speed", "↓${server.rxSpeed} ↑${server.txSpeed}")
                    DetailRow("Traffic", "↓${server.netInTransfer} ↑${server.netOutTransfer}")
                    if (server.virtualization.isNotEmpty()) {
                        DetailRow("Virtualization", server.virtualization)
                    }
                    if (server.uptime.isNotEmpty()) {
                        DetailRow("Uptime", server.uptime)
                    }
                    DetailRow("Last Active", server.lastActiveText)
                }
            }
        }
    }
}

@Composable
private fun CompactProgressBar(label: String, percentage: Float, color: Color) {
    val animatedProgress by animateFloatAsState(
        targetValue = percentage / 100f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Column(modifier = Modifier.width(80.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${percentage.toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CompactMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EmptyServerState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SuccessToast() {
    Surface(
        color = Color(0xFF4ADE80),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 8.dp,
        modifier = Modifier.padding(top = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.White
            )
            Text(
                text = "Data refreshed successfully",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}
