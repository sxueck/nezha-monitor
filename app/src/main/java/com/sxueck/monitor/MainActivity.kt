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
import com.sxueck.monitor.data.model.LoginRequest
import com.sxueck.monitor.data.model.MonitorConfig
import com.sxueck.monitor.data.model.ServerDisplayData
import com.sxueck.monitor.data.model.WidgetSnapshot
import com.sxueck.monitor.data.network.NezhaNetwork
import com.sxueck.monitor.data.store.AppPreferences
import com.sxueck.monitor.ui.stats.TrafficStatsActivity
import com.sxueck.monitor.ui.theme.NezhaMonitorTheme
import com.sxueck.monitor.worker.MonitorWorkScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class TestStatus(
    val success: Boolean,
    val message: String
)

private fun parseExpireTime(expire: String): Long {
    return when {
        expire.isBlank() -> 0L
        expire.toLongOrNull() != null -> {
            // Unix timestamp (seconds)
            val ts = expire.toLong()
            if (ts > 1_000_000_000_000L) ts / 1000 else ts
        }
        else -> {
            // Try parsing as ISO 8601 date
            try {
                java.time.Instant.parse(expire).epochSecond
            } catch (_: Exception) {
                0L
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferences = AppPreferences(applicationContext)

        setContent {
            NezhaMonitorTheme {
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
    val config by preferences.configFlow.collectAsStateWithLifecycle(initialValue = MonitorConfig())
    val snapshot by preferences.snapshotFlow.collectAsStateWithLifecycle(initialValue = WidgetSnapshot())
    val scope = rememberCoroutineScope()
    
    var dragOffsetX by remember { mutableStateOf(0f) }

    // 默认不显示配置卡片，只有点击设置按钮时才显示
    var showConfig by rememberSaveable { mutableStateOf(false) }
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
                    IconButton(onClick = { showConfig = !showConfig }) {
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
                                // 左滑超过200px，进入流量统计页面
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
                AnimatedVisibility(
                    visible = showConfig,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    ConfigCard(
                        config = config,
                        preferences = preferences,
                        onDismiss = { showConfig = false }
                    )
                }

                // Overall Stats Card
                OverallStatsCard(snapshot = snapshot, isRefreshing = isRefreshing)

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
private fun OverallStatsCard(snapshot: WidgetSnapshot, isRefreshing: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isRefreshing) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
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
                    
                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = snapshot.status.uppercase(),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = statusColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        label = "Total",
                        value = snapshot.tagTotal.toString(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    StatItem(
                        label = "Online",
                        value = snapshot.tagOnline.toString(),
                        color = Color(0xFF4ADE80)
                    )
                    StatItem(
                        label = "Offline",
                        value = snapshot.tagOffline.toString(),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (snapshot.status == "ok") {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
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
private fun ConfigCard(
    config: MonitorConfig,
    preferences: AppPreferences,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var baseUrl by rememberSaveable(config.baseUrl) { mutableStateOf(config.baseUrl) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var tagsInput by rememberSaveable(config.tags.joinToString(",")) { mutableStateOf(config.tags.joinToString(",")) }
    var isLoading by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var testStatus by remember { mutableStateOf<TestStatus?>(null) }
    var apiToken by rememberSaveable(config.apiToken) { mutableStateOf(config.apiToken) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Configuration",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = baseUrl,
                onValueChange = {
                    baseUrl = it
                    apiToken = ""
                    testStatus = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Panel URL") },
                placeholder = { Text("https://nezha.example.com") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    apiToken = ""
                    testStatus = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Username") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    apiToken = ""
                    testStatus = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = tagsInput,
                onValueChange = { tagsInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tags (comma separated)") },
                placeholder = { Text("prod, sg, hk") },
                minLines = 2,
                shape = RoundedCornerShape(12.dp)
            )

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            testStatus?.let { status ->
                Surface(
                    color = if (status.success) {
                        Color(0xFF4ADE80).copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (status.success) Icons.Default.CheckCircle else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (status.success) Color(0xFF4ADE80) else MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = status.message,
                            color = if (status.success) Color(0xFF22C55E) else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            isTesting = true
                            errorMessage = null
                            testStatus = null
                            try {
                                if (baseUrl.isBlank()) {
                                    errorMessage = "Please enter panel URL"
                                    return@launch
                                }
                                if (username.isBlank() || password.isBlank()) {
                                    errorMessage = "Please enter username and password"
                                    return@launch
                                }

                                val normalizedUrl = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"
                                val api = NezhaNetwork.createApi(normalizedUrl, "")
                                
                                val response = api.login(LoginRequest(username, password))
                                
                                if (response.isSuccessful && response.body()?.success == true) {
                                    val loginData = response.body()?.data
                                    val token = loginData?.token
                                    if (!token.isNullOrBlank()) {
                                        apiToken = token
                                        // Save credentials for auto-relogin
                                        preferences.saveCredentials(username, password)
                                        // Parse and save token expiry
                                        val expireAt = parseExpireTime(loginData.expire)
                                        preferences.saveTokenWithExpiry(token, expireAt)
                                        testStatus = TestStatus(
                                            success = true,
                                            message = "Connection successful! Token received."
                                        )
                                    } else {
                                        testStatus = TestStatus(
                                            success = false,
                                            message = "Login succeeded but no token received"
                                        )
                                    }
                                } else {
                                    val errorMsg = response.body()?.error ?: "HTTP ${response.code()}"
                                    testStatus = TestStatus(
                                        success = false,
                                        message = "Connection failed: $errorMsg"
                                    )
                                }
                            } catch (e: Exception) {
                                testStatus = TestStatus(
                                    success = false,
                                    message = "Connection error: ${e.message ?: "Unknown error"}"
                                )
                            } finally {
                                isTesting = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isTesting && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Test Connection")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    val tags = tagsInput.split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }

                                    if (baseUrl.isBlank()) {
                                        errorMessage = "Please enter panel URL"
                                        return@launch
                                    }

                                    if (apiToken.isBlank()) {
                                        errorMessage = "Please test connection first to obtain token"
                                        return@launch
                                    }

                                    preferences.updateConfig(
                                        baseUrl = baseUrl,
                                        apiToken = apiToken,
                                        tags = tags
                                    )
                                    // Ensure credentials are saved (in case user modified them after test)
                                    if (username.isNotBlank() && password.isNotBlank()) {
                                        preferences.saveCredentials(username, password)
                                    }

                                    MonitorWorkScheduler.scheduleNow(preferences.context)
                                    onDismiss()
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Unknown error"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading && apiToken.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            }
        }
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
