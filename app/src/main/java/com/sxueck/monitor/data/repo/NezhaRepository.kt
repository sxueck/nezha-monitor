package com.sxueck.monitor.data.repo

import com.sxueck.monitor.data.model.HostState
import com.sxueck.monitor.data.model.MonitorConfig
import com.sxueck.monitor.data.model.MonitoredServer
import com.sxueck.monitor.data.model.ServerData
import com.sxueck.monitor.data.model.SyncPayload
import com.sxueck.monitor.data.model.TagStats
import com.sxueck.monitor.data.model.WidgetSnapshot
import com.sxueck.monitor.data.network.NezhaApi

class NezhaRepository(private val api: NezhaApi) {
    suspend fun fetch(config: MonitorConfig, carouselIndex: Int, nowEpochSec: Long): SyncPayload {
        android.util.Log.d("NezhaRepository", "=== Fetch started, nowEpochSec: $nowEpochSec ===")
        
        val serverResponse = api.getServerList()
        val groupResponse = api.getServerGroups()
        
        android.util.Log.d("NezhaRepository", "Response successful: ${serverResponse.isSuccessful}, code: ${serverResponse.code()}")

        if (!serverResponse.isSuccessful) {
            return SyncPayload(
                snapshot = WidgetSnapshot(
                    status = "error",
                    message = "API Error: ${serverResponse.code()}",
                    updatedAtEpochSec = nowEpochSec
                ),
                servers = emptyList(),
                nextCarouselIndex = 0
            )
        }

        val body = serverResponse.body()
        if (body == null || !body.success) {
            return SyncPayload(
                snapshot = WidgetSnapshot(
                    status = "error",
                    message = body?.error ?: "No data",
                    updatedAtEpochSec = nowEpochSec
                ),
                servers = emptyList(),
                nextCarouselIndex = 0
            )
        }

        val serverList = body.data ?: emptyList()
        android.util.Log.d("NezhaRepository", "Server list size: ${serverList.size}")
        
        if (serverList.isNotEmpty()) {
            val firstServer = serverList.first()
            android.util.Log.d("NezhaRepository", "First server: id=${firstServer.id}, name=${firstServer.name}")
            android.util.Log.d("NezhaRepository", "First server state: ${firstServer.state}")
            android.util.Log.d("NezhaRepository", "First server host: ${firstServer.host}")
            android.util.Log.d("NezhaRepository", "First server lastActive: ${firstServer.lastActive}")
        }
        
        if (serverList.isEmpty()) {
            return SyncPayload(
                snapshot = WidgetSnapshot(
                    status = "error",
                    message = "No server data",
                    updatedAtEpochSec = nowEpochSec
                ),
                servers = emptyList(),
                nextCarouselIndex = 0
            )
        }

        // Build group map from group response
        val groupMap = if (groupResponse.isSuccessful) {
            groupResponse.body()?.data?.associateBy({ it.id }, { it.name }) ?: emptyMap()
        } else {
            emptyMap()
        }

        val servers = serverList.map { server ->
            mapToMonitoredServer(server, groupMap, nowEpochSec)
        }

        val filteredServers = if (config.tags.isEmpty()) {
            servers
        } else {
            servers.filter { server ->
                config.tags.any { tag ->
                    server.tag.equals(tag, ignoreCase = true)
                }
            }
        }

        if (filteredServers.isEmpty()) {
            return SyncPayload(
                snapshot = WidgetSnapshot(
                    status = "error",
                    message = "No server matches tags: ${config.tags.joinToString(", ")}",
                    updatedAtEpochSec = nowEpochSec
                ),
                servers = emptyList(),
                nextCarouselIndex = 0
            )
        }

        // Build server display data for all servers
        val serverDisplayData = filteredServers.map { server ->
            com.sxueck.monitor.data.model.ServerDisplayData(
                id = server.id,
                name = server.name,
                tag = server.tag,
                isOffline = server.isOffline,
                cpuPercent = server.cpuPercent ?: 0.0,
                memoryPercent = server.memoryPercent ?: 0.0,
                memoryUsed = formatBytes(server.memoryUsed),
                memoryTotal = formatBytes(server.memoryTotal),
                diskPercent = server.diskPercent ?: 0.0,
                diskUsed = formatBytes(server.diskUsed),
                diskTotal = formatBytes(server.diskTotal),
                load1 = server.load1 ?: 0.0,
                load5 = server.load5 ?: 0.0,
                load15 = server.load15 ?: 0.0,
                rxSpeed = formatSpeed(server.rxSpeed),
                txSpeed = formatSpeed(server.txSpeed),
                netInTransfer = formatBytes(server.netInTransfer),
                netOutTransfer = formatBytes(server.netOutTransfer),
                platform = server.platform ?: "",
                arch = server.arch ?: "",
                virtualization = server.virtualization ?: "",
                lastActiveText = formatLastActive(nowEpochSec, server.lastActiveEpochSec),
                platformVersion = server.platformVersion ?: "",
                uptime = formatUptime(server.uptime)
            )
        }

        // Debug: Log server states
        android.util.Log.d("NezhaRepository", "Total servers: ${filteredServers.size}")
        filteredServers.forEach { server ->
            android.util.Log.d("NezhaRepository", "Server: ${server.name}, isOffline: ${server.isOffline}, lastActive: ${server.lastActiveEpochSec}")
        }
        
        // Calculate overall stats
        val totalStats = TagStats(
            total = filteredServers.size,
            online = filteredServers.count { !it.isOffline },
            offline = filteredServers.count { it.isOffline }
        )

        // 计算最高负载设备（使用 load1 作为负载指标，过滤掉 load 为 0 或 null 的）
        val highestLoadServer = filteredServers
            .filter { !it.isOffline && it.load1 != null && it.load1 > 0 }
            .maxByOrNull { it.load1!! }
        
        // 计算总体流量统计
        val totalNetIn = filteredServers.mapNotNull { it.netInTransfer }.sum()
        val totalNetOut = filteredServers.mapNotNull { it.netOutTransfer }.sum()

        val snapshot = WidgetSnapshot(
            status = "ok",
            message = "OK",
            updatedAtEpochSec = nowEpochSec,
            currentTag = if (config.tags.isEmpty()) "All" else config.tags.joinToString(", "),
            tagTotal = totalStats.total,
            tagOnline = totalStats.online,
            tagOffline = totalStats.offline,
            serverName = "${filteredServers.size} Servers",
            cpuText = "Avg CPU ${"%.1f".format(filteredServers.mapNotNull { it.cpuPercent }.average())}%",
            memoryText = "Avg MEM ${"%.1f".format(filteredServers.mapNotNull { it.memoryPercent }.average())}%",
            netText = "${filteredServers.count { !it.isOffline }} online",
            trendText = "${filteredServers.count { it.isOffline }} offline",
            lastActiveText = "Updated ${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(nowEpochSec * 1000))}",
            servers = serverDisplayData,
            highestLoadServer = highestLoadServer?.name ?: "-",
            highestLoadValue = if (highestLoadServer?.load1 != null && highestLoadServer.load1 > 0) "%.2f".format(highestLoadServer.load1) else "--",
            totalNetIn = formatBytes(totalNetIn),
            totalNetOut = formatBytes(totalNetOut)
        )

        return SyncPayload(snapshot = snapshot, servers = filteredServers, nextCarouselIndex = 0)
    }

