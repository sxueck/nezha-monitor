package com.sxueck.monitor.data.traffic

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 单个时间点的流量数据快照
 */
@Serializable
data class TrafficSnapshot(
    val timestamp: Long,           // 时间戳（秒）
    val serverId: Long,            // 服务器ID
    val serverName: String,      // 服务器名称
    val netInTransfer: Long,     // 累计下行流量（字节）
    val netOutTransfer: Long     // 累计上行流量（字节）
)

/**
 * 时间段内的流量统计
 */
@Serializable
data class TrafficStats(
    val period: String,           // 时间段标识 ("hour", "day", "week")
    val startTime: Long,          // 开始时间戳
    val endTime: Long,            // 结束时间戳
    val serverId: Long,           // 服务器ID
    val serverName: String,       // 服务器名称
    val netInDelta: Long,         // 下行流量增量
    val netOutDelta: Long,       // 上行流量增量
    val netTotalDelta: Long      // 总流量增量
) {
    fun formatNetIn(): String = formatBytes(netInDelta)
    fun formatNetOut(): String = formatBytes(netOutDelta)
    fun formatNetTotal(): String = formatBytes(netTotalDelta)
}

/**
 * 所有服务器的聚合流量统计
 */
@Serializable
data class AggregatedTrafficStats(
    val period: String,
    val startTime: Long,
    val endTime: Long,
    val totalNetIn: Long,
    val totalNetOut: Long,
    val totalNet: Long,
    val serverCount: Int,
    val serverStats: List<TrafficStats>
) {
    fun formatTotalNetIn(): String = formatBytes(totalNetIn)
    fun formatTotalNetOut(): String = formatBytes(totalNetOut)
    fun formatTotalNet(): String = formatBytes(totalNet)
}

/**
 * 流量历史记录存储
 */
@Serializable
data class TrafficHistory(
    val serverId: Long,
    val snapshots: MutableList<TrafficSnapshot> = mutableListOf()
) {
    fun addSnapshot(snapshot: TrafficSnapshot) {
        snapshots.add(snapshot)
        // 只保留最近7天的数据（每小时一个点，共168个点）
        val cutoffTime = System.currentTimeMillis() / 1000 - 7 * 24 * 3600
        snapshots.removeAll { it.timestamp < cutoffTime }
        // 按时间排序
        snapshots.sortBy { it.timestamp }
    }
    
    fun getSnapshotsForPeriod(startTime: Long, endTime: Long): List<TrafficSnapshot> {
        return snapshots.filter { it.timestamp in startTime..endTime }
    }
}

/**
 * 格式化字节数为可读字符串
 */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val adjustedDigitGroups = digitGroups.coerceIn(0, units.size - 1)
    return String.format("%.2f %s", bytes / Math.pow(1024.0, adjustedDigitGroups.toDouble()), units[adjustedDigitGroups])
}

/**
 * 计算流量统计
 */
fun calculateTrafficStats(
    snapshots: List<TrafficSnapshot>,
    period: String,
    startTime: Long,
    endTime: Long
): TrafficStats? {
    if (snapshots.size < 2) return null
    
    val sortedSnapshots = snapshots.sortedBy { it.timestamp }
    val firstSnapshot = sortedSnapshots.first()
    val lastSnapshot = sortedSnapshots.last()
    
    // 计算增量（注意：累计流量只增不减，如果数据异常则取绝对值）
    val netInDelta = (lastSnapshot.netInTransfer - firstSnapshot.netInTransfer).coerceAtLeast(0)
    val netOutDelta = (lastSnapshot.netOutTransfer - firstSnapshot.netOutTransfer).coerceAtLeast(0)
    
    return TrafficStats(
        period = period,
        startTime = startTime,
        endTime = endTime,
        serverId = firstSnapshot.serverId,
        serverName = firstSnapshot.serverName,
        netInDelta = netInDelta,
        netOutDelta = netOutDelta,
        netTotalDelta = netInDelta + netOutDelta
    )
}
