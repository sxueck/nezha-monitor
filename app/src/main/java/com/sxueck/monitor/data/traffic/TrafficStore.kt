package com.sxueck.monitor.data.traffic

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.trafficDataStore by preferencesDataStore(name = "traffic_history")

/**
 * 流量历史数据存储
 */
class TrafficStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * 保存服务器流量快照
     */
    suspend fun saveSnapshot(snapshot: TrafficSnapshot) {
        val key = stringPreferencesKey("traffic_${snapshot.serverId}")
        context.trafficDataStore.edit { prefs ->
            val historyJson = prefs[key]
            val history = if (historyJson.isNullOrBlank()) {
                TrafficHistory(serverId = snapshot.serverId)
            } else {
                runCatching {
                    json.decodeFromString<TrafficHistory>(historyJson)
                }.getOrElse {
                    TrafficHistory(serverId = snapshot.serverId)
                }
            }
            
            history.addSnapshot(snapshot)
            prefs[key] = json.encodeToString(history)
        }
    }
    
    /**
     * 批量保存多个服务器的快照
     */
    suspend fun saveSnapshots(snapshots: List<TrafficSnapshot>) {
        snapshots.forEach { saveSnapshot(it) }
    }
    
    /**
     * 获取服务器的流量历史
     */
    suspend fun getHistory(serverId: Long): TrafficHistory {
        val key = stringPreferencesKey("traffic_$serverId")
        val prefs = context.trafficDataStore.data.first()
        val historyJson = prefs[key]
        
        return if (historyJson.isNullOrBlank()) {
            TrafficHistory(serverId = serverId)
        } else {
            runCatching {
                json.decodeFromString<TrafficHistory>(historyJson)
            }.getOrElse {
                TrafficHistory(serverId = serverId)
            }
        }
    }
    
    /**
     * 获取所有服务器的流量历史
     */
    suspend fun getAllHistories(): List<TrafficHistory> {
        val prefs = context.trafficDataStore.data.first()
        return prefs.asMap().entries
            .filter { it.key.name.startsWith("traffic_") }
            .mapNotNull { entry ->
                val value = entry.value as? String
                if (value.isNullOrBlank()) null
                else runCatching {
                    json.decodeFromString<TrafficHistory>(value)
                }.getOrNull()
            }
    }
    
    /**
     * 计算指定时间段的流量统计
     */
    suspend fun calculateStats(
        serverId: Long,
        period: String,
        startTime: Long,
        endTime: Long
    ): TrafficStats? {
        val history = getHistory(serverId)
        val snapshots = history.getSnapshotsForPeriod(startTime, endTime)
        return calculateTrafficStats(snapshots, period, startTime, endTime)
    }
    
    /**
     * 计算所有服务器的聚合统计
     */
    suspend fun calculateAggregatedStats(
        period: String,
        startTime: Long,
        endTime: Long
    ): AggregatedTrafficStats {
        val histories = getAllHistories()
        val serverStats = histories.mapNotNull { history ->
            val snapshots = history.getSnapshotsForPeriod(startTime, endTime)
            calculateTrafficStats(snapshots, period, startTime, endTime)
        }
        
        val totalNetIn = serverStats.sumOf { it.netInDelta }
        val totalNetOut = serverStats.sumOf { it.netOutDelta }
        
        return AggregatedTrafficStats(
            period = period,
            startTime = startTime,
            endTime = endTime,
            totalNetIn = totalNetIn,
            totalNetOut = totalNetOut,
            totalNet = totalNetIn + totalNetOut,
            serverCount = serverStats.size,
            serverStats = serverStats.sortedByDescending { it.netTotalDelta }
        )
    }
    
    /**
     * 清理过期数据
     */
    suspend fun cleanupOldData() {
        val cutoffTime = System.currentTimeMillis() / 1000 - 7 * 24 * 3600
        context.trafficDataStore.edit { prefs ->
            val keys = prefs.asMap().keys.filter { it.name.startsWith("traffic_") }
            keys.forEach { key ->
                val historyJson = prefs[key] as? String
                if (!historyJson.isNullOrBlank()) {
                    val history = runCatching {
                        json.decodeFromString<TrafficHistory>(historyJson)
                    }.getOrNull()
                    
                    if (history != null) {
                        history.snapshots.removeAll { it.timestamp < cutoffTime }
                        if (history.snapshots.isEmpty()) {
                            prefs.remove(key)
                        } else {
                            prefs[key] = json.encodeToString(history)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 时间段计算辅助函数
 */
object TrafficPeriodHelper {
    /**
     * 获取最近一小时的时间范围
     */
    fun getLastHourRange(): Pair<Long, Long> {
        val now = System.currentTimeMillis() / 1000
        return Pair(now - 3600, now)
    }
    
    /**
     * 获取最近一天的时间范围
     */
    fun getLastDayRange(): Pair<Long, Long> {
        val now = System.currentTimeMillis() / 1000
        return Pair(now - 86400, now)
    }
    
    /**
     * 获取最近一周的时间范围
     */
    fun getLastWeekRange(): Pair<Long, Long> {
        val now = System.currentTimeMillis() / 1000
        return Pair(now - 7 * 86400, now)
    }
    
    /**
     * 格式化时间段描述
     */
    fun formatPeriodDescription(period: String): String {
        return when (period) {
            "hour" -> "Last Hour"
            "day" -> "Last 24 Hours"
            "week" -> "Last 7 Days"
            else -> period
        }
    }
}
