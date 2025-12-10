import {create} from 'zustand';
import type {
  VpnState,
  VpnServer,
  ConnectionStats,
  VpnStateType,
} from '../types/vpn';

interface VpnStore {
  // State
  vpnState: VpnState;
  stats: ConnectionStats;
  servers: VpnServer[];
  selectedServer: VpnServer | null;
  isLoading: boolean;

  // Actions
  setVpnStatus: (status: VpnStateType, error?: string) => void;
  setConnectedServer: (server: VpnServer) => void;
  disconnect: () => void;
  updateStats: (stats: Partial<ConnectionStats>) => void;
  setServers: (servers: VpnServer[]) => void;
  selectServer: (server: VpnServer) => void;
  setLoading: (loading: boolean) => void;
}

const initialStats: ConnectionStats = {
  bytesReceived: 0,
  bytesSent: 0,
  packetsReceived: 0,
  packetsSent: 0,
  duration: 0,
  downloadSpeedBps: 0,
  uploadSpeedBps: 0,
  sessionStartTime: 0,
};

export const useVpnStore = create<VpnStore>((set, get) => ({
  // Initial State
  vpnState: {status: 'idle'},
  stats: initialStats,
  servers: [],
  selectedServer: null,
  isLoading: false,

  // Actions
  setVpnStatus: (status, error) =>
    set(state => ({
      vpnState: {
        ...state.vpnState,
        status,
        error,
        connectedAt: status === 'connected' ? Date.now() : state.vpnState.connectedAt,
      },
    })),

  setConnectedServer: server =>
    set({
      vpnState: {
        status: 'connected',
        server,
        connectedAt: Date.now(),
      },
      selectedServer: server,
      stats: {...initialStats, sessionStartTime: Date.now()},
    }),

  disconnect: () =>
    set({
      vpnState: {status: 'idle'},
      stats: initialStats,
    }),

  updateStats: stats =>
    set(state => ({
      stats: {...state.stats, ...stats},
    })),

  setServers: servers => set({servers}),

  selectServer: server => set({selectedServer: server}),

  setLoading: isLoading => set({isLoading}),
}));
