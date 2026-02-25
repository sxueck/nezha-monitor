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
    val surface = ColorProvider(Color(0xFF1E293B))
    val surfaceHighlight = ColorProvider(Color(0xFF334155))
    val textPrimary = ColorProvider(Color(0xFFF8FAFC))
    val textSecondary = ColorProvider(Color(0xFF94A3B8))
    val textMuted = ColorProvider(Color(0xFF64748B))
    
    val accentOnline = ColorProvider(Color(0xFF22D3EE))
    val accentTrafficIn = ColorProvider(Color(0xFF34D399))
    val accentTrafficOut = ColorProvider(Color(0xFFA78BFA))
    val accentOffline = ColorProvider(Color(0xFFEF4444))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgDeep)
            .cornerRadius(20.dp)
            .padding(16.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = androidx.glance.layout.Alignment.Vertical.CenterVertically
            ) {
                val statusColor = when (snapshot.status) {
                    "ok" -> accentOnline
                    "error" -> accentOffline
                    else -> textMuted
                }
                Box(
                    modifier = GlanceModifier
                        .size(8.dp)
                        .background(statusColor)
                        .cornerRadius(4.dp),
                    contentAlignment = androidx.glance.layout.Alignment.Center
                ) {}
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    text = "NEZHA",
                    style = TextStyle(
                        color = textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text = snapshot.lastActiveText.replace("Updated ", "").replace("Last active ", ""),
                    style = TextStyle(
                        color = textMuted,
                        fontSize = 10.sp
                    )
                )
            }

            if (snapshot.servers.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                        .background(surface)
                        .cornerRadius(12.dp),
                    contentAlignment = androidx.glance.layout.Alignment.Center
                ) {
                    Text(
                        text = snapshot.message.takeIf { it.isNotBlank() } ?: "Waiting for data...",
                        style = TextStyle(
                            color = textSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            } else {
                Spacer(GlanceModifier.height(16.dp))
                
                // 核心指标区：在线数量 + 流量（放到右侧）
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = androidx.glance.layout.Alignment.Vertical.Bottom
                ) {
                    // 在线数量
                    Text(
                        text = "${snapshot.tagOnline}",
                        style = TextStyle(
                            color = accentOnline,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    Column {
                        Text(
                            text = "/${snapshot.tagTotal}",
                            style = TextStyle(
                                color = textSecondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = "ONLINE",
                            style = TextStyle(
                                color = textMuted,
                                fontSize = 9.sp
                            )
                        )
                    }
                    
                    Spacer(GlanceModifier.defaultWeight())
                    
                    // 流量统计（使用瘦箭头，放到右侧）
                    Column(horizontalAlignment = androidx.glance.layout.Alignment.Horizontal.End) {
                        // 下行
                        Row(verticalAlignment = androidx.glance.layout.Alignment.Vertical.CenterVertically) {
                            Text(
                                text = "<- ",
                                style = TextStyle(
                                    color = accentTrafficIn,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = snapshot.totalNetIn,
                                style = TextStyle(
                                    color = textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                        Spacer(GlanceModifier.height(2.dp))
                        // 上行
                        Row(verticalAlignment = androidx.glance.layout.Alignment.Vertical.CenterVertically) {
                            Text(
                                text = "-> ",
                                style = TextStyle(
                                    color = accentTrafficOut,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = snapshot.totalNetOut,
                                style = TextStyle(
                                    color = textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                // 如果有离线设备，显示警告
                if (snapshot.tagOffline > 0) {
                    Spacer(GlanceModifier.height(12.dp))
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(ColorProvider(Color(0xFFEF4444).copy(alpha = 0.1f)))
                            .cornerRadius(12.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = androidx.glance.layout.Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                text = "!",
                                style = TextStyle(
                                    color = accentOffline,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(GlanceModifier.width(8.dp))
                            Text(
                                text = "${snapshot.tagOffline} servers offline",
                                style = TextStyle(
                                    color = accentOffline,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
