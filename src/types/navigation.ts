import type {NativeStackScreenProps} from '@react-navigation/native-stack';
import type {BottomTabScreenProps} from '@react-navigation/bottom-tabs';
import type {CompositeScreenProps} from '@react-navigation/native';

// Root Stack Navigator
export type RootStackParamList = {
  Splash: undefined;
  Main: undefined;
  ServerSelect: undefined;
  SplitTunnel: undefined;
};

// Bottom Tab Navigator
export type MainTabParamList = {
  Home: undefined;
  Servers: undefined;
  Stats: undefined;
  Settings: undefined;
};

// Screen Props Types
export type RootStackScreenProps<T extends keyof RootStackParamList> =
  NativeStackScreenProps<RootStackParamList, T>;

export type MainTabScreenProps<T extends keyof MainTabParamList> =
  CompositeScreenProps<
    BottomTabScreenProps<MainTabParamList, T>,
    NativeStackScreenProps<RootStackParamList>
  >;

// Navigation Types
declare global {
  namespace ReactNavigation {
    interface RootParamList extends RootStackParamList {}
  }
}
