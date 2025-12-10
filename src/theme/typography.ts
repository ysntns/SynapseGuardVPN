import {TextStyle} from 'react-native';

export const typography: Record<string, TextStyle> = {
  // Headings
  h1: {
    fontSize: 32,
    fontWeight: '700',
    lineHeight: 40,
    letterSpacing: -0.5,
  },
  h2: {
    fontSize: 24,
    fontWeight: '700',
    lineHeight: 32,
    letterSpacing: -0.25,
  },
  h3: {
    fontSize: 20,
    fontWeight: '600',
    lineHeight: 28,
  },
  h4: {
    fontSize: 18,
    fontWeight: '600',
    lineHeight: 24,
  },

  // Body Text
  bodyLarge: {
    fontSize: 16,
    fontWeight: '400',
    lineHeight: 24,
  },
  bodyMedium: {
    fontSize: 14,
    fontWeight: '400',
    lineHeight: 20,
  },
  bodySmall: {
    fontSize: 12,
    fontWeight: '400',
    lineHeight: 16,
  },

  // Labels
  labelLarge: {
    fontSize: 14,
    fontWeight: '600',
    lineHeight: 20,
    letterSpacing: 0.1,
  },
  labelMedium: {
    fontSize: 12,
    fontWeight: '600',
    lineHeight: 16,
    letterSpacing: 0.5,
  },
  labelSmall: {
    fontSize: 10,
    fontWeight: '600',
    lineHeight: 14,
    letterSpacing: 0.5,
  },

  // Special
  button: {
    fontSize: 14,
    fontWeight: '600',
    lineHeight: 20,
    letterSpacing: 0.25,
  },
  caption: {
    fontSize: 11,
    fontWeight: '400',
    lineHeight: 14,
  },
  overline: {
    fontSize: 10,
    fontWeight: '600',
    lineHeight: 14,
    letterSpacing: 1.5,
    textTransform: 'uppercase',
  },
};
