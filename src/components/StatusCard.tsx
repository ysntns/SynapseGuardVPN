import React from 'react';
import {View, Text, StyleSheet} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';
import {colors, typography, spacing, borderRadius} from '../theme';
import type {VpnStateType, VpnServer} from '../types/vpn';

interface StatusCardProps {
  status: VpnStateType;
  server?: VpnServer | null;
  duration?: string;
}

const getStatusText = (status: VpnStateType): string => {
  switch (status) {
    case 'connected':
      return 'Protected';
    case 'connecting':
      return 'Connecting...';
    case 'disconnecting':
      return 'Disconnecting...';
    case 'error':
      return 'Connection Error';
    default:
      return 'Not Protected';
  }
};

const getStatusColor = (status: VpnStateType): string => {
  switch (status) {
    case 'connected':
      return colors.connected;
    case 'connecting':
    case 'disconnecting':
      return colors.connecting;
    case 'error':
      return colors.errorStatus;
    default:
      return colors.disconnected;
  }
};

const getStatusIcon = (status: VpnStateType): string => {
  switch (status) {
    case 'connected':
      return 'shield-check';
    case 'connecting':
    case 'disconnecting':
      return 'shield-sync';
    case 'error':
      return 'shield-alert';
    default:
      return 'shield-off';
  }
};

export const StatusCard: React.FC<StatusCardProps> = ({
  status,
  server,
  duration,
}) => {
  const statusColor = getStatusColor(status);
  const statusText = getStatusText(status);
  const statusIcon = getStatusIcon(status);

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Icon name={statusIcon} size={24} color={statusColor} />
        <Text style={[styles.statusText, {color: statusColor}]}>
          {statusText}
        </Text>
      </View>

      {server && status === 'connected' && (
        <View style={styles.serverInfo}>
          <Text style={styles.serverFlag}>{server.flag}</Text>
          <View style={styles.serverDetails}>
            <Text style={styles.serverName}>{server.name}</Text>
            <Text style={styles.serverCity}>{server.city}</Text>
          </View>
          {duration && (
            <View style={styles.durationContainer}>
              <Icon name="clock-outline" size={14} color={colors.textSecondary} />
              <Text style={styles.duration}>{duration}</Text>
            </View>
          )}
        </View>
      )}

      {!server && status === 'idle' && (
        <Text style={styles.hint}>Select a server to connect</Text>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.lg,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
  },
  statusText: {
    ...typography.h4,
    fontWeight: '600',
  },
  serverInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: spacing.md,
    paddingTop: spacing.md,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  serverFlag: {
    fontSize: 32,
    marginRight: spacing.md,
  },
  serverDetails: {
    flex: 1,
  },
  serverName: {
    ...typography.bodyLarge,
    color: colors.textPrimary,
    fontWeight: '500',
  },
  serverCity: {
    ...typography.bodySmall,
    color: colors.textSecondary,
  },
  durationContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.xs,
  },
  duration: {
    ...typography.bodySmall,
    color: colors.textSecondary,
  },
  hint: {
    ...typography.bodyMedium,
    color: colors.textTertiary,
    marginTop: spacing.sm,
  },
});
