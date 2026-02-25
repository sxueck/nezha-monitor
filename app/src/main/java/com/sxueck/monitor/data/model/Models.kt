package com.sxueck.monitor.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MonitorConfig(
    val baseUrl: String = "",
    val apiToken: String = "",
    val tags: List<String> = emptyList()
)

@Serializable
data class WidgetSnapshot(
    val status: String = "idle",
    val message: String = "Configure panel URL and token in app.",
    val updatedAtEpochSec: Long = 0,
    val currentTag: String = "-",
    val tagTotal: Int = 0,
    val tagOnline: Int = 0,
    val tagOffline: Int = 0,
    val serverName: String = "-",
    val cpuText: String = "CPU --",
    val memoryText: String = "MEM --",
    val netText: String = "NET --",
    val trendText: String = "Trend --",
    val lastActiveText: String = "Last active --",
    val servers: List<ServerDisplayData> = emptyList(),
    val highestLoadServer: String = "-",
    val highestLoadValue: String = "--",
    val totalNetIn: String = "--",
    val totalNetOut: String = "--",
    val dailyNetIn: String = "--",
    val dailyNetOut: String = "--"
)

@Serializable
data class ServerDisplayData(
    val id: Long = 0,
    val name: String = "",
    val tag: String = "",
    val isOffline: Boolean = false,
    val cpuPercent: Double = 0.0,
    val memoryPercent: Double = 0.0,
    val memoryUsed: String = "",
    val memoryTotal: String = "",
    val diskPercent: Double = 0.0,
    val diskUsed: String = "",
    val diskTotal: String = "",
    val load1: Double = 0.0,
    val load5: Double = 0.0,
    val load15: Double = 0.0,
    val rxSpeed: String = "",
    val txSpeed: String = "",
    val netInTransfer: String = "",
    val netOutTransfer: String = "",
    val platform: String = "",
    val arch: String = "",
    val virtualization: String = "",
    val lastActiveText: String = "",
    val hasHostData: Boolean = false,
    val platformVersion: String = "",
    val uptime: String = ""
)

data class TagStats(
    val total: Int,
    val online: Int,
    val offline: Int
)

data class MonitoredServer(
    val id: Long,
    val name: String,
    val tag: String,
    val lastActiveEpochSec: Long?,
    val cpuPercent: Double?,
    val memoryPercent: Double?,
    val memoryUsed: Long?,
    val memoryTotal: Long?,
    val diskPercent: Double?,
    val diskUsed: Long?,
    val diskTotal: Long?,
    val load1: Double?,
    val load5: Double?,
    val load15: Double?,
    val rxSpeed: Double?,
    val txSpeed: Double?,
    val netInTransfer: Long?,
    val netOutTransfer: Long?,
    val platform: String?,
    val arch: String?,
    val virtualization: String?,
    val platformVersion: String?,
    val uptime: Long?,
    val isOffline: Boolean
) {
    val dedupeKey: String
        get() = "$tag#$id"
}

data class SyncPayload(
    val snapshot: WidgetSnapshot,
    val servers: List<MonitoredServer>,
    val nextCarouselIndex: Int
)

@Serializable
data class LoginRequest(
    val username: String = "",
    val password: String = ""
)

@Serializable
data class LoginResponse(
    val token: String = "",
    val expire: String = ""
)

@Serializable
data class ServerData(
    val id: Long = 0,
    val name: String = "",
    val uuid: String = "",
    @SerialName("display_index") val displayIndex: Int = 0,
    @SerialName("note") val note: String = "",
    @SerialName("public_note") val publicNote: String = "",
    @SerialName("hide_for_guest") val hideForGuest: Boolean = false,
    @SerialName("enable_ddns") val enableDdns: Boolean = false,
    @SerialName("ddns_profiles") val ddnsProfiles: List<Long> = emptyList(),
    @SerialName("override_ddns_domains") val overrideDdnsDomains: Map<String, List<String>> = emptyMap(),
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("last_active") val lastActive: String = "",
    val geoip: GeoIP? = null,
    val host: Host? = null,
    val state: HostState? = null,
    @SerialName("server_group") val serverGroup: List<Long>? = null
)

@Serializable
data class Host(
    val cpu: List<String> = emptyList(),
    @SerialName("mem_total") val memTotal: Long = 0,
    @SerialName("disk_total") val diskTotal: Long = 0,
    @SerialName("swap_total") val swapTotal: Long = 0,
    val platform: String = "",
    @SerialName("platform_version") val platformVersion: String = "",
    val arch: String = "",
    val virtualization: String = "",
    @SerialName("boot_time") val bootTime: Long = 0,
    val version: String = "",
    val gpu: List<String> = emptyList()
)

@Serializable
data class HostState(
    val cpu: Double = 0.0,
    @SerialName("mem_used") val memUsed: Long = 0,
    @SerialName("disk_used") val diskUsed: Long = 0,
    val gpu: List<Double> = emptyList(),
    @SerialName("swap_used") val swapUsed: Long = 0,
    @SerialName("net_in_speed") val netInSpeed: Long = 0,
    @SerialName("net_out_speed") val netOutSpeed: Long = 0,
    @SerialName("net_in_transfer") val netInTransfer: Long = 0,
    @SerialName("net_out_transfer") val netOutTransfer: Long = 0,
    @SerialName("load1") val load1: Double = 0.0,
    @SerialName("load5") val load5: Double = 0.0,
    @SerialName("load15") val load15: Double = 0.0,
    @SerialName("tcp_conn_count") val tcpConnCount: Int = 0,
    @SerialName("udp_conn_count") val udpConnCount: Int = 0,
    @SerialName("process_count") val processCount: Int = 0,
    val temperatures: List<SensorTemperature> = emptyList(),
    val uptime: Long = 0
)

@Serializable
data class SensorTemperature(
    val name: String = "",
    val temperature: Double = 0.0
)

@Serializable
data class GeoIP(
    val country: String = "",
    val countryCode: String = "",
    val city: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val asn: String = "",
    val org: String = "",
    val isp: String = "",
    val ipv4: String = "",
    val ipv6: String = ""
)

@Serializable
data class ServerGroup(
    val id: Long = 0,
    val name: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class ServerGroupResponseItem(
    val id: Long = 0,
    val name: String = "",
    val serverCount: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class CommonResponse<T>(
    val success: Boolean = false,
    val data: T? = null,
    val error: String = ""
)

typealias ServerResponse<T> = CommonResponse<T>
