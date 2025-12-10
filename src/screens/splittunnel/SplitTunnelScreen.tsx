import React, {useState, useMemo} from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TextInput,
  TouchableOpacity,
  Switch,
  StatusBar,
} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';
import {useSettingsStore} from '../../stores';
import {colors, typography, spacing, borderRadius} from '../../theme';
import type {RootStackScreenProps} from '../../types/navigation';
import type {AppInfo} from '../../types/vpn';

type Props = RootStackScreenProps<'SplitTunnel'>;

// Mock installed apps
const MOCK_APPS: AppInfo[] = [
  {packageName: 'com.google.chrome', appName: 'Chrome', isExcluded: false},
  {packageName: 'com.spotify.music', appName: 'Spotify', isExcluded: false},
  {packageName: 'com.netflix.mediaclient', appName: 'Netflix', isExcluded: false},
  {packageName: 'com.whatsapp', appName: 'WhatsApp', isExcluded: false},
  {packageName: 'com.instagram.android', appName: 'Instagram', isExcluded: false},
  {packageName: 'com.twitter.android', appName: 'Twitter', isExcluded: false},
  {packageName: 'com.google.android.youtube', appName: 'YouTube', isExcluded: false},
  {packageName: 'com.facebook.katana', appName: 'Facebook', isExcluded: false},
  {packageName: 'com.discord', appName: 'Discord', isExcluded: false},
  {packageName: 'com.amazon.mShop.android.shopping', appName: 'Amazon', isExcluded: false},
  {packageName: 'com.paypal.android.p2pmobile', appName: 'PayPal', isExcluded: false},
  {packageName: 'com.google.android.apps.maps', appName: 'Google Maps', isExcluded: false},
  {packageName: 'com.ubercab', appName: 'Uber', isExcluded: false},
  {packageName: 'com.zhiliaoapp.musically', appName: 'TikTok', isExcluded: false},
  {packageName: 'org.telegram.messenger', appName: 'Telegram', isExcluded: false},
];

export const SplitTunnelScreen: React.FC<Props> = ({navigation}) => {
  const {settings, addExcludedApp, removeExcludedApp} = useSettingsStore();
  const [searchQuery, setSearchQuery] = useState('');

  const apps = useMemo(() => {
    return MOCK_APPS.filter(app =>
      app.appName.toLowerCase().includes(searchQuery.toLowerCase()),
    ).map(app => ({
      ...app,
      isExcluded: settings.excludedApps.includes(app.packageName),
    }));
  }, [searchQuery, settings.excludedApps]);

  const handleToggle = (packageName: string, isExcluded: boolean) => {
    if (isExcluded) {
      removeExcludedApp(packageName);
    } else {
      addExcludedApp(packageName);
    }
  };

  const excludedCount = settings.excludedApps.length;

  const renderAppItem = ({item}: {item: AppInfo}) => (
    <View style={styles.appItem}>
      <View style={styles.appIconPlaceholder}>
        <Icon name="android" size={24} color={colors.primary} />
      </View>
      <View style={styles.appInfo}>
        <Text style={styles.appName}>{item.appName}</Text>
        <Text style={styles.packageName} numberOfLines={1}>
          {item.packageName}
        </Text>
      </View>
      <Switch
        value={item.isExcluded}
        onValueChange={() => handleToggle(item.packageName, item.isExcluded)}
        trackColor={{
          false: colors.surfaceLight,
          true: `${colors.primary}80`,
        }}
        thumbColor={item.isExcluded ? colors.primary : colors.textTertiary}
      />
    </View>
  );

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor={colors.background} />

      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity
          style={styles.backButton}
          onPress={() => navigation.goBack()}>
          <Icon name="arrow-left" size={24} color={colors.textPrimary} />
        </TouchableOpacity>
        <View style={styles.headerContent}>
          <Text style={styles.headerTitle}>Split Tunneling</Text>
          <Text style={styles.headerSubtitle}>
            {excludedCount} app{excludedCount !== 1 ? 's' : ''} excluded
          </Text>
        </View>
      </View>

      {/* Info Card */}
      <View style={styles.infoCard}>
        <Icon name="information" size={20} color={colors.info} />
        <Text style={styles.infoText}>
          Excluded apps will bypass the VPN and use your regular internet connection.
        </Text>
      </View>

      {/* Search Bar */}
      <View style={styles.searchContainer}>
        <Icon name="magnify" size={20} color={colors.textTertiary} />
        <TextInput
          style={styles.searchInput}
          placeholder="Search apps..."
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

      {/* Quick Actions */}
      <View style={styles.quickActions}>
        <TouchableOpacity
          style={styles.quickActionButton}
          onPress={() => {
            MOCK_APPS.forEach(app => {
              if (!settings.excludedApps.includes(app.packageName)) {
                addExcludedApp(app.packageName);
              }
            });
          }}>
          <Text style={styles.quickActionText}>Exclude All</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.quickActionButton}
          onPress={() => {
            settings.excludedApps.forEach(pkg => removeExcludedApp(pkg));
          }}>
          <Text style={styles.quickActionText}>Include All</Text>
        </TouchableOpacity>
      </View>

      {/* App List */}
      <FlatList
        data={apps}
        keyExtractor={item => item.packageName}
        renderItem={renderAppItem}
        contentContainerStyle={styles.listContent}
        showsVerticalScrollIndicator={false}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Icon name="apps" size={48} color={colors.textTertiary} />
            <Text style={styles.emptyText}>No apps found</Text>
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
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.xl,
    paddingBottom: spacing.md,
  },
  backButton: {
    padding: spacing.sm,
    marginRight: spacing.sm,
    marginLeft: -spacing.sm,
  },
  headerContent: {
    flex: 1,
  },
  headerTitle: {
    ...typography.h3,
    color: colors.textPrimary,
  },
  headerSubtitle: {
    ...typography.bodySmall,
    color: colors.textSecondary,
    marginTop: spacing.xs,
  },
  infoCard: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    backgroundColor: 'rgba(59, 130, 246, 0.1)',
    marginHorizontal: spacing.lg,
    padding: spacing.md,
    borderRadius: borderRadius.md,
    gap: spacing.sm,
    marginBottom: spacing.md,
  },
  infoText: {
    ...typography.bodySmall,
    color: colors.info,
    flex: 1,
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
  quickActions: {
    flexDirection: 'row',
    paddingHorizontal: spacing.lg,
    gap: spacing.md,
    marginBottom: spacing.md,
  },
  quickActionButton: {
    flex: 1,
    backgroundColor: colors.surface,
    paddingVertical: spacing.sm,
    borderRadius: borderRadius.sm,
    alignItems: 'center',
  },
  quickActionText: {
    ...typography.labelMedium,
    color: colors.primary,
  },
  listContent: {
    paddingHorizontal: spacing.lg,
    paddingBottom: spacing.huge,
  },
  appItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    padding: spacing.md,
    borderRadius: borderRadius.md,
    marginBottom: spacing.sm,
  },
  appIconPlaceholder: {
    width: 44,
    height: 44,
    borderRadius: borderRadius.sm,
    backgroundColor: colors.backgroundTertiary,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.md,
  },
  appInfo: {
    flex: 1,
  },
  appName: {
    ...typography.bodyLarge,
    color: colors.textPrimary,
    fontWeight: '500',
  },
  packageName: {
    ...typography.caption,
    color: colors.textTertiary,
    marginTop: spacing.xs,
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
