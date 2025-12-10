import {create} from 'zustand';
import type {VpnSettings, VpnProtocol} from '../types/vpn';

interface SettingsStore {
  // State
  settings: VpnSettings;

  // Actions
  updateSettings: (settings: Partial<VpnSettings>) => void;
  toggleKillSwitch: () => void;
  toggleSplitTunneling: () => void;
  toggleAutoConnect: () => void;
  toggleNotifications: () => void;
  setPreferredProtocol: (protocol: VpnProtocol) => void;
  setCustomDns: (dns: string[]) => void;
  setExcludedApps: (apps: string[]) => void;
  addExcludedApp: (packageName: string) => void;
  removeExcludedApp: (packageName: string) => void;
  setLanguage: (language: string) => void;
  resetSettings: () => void;
}

const defaultSettings: VpnSettings = {
  autoConnect: false,
  killSwitch: true,
  splitTunneling: false,
  excludedApps: [],
  preferredProtocol: 'wireguard',
  customDns: ['1.1.1.1', '1.0.0.1'],
  darkMode: true,
  notifications: true,
  language: 'en',
};

export const useSettingsStore = create<SettingsStore>((set, get) => ({
  // Initial State
  settings: defaultSettings,

  // Actions
  updateSettings: newSettings =>
    set(state => ({
      settings: {...state.settings, ...newSettings},
    })),

  toggleKillSwitch: () =>
    set(state => ({
      settings: {...state.settings, killSwitch: !state.settings.killSwitch},
    })),

  toggleSplitTunneling: () =>
    set(state => ({
      settings: {
        ...state.settings,
        splitTunneling: !state.settings.splitTunneling,
      },
    })),

  toggleAutoConnect: () =>
    set(state => ({
      settings: {...state.settings, autoConnect: !state.settings.autoConnect},
    })),

  toggleNotifications: () =>
    set(state => ({
      settings: {
        ...state.settings,
        notifications: !state.settings.notifications,
      },
    })),

  setPreferredProtocol: protocol =>
    set(state => ({
      settings: {...state.settings, preferredProtocol: protocol},
    })),

  setCustomDns: dns =>
    set(state => ({
      settings: {...state.settings, customDns: dns},
    })),

  setExcludedApps: apps =>
    set(state => ({
      settings: {...state.settings, excludedApps: apps},
    })),

  addExcludedApp: packageName =>
    set(state => ({
      settings: {
        ...state.settings,
        excludedApps: [...state.settings.excludedApps, packageName],
      },
    })),

  removeExcludedApp: packageName =>
    set(state => ({
      settings: {
        ...state.settings,
        excludedApps: state.settings.excludedApps.filter(p => p !== packageName),
      },
    })),

  setLanguage: language =>
    set(state => ({
      settings: {...state.settings, language},
    })),

  resetSettings: () => set({settings: defaultSettings}),
}));
