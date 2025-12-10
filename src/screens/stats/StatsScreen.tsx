import React from 'react';
import {View, Text, StyleSheet, ScrollView, StatusBar} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';
import {StatsCard} from '../../components';
import {useVpnStore} from '../../stores';
import {colors, typography, spacing, borderRadius} from '../../theme';
import type {MainTabScreenProps} from '../../types/navigation';

type Props = MainTabScreenProps<'Stats'>;

const formatBytes = (bytes: number): string => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
};

const formatSpeed = (bps: number): string => {
  if (bps < 1024) return `${bps} B/s`;
  if (bps < 1024 * 1024) return `${(bps / 1024).toFixed(1)} KB/s`;
  return `${(bps / (1024 * 1024)).toFixed(1)} MB/s`;
};

const formatDuration = (ms: number): string => {
  const seconds = Math.floor(ms / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);

  if (days > 0) return `${days}d ${hours % 24}h`;
  if (hours > 0) return `${hours}h ${minutes % 60}m`;
  if (minutes > 0) return `${minutes}m ${seconds % 60}s`;
  return `${seconds}s`;
};

export const StatsScreen: React.FC<Props> = () => {
  const {vpnState, stats} = useVpnStore();

  const isConnected = vpnState.status === 'connected';
  const duration = vpnState.connectedAt
    ? Date.now() - vpnState.connectedAt
    : 0;

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor={colors.background} />

      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Statistics</Text>
      </View>

      <ScrollView
        style={styles.content}
        contentContainerStyle={styles.contentContainer}
        showsVerticalScrollIndicator={false}>
        {/* Connection Status */}
        <View style={styles.statusCard}>
          <View
            style={[
              styles.statusIndicator,
              {backgroundColor: isConnected ? colors.connected : colors.disconnected},
            ]}
          />
          <View style={styles.statusInfo}>
            <Text style={styles.statusLabel}>
              {isConnected ? 'Connected' : 'Disconnected'}
            </Text>
            {isConnected && vpnState.server && (
              <Text style={styles.statusServer}>
                {vpnState.server.flag} {vpnState.server.name}
              </Text>
            )}
          </View>
          {isConnected && (
            <View style={styles.durationBadge}>
              <Icon name="clock-outline" size={14} color={colors.primary} />
              <Text style={styles.durationText}>{formatDuration(duration)}</Text>
            </View>
          )}
        </View>

        {/* Speed Stats */}
        <Text style={styles.sectionTitle}>Current Speed</Text>
        <View style={styles.statsRow}>
          <StatsCard
            icon="download"
            label="Download"
            value={isConnected ? formatSpeed(stats.downloadSpeedBps) : '-'}
            color={colors.success}
          />
          <View style={{width: spacing.md}} />
          <StatsCard
            icon="upload"
            label="Upload"
            value={isConnected ? formatSpeed(stats.uploadSpeedBps) : '-'}
            color={colors.info}
          />
        </View>

        {/* Data Usage */}
        <Text style={styles.sectionTitle}>Session Data</Text>
        <View style={styles.statsRow}>
          <StatsCard
            icon="arrow-down-bold"
            label="Downloaded"
            value={isConnected ? formatBytes(stats.bytesReceived) : '-'}
            color={colors.success}
          />
          <View style={{width: spacing.md}} />
          <StatsCard
            icon="arrow-up-bold"
            label="Uploaded"
            value={isConnected ? formatBytes(stats.bytesSent) : '-'}
            color={colors.info}
          />
        </View>

        {/* Packets */}
        <Text style={styles.sectionTitle}>Packets</Text>
        <View style={styles.statsRow}>
          <StatsCard
            icon="package-down"
            label="Received"
            value={isConnected ? stats.packetsReceived.toLocaleString() : '-'}
            color={colors.primary}
          />
          <View style={{width: spacing.md}} />
          <StatsCard
            icon="package-up"
            label="Sent"
            value={isConnected ? stats.packetsSent.toLocaleString() : '-'}
            color={colors.warning}
          />
        </View>

        {/* Server Info */}
        {isConnected && vpnState.server && (
          <>
            <Text style={styles.sectionTitle}>Server Info</Text>
            <View style={styles.serverInfoCard}>
              <View style={styles.serverInfoRow}>
                <Text style={styles.serverInfoLabel}>Location</Text>
                <Text style={styles.serverInfoValue}>
                  {vpnState.server.flag} {vpnState.server.city}, {vpnState.server.country}
                </Text>
              </View>
              <View style={styles.serverInfoRow}>
                <Text style={styles.serverInfoLabel}>Protocol</Text>
                <Text style={styles.serverInfoValue}>
                  {vpnState.server.protocol.toUpperCase()}
                </Text>
              </View>
              <View style={styles.serverInfoRow}>
                <Text style={styles.serverInfoLabel}>Latency</Text>
                <Text style={styles.serverInfoValue}>{vpnState.server.latency} ms</Text>
              </View>
              <View style={styles.serverInfoRow}>
                <Text style={styles.serverInfoLabel}>Server Load</Text>
                <Text style={styles.serverInfoValue}>{vpnState.server.load}%</Text>
              </View>
            </View>
          </>
        )}

        {/* Not Connected Message */}
        {!isConnected && (
          <View style={styles.notConnectedCard}>
            <Icon name="chart-line-variant" size={48} color={colors.textTertiary} />
            <Text style={styles.notConnectedTitle}>No Active Connection</Text>
            <Text style={styles.notConnectedText}>
              Connect to a VPN server to view real-time statistics
            </Text>
          </View>
        )}
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  header: {
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.xl,
    paddingBottom: spacing.md,
  },
  headerTitle: {
    ...typography.h2,
    color: colors.textPrimary,
  },
  content: {
    flex: 1,
  },
  contentContainer: {
    paddingHorizontal: spacing.lg,
    paddingBottom: spacing.huge,
  },
  statusCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    padding: spacing.lg,
    borderRadius: borderRadius.lg,
    marginBottom: spacing.xl,
  },
  statusIndicator: {
    width: 12,
    height: 12,
    borderRadius: 6,
    marginRight: spacing.md,
  },
  statusInfo: {
    flex: 1,
  },
  statusLabel: {
    ...typography.bodyLarge,
    color: colors.textPrimary,
    fontWeight: '600',
  },
  statusServer: {
    ...typography.bodySmall,
    color: colors.textSecondary,
    marginTop: spacing.xs,
  },
  durationBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.backgroundTertiary,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: borderRadius.sm,
    gap: spacing.xs,
  },
  durationText: {
    ...typography.labelMedium,
    color: colors.primary,
  },
  sectionTitle: {
    ...typography.overline,
    color: colors.textTertiary,
    marginBottom: spacing.md,
    marginTop: spacing.lg,
  },
  statsRow: {
    flexDirection: 'row',
  },
  serverInfoCard: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.lg,
  },
  serverInfoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  serverInfoLabel: {
    ...typography.bodyMedium,
    color: colors.textSecondary,
  },
  serverInfoValue: {
    ...typography.bodyMedium,
    color: colors.textPrimary,
    fontWeight: '500',
  },
  notConnectedCard: {
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.xxxl,
    marginTop: spacing.xl,
  },
  notConnectedTitle: {
    ...typography.h4,
    color: colors.textSecondary,
    marginTop: spacing.lg,
    marginBottom: spacing.sm,
  },
  notConnectedText: {
    ...typography.bodyMedium,
    color: colors.textTertiary,
    textAlign: 'center',
  },
});