    private fun mapToMonitoredServer(server: ServerData, groupMap: Map<Long, String>, nowEpochSec: Long): MonitoredServer {
        val lastActiveEpochSec = parseLastActive(server.lastActive)
        
        val state = server.state
        val host = server.host
        
        val cpuPercent = state?.cpu
        val memUsed = state?.memUsed
        val memTotal = host?.memTotal
        val memoryPercent = calculateMemoryPercent(memUsed, memTotal)
        
        val diskUsed = state?.diskUsed
        val diskTotal = host?.diskTotal
        val diskPercent = calculateDiskPercent(diskUsed, diskTotal)
        
        val rxSpeed = state?.netInSpeed?.toDouble()
        val txSpeed = state?.netOutSpeed?.toDouble()

        // Get tag from server group
        val tag = server.serverGroup?.firstOrNull()?.let { groupId ->
            groupMap[groupId]
        } ?: "default"

        return MonitoredServer(
            id = server.id,
            name = server.name,
            tag = tag,
            lastActiveEpochSec = lastActiveEpochSec,
            cpuPercent = cpuPercent,
            memoryPercent = memoryPercent,
            memoryUsed = memUsed,
            memoryTotal = memTotal,
            diskPercent = diskPercent,
            diskUsed = diskUsed,
            diskTotal = diskTotal,
            load1 = state?.load1,
            load5 = state?.load5,
            load15 = state?.load15,
            rxSpeed = rxSpeed,
            txSpeed = txSpeed,
            netInTransfer = state?.netInTransfer,
            netOutTransfer = state?.netOutTransfer,
            platform = host?.platform,
            arch = host?.arch,
            virtualization = host?.virtualization,
            platformVersion = host?.platformVersion,
            uptime = state?.uptime,
            isOffline = isOffline(nowEpochSec = nowEpochSec, lastActive = lastActiveEpochSec)
        )
    }

