// SynapseGuard VPN - Color Palette
// Dark theme with cyan accents

export const colors = {
  // Primary Colors
  primary: '#00D9FF',
  primaryDark: '#00A3CC',
  primaryLight: '#4DE8FF',

  // Background Colors
  background: '#0A0E14',
  backgroundSecondary: '#121820',
  backgroundTertiary: '#1A2230',
  surface: '#1E2738',
  surfaceLight: '#2A3548',

  // Text Colors
  textPrimary: '#FFFFFF',
  textSecondary: '#A0AEC0',
  textTertiary: '#64748B',
  textDisabled: '#475569',

  // Status Colors
  success: '#10B981',
  warning: '#F59E0B',
  error: '#EF4444',
  info: '#3B82F6',

  // VPN Status Colors
  connected: '#10B981',
  connecting: '#F59E0B',
  disconnected: '#64748B',
  errorStatus: '#EF4444',

  // Border & Divider
  border: '#2A3548',
  divider: '#1E2738',

  // Overlay
  overlay: 'rgba(0, 0, 0, 0.7)',
  overlayLight: 'rgba(0, 0, 0, 0.5)',

  // Gradient Colors
  gradientStart: '#00D9FF',
  gradientEnd: '#0066FF',

  // Server Load Colors
  loadLow: '#10B981',
  loadMedium: '#F59E0B',
  loadHigh: '#EF4444',

  // Transparent
  transparent: 'transparent',
  white: '#FFFFFF',
  black: '#000000',
};

export type ColorKeys = keyof typeof colors;
