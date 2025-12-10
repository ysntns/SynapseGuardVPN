import React, {useCallback, useEffect, useState} from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  StatusBar,
} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';
import {
  CircularConnectionButton,
  StatusCard,
  StatsCard,
} from '../../components';
import {useVpnStore} from '../../stores';
import {colors, typography, spacing, borderRadius} from '../../theme';
import type {MainTabScreenProps} from '../../types/navigation';

type Props = MainTabScreenProps<'Home'>;

const formatDuration = (ms: number): string => {
  const seconds = Math.floor(ms / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);

  if (hours > 0) {
    return `${hours}h ${minutes % 60}m`;
  }
  if (minutes > 0) {
    return `${minutes}m ${seconds % 60}s`;
  }
  return `${seconds}s`;
};

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

export const HomeScreen: React.FC<Props> = ({navigation}) => {
  const {vpnState, stats, selectedServer, setVpnStatus, setConnectedServer, disconnect} =
    useVpnStore();
  const [duration, setDuration] = useState('00:00');

  // Update duration timer
  useEffect(() => {
    if (vpnState.status === 'connected' && vpnState.connectedAt) {
      const timer = setInterval(() => {
        const elapsed = Date.now() - vpnState.connectedAt!;
        setDuration(formatDuration(elapsed));
      }, 1000);
      return () => clearInterval(timer);
    }
  }, [vpnState.status, vpnState.connectedAt]);

  const handleConnectionToggle = useCallback(() => {
    if (vpnState.status === 'connected') {
      setVpnStatus('disconnecting');
      // Simulate disconnect
      setTimeout(() => {
        disconnect();
      }, 1000);
    } else if (vpnState.status === 'idle') {
      if (selectedServer) {
        setVpnStatus('connecting');
        // Simulate connect
        setTimeout(() => {
          setConnectedServer(selectedServer);
        }, 2000);
      } else {
        // Navigate to server selection
        navigation.navigate('Servers');
      }
    }
  }, [vpnState.status, selectedServer, setVpnStatus, disconnect, setConnectedServer, navigation]);

  const handleServerPress = useCallback(() => {
    navigation.navigate('Servers');
  }, [navigation]);

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor={colors.background} />

      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>SynapseGuard</Text>
        <TouchableOpacity style={styles.headerButton}>
          <Icon name="bell-outline" size={24} color={colors.textPrimary} />
        </TouchableOpacity>
      </View>

      <ScrollView
        style={styles.content}
        contentContainerStyle={styles.contentContainer}
        showsVerticalScrollIndicator={false}>
        {/* Status Card */}
        <StatusCard
          status={vpnState.status}
          server={vpnState.server || selectedServer}
          duration={vpnState.status === 'connected' ? duration : undefined}
        />

        {/* Connection Button */}
        <View style={styles.buttonContainer}>
          <CircularConnectionButton
            status={vpnState.status}
            onPress={handleConnectionToggle}
            size={180}
          />
          <Text style={styles.buttonHint}>
            {vpnState.status === 'idle'
              ? 'Tap to connect'
              : vpnState.status === 'connected'
              ? 'Tap to disconnect'
              : ''}
          </Text>
        </View>

        {/* Server Selection */}
        <TouchableOpacity
          style={styles.serverSelector}
          onPress={handleServerPress}
          activeOpacity={0.7}>
          <View style={styles.serverInfo}>
            {selectedServer ? (
              <>
                <Text style={styles.serverFlag}>{selectedServer.flag}</Text>
                <View>
                  <Text style={styles.serverName}>{selectedServer.name}</Text>
                  <Text style={styles.serverCity}>{selectedServer.city}</Text>
                </View>
              </>
            ) : (
              <>
                <Icon name="server" size={24} color={colors.textSecondary} />
                <Text style={styles.selectServerText}>Select Server</Text>
              </>
            )}
          </View>
          <Icon name="chevron-right" size={24} color={colors.textTertiary} />
        </TouchableOpacity>

        {/* Stats (only when connected) */}
        {vpnState.status === 'connected' && (
          <View style={styles.statsContainer}>
            <Text style={styles.sectionTitle}>Connection Stats</Text>
            <View style={styles.statsRow}>
              <StatsCard
                icon="download"
                label="Download"
                value={formatSpeed(stats.downloadSpeedBps)}
                subValue={formatBytes(stats.bytesReceived)}
                color={colors.success}
              />
              <View style={{width: spacing.md}} />
              <StatsCard
                icon="upload"
                label="Upload"
                value={formatSpeed(stats.uploadSpeedBps)}
                subValue={formatBytes(stats.bytesSent)}
                color={colors.info}
              />
            </View>
          </View>
        )}

        {/* Quick Actions */}
        <View style={styles.quickActions}>
          <TouchableOpacity
            style={styles.quickAction}
            onPress={() => navigation.navigate('Stats')}>
            <Icon name="chart-line" size={20} color={colors.primary} />
            <Text style={styles.quickActionText}>Statistics</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.quickAction}
            onPress={() => navigation.navigate('Settings')}>
            <Icon name="cog" size={20} color={colors.primary} />
            <Text style={styles.quickActionText}>Settings</Text>
          </TouchableOpacity>
        </View>
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
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
    paddingTop: spacing.xl,
  },
  headerTitle: {
    ...typography.h3,
    color: colors.textPrimary,
  },
  headerButton: {
    padding: spacing.sm,
  },
  content: {
    flex: 1,
  },
  contentContainer: {
    padding: spacing.lg,
    paddingBottom: spacing.huge,
  },
  buttonContainer: {
    alignItems: 'center',
    marginVertical: spacing.xxxl,
  },
  buttonHint: {
    ...typography.bodyMedium,
    color: colors.textTertiary,
    marginTop: spacing.lg,
  },
  serverSelector: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: colors.surface,
    padding: spacing.lg,
    borderRadius: borderRadius.lg,
    marginBottom: spacing.xl,
  },
  serverInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
  },
  serverFlag: {
    fontSize: 32,
  },
  serverName: {
    ...typography.bodyLarge,
    color: colors.textPrimary,
    fontWeight: '600',
  },
  serverCity: {
    ...typography.bodySmall,
    color: colors.textSecondary,
  },
  selectServerText: {
    ...typography.bodyLarge,
    color: colors.textSecondary,
  },
  statsContainer: {
    marginBottom: spacing.xl,
  },
  sectionTitle: {
    ...typography.labelLarge,
    color: colors.textSecondary,
    marginBottom: spacing.md,
  },
  statsRow: {
    flexDirection: 'row',
  },
  quickActions: {
    flexDirection: 'row',
    gap: spacing.md,
  },
  quickAction: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.sm,
    backgroundColor: colors.surface,
    padding: spacing.md,
    borderRadius: borderRadius.md,
  },
  quickActionText: {
    ...typography.labelMedium,
    color: colors.textPrimary,
  },
});