    private fun calculateMemoryPercent(memUsed: Long?, memTotal: Long?): Double? {
        if (memUsed == null || memTotal == null || memTotal == 0L) return null
        return (memUsed.toDouble() / memTotal.toDouble()) * 100.0
    }

    private fun calculateDiskPercent(diskUsed: Long?, diskTotal: Long?): Double? {
        if (diskUsed == null || diskTotal == null || diskTotal == 0L) return null
        return (diskUsed.toDouble() / diskTotal.toDouble()) * 100.0
    }

    private fun formatBytes(bytes: Long?): String {
        if (bytes == null || bytes == 0L) return "--"
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        if (gb >= 1024.0) {
            return "${"%.1f".format(gb / 1024.0)}TB"
        }
        return "${"%.1f".format(gb)}GB"
    }

    private fun parseLastActive(lastActive: String): Long? {
        if (lastActive.isBlank()) return null
        return try {
            java.time.ZonedDateTime.parse(lastActive)?.toEpochSecond()
        } catch (e: Exception) {
            null
        }
    }

    private fun buildTagStats(servers: List<MonitoredServer>): Map<String, TagStats> {
        return servers.groupBy { it.tag }
            .mapValues { (_, list) ->
                val offline = list.count { it.isOffline }
                TagStats(
                    total = list.size,
                    online = list.size - offline,
                    offline = offline
                )
            }
    }

    private fun isOffline(nowEpochSec: Long, lastActive: Long?): Boolean {
        if (lastActive == null) {
            return true
        }
        return nowEpochSec - lastActive > OFFLINE_THRESHOLD_SECONDS
    }

    private fun formatPercent(label: String, value: Double?): String {
        val content = if (value == null) "--" else "${"%.1f".format(value)}%"
        return "$label $content"
    }

    private fun formatNet(rx: Double?, tx: Double?): String {
        if (rx == null && tx == null) {
            return "NET --"
        }
        val rxText = formatSpeed(rx)
        val txText = formatSpeed(tx)
        return "NET ${rxText} / ${txText}"
    }

    private fun formatSpeed(bytesPerSec: Double?): String {
        if (bytesPerSec == null || bytesPerSec == 0.0) {
            return "--"
        }
        val kb = bytesPerSec / 1024.0
        if (kb < 1024.0) {
            return "${"%.1f".format(kb)}KB/s"
        }
        val mb = kb / 1024.0
        return "${"%.1f".format(mb)}MB/s"
    }

    private fun buildTrendText(cpu: Double?): String {
        if (cpu == null) {
            return "Trend --"
        }
        val hint = when {
            cpu >= 80.0 -> "High"
            cpu >= 50.0 -> "Medium"
            else -> "Low"
        }
        return "Trend CPU $hint"
    }

    private fun formatLastActive(nowEpochSec: Long, lastActive: Long?): String {
        if (lastActive == null) {
            return "Last active --"
        }
        val delta = (nowEpochSec - lastActive).coerceAtLeast(0)
        return "Last active ${delta}s ago"
    }

    private fun formatUptime(seconds: Long?): String {
        if (seconds == null || seconds <= 0) return "--"
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }

    private companion object {
        // 将离线阈值从5分钟增加到10分钟，以适应不同的Agent上报间隔
        const val OFFLINE_THRESHOLD_SECONDS = 600L
    }
}
