package com.sxueck.monitor.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.sxueck.monitor.data.model.WidgetSnapshot
import com.sxueck.monitor.data.store.AppPreferences

/**
 * 1x1 简洁小组件
 * 仅显示在线数/总数 (X/Y)
 */
class NezhaWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = AppPreferences(context).getSnapshotOnce()
        provideContent {
            WidgetContent(snapshot)
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetContent(snapshot: WidgetSnapshot) {
    val bgDeep = ColorProvider(Color(0xFF0F172A))
    val textPrimary = ColorProvider(Color(0xFFF8FAFC))
    val textSecondary = ColorProvider(Color(0xFF94A3B8))
    val textMuted = ColorProvider(Color(0xFF64748B))
    
    val accentOnline = ColorProvider(Color(0xFF22D3EE))
    val accentOffline = ColorProvider(Color(0xFFEF4444))
    
    // 根据状态选择边框颜色
    val borderColor = when {
        snapshot.tagOffline > 0 -> accentOffline
        snapshot.tagOnline > 0 -> accentOnline
        else -> textSecondary
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgDeep)
            .cornerRadius(16.dp)
            .padding(4.dp),
        contentAlignment = androidx.glance.layout.Alignment.Center
    ) {
        if (snapshot.servers.isEmpty()) {
            // 无数据时显示 "--"
            Text(
                text = "--",
                style = TextStyle(
                    color = textSecondary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        } else {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = androidx.glance.layout.Alignment.Vertical.CenterVertically,
                horizontalAlignment = androidx.glance.layout.Alignment.Horizontal.CenterHorizontally
            ) {
                // 主内容：显示 X/Y 格式
                Row(
                    verticalAlignment = androidx.glance.layout.Alignment.Vertical.CenterVertically,
                    horizontalAlignment = androidx.glance.layout.Alignment.Horizontal.CenterHorizontally
                ) {
                    // 在线数
                    Text(
                        text = snapshot.tagOnline.toString(),
                        style = TextStyle(
                            color = accentOnline,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    // 分隔符
                    Text(
                        text = "/",
                        style = TextStyle(
                            color = textSecondary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                    
                    // 总数
                    Text(
                        text = snapshot.tagTotal.toString(),
                        style = TextStyle(
                            color = textPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                
                // 底部：每日流量统计
                Row(
                    modifier = GlanceModifier.padding(top = 4.dp),
                    verticalAlignment = androidx.glance.layout.Alignment.Vertical.CenterVertically,
                    horizontalAlignment = androidx.glance.layout.Alignment.Horizontal.CenterHorizontally
                ) {
                    // 下载流量
                    Text(
                        text = "${snapshot.dailyNetIn}",
                        style = TextStyle(
                            color = textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                    
                    Box(modifier = GlanceModifier.padding(horizontal = 8.dp)) {
                        Text(
                            text = "|",
                            style = TextStyle(
                                color = textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                    
                    // 上传流量
                    Text(
                        text = "${snapshot.dailyNetOut}",
                        style = TextStyle(
                            color = textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}