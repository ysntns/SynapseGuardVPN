package com.synapseguard.vpn.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synapseguard.vpn.domain.model.VpnState
import com.synapseguard.vpn.presentation.theme.*

enum class ConnectionButtonState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

@Composable
fun CircularConnectionButton(
    vpnState: VpnState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp
) {
    val buttonState = when (vpnState) {
        is VpnState.Idle, is VpnState.Error -> ConnectionButtonState.DISCONNECTED
        is VpnState.Connecting, is VpnState.Disconnecting -> ConnectionButtonState.CONNECTING
        is VpnState.Connected -> ConnectionButtonState.CONNECTED
    }

    // Infinite rotation animation for connecting state
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulse animation for connecting state
    val pulseAnimation by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Glow animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated circular ring
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.toPx() - strokeWidth) / 2

            when (buttonState) {
                ConnectionButtonState.DISCONNECTED -> {
                    // Red ring for disconnected state
                    drawCircle(
                        color = StatusDisconnected,
                        radius = radius,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                ConnectionButtonState.CONNECTING -> {
                    // Animated rotating gradient ring
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                StatusConnecting,
                                StatusConnecting.copy(alpha = 0.3f),
                                StatusConnecting
                            )
                        ),
                        startAngle = rotation,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Glow effect
                    drawCircle(
                        color = StatusConnecting.copy(alpha = glowAlpha * 0.3f),
                        radius = radius + 20.dp.toPx()
                    )
                }

                ConnectionButtonState.CONNECTED -> {
                    // Green ring with glow for connected state
                    drawCircle(
                        color = StatusConnected.copy(alpha = glowAlpha * 0.4f),
                        radius = radius + 15.dp.toPx()
                    )

                    drawCircle(
                        color = StatusConnected,
                        radius = radius,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Inner circle with gradient background
        Canvas(
            modifier = Modifier.size(size - 32.dp)
        ) {
            val innerRadius = this.size.minDimension / 2

            val backgroundColor = when (buttonState) {
                ConnectionButtonState.DISCONNECTED -> BackgroundSecondary
                ConnectionButtonState.CONNECTING -> BackgroundCard
                ConnectionButtonState.CONNECTED -> BackgroundSecondary
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        backgroundColor,
                        BackgroundPrimary
                    ),
                    center = Offset(this.size.width / 2, this.size.height / 2),
                    radius = innerRadius
                ),
                radius = innerRadius
            )
        }

        // Center text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val (text, color) = when (buttonState) {
                ConnectionButtonState.DISCONNECTED -> "TAP TO\nCONNECT" to StatusDisconnected
                ConnectionButtonState.CONNECTING -> "CONNECTING" to StatusConnecting
                ConnectionButtonState.CONNECTED -> "CONNECTED" to StatusConnected
            }

            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                lineHeight = 24.sp,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
