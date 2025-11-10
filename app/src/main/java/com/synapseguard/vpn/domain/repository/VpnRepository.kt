package com.synapseguard.vpn.domain.repository

import com.synapseguard.vpn.domain.model.ConnectionConfig
import com.synapseguard.vpn.domain.model.ConnectionStats
import com.synapseguard.vpn.domain.model.VpnState
import kotlinx.coroutines.flow.Flow

interface VpnRepository {
    fun observeVpnState(): Flow<VpnState>
    fun observeConnectionStats(): Flow<ConnectionStats>
    suspend fun connect(config: ConnectionConfig): Result<Unit>
    suspend fun disconnect(): Result<Unit>
    suspend fun isVpnActive(): Boolean
}
