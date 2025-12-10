import {useCallback, useEffect} from 'react';
import {VpnService} from '../services/VpnService';
import {useVpnStore} from '../stores';
import type {VpnServer} from '../types/vpn';

export const useVpn = () => {
  const {
    vpnState,
    stats,
    selectedServer,
    setVpnStatus,
    setConnectedServer,
    disconnect: storeDisconnect,
    updateStats,
  } = useVpnStore();

  // Listen to VPN state changes
  useEffect(() => {
    const unsubscribe = VpnService.addStateListener(state => {
      setVpnStatus(state);
    });
    return unsubscribe;
  }, [setVpnStatus]);

  // Listen to VPN stats updates
  useEffect(() => {
    const unsubscribe = VpnService.addStatsListener(newStats => {
      updateStats(newStats);
    });
    return unsubscribe;
  }, [updateStats]);

  // Poll stats when connected
  useEffect(() => {
    if (vpnState.status !== 'connected') return;

    const interval = setInterval(async () => {
      try {
        const newStats = await VpnService.getStats();
        updateStats(newStats);
      } catch (error) {
        console.error('Failed to get VPN stats:', error);
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [vpnState.status, updateStats]);

  const connect = useCallback(
    async (server?: VpnServer) => {
      const targetServer = server || selectedServer;
      if (!targetServer) {
        throw new Error('No server selected');
      }

      setVpnStatus('connecting');

      try {
        await VpnService.connect(targetServer);
        setConnectedServer(targetServer);
      } catch (error) {
        setVpnStatus('error', (error as Error).message);
        throw error;
      }
    },
    [selectedServer, setVpnStatus, setConnectedServer],
  );

  const disconnect = useCallback(async () => {
    setVpnStatus('disconnecting');

    try {
      await VpnService.disconnect();
      storeDisconnect();
    } catch (error) {
      setVpnStatus('error', (error as Error).message);
      throw error;
    }
  }, [setVpnStatus, storeDisconnect]);

  const toggleConnection = useCallback(async () => {
    if (vpnState.status === 'connected') {
      await disconnect();
    } else if (vpnState.status === 'idle') {
      await connect();
    }
  }, [vpnState.status, connect, disconnect]);

  return {
    vpnState,
    stats,
    selectedServer,
    connect,
    disconnect,
    toggleConnection,
    isConnected: vpnState.status === 'connected',
    isConnecting: vpnState.status === 'connecting',
    isDisconnecting: vpnState.status === 'disconnecting',
  };
};
