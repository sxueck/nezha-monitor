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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sxueck.monitor.data.traffic.*
import com.sxueck.monitor.ui.theme.NezhaMonitorTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TrafficStatsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val trafficStore = TrafficStore(applicationContext)
        
        setContent {
            NezhaMonitorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TrafficStatsScreen(
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
    trafficStore: TrafficStore,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
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
            TopAppBar(
                title = {
                    Text(
                        text = "TRAFFIC STATISTICS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
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
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period selector
            PeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { selectedPeriod = it }
            )
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (aggregatedStats != null) {
                // Overall stats card
                OverallTrafficCard(stats = aggregatedStats!!)
                
                // Server list
                Text(
                    text = "Servers (${aggregatedStats!!.serverCount})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = aggregatedStats!!.serverStats,
                        key = { it.serverId }
                    ) { serverStats ->
                        ServerTrafficCard(stats = serverStats)
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
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

@Composable
private fun PeriodSelector(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PeriodButton(
            text = "Hour",
            period = "hour",
            isSelected = selectedPeriod == "hour",
            onClick = { onPeriodSelected("hour") },
            modifier = Modifier.weight(1f)
        )
        PeriodButton(
            text = "Day",
            period = "day",
            isSelected = selectedPeriod == "day",
            onClick = { onPeriodSelected("day") },
            modifier = Modifier.weight(1f)
        )
        PeriodButton(
            text = "Week",
            period = "week",
            isSelected = selectedPeriod == "week",
            onClick = { onPeriodSelected("week") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PeriodButton(
    text: String,
    period: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) 
                MaterialTheme.colorScheme.onPrimary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

@Composable
private fun OverallTrafficCard(stats: AggregatedTrafficStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Text(
                    text = TrafficPeriodHelper.formatPeriodDescription(stats.period),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TrafficStatItem(
                        label = "Total",
                        value = stats.formatTotalNet(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    TrafficStatItem(
                        label = "Download",
                        value = stats.formatTotalNetIn(),
                        color = Color(0xFF4ADE80)
                    )
                    TrafficStatItem(
                        label = "Upload",
                        value = stats.formatTotalNetOut(),
                        color = Color(0xFFF59E0B)
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerTrafficCard(stats: TrafficStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stats.serverName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TrafficDetailRow("Total", stats.formatNetTotal(), MaterialTheme.colorScheme.primary)
            TrafficDetailRow("Download", stats.formatNetIn(), Color(0xFF4ADE80))
            TrafficDetailRow("Upload", stats.formatNetOut(), Color(0xFFF59E0B))
        }
    }
}

@Composable
private fun TrafficStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
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
private fun TrafficDetailRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = color
        )
    }
}
