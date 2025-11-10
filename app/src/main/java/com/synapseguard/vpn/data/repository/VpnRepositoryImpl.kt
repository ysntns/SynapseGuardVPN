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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
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

    override fun observeVpnState(): Flow<VpnState> = _vpnState.asStateFlow()

    override fun observeConnectionStats(): Flow<ConnectionStats> = _connectionStats.asStateFlow()

    override suspend fun connect(config: ConnectionConfig): Result<Unit> = withContext(ioDispatcher) {
        try {
            // Check VPN permission
            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent != null) {
                return@withContext Result.failure(
                    SecurityException("VPN permission not granted")
                )
            }

            _vpnState.value = VpnState.Connecting

            // Start VPN service
            val serviceIntent = Intent(
                context,
                Class.forName("com.synapseguard.vpn.service.core.VpnConnectionService")
            ).apply {
                action = "com.synapseguard.vpn.ACTION_CONNECT"
            }

            context.startForegroundService(serviceIntent)

            // Simulate connection for now
            _vpnState.value = VpnState.Connected(
                server = config.server,
                connectedAt = System.currentTimeMillis()
            )

            Timber.d("VPN connected to ${config.server.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect VPN")
            _vpnState.value = VpnState.Error(e.message ?: "Connection failed", e)
            Result.failure(e)
        }
    }

    override suspend fun disconnect(): Result<Unit> = withContext(ioDispatcher) {
        try {
            _vpnState.value = VpnState.Disconnecting

            // Stop VPN service
            val serviceIntent = Intent(
                context,
                Class.forName("com.synapseguard.vpn.service.core.VpnConnectionService")
            ).apply {
                action = "com.synapseguard.vpn.ACTION_DISCONNECT"
            }

            context.startService(serviceIntent)

            _vpnState.value = VpnState.Idle
            _connectionStats.value = ConnectionStats()

            Timber.d("VPN disconnected")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to disconnect VPN")
            Result.failure(e)
        }
    }

    override suspend fun isVpnActive(): Boolean {
        return _vpnState.value is VpnState.Connected
    }
}
