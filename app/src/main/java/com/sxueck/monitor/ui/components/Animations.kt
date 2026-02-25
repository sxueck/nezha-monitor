package com.sxueck.monitor.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sxueck.monitor.ui.theme.NezhaColors

@Composable
fun LoadingDots(
    modifier: Modifier = Modifier,
    color: Color = NezhaColors.AccentPrimary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    
    @Composable
    fun Dot(delay: Int) {
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = delay, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = delay, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        
        Box(
            modifier = Modifier
                .size(8.dp)
                .scale(scale)
                .alpha(alpha)
                .background(color, CircleShape)
        )
    }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dot(0)
        Dot(150)
        Dot(300)
    }
}

@Composable
fun PulseIndicator(
    modifier: Modifier = Modifier,
    color: Color = NezhaColors.Success,
    isActive: Boolean = true
) {
    if (!isActive) {
        Box(
            modifier = modifier
                .size(12.dp)
                .background(NezhaColors.TextMuted, CircleShape)
        )
        return
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )
    
    Box(
        modifier = modifier.size(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .alpha(alpha)
                .background(color, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
    }
}

@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    color: Color = NezhaColors.SurfaceVariant
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    Box(
        modifier = modifier.background(color)
    )
}
