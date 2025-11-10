package com.synapseguard.vpn.domain.repository

import com.synapseguard.vpn.domain.model.VpnProtocol
import com.synapseguard.vpn.domain.model.VpnServer
import kotlinx.coroutines.flow.Flow

interface ServerRepository {
    val availableServers: Flow<List<VpnServer>>

    suspend fun getServers(): Result<List<VpnServer>>
    suspend fun getServersByProtocol(protocol: VpnProtocol): Result<List<VpnServer>>
    suspend fun getFavoriteServers(): Flow<List<VpnServer>>
    suspend fun addToFavorites(serverId: String): Result<Unit>
    suspend fun removeFromFavorites(serverId: String): Result<Unit>
    suspend fun refreshServers(): Result<Unit>
    suspend fun refreshServerLatencies(): Result<List<VpnServer>>
    suspend fun selectBestServer(): VpnServer?
    suspend fun saveSelectedServer(serverId: String)
    suspend fun getSelectedServerId(): String?
}
