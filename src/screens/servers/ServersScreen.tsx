import React, {useState, useMemo, useCallback} from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TextInput,
  TouchableOpacity,
  StatusBar,
  Alert,
} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';
import {ServerListItem} from '../../components';
import {useVpnStore} from '../../stores';
import {colors, typography, spacing, borderRadius} from '../../theme';
import type {MainTabScreenProps} from '../../types/navigation';
import type {VpnServer} from '../../types/vpn';
import {VpnService} from '../../services/VpnService';
import {VPN_SERVERS, isServerConfigured} from '../../config/servers';

type Props = MainTabScreenProps<'Servers'>;

type SortOption = 'name' | 'latency' | 'load';

export const ServersScreen: React.FC<Props> = ({navigation}) => {
  const {vpnState, selectedServer, selectServer, setVpnStatus, setConnectedServer} =
    useVpnStore();
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState<SortOption>('latency');

  const filteredServers = useMemo(() => {
    let servers = VPN_SERVERS.filter(
      server =>
        server.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        server.city.toLowerCase().includes(searchQuery.toLowerCase()),
    );

    // Sort servers
    servers.sort((a, b) => {
      switch (sortBy) {
        case 'name':
          return a.name.localeCompare(b.name);
        case 'latency':
          return a.latency - b.latency;
        case 'load':
          return a.load - b.load;
        default:
          return 0;
      }
    });

    return servers;
  }, [searchQuery, sortBy]);

  const handleServerPress = useCallback(
    async (server: VpnServer) => {
      // Check if server is properly configured
      if (!isServerConfigured(server)) {
        Alert.alert(
          'Server Not Configured',
          'This server is not configured yet. Please update src/config/servers.ts with your WireGuard server details.\n\nSee WIREGUARD_SETUP.md for instructions.',
          [{text: 'OK'}],
        );
        return;
      }

      selectServer(server);

      // If not connected or already connected, connect to the selected server
      if (vpnState.status === 'idle' || vpnState.status === 'connected') {
        // Disconnect first if already connected
        if (vpnState.status === 'connected') {
          setVpnStatus('disconnecting');
          await VpnService.disconnect();
        }

        setVpnStatus('connecting');

        try {
          const success = await VpnService.connect(server);
          if (success) {
            setConnectedServer(server);
            navigation.navigate('Home');
          } else {
            setVpnStatus('error');
            Alert.alert('Connection Failed', 'Failed to connect to the VPN server.');
          }
        } catch (error: any) {
          setVpnStatus('error');
          Alert.alert(
            'Connection Error',
            error.message || 'An error occurred while connecting.',
          );
        }
      }
    },
    [vpnState.status, selectServer, setVpnStatus, setConnectedServer, navigation],
  );

  const renderSortButton = (option: SortOption, label: string) => (
    <TouchableOpacity
      style={[styles.sortButton, sortBy === option && styles.sortButtonActive]}
      onPress={() => setSortBy(option)}>
      <Text
        style={[
          styles.sortButtonText,
          sortBy === option && styles.sortButtonTextActive,
        ]}>
        {label}
      </Text>
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor={colors.background} />

      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Servers</Text>
        <Text style={styles.serverCount}>{VPN_SERVERS.length} locations</Text>
      </View>

      {/* Search Bar */}
      <View style={styles.searchContainer}>
        <Icon name="magnify" size={20} color={colors.textTertiary} />
        <TextInput
          style={styles.searchInput}
          placeholder="Search servers..."
          placeholderTextColor={colors.textTertiary}
          value={searchQuery}
          onChangeText={setSearchQuery}
        />
        {searchQuery.length > 0 && (
          <TouchableOpacity onPress={() => setSearchQuery('')}>
            <Icon name="close-circle" size={18} color={colors.textTertiary} />
          </TouchableOpacity>
        )}
      </View>

      {/* Sort Options */}
      <View style={styles.sortContainer}>
        <Text style={styles.sortLabel}>Sort by:</Text>
        {renderSortButton('latency', 'Fastest')}
        {renderSortButton('load', 'Load')}
        {renderSortButton('name', 'Name')}
      </View>

      {/* Server List */}
      <FlatList
        data={filteredServers}
        keyExtractor={item => item.id}
        contentContainerStyle={styles.listContent}
        showsVerticalScrollIndicator={false}
        renderItem={({item}) => (
          <ServerListItem
            server={item}
            isSelected={selectedServer?.id === item.id}
            isConnected={vpnState.server?.id === item.id}
            onPress={() => handleServerPress(item)}
          />
        )}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Icon name="server-off" size={48} color={colors.textTertiary} />
            <Text style={styles.emptyText}>No servers found</Text>
          </View>
        }
      />
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
  serverCount: {
    ...typography.bodyMedium,
    color: colors.textSecondary,
    marginTop: spacing.xs,
  },
  searchContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    marginHorizontal: spacing.lg,
    paddingHorizontal: spacing.md,
    borderRadius: borderRadius.md,
    marginBottom: spacing.md,
  },
  searchInput: {
    flex: 1,
    ...typography.bodyMedium,
    color: colors.textPrimary,
    paddingVertical: spacing.md,
    paddingHorizontal: spacing.sm,
  },
  sortContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing.lg,
    marginBottom: spacing.md,
    gap: spacing.sm,
  },
  sortLabel: {
    ...typography.labelMedium,
    color: colors.textSecondary,
  },
  sortButton: {
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: borderRadius.sm,
    backgroundColor: colors.surface,
  },
  sortButtonActive: {
    backgroundColor: colors.primary,
  },
  sortButtonText: {
    ...typography.labelSmall,
    color: colors.textSecondary,
  },
  sortButtonTextActive: {
    color: colors.background,
  },
  listContent: {
    paddingHorizontal: spacing.lg,
    paddingBottom: spacing.huge,
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: spacing.huge,
  },
  emptyText: {
    ...typography.bodyLarge,
    color: colors.textTertiary,
    marginTop: spacing.md,
  },
});
