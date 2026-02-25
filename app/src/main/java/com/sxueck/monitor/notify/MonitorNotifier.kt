package com.sxueck.monitor.notify

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sxueck.monitor.data.model.MonitoredServer

class MonitorNotifier(private val context: Context) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Nezha Offline Alert",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "Notifies when server goes offline"
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun notifyOffline(servers: List<MonitoredServer>) {
        if (servers.isEmpty()) {
            return
        }

        val title = if (servers.size == 1) {
            "Server offline: ${servers.first().name}"
        } else {
            "${servers.size} servers offline"
        }

        val body = servers.take(3)
            .joinToString(separator = ", ") { "${it.name}(${it.tag})" }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "nezha_offline_alert"
        const val NOTIFICATION_ID = 1001
    }
}
