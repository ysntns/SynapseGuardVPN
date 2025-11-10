package com.synapseguard.vpn.data.repository

import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.synapseguard.vpn.di.IoDispatcher
import com.synapseguard.vpn.domain.model.ConnectionConfig
import com.synapseguard.vpn.domain.model.ConnectionStats
import com.synapseguard.vpn.domain.model.VpnState
import com.synapseguard.vpn.domain.repository.VpnRepository
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
            val serviceIntent = Intent(
                context,
                Class.forName("com.synapseguard.vpn.service.core.VpnConnectionService")
            ).apply {
                action = "com.synapseguard.vpn.ACTION_CONNECT"
                putExtra("server_address", config.server.ipAddress)
                putExtra("server_port", config.server.port)
                putExtra("kill_switch", config.enableKillSwitch)
                putExtra("split_tunneling", config.enableSplitTunneling)
                putStringArrayListExtra("excluded_apps", ArrayList(config.excludedApps))
                putStringArrayListExtra("dns_servers", ArrayList(config.dns))
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
            val serviceIntent = Intent(
                context,
                Class.forName("com.synapseguard.vpn.service.core.VpnConnectionService")
            ).apply {
                action = "com.synapseguard.vpn.ACTION_DISCONNECT"
            }

            context.startService(serviceIntent)

            // Wait a bit for service to stop
            delay(500)

            _vpnState.value = VpnState.Idle
            _connectionStats.value = ConnectionStats()
            currentConfig = null

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
                // Poll for service state
                // In production, this would use broadcasts or IPC
                var connected = false

                while (isActive && !connected) {
                    delay(500)

                    // Try to get service instance
                    try {
                        val serviceClass = Class.forName("com.synapseguard.vpn.service.core.VpnConnectionService")
                        val getInstanceMethod = serviceClass.getMethod("getInstance")
                        val serviceInstance = getInstanceMethod.invoke(null)

                        if (serviceInstance != null) {
                            // Get connection state via reflection
                            val getStateMethod = serviceClass.getMethod("getConnectionState")
                            val stateFlow = getStateMethod.invoke(serviceInstance)

                            if (stateFlow != null) {
                                // Successfully connected to service
                                connected = true
                                _vpnState.value = VpnState.Connected(
                                    server = config.server,
                                    connectedAt = System.currentTimeMillis()
                                )

                                // Start monitoring stats
                                startStatsMonitoring()
                            }
                        }
                    } catch (e: Exception) {
                        // Service not ready yet
                        Timber.d("Waiting for service to be ready...")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in state monitoring")
            }
        }
    }

    private fun startStatsMonitoring() {
        repositoryScope.launch {
            while (isActive && _vpnState.value is VpnState.Connected) {
                try {
                    // Get stats from service
                    val serviceClass = Class.forName("com.synapseguard.vpn.service.core.VpnConnectionService")
                    val getInstanceMethod = serviceClass.getMethod("getInstance")
                    val serviceInstance = getInstanceMethod.invoke(null)

                    if (serviceInstance != null) {
                        val getStatsMethod = serviceClass.getMethod("getConnectionStats")
                        val statsFlow = getStatsMethod.invoke(serviceInstance)

                        if (statsFlow != null && statsFlow is kotlinx.coroutines.flow.StateFlow<*>) {
                            val stats = statsFlow.value
                            if (stats is com.synapseguard.vpn.service.core.ConnectionStats) {
                                // Convert service stats to domain stats
                                _connectionStats.value = ConnectionStats(
                                    bytesReceived = stats.bytesReceived,
                                    bytesSent = stats.bytesSent,
                                    packetsReceived = stats.packetsReceived,
                                    packetsSent = stats.packetsSent
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Service might be stopping
                }

                delay(1000)  // Update every second
            }
        }
    }
}
