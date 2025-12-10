// VPN Protocol Types
export type VpnProtocol = 'wireguard' | 'openvpn' | 'v2ray';

// VPN Connection State
export type VpnStateType = 'idle' | 'connecting' | 'connected' | 'disconnecting' | 'error';

export interface VpnState {
  status: VpnStateType;
  server?: VpnServer;
  connectedAt?: number;
  error?: string;
}

// Server Information
export interface VpnServer {
  id: string;
  name: string;
  country: string;
  countryCode: string;
  city: string;
  ipAddress: string;
  port: number;
  protocol: VpnProtocol;
  latency: number;
  load: number;
  isPremium: boolean;
  flag: string;
}

// Connection Statistics
export interface ConnectionStats {
  bytesReceived: number;
  bytesSent: number;
  packetsReceived: number;
  packetsSent: number;
  duration: number;
  downloadSpeedBps: number;
  uploadSpeedBps: number;
  sessionStartTime: number;
}

// Connection Configuration
export interface ConnectionConfig {
  server: VpnServer;
  enableKillSwitch: boolean;
  enableSplitTunneling: boolean;
  excludedApps: string[];
  dns: string[];
}

// User Settings
export interface VpnSettings {
  autoConnect: boolean;
  killSwitch: boolean;
  splitTunneling: boolean;
  excludedApps: string[];
  preferredProtocol: VpnProtocol;
  customDns: string[];
  darkMode: boolean;
  notifications: boolean;
  language: string;
}

// Installed App Info (for split tunneling)
export interface AppInfo {
  packageName: string;
  appName: string;
  icon?: string;
  isExcluded: boolean;
}

// User
export interface User {
  id: string;
  email: string;
  name?: string;
  isPremium: boolean;
  subscriptionEndDate?: number;
}
