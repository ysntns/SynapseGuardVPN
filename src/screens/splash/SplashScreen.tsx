import React, {useEffect} from 'react';
import {View, Text, StyleSheet, StatusBar} from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  withSequence,
  withDelay,
  Easing,
} from 'react-native-reanimated';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';
import {colors, typography, spacing} from '../../theme';
import type {RootStackScreenProps} from '../../types/navigation';

type Props = RootStackScreenProps<'Splash'>;

export const SplashScreen: React.FC<Props> = ({navigation}) => {
  const logoScale = useSharedValue(0.5);
  const logoOpacity = useSharedValue(0);
  const textOpacity = useSharedValue(0);
  const subtitleOpacity = useSharedValue(0);

  useEffect(() => {
    // Logo animation
    logoScale.value = withSequence(
      withTiming(1.2, {duration: 400, easing: Easing.out(Easing.back)}),
      withTiming(1, {duration: 200}),
    );
    logoOpacity.value = withTiming(1, {duration: 400});

    // Text animations
    textOpacity.value = withDelay(300, withTiming(1, {duration: 400}));
    subtitleOpacity.value = withDelay(500, withTiming(1, {duration: 400}));

    // Navigate to main after splash
    const timer = setTimeout(() => {
      navigation.replace('Main');
    }, 2000);

    return () => clearTimeout(timer);
  }, [navigation, logoScale, logoOpacity, textOpacity, subtitleOpacity]);

  const logoAnimatedStyle = useAnimatedStyle(() => ({
    transform: [{scale: logoScale.value}],
    opacity: logoOpacity.value,
  }));

  const textAnimatedStyle = useAnimatedStyle(() => ({
    opacity: textOpacity.value,
  }));

  const subtitleAnimatedStyle = useAnimatedStyle(() => ({
    opacity: subtitleOpacity.value,
  }));

  return (
    <View style={styles.container}>
      <StatusBar
        barStyle="light-content"
        backgroundColor={colors.background}
      />

      <Animated.View style={[styles.logoContainer, logoAnimatedStyle]}>
        <View style={styles.logoCircle}>
          <Icon name="shield-check" size={64} color={colors.primary} />
        </View>
      </Animated.View>

      <Animated.Text style={[styles.title, textAnimatedStyle]}>
        SynapseGuard
      </Animated.Text>

      <Animated.Text style={[styles.subtitle, subtitleAnimatedStyle]}>
        Secure • Private • Fast
      </Animated.Text>

      <View style={styles.footer}>
        <Text style={styles.version}>Version 1.0.0</Text>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
    alignItems: 'center',
    justifyContent: 'center',
  },
  logoContainer: {
    marginBottom: spacing.xl,
  },
  logoCircle: {
    width: 120,
    height: 120,
    borderRadius: 60,
    backgroundColor: colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 2,
    borderColor: colors.primary,
  },
  title: {
    ...typography.h1,
    color: colors.textPrimary,
    marginBottom: spacing.sm,
  },
  subtitle: {
    ...typography.bodyLarge,
    color: colors.primary,
  },
  footer: {
    position: 'absolute',
    bottom: spacing.xxxl,
  },
  version: {
    ...typography.caption,
    color: colors.textTertiary,
  },
});
