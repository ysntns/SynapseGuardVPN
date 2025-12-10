import React from 'react';
import {StyleSheet, View} from 'react-native';
import {createBottomTabNavigator} from '@react-navigation/bottom-tabs';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';
import {
  HomeScreen,
  ServersScreen,
  StatsScreen,
  SettingsScreen,
} from '../screens';
import type {MainTabParamList} from '../types/navigation';
import {colors, typography, spacing} from '../theme';

const Tab = createBottomTabNavigator<MainTabParamList>();

const getTabIcon = (routeName: string, focused: boolean) => {
  let iconName: string;

  switch (routeName) {
    case 'Home':
      iconName = focused ? 'shield-check' : 'shield-outline';
      break;
    case 'Servers':
      iconName = focused ? 'server' : 'server';
      break;
    case 'Stats':
      iconName = focused ? 'chart-line' : 'chart-line-variant';
      break;
    case 'Settings':
      iconName = focused ? 'cog' : 'cog-outline';
      break;
    default:
      iconName = 'circle';
  }

  return iconName;
};

export const MainTabNavigator: React.FC = () => {
  return (
    <Tab.Navigator
      screenOptions={({route}) => ({
        headerShown: false,
        tabBarStyle: styles.tabBar,
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.textTertiary,
        tabBarLabelStyle: styles.tabBarLabel,
        tabBarIcon: ({focused, color, size}) => {
          const iconName = getTabIcon(route.name, focused);
          return (
            <View style={focused ? styles.activeIconContainer : undefined}>
              <Icon name={iconName} size={24} color={color} />
            </View>
          );
        },
      })}>
      <Tab.Screen
        name="Home"
        component={HomeScreen}
        options={{tabBarLabel: 'Home'}}
      />
      <Tab.Screen
        name="Servers"
        component={ServersScreen}
        options={{tabBarLabel: 'Servers'}}
      />
      <Tab.Screen
        name="Stats"
        component={StatsScreen}
        options={{tabBarLabel: 'Stats'}}
      />
      <Tab.Screen
        name="Settings"
        component={SettingsScreen}
        options={{tabBarLabel: 'Settings'}}
      />
    </Tab.Navigator>
  );
};

const styles = StyleSheet.create({
  tabBar: {
    backgroundColor: colors.backgroundSecondary,
    borderTopColor: colors.border,
    borderTopWidth: 1,
    height: 60,
    paddingBottom: spacing.sm,
    paddingTop: spacing.sm,
  },
  tabBarLabel: {
    ...typography.labelSmall,
    marginTop: spacing.xs,
  },
  activeIconContainer: {
    backgroundColor: `${colors.primary}20`,
    padding: spacing.xs,
    borderRadius: 8,
  },
});
