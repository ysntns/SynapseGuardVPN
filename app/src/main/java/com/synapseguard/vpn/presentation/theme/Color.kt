package com.synapseguard.vpn.presentation.theme

import androidx.compose.ui.graphics.Color

// SynapseGuard Design System - Dark Theme Colors

// Background Colors - Deep Blue/Black
val BackgroundPrimary = Color(0xFF0A0E1A)      // Main background
val BackgroundSecondary = Color(0xFF141824)    // Cards, elevated surfaces
val BackgroundTertiary = Color(0xFF1A1F2E)     // Slightly elevated elements

// Accent Colors - Cyan/Neon Blue
val AccentPrimary = Color(0xFF00D9FF)          // Primary accent (cyan)
val AccentSecondary = Color(0xFF00F0FF)        // Brighter cyan for highlights
val AccentTertiary = Color(0xFF0096FF)         // Darker cyan for pressed states

// Status Colors
val StatusConnected = Color(0xFF00FF88)        // Green - Connected state
val StatusDisconnected = Color(0xFFFF3B5C)     // Red - Disconnected/Error state
val StatusConnecting = Color(0xFF00D9FF)       // Cyan - Connecting animation
val StatusWarning = Color(0xFFFFC107)          // Yellow - Warnings

// Text Colors
val TextPrimary = Color(0xFFFFFFFF)            // White - Main text
val TextSecondary = Color(0xB3FFFFFF)          // 70% White - Secondary text
val TextTertiary = Color(0x80FFFFFF)           // 50% White - Disabled/hints
val TextAccent = Color(0xFF00D9FF)             // Cyan - Links, accents

// Icon Colors
val IconPrimary = Color(0xFFFFFFFF)            // White icons
val IconSecondary = Color(0xB3FFFFFF)          // 70% White icons
val IconAccent = Color(0xFF00D9FF)             // Cyan icons for emphasis
val IconRed = Color(0xFFFF3B5C)                // Red icons (security)
val IconGreen = Color(0xFF00FF88)              // Green icons (success)
val IconBlue = Color(0xFF0096FF)               // Blue icons (info)
val IconYellow = Color(0xFFFFC107)             // Yellow icons (warning)

// Surface Colors for Cards and Components
val SurfaceElevated = Color(0xFF1E2332)        // Elevated cards
val SurfaceHighlight = Color(0x1A00D9FF)       // Accent overlay (10% cyan)
val SurfaceSelected = Color(0x3300D9FF)        // Selected items (20% cyan)

// Border Colors
val BorderPrimary = Color(0x33FFFFFF)          // 20% White - Default borders
val BorderAccent = Color(0xFF00D9FF)           // Cyan - Active borders
val BorderSelected = Color(0xFF00F0FF)         // Bright cyan - Selected state

// Gradient Colors for Animations
val GradientStart = Color(0xFF0A0E1A)
val GradientMiddle = Color(0xFF00D9FF)
val GradientEnd = Color(0xFF141824)

// Chart and Graph Colors
val ChartUpload = Color(0xFF00F0FF)            // Upload data (bright cyan)
val ChartDownload = Color(0xFF00FF88)          // Download data (green)

// Legacy compatibility (to be phased out)
@Deprecated("Use StatusConnected instead", ReplaceWith("StatusConnected"))
val VpnConnected = StatusConnected

@Deprecated("Use StatusDisconnected instead", ReplaceWith("StatusDisconnected"))
val VpnDisconnected = StatusDisconnected

@Deprecated("Use StatusConnecting instead", ReplaceWith("StatusConnecting"))
val VpnConnecting = StatusConnecting

@Deprecated("Use BackgroundPrimary instead", ReplaceWith("BackgroundPrimary"))
val BackgroundDark = BackgroundPrimary

@Deprecated("Use BackgroundSecondary instead", ReplaceWith("BackgroundSecondary"))
val SurfaceDark = BackgroundSecondary
