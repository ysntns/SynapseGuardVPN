package com.synapseguard.vpn.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synapseguard.vpn.R
import com.synapseguard.vpn.presentation.theme.*
import kotlinx.coroutines.delay

/**
 * Splash Screen - Displays SynapseGuard branding with animations
 * Following the BCI-Optimized design with futuristic aesthetic
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "splash_glow")

    // Pulsing glow effect
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Logo scale animation
    var logoScale by remember { mutableFloatStateOf(0f) }
    var textAlpha by remember { mutableFloatStateOf(0f) }

    // Animate on launch
    LaunchedEffect(Unit) {
        // Logo scale in
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        ) { value, _ ->
            logoScale = value
        }

        // Text fade in
        delay(400)
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = LinearEasing)
        ) { value, _ ->
            textAlpha = value
        }

        // Wait before navigating
        delay(2000)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BackgroundPrimary,
                        BackgroundSecondary,
                        BackgroundPrimary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Logo with glow effect
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(logoScale),
                contentAlignment = Alignment.Center
            ) {
                // Glow effect behind logo
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .alpha(glowAlpha)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    CyanPrimary.copy(alpha = 0.3f),
                                    BackgroundPrimary.copy(alpha = 0f)
                                )
                            )
                        )
                )

                // SynapseGuard Logo - Vector Drawable
                Icon(
                    painter = painterResource(id = R.drawable.ic_synapseguard_logo),
                    contentDescription = "SynapseGuard Logo",
                    modifier = Modifier.fillMaxSize(),
                    tint = Color.Unspecified // Use original colors from drawable
                )
            }

            // App name and tagline
            Column(
                modifier = Modifier.alpha(textAlpha),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "SynapseGuard",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "BCI-Optimized Secure VPN",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )

                // Version or loading indicator
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Initializing Secure Connection...",
                    fontSize = 12.sp,
                    color = CyanPrimary.copy(alpha = glowAlpha)
                )
            }
        }
    }
}
