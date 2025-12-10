import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  StatusBar,
  Alert,
} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';
import {
  SettingsToggleItem,
  SettingsNavigationItem,
} from '../../components/SettingsToggleItem';
import {useSettingsStore, useVpnStore} from '../../stores';
import {colors, typography, spacing, borderRadius} from '../../theme';
import type {MainTabScreenProps} from '../../types/navigation';

type Props = MainTabScreenProps<'Settings'>;

export const SettingsScreen: React.FC<Props> = ({navigation}) => {
  const {settings, toggleKillSwitch, toggleSplitTunneling, toggleAutoConnect, toggleNotifications} =
    useSettingsStore();
  const {vpnState} = useVpnStore();

  const isConnected = vpnState.status === 'connected';

  const handleProtocolPress = () => {
    Alert.alert(
      'Select Protocol',
      'Choose your preferred VPN protocol',
      [
        {text: 'WireGuard (Recommended)', onPress: () => {}},
        {text: 'OpenVPN', onPress: () => {}},
        {text: 'Cancel', style: 'cancel'},
      ],
    );
  };

  const handleDnsPress = () => {
    Alert.alert(
      'Custom DNS',
      'Configure custom DNS servers',
      [
        {text: 'Cloudflare (1.1.1.1)', onPress: () => {}},
        {text: 'Google (8.8.8.8)', onPress: () => {}},
        {text: 'Custom...', onPress: () => {}},
        {text: 'Cancel', style: 'cancel'},
      ],
    );
  };

  const handleSplitTunnelPress = () => {
    navigation.getParent()?.navigate('SplitTunnel');
  };

  const handleLanguagePress = () => {
    Alert.alert(
      'Language',
      'Select your preferred language',
      [
        {text: 'English', onPress: () => {}},
        {text: 'Türkçe', onPress: () => {}},
        {text: 'Deutsch', onPress: () => {}},
        {text: 'Français', onPress: () => {}},
        {text: 'Español', onPress: () => {}},
        {text: 'Cancel', style: 'cancel'},
      ],
    );
  };

  const handleAboutPress = () => {
    Alert.alert(
      'About SynapseGuard',
      'Version 1.0.0\n\nSecure VPN for everyone.\n\n© 2024 SynapseGuard',
    );
  };

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor={colors.background} />

      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Settings</Text>
      </View>

      <ScrollView
        style={styles.content}
        contentContainerStyle={styles.contentContainer}
        showsVerticalScrollIndicator={false}>
        {/* Connection Section */}
        <Text style={styles.sectionTitle}>Connection</Text>
        <View style={styles.section}>
          <SettingsToggleItem
            icon="power"
            title="Auto Connect"
            description="Connect automatically when app starts"
            value={settings.autoConnect}
            onValueChange={toggleAutoConnect}
          />
          <SettingsNavigationItem
            icon="protocol"
            title="Protocol"
            description="VPN protocol to use"
            value={settings.preferredProtocol.toUpperCase()}
            onPress={handleProtocolPress}
          />
          <SettingsNavigationItem
            icon="dns"
            title="Custom DNS"
            description="Use custom DNS servers"
            value={settings.customDns[0]}
            onPress={handleDnsPress}
          />
        </View>

        {/* Security Section */}
        <Text style={styles.sectionTitle}>Security</Text>
        <View style={styles.section}>
          <SettingsToggleItem
            icon="shield-lock"
            title="Kill Switch"
            description="Block internet if VPN disconnects"
            value={settings.killSwitch}
            onValueChange={toggleKillSwitch}
            disabled={isConnected}
          />
          <SettingsToggleItem
            icon="source-branch"
            title="Split Tunneling"
            description="Exclude apps from VPN"
            value={settings.splitTunneling}
            onValueChange={toggleSplitTunneling}
            disabled={isConnected}
          />
          {settings.splitTunneling && (
            <SettingsNavigationItem
              icon="apps"
              title="Manage Apps"
              description="Select apps to exclude"
              value={`${settings.excludedApps.length} apps`}
              onPress={handleSplitTunnelPress}
            />
          )}
        </View>

        {/* General Section */}
        <Text style={styles.sectionTitle}>General</Text>
        <View style={styles.section}>
          <SettingsToggleItem
            icon="bell"
            title="Notifications"
            description="Show connection notifications"
            value={settings.notifications}
            onValueChange={toggleNotifications}
          />
          <SettingsNavigationItem
            icon="translate"
            title="Language"
            description="App language"
            value={settings.language.toUpperCase()}
            onPress={handleLanguagePress}
          />
        </View>

        {/* About Section */}
        <Text style={styles.sectionTitle}>About</Text>
        <View style={styles.section}>
          <SettingsNavigationItem
            icon="information"
            title="About SynapseGuard"
            description="Version, licenses, and more"
            onPress={handleAboutPress}
          />
          <SettingsNavigationItem
            icon="help-circle"
            title="Help & Support"
            description="FAQ and contact support"
            onPress={() => {}}
          />
          <SettingsNavigationItem
            icon="file-document"
            title="Privacy Policy"
            onPress={() => {}}
          />
          <SettingsNavigationItem
            icon="file-certificate"
            title="Terms of Service"
            onPress={() => {}}
          />
        </View>

        {/* Footer */}
        <View style={styles.footer}>
          <Text style={styles.footerText}>SynapseGuard VPN v1.0.0</Text>
          <Text style={styles.footerText}>© 2024 All rights reserved</Text>
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
  sectionTitle: {
    ...typography.overline,
    color: colors.textTertiary,
    marginTop: spacing.xl,
    marginBottom: spacing.md,
  },
  section: {
    backgroundColor: colors.backgroundSecondary,
    borderRadius: borderRadius.lg,
    overflow: 'hidden',
  },
  footer: {
    alignItems: 'center',
    marginTop: spacing.xxxl,
    paddingBottom: spacing.xl,
  },
  footerText: {
    ...typography.caption,
    color: colors.textTertiary,
    marginBottom: spacing.xs,
  },
});
