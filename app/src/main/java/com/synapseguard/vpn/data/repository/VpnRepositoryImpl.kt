package com.synapseguard.vpn.data.repository

import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.synapseguard.vpn.di.IoDispatcher
import com.synapseguard.vpn.domain.model.ConnectionConfig
import com.synapseguard.vpn.domain.model.ConnectionStats
import com.synapseguard.vpn.domain.model.VpnState
import com.synapseguard.vpn.domain.repository.VpnRepository
import com.synapseguard.vpn.service.core.ConnectionState
import com.synapseguard.vpn.service.core.VpnConnectionService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : VpnRepository {

    private val _vpnState = MutableStateFlow<VpnState>(VpnState.Idle)
    private val _connectionStats = MutableStateFlow(ConnectionStats())

    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private var stateMonitorJob: Job? = null
    private var statsMonitorJob: Job? = null
    private var currentConfig: ConnectionConfig? = null

    override fun observeVpnState(): Flow<VpnState> = _vpnState.asStateFlow()

    override fun observeConnectionStats(): Flow<ConnectionStats> = _connectionStats.asStateFlow()

    override suspend fun connect(config: ConnectionConfig): Result<Unit> = withContext(ioDispatcher) {
        try {
            // Double-check VPN permission (should already be granted by MainActivity)
            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent != null) {
                Timber.w("VPN permission check failed - permission not granted")
                _vpnState.value = VpnState.Error("VPN permission required. Please grant permission and try again.")
                return@withContext Result.failure(
                    SecurityException("VPN permission not granted. Please grant permission and try again.")
                )
            }

            currentConfig = config

            Timber.d("Starting VPN connection to ${config.server.name}")
            _vpnState.value = VpnState.Connecting

            // Start VPN service with configuration
            val serviceIntent = Intent(context, VpnConnectionService::class.java).apply {
                action = VpnConnectionService.ACTION_CONNECT
                putExtra(VpnConnectionService.EXTRA_SERVER_ADDRESS, config.server.ipAddress)
                putExtra(VpnConnectionService.EXTRA_SERVER_PORT, config.server.port)
                putExtra(VpnConnectionService.EXTRA_KILL_SWITCH, config.enableKillSwitch)
                putExtra(VpnConnectionService.EXTRA_SPLIT_TUNNELING, config.enableSplitTunneling)
                putStringArrayListExtra(VpnConnectionService.EXTRA_EXCLUDED_APPS, ArrayList(config.excludedApps))
                putStringArrayListExtra(VpnConnectionService.EXTRA_DNS_SERVERS, ArrayList(config.dns))
            }

            context.startForegroundService(serviceIntent)

            // Start monitoring service state
            startStateMonitoring(config)

            // Wait for connection to establish (with timeout)
            var attempts = 0
            while (attempts < 50 && _vpnState.value is VpnState.Connecting) {
                delay(100)
                attempts++
            }

            if (_vpnState.value is VpnState.Connected) {
                Timber.d("VPN connected successfully to ${config.server.name}")
                Result.success(Unit)
            } else if (_vpnState.value is VpnState.Error) {
                Result.failure(Exception("Connection failed"))
            } else {
                // Still connecting or timeout
                Result.success(Unit)
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to connect VPN")
            _vpnState.value = VpnState.Error(e.message ?: "Connection failed", e)
            Result.failure(e)
        }
    }

    override suspend fun disconnect(): Result<Unit> = withContext(ioDispatcher) {
        try {
            _vpnState.value = VpnState.Disconnecting

            // Stop state monitoring
            stateMonitorJob?.cancel()

            // Stop VPN service
            val serviceIntent = Intent(context, VpnConnectionService::class.java).apply {
                action = VpnConnectionService.ACTION_DISCONNECT
            }

            context.startService(serviceIntent)

            // Wait a bit for service to stop
            delay(500)

            _vpnState.value = VpnState.Idle
            _connectionStats.value = ConnectionStats()
            currentConfig = null
            statsMonitorJob?.cancel()
            statsMonitorJob = null

            Timber.d("VPN disconnected")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to disconnect VPN")
            _vpnState.value = VpnState.Idle
            _connectionStats.value = ConnectionStats()
            Result.failure(e)
        }
    }

    override suspend fun isVpnActive(): Boolean {
        return _vpnState.value is VpnState.Connected
    }

    private fun startStateMonitoring(config: ConnectionConfig) {
        stateMonitorJob?.cancel()
        stateMonitorJob = repositoryScope.launch {
            try {
                while (isActive) {
                    val service = VpnConnectionService.getInstance()
                    if (service == null) {
                        Timber.d("Waiting for VpnConnectionService instance...")
                        delay(300)
                        continue
                    }

                    Timber.d("VpnConnectionService instance acquired, observing state")
                    service.getConnectionState().collect { state ->
                        when (state) {
                            ConnectionState.CONNECTING -> _vpnState.value = VpnState.Connecting
                            ConnectionState.CONNECTED -> {
                                _vpnState.value = VpnState.Connected(
                                    server = config.server,
                                    connectedAt = System.currentTimeMillis()
                                )
                                if (statsMonitorJob == null) {
                                    startStatsMonitoring(service)
                                }
                            }
                            ConnectionState.DISCONNECTING -> _vpnState.value = VpnState.Disconnecting
                            ConnectionState.ERROR -> _vpnState.value = VpnState.Error("VPN connection error")
                            ConnectionState.IDLE -> {
                                _vpnState.value = VpnState.Idle
                                _connectionStats.value = ConnectionStats()
                                statsMonitorJob?.cancel()
                                statsMonitorJob = null
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in state monitoring")
            }
        }
    }

    private fun startStatsMonitoring(service: VpnConnectionService) {
        statsMonitorJob?.cancel()
        statsMonitorJob = repositoryScope.launch {
            service.getConnectionStats().collect { stats ->
                val now = System.currentTimeMillis()
                _connectionStats.value = ConnectionStats(
                    bytesReceived = stats.bytesReceived,
                    bytesSent = stats.bytesSent,
                    packetsReceived = stats.packetsReceived,
                    packetsSent = stats.packetsSent,
                    duration = stats.duration,
                    timestamp = stats.timestamp,
                    downloadSpeedBps = stats.downloadSpeedBps,
                    uploadSpeedBps = stats.uploadSpeedBps,
                    sessionStartTime = if (stats.sessionStartTime > 0) stats.sessionStartTime else now
                )
            }
        }
    }
}
