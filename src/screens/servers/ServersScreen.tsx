import React, {useState, useMemo} from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TextInput,
  TouchableOpacity,
  StatusBar,
} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';
import {ServerListItem} from '../../components';
import {useVpnStore} from '../../stores';
import {colors, typography, spacing, borderRadius} from '../../theme';
import type {MainTabScreenProps} from '../../types/navigation';
import type {VpnServer} from '../../types/vpn';

type Props = MainTabScreenProps<'Servers'>;

// Mock server data
const MOCK_SERVERS: VpnServer[] = [
  {
    id: '1',
    name: 'Germany',
    country: 'Germany',
    countryCode: 'DE',
    city: 'Frankfurt',
    ipAddress: '185.244.214.1',
    port: 51820,
    protocol: 'wireguard',
    latency: 25,
    load: 45,
    isPremium: false,
    flag: '🇩🇪',
  },
  {
    id: '2',
    name: 'Netherlands',
    country: 'Netherlands',
    countryCode: 'NL',
    city: 'Amsterdam',
    ipAddress: '185.244.214.2',
    port: 51820,
    protocol: 'wireguard',
    latency: 30,
    load: 62,
    isPremium: false,
    flag: '🇳🇱',
  },
  {
    id: '3',
    name: 'United States',
    country: 'United States',
    countryCode: 'US',
    city: 'New York',
    ipAddress: '185.244.214.3',
    port: 51820,
    protocol: 'wireguard',
    latency: 85,
    load: 78,
    isPremium: false,
    flag: '🇺🇸',
  },
  {
    id: '4',
    name: 'United Kingdom',
    country: 'United Kingdom',
    countryCode: 'GB',
    city: 'London',
    ipAddress: '185.244.214.4',
    port: 51820,
    protocol: 'wireguard',
    latency: 35,
    load: 55,
    isPremium: false,
    flag: '🇬🇧',
  },
  {
    id: '5',
    name: 'Japan',
    country: 'Japan',
    countryCode: 'JP',
    city: 'Tokyo',
    ipAddress: '185.244.214.5',
    port: 51820,
    protocol: 'wireguard',
    latency: 120,
    load: 40,
    isPremium: true,
    flag: '🇯🇵',
  },
  {
    id: '6',
    name: 'Singapore',
    country: 'Singapore',
    countryCode: 'SG',
    city: 'Singapore',
    ipAddress: '185.244.214.6',
    port: 51820,
    protocol: 'wireguard',
    latency: 95,
    load: 35,
    isPremium: true,
    flag: '🇸🇬',
  },
  {
    id: '7',
    name: 'Australia',
    country: 'Australia',
    countryCode: 'AU',
    city: 'Sydney',
    ipAddress: '185.244.214.7',
    port: 51820,
    protocol: 'wireguard',
    latency: 180,
    load: 25,
    isPremium: false,
    flag: '🇦🇺',
  },
  {
    id: '8',
    name: 'Canada',
    country: 'Canada',
    countryCode: 'CA',
    city: 'Toronto',
    ipAddress: '185.244.214.8',
    port: 51820,
    protocol: 'wireguard',
    latency: 90,
    load: 50,
    isPremium: false,
    flag: '🇨🇦',
  },
  {
    id: '9',
    name: 'Turkey',
    country: 'Turkey',
    countryCode: 'TR',
    city: 'Istanbul',
    ipAddress: '185.244.214.9',
    port: 51820,
    protocol: 'wireguard',
    latency: 45,
    load: 30,
    isPremium: false,
    flag: '🇹🇷',
  },
];

type SortOption = 'name' | 'latency' | 'load';

export const ServersScreen: React.FC<Props> = ({navigation}) => {
  const {vpnState, selectedServer, selectServer, setVpnStatus, setConnectedServer} =
    useVpnStore();
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState<SortOption>('latency');

  const filteredServers = useMemo(() => {
    let servers = MOCK_SERVERS.filter(
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

  const handleServerPress = (server: VpnServer) => {
    selectServer(server);

    // If not connected, connect to the selected server
    if (vpnState.status === 'idle') {
      setVpnStatus('connecting');
      setTimeout(() => {
        setConnectedServer(server);
        navigation.navigate('Home');
      }, 2000);
    } else if (vpnState.status === 'connected') {
      // Switch server
      setVpnStatus('connecting');
      setTimeout(() => {
        setConnectedServer(server);
      }, 1500);
    }
  };

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
        <Text style={styles.serverCount}>{MOCK_SERVERS.length} locations</Text>
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
