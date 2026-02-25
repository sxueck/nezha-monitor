package com.sxueck.monitor.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object MonitorWorkScheduler {
    private const val UNIQUE_SYNC_WORK = "nezha_sync_once"
    private const val REFRESH_INTERVAL_MINUTES = 5L

    fun scheduleNow(context: Context) {
        enqueue(context = context, delayMinutes = 0)
    }

    fun scheduleNext(context: Context) {
        enqueue(context = context, delayMinutes = REFRESH_INTERVAL_MINUTES)
    }

    private fun enqueue(context: Context, delayMinutes: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val requestBuilder = OneTimeWorkRequestBuilder<MonitorSyncWorker>()
            .setConstraints(constraints)

        if (delayMinutes > 0) {
            requestBuilder.setInitialDelay(delayMinutes, TimeUnit.MINUTES)
        }

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                UNIQUE_SYNC_WORK,
                ExistingWorkPolicy.REPLACE,
                requestBuilder.build()
            )
    }
}
