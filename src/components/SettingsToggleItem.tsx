import React from 'react';
import {View, Text, Switch, TouchableOpacity, StyleSheet} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';
import {colors, typography, spacing, borderRadius} from '../theme';

interface SettingsToggleItemProps {
  icon: string;
  title: string;
  description?: string;
  value: boolean;
  onValueChange: (value: boolean) => void;
  disabled?: boolean;
}

export const SettingsToggleItem: React.FC<SettingsToggleItemProps> = ({
  icon,
  title,
  description,
  value,
  onValueChange,
  disabled = false,
}) => {
  return (
    <View style={[styles.container, disabled && styles.disabled]}>
      <View style={styles.iconContainer}>
        <Icon
          name={icon}
          size={22}
          color={disabled ? colors.textDisabled : colors.primary}
        />
      </View>
      <View style={styles.content}>
        <Text style={[styles.title, disabled && styles.disabledText]}>
          {title}
        </Text>
        {description && (
          <Text style={[styles.description, disabled && styles.disabledText]}>
            {description}
          </Text>
        )}
      </View>
      <Switch
        value={value}
        onValueChange={onValueChange}
        disabled={disabled}
        trackColor={{
          false: colors.surfaceLight,
          true: `${colors.primary}80`,
        }}
        thumbColor={value ? colors.primary : colors.textTertiary}
      />
    </View>
  );
};

interface SettingsNavigationItemProps {
  icon: string;
  title: string;
  description?: string;
  value?: string;
  onPress: () => void;
  disabled?: boolean;
}

export const SettingsNavigationItem: React.FC<SettingsNavigationItemProps> = ({
  icon,
  title,
  description,
  value,
  onPress,
  disabled = false,
}) => {
  return (
    <TouchableOpacity
      style={[styles.container, disabled && styles.disabled]}
      onPress={onPress}
      disabled={disabled}
      activeOpacity={0.7}>
      <View style={styles.iconContainer}>
        <Icon
          name={icon}
          size={22}
          color={disabled ? colors.textDisabled : colors.primary}
        />
      </View>
      <View style={styles.content}>
        <Text style={[styles.title, disabled && styles.disabledText]}>
          {title}
        </Text>
        {description && (
          <Text style={[styles.description, disabled && styles.disabledText]}>
            {description}
          </Text>
        )}
      </View>
      <View style={styles.rightSection}>
        {value && <Text style={styles.value}>{value}</Text>}
        <Icon name="chevron-right" size={20} color={colors.textTertiary} />
      </View>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
    borderRadius: borderRadius.md,
    marginBottom: spacing.sm,
  },
  disabled: {
    opacity: 0.5,
  },
  iconContainer: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: colors.backgroundTertiary,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.md,
  },
  content: {
    flex: 1,
  },
  title: {
    ...typography.bodyLarge,
    color: colors.textPrimary,
    fontWeight: '500',
  },
  description: {
    ...typography.bodySmall,
    color: colors.textSecondary,
    marginTop: spacing.xs,
  },
  disabledText: {
    color: colors.textDisabled,
  },
  rightSection: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
  },
  value: {
    ...typography.bodyMedium,
    color: colors.textSecondary,
  },
});
