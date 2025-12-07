package com.synapseguard.vpn.presentation.theme

import androidx.compose.ui.graphics.Color

// SynapseGuard Design System - Dark Theme Colors
// Exact colors from mockup design

// Background Colors - Deep Blue/Black
val BackgroundPrimary = Color(0xFF0A0E1A)          // Main background
val BackgroundSecondary = Color(0xFF141824)        // Secondary background
val BackgroundCard = Color(0xFF1A1F2E)             // Card backgrounds
val BackgroundCardElevated = Color(0xFF1F2535)     // Elevated card backgrounds

// Accent Colors - Cyan/Neon Blue
val CyanPrimary = Color(0xFF00D9FF)                // Primary cyan accent
val CyanLight = Color(0xFF00F0FF)                  // Lighter cyan for highlights
val CyanDark = Color(0xFF0099CC)                   // Darker cyan for pressed states

// Status Colors
val StatusDisconnected = Color(0xFFFF3B5C)         // Red - Disconnected/Error
val StatusConnected = Color(0xFF00FF88)            // Green - Connected
val StatusConnecting = Color(0xFFFFB800)           // Yellow - Connecting/In Progress

// Text Colors
val TextPrimary = Color(0xFFFFFFFF)                // White - Primary text
val TextSecondary = Color(0xFF8A8FA3)              // Gray - Secondary text
val TextTertiary = Color(0xFF5A5F73)               // Darker gray - Tertiary/disabled

// Circuit/Tech Accent Colors
val TechAccent = Color(0xFF00F0FF)                 // Bright cyan for tech elements
val TechGlow = Color(0x3300F0FF)                   // Cyan glow (20% opacity)

// Icon Colors
val IconPrimary = Color(0xFFFFFFFF)                // White icons
val IconSecondary = Color(0xFF8A8FA3)              // Gray icons
val IconAccent = Color(0xFF00D9FF)                 // Cyan icons for emphasis
val IconRed = Color(0xFFFF3B5C)                    // Red icons (security/danger)
val IconGreen = Color(0xFF00FF88)                  // Green icons (success)
val IconBlue = Color(0xFF0099CC)                   // Blue icons (info)
val IconYellow = Color(0xFFFFB800)                 // Yellow icons (warning)

// Surface Colors for Cards and Components
val SurfaceElevated = Color(0xFF1F2535)            // Elevated cards
val SurfaceHighlight = Color(0x1A00D9FF)           // Accent overlay (10% cyan)
val SurfaceSelected = Color(0x3300D9FF)            // Selected items (20% cyan)

// Border Colors
val BorderPrimary = Color(0x33FFFFFF)              // 20% White - Default borders
val BorderAccent = Color(0xFF00D9FF)               // Cyan - Active borders
val BorderSelected = Color(0xFF00F0FF)             // Bright cyan - Selected state

// Additional Colors
val BackgroundAccent = Color(0xFF1A3A4A)           // Accent background
val DividerColor = Color(0x33FFFFFF)               // Divider color (20% white)

// Gradient Colors for Animations
val GradientStart = Color(0xFF0A0E1A)              // Dark blue/black
val GradientMiddle = Color(0xFF00D9FF)             // Cyan
val GradientEnd = Color(0xFF141824)                // Secondary dark

// Chart and Graph Colors
val ChartUpload = Color(0xFF00F0FF)                // Upload data (bright cyan)
val ChartDownload = Color(0xFF00FF88)              // Download data (green)

// Legacy compatibility (deprecated - use new names)
@Deprecated("Use CyanPrimary instead", ReplaceWith("CyanPrimary"))
val AccentPrimary = CyanPrimary

@Deprecated("Use CyanLight instead", ReplaceWith("CyanLight"))
val AccentSecondary = CyanLight

@Deprecated("Use CyanDark instead", ReplaceWith("CyanDark"))
val AccentTertiary = CyanDark

@Deprecated("Use StatusConnected instead", ReplaceWith("StatusConnected"))
val VpnConnected = StatusConnected

@Deprecated("Use StatusDisconnected instead", ReplaceWith("StatusDisconnected"))
val VpnDisconnected = StatusDisconnected

@Deprecated("Use StatusConnecting instead", ReplaceWith("StatusConnecting"))
val VpnConnecting = StatusConnecting

@Deprecated("Use BackgroundPrimary instead", ReplaceWith("BackgroundPrimary"))
val BackgroundDark = BackgroundPrimary

@Deprecated("Use BackgroundCard instead", ReplaceWith("BackgroundCard"))
val SurfaceDark = BackgroundCard

@Deprecated("Use BackgroundCard instead", ReplaceWith("BackgroundCard"))
val BackgroundTertiary = BackgroundCard
