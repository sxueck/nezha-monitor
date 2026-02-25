package com.sxueck.monitor.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sxueck.monitor.data.model.MonitoredServer
import com.sxueck.monitor.data.model.WidgetSnapshot
import com.sxueck.monitor.data.network.NezhaNetwork
import com.sxueck.monitor.data.network.TokenManager
import com.sxueck.monitor.data.repo.NezhaRepository
import com.sxueck.monitor.data.store.AppPreferences
import com.sxueck.monitor.data.traffic.TrafficPeriodHelper
import com.sxueck.monitor.data.traffic.TrafficSnapshot
import com.sxueck.monitor.data.traffic.TrafficStore
import com.sxueck.monitor.notify.MonitorNotifier
import com.sxueck.monitor.widget.NezhaWidget

class MonitorSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val preferences = AppPreferences(applicationContext)
        val notifier = MonitorNotifier(applicationContext)

        return runCatching {
            val config = preferences.getConfigOnce()
            if (config.baseUrl.isBlank() || config.apiToken.isBlank()) {
                preferences.saveSnapshot(
                    WidgetSnapshot(
                        status = "idle",
                        message = "Set URL/token in app first.",
                        updatedAtEpochSec = currentEpochSec()
                    )
                )
                NezhaWidget().updateAll(applicationContext)
                Result.success()
            } else {
                // Get valid token (auto-refresh if expired)
                val validToken = TokenManager.getValidToken(preferences)
                    ?: return@runCatching handleAuthFailure(preferences)

                val api = NezhaNetwork.createApi(config.baseUrl, validToken)
                val repo = NezhaRepository(api)
                val payload = repo.fetch(
                    config = config,
                    carouselIndex = preferences.getCarouselIndex(),
                    nowEpochSec = currentEpochSec()
                )

                // Calculate daily traffic
                val dailyTraffic = calculateDailyTraffic()
                
                // Create updated snapshot with daily traffic
                val updatedSnapshot = payload.snapshot.copy(
                    dailyNetIn = dailyTraffic.first,
                    dailyNetOut = dailyTraffic.second
                )
                
                preferences.saveSnapshot(updatedSnapshot)
                preferences.saveCarouselIndex(payload.nextCarouselIndex)
                preferences.saveLastSuccessAt(updatedSnapshot.updatedAtEpochSec)
                handleOfflineNotifications(payload.servers, preferences, notifier)
                
                // Save traffic snapshots
                saveTrafficSnapshots(payload.servers)

                NezhaWidget().updateAll(applicationContext)
                Result.success()
            }
        }.getOrElse { e ->
            android.util.Log.e("MonitorSyncWorker", "Error during sync", e)
            val lastSuccessAt = preferences.getLastSuccessAt()
            preferences.saveSnapshot(
                WidgetSnapshot(
                    status = "error",
                    message = "Data temporarily unavailable.",
                    updatedAtEpochSec = if (lastSuccessAt > 0) lastSuccessAt else currentEpochSec()
                )
            )
            NezhaWidget().updateAll(applicationContext)
            Result.success()
        }.also {
            MonitorWorkScheduler.scheduleNext(applicationContext)
        }
    }

    private suspend fun handleAuthFailure(preferences: AppPreferences): Result {
        preferences.saveSnapshot(
            WidgetSnapshot(
                status = "auth_error",
                message = "Authentication failed. Please re-login in app.",
                updatedAtEpochSec = currentEpochSec()
            )
        )
        NezhaWidget().updateAll(applicationContext)
        return Result.success()
    }

    private suspend fun handleOfflineNotifications(
        servers: List<MonitoredServer>,
        preferences: AppPreferences,
        notifier: MonitorNotifier
    ) {
        val previousOfflineKeys = preferences.getOfflineNotifiedSet()
        val currentOfflineMap = servers
            .filter { it.isOffline }
            .associateBy { it.dedupeKey }

        val currentOfflineKeys = currentOfflineMap.keys
        val newlyOfflineKeys = currentOfflineKeys - previousOfflineKeys
        if (newlyOfflineKeys.isNotEmpty()) {
            val newlyOfflineServers = newlyOfflineKeys.mapNotNull { currentOfflineMap[it] }
            notifier.notifyOffline(newlyOfflineServers)
        }

        preferences.saveOfflineNotifiedSet(currentOfflineKeys)
    }
    
    private suspend fun saveTrafficSnapshots(servers: List<MonitoredServer>) {
        val trafficStore = TrafficStore(applicationContext)
        val timestamp = currentEpochSec()
        
        val snapshots = servers.mapNotNull { server ->
            if (server.netInTransfer != null && server.netOutTransfer != null) {
                TrafficSnapshot(
                    timestamp = timestamp,
                    serverId = server.id,
                    serverName = server.name,
                    netInTransfer = server.netInTransfer,
                    netOutTransfer = server.netOutTransfer
                )
            } else null
        }
        
        trafficStore.saveSnapshots(snapshots)
    }

    private fun currentEpochSec(): Long = System.currentTimeMillis() / 1000
    
    private suspend fun calculateDailyTraffic(): Pair<String, String> {
        return runCatching {
            val trafficStore = TrafficStore(applicationContext)
            val (startTime, endTime) = TrafficPeriodHelper.getLastDayRange()
            val stats = trafficStore.calculateAggregatedStats("day", startTime, endTime)
            
            val formatBytes: (Long) -> String = { bytes ->
                when {
                    bytes >= 1024 * 1024 * 1024 * 1024L -> "%.1fTB".format(bytes / (1024.0 * 1024 * 1024 * 1024))
                    bytes >= 1024 * 1024 * 1024L -> "%.1fGB".format(bytes / (1024.0 * 1024 * 1024))
                    bytes >= 1024 * 1024L -> "%.1fMB".format(bytes / (1024.0 * 1024))
                    bytes >= 1024L -> "%.1fKB".format(bytes / 1024.0)
                    else -> "${bytes}B"
                }
            }
            
            Pair(formatBytes(stats.totalNetIn), formatBytes(stats.totalNetOut))
        }.getOrElse { 
            android.util.Log.e("MonitorSyncWorker", "Error calculating daily traffic", it)
            Pair("--", "--") 
        }
    }
}
