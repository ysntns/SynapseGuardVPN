import {NativeModules, NativeEventEmitter, Platform} from 'react-native';
import type {VpnServer, ConnectionStats, VpnStateType} from '../types/vpn';

const {VpnModule} = NativeModules;

interface VpnModuleInterface {
  prepare(): Promise<boolean>;
  connect(config: {
    serverAddress: string;
    serverPort: number;
    protocol: string;
  }): Promise<boolean>;
  disconnect(): Promise<boolean>;
  getConnectionState(): Promise<VpnStateType>;
  getConnectionStats(): Promise<ConnectionStats>;
  setKillSwitch(enabled: boolean): Promise<boolean>;
  setSplitTunneling(enabled: boolean, excludedApps: string[]): Promise<boolean>;
  setCustomDns(dnsServers: string[]): Promise<boolean>;
}

const vpnModule = VpnModule as VpnModuleInterface;

// Event emitter for VPN events
const vpnEventEmitter = Platform.OS === 'android'
  ? new NativeEventEmitter(VpnModule)
  : null;

export type VpnStateListener = (state: VpnStateType) => void;
export type VpnStatsListener = (stats: ConnectionStats) => void;

class VpnServiceClass {
  private stateListeners: Set<VpnStateListener> = new Set();
  private statsListeners: Set<VpnStatsListener> = new Set();
  private stateSubscription: any = null;
  private statsSubscription: any = null;

  constructor() {
    this.setupEventListeners();
  }

  private setupEventListeners() {
    if (vpnEventEmitter) {
      this.stateSubscription = vpnEventEmitter.addListener(
        'VpnStateChanged',
        (event: {state: VpnStateType}) => {
          this.stateListeners.forEach(listener => listener(event.state));
        },
      );

      this.statsSubscription = vpnEventEmitter.addListener(
        'VpnStatsUpdated',
        (event: ConnectionStats) => {
          this.statsListeners.forEach(listener => listener(event));
        },
      );
    }
  }

  /**
   * Prepare VPN (request permission if needed)
   */
  async prepare(): Promise<boolean> {
    if (Platform.OS !== 'android') {
      console.warn('VPN is only supported on Android');
      return false;
    }
    return vpnModule.prepare();
  }

  /**
   * Connect to a VPN server
   */
  async connect(server: VpnServer): Promise<boolean> {
    if (Platform.OS !== 'android') {
      console.warn('VPN is only supported on Android');
      return false;
    }

    // First, prepare VPN
    const prepared = await this.prepare();
    if (!prepared) {
      throw new Error('VPN permission denied');
    }

    return vpnModule.connect({
      serverAddress: server.ipAddress,
      serverPort: server.port,
      protocol: server.protocol,
    });
  }

  /**
   * Disconnect from VPN
   */
  async disconnect(): Promise<boolean> {
    if (Platform.OS !== 'android') {
      return false;
    }
    return vpnModule.disconnect();
  }

  /**
   * Get current connection state
   */
  async getState(): Promise<VpnStateType> {
    if (Platform.OS !== 'android') {
      return 'idle';
    }
    return vpnModule.getConnectionState();
  }

  /**
   * Get connection statistics
   */
  async getStats(): Promise<ConnectionStats> {
    if (Platform.OS !== 'android') {
      return {
        bytesReceived: 0,
        bytesSent: 0,
        packetsReceived: 0,
        packetsSent: 0,
        duration: 0,
        downloadSpeedBps: 0,
        uploadSpeedBps: 0,
        sessionStartTime: 0,
      };
    }
    return vpnModule.getConnectionStats();
  }

  /**
   * Enable/disable kill switch
   */
  async setKillSwitch(enabled: boolean): Promise<boolean> {
    if (Platform.OS !== 'android') {
      return false;
    }
    return vpnModule.setKillSwitch(enabled);
  }

  /**
   * Enable/disable split tunneling
   */
  async setSplitTunneling(
    enabled: boolean,
    excludedApps: string[],
  ): Promise<boolean> {
    if (Platform.OS !== 'android') {
      return false;
    }
    return vpnModule.setSplitTunneling(enabled, excludedApps);
  }

  /**
   * Set custom DNS servers
   */
  async setCustomDns(dnsServers: string[]): Promise<boolean> {
    if (Platform.OS !== 'android') {
      return false;
    }
    return vpnModule.setCustomDns(dnsServers);
  }

  /**
   * Add state change listener
   */
  addStateListener(listener: VpnStateListener): () => void {
    this.stateListeners.add(listener);
    return () => this.stateListeners.delete(listener);
  }

  /**
   * Add stats update listener
   */
  addStatsListener(listener: VpnStatsListener): () => void {
    this.statsListeners.add(listener);
    return () => this.statsListeners.delete(listener);
  }

  /**
   * Clean up
   */
  destroy() {
    this.stateSubscription?.remove();
    this.statsSubscription?.remove();
    this.stateListeners.clear();
    this.statsListeners.clear();
  }
}

export const VpnService = new VpnServiceClass();
