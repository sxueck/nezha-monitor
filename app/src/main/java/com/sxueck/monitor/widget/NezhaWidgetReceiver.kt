package com.sxueck.monitor.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.sxueck.monitor.worker.MonitorWorkScheduler

class NezhaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NezhaWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        MonitorWorkScheduler.scheduleNow(context)
    }
}
