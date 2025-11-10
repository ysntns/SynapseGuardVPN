package com.synapseguard.vpn.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * SynapseGuard VPN Dark Color Scheme
 * Following the BCI-Optimized design system with deep blue/black backgrounds
 * and cyan accent colors for a futuristic, secure aesthetic.
 */
private val SynapseGuardColorScheme = darkColorScheme(
    // Primary colors - Cyan accent
    primary = AccentPrimary,
    onPrimary = BackgroundPrimary,
    primaryContainer = AccentTertiary,
    onPrimaryContainer = TextPrimary,

    // Secondary colors - Status indicators
    secondary = StatusConnected,
    onSecondary = BackgroundPrimary,
    secondaryContainer = StatusConnecting,
    onSecondaryContainer = TextPrimary,

    // Tertiary colors - Warning/Error states
    tertiary = StatusDisconnected,
    onTertiary = TextPrimary,
    tertiaryContainer = StatusWarning,
    onTertiaryContainer = BackgroundPrimary,

    // Background colors
    background = BackgroundPrimary,
    onBackground = TextPrimary,

    // Surface colors
    surface = BackgroundSecondary,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundTertiary,
    onSurfaceVariant = TextSecondary,

    // Surface tint
    surfaceTint = AccentPrimary,

    // Inverse colors
    inverseSurface = TextPrimary,
    inverseOnSurface = BackgroundPrimary,
    inversePrimary = AccentPrimary,

    // Error colors
    error = StatusDisconnected,
    onError = TextPrimary,
    errorContainer = StatusDisconnected,
    onErrorContainer = TextPrimary,

    // Outline colors
    outline = BorderPrimary,
    outlineVariant = BorderAccent,

    // Scrim
    scrim = BackgroundPrimary
)

@Composable
fun SynapseGuardVPNTheme(
    darkTheme: Boolean = true, // Always use dark theme for SynapseGuard
    dynamicColor: Boolean = false, // Disable dynamic color to use our design system
    content: @Composable () -> Unit
) {
    // Always use our custom color scheme, ignoring system theme and dynamic colors
    val colorScheme = SynapseGuardColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar to match background
            window.statusBarColor = BackgroundPrimary.toArgb()
            // Set navigation bar to match background
            window.navigationBarColor = BackgroundPrimary.toArgb()
            // Use light status bar icons (white) on dark background
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
