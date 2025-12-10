import React from 'react';
import {View, Text, StyleSheet} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';
import {colors, typography, spacing, borderRadius} from '../theme';

interface StatsCardProps {
  icon: string;
  label: string;
  value: string;
  subValue?: string;
  color?: string;
}

export const StatsCard: React.FC<StatsCardProps> = ({
  icon,
  label,
  value,
  subValue,
  color = colors.primary,
}) => {
  return (
    <View style={styles.container}>
      <View style={[styles.iconContainer, {backgroundColor: `${color}20`}]}>
        <Icon name={icon} size={24} color={color} />
      </View>
      <Text style={styles.label}>{label}</Text>
      <Text style={styles.value}>{value}</Text>
      {subValue && <Text style={styles.subValue}>{subValue}</Text>}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.lg,
    alignItems: 'center',
    flex: 1,
    minWidth: 100,
  },
  iconContainer: {
    width: 48,
    height: 48,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.sm,
  },
  label: {
    ...typography.labelSmall,
    color: colors.textSecondary,
    marginBottom: spacing.xs,
  },
  value: {
    ...typography.h4,
    color: colors.textPrimary,
  },
  subValue: {
    ...typography.caption,
    color: colors.textTertiary,
    marginTop: spacing.xs,
  },
});
