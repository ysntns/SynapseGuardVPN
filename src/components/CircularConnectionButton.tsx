import React, {useEffect} from 'react';
import {
  View,
  TouchableOpacity,
  StyleSheet,
  ViewStyle,
} from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withRepeat,
  withTiming,
  withSequence,
  Easing,
  cancelAnimation,
} from 'react-native-reanimated';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';
import {colors, spacing} from '../theme';
import type {VpnStateType} from '../types/vpn';

interface CircularConnectionButtonProps {
  status: VpnStateType;
  onPress: () => void;
  size?: number;
  style?: ViewStyle;
}

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

const getIconName = (status: VpnStateType): string => {
  switch (status) {
    case 'connected':
      return 'shield-check';
    case 'connecting':
    case 'disconnecting':
      return 'shield-sync';
    case 'error':
      return 'shield-alert';
    default:
      return 'shield-outline';
  }
};

export const CircularConnectionButton: React.FC<CircularConnectionButtonProps> = ({
  status,
  onPress,
  size = 180,
  style,
}) => {
  const pulseScale = useSharedValue(1);
  const rotation = useSharedValue(0);

  const isAnimating = status === 'connecting' || status === 'disconnecting';

  useEffect(() => {
    if (isAnimating) {
      // Pulse animation
      pulseScale.value = withRepeat(
        withSequence(
          withTiming(1.1, {duration: 800, easing: Easing.inOut(Easing.ease)}),
          withTiming(1, {duration: 800, easing: Easing.inOut(Easing.ease)}),
        ),
        -1,
        false,
      );

      // Rotation animation for connecting state
      rotation.value = withRepeat(
        withTiming(360, {duration: 2000, easing: Easing.linear}),
        -1,
        false,
      );
    } else {
      cancelAnimation(pulseScale);
      cancelAnimation(rotation);
      pulseScale.value = withTiming(1);
      rotation.value = withTiming(0);
    }

    return () => {
      cancelAnimation(pulseScale);
      cancelAnimation(rotation);
    };
  }, [isAnimating, pulseScale, rotation]);

  const animatedOuterStyle = useAnimatedStyle(() => ({
    transform: [{scale: pulseScale.value}],
  }));

  const animatedIconStyle = useAnimatedStyle(() => ({
    transform: [{rotate: `${rotation.value}deg`}],
  }));

  const statusColor = getStatusColor(status);
  const iconName = getIconName(status);

  return (
    <View style={[styles.container, style]}>
      {/* Outer glow ring */}
      <Animated.View
        style={[
          styles.outerRing,
          {
            width: size + 40,
            height: size + 40,
            borderRadius: (size + 40) / 2,
            borderColor: statusColor,
            opacity: status === 'connected' ? 0.3 : 0.15,
          },
          animatedOuterStyle,
        ]}
      />

      {/* Middle ring */}
      <View
        style={[
          styles.middleRing,
          {
            width: size + 20,
            height: size + 20,
            borderRadius: (size + 20) / 2,
            borderColor: statusColor,
          },
        ]}
      />

      {/* Main button */}
      <TouchableOpacity
        onPress={onPress}
        activeOpacity={0.8}
        style={[
          styles.button,
          {
            width: size,
            height: size,
            borderRadius: size / 2,
            borderColor: statusColor,
            backgroundColor: colors.surface,
          },
        ]}>
        <Animated.View style={isAnimating ? animatedIconStyle : undefined}>
          <Icon name={iconName} size={size * 0.4} color={statusColor} />
        </Animated.View>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  outerRing: {
    position: 'absolute',
    borderWidth: 2,
  },
  middleRing: {
    position: 'absolute',
    borderWidth: 1,
    opacity: 0.3,
  },
  button: {
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 3,
    elevation: 8,
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 4},
    shadowOpacity: 0.3,
    shadowRadius: 8,
  },
});
