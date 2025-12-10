import React from 'react';
import {View, Text, TouchableOpacity, StyleSheet} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';
import {colors, typography, spacing, borderRadius} from '../theme';
import type {VpnServer} from '../types/vpn';

interface ServerListItemProps {
  server: VpnServer;
  isSelected: boolean;
  isConnected: boolean;
  onPress: () => void;
}

const getLoadColor = (load: number): string => {
  if (load < 50) return colors.loadLow;
  if (load < 80) return colors.loadMedium;
  return colors.loadHigh;
};

const getLatencyColor = (latency: number): string => {
  if (latency < 50) return colors.loadLow;
  if (latency < 100) return colors.loadMedium;
  return colors.loadHigh;
};

export const ServerListItem: React.FC<ServerListItemProps> = ({
  server,
  isSelected,
  isConnected,
  onPress,
}) => {
  const loadColor = getLoadColor(server.load);
  const latencyColor = getLatencyColor(server.latency);

  return (
    <TouchableOpacity
      onPress={onPress}
      activeOpacity={0.7}
      style={[
        styles.container,
        isSelected && styles.selectedContainer,
        isConnected && styles.connectedContainer,
      ]}>
      {/* Flag & Name */}
      <View style={styles.leftSection}>
        <Text style={styles.flag}>{server.flag}</Text>
        <View style={styles.nameContainer}>
          <Text style={styles.name}>{server.name}</Text>
          <Text style={styles.city}>{server.city}</Text>
        </View>
      </View>

      {/* Stats */}
      <View style={styles.rightSection}>
        {/* Latency */}
        <View style={styles.statItem}>
          <Icon name="signal" size={14} color={latencyColor} />
          <Text style={[styles.statText, {color: latencyColor}]}>
            {server.latency}ms
          </Text>
        </View>

        {/* Load */}
        <View style={styles.statItem}>
          <View style={[styles.loadBar, {backgroundColor: colors.surfaceLight}]}>
            <View
              style={[
                styles.loadFill,
                {
                  width: `${server.load}%`,
                  backgroundColor: loadColor,
                },
              ]}
            />
          </View>
          <Text style={[styles.statText, {color: loadColor}]}>
            {server.load}%
          </Text>
        </View>

        {/* Premium badge */}
        {server.isPremium && (
          <View style={styles.premiumBadge}>
            <Icon name="crown" size={12} color={colors.warning} />
          </View>
        )}

        {/* Connected indicator */}
        {isConnected && (
          <Icon name="check-circle" size={20} color={colors.connected} />
        )}

        {/* Chevron */}
        {!isConnected && (
          <Icon name="chevron-right" size={20} color={colors.textTertiary} />
        )}
      </View>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: colors.surface,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
    borderRadius: borderRadius.md,
    marginBottom: spacing.sm,
  },
  selectedContainer: {
    borderWidth: 1,
    borderColor: colors.primary,
  },
  connectedContainer: {
    borderWidth: 1,
    borderColor: colors.connected,
    backgroundColor: 'rgba(16, 185, 129, 0.1)',
  },
  leftSection: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  flag: {
    fontSize: 28,
    marginRight: spacing.md,
  },
  nameContainer: {
    flex: 1,
  },
  name: {
    ...typography.bodyLarge,
    color: colors.textPrimary,
    fontWeight: '600',
  },
  city: {
    ...typography.bodySmall,
    color: colors.textSecondary,
  },
  rightSection: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
  },
  statItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.xs,
  },
  statText: {
    ...typography.labelSmall,
  },
  loadBar: {
    width: 40,
    height: 4,
    borderRadius: 2,
    overflow: 'hidden',
  },
  loadFill: {
    height: '100%',
    borderRadius: 2,
  },
  premiumBadge: {
    backgroundColor: 'rgba(245, 158, 11, 0.2)',
    padding: spacing.xs,
    borderRadius: borderRadius.xs,
  },
});
