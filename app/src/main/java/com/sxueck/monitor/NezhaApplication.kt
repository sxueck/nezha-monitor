package com.sxueck.monitor

import android.app.Application
import com.sxueck.monitor.notify.MonitorNotifier
import com.sxueck.monitor.worker.MonitorWorkScheduler

class NezhaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MonitorNotifier(this).ensureChannel()
        MonitorWorkScheduler.scheduleNow(this)
    }
}
