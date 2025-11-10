package com.synapseguard.vpn.data.repository

import com.synapseguard.vpn.data.local.dao.ServerDao
import com.synapseguard.vpn.data.local.entity.toDomain
import com.synapseguard.vpn.data.local.entity.toEntity
import com.synapseguard.vpn.data.remote.VpnApiService
import com.synapseguard.vpn.data.remote.dto.toDomain
import com.synapseguard.vpn.di.IoDispatcher
import com.synapseguard.vpn.domain.model.VpnProtocol
import com.synapseguard.vpn.domain.model.VpnServer
import com.synapseguard.vpn.domain.repository.ServerRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepositoryImpl @Inject constructor(
    private val serverDao: ServerDao,
    private val apiService: VpnApiService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ServerRepository {

    override suspend fun getServers(): Result<List<VpnServer>> = withContext(ioDispatcher) {
        try {
            // Try to get from local database first
            val localServers = serverDao.getAllServers()
            if (localServers.isNotEmpty()) {
                Timber.d("Retrieved ${localServers.size} servers from local database")
                return@withContext Result.success(localServers.map { it.toDomain() })
            }

            // If no local data, fetch from API
            refreshServers()

            val servers = serverDao.getAllServers().map { it.toDomain() }
            Result.success(servers)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get servers")
            Result.failure(e)
        }
    }

    override suspend fun getServersByProtocol(protocol: VpnProtocol): Result<List<VpnServer>> =
        withContext(ioDispatcher) {
            try {
                val servers = serverDao.getServersByProtocol(protocol.name)
                    .map { it.toDomain() }
                Result.success(servers)
            } catch (e: Exception) {
                Timber.e(e, "Failed to get servers by protocol")
                Result.failure(e)
            }
        }

    override suspend fun getFavoriteServers(): Flow<List<VpnServer>> {
        return serverDao.getFavoriteServers().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addToFavorites(serverId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            serverDao.updateFavoriteStatus(serverId, true)
            Timber.d("Added server $serverId to favorites")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add server to favorites")
            Result.failure(e)
        }
    }

    override suspend fun removeFromFavorites(serverId: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                serverDao.updateFavoriteStatus(serverId, false)
                Timber.d("Removed server $serverId from favorites")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove server from favorites")
                Result.failure(e)
            }
        }

    override suspend fun refreshServers(): Result<Unit> = withContext(ioDispatcher) {
        try {
            val response = apiService.getServers()
            if (response.isSuccessful && response.body()?.success == true) {
                val servers = response.body()!!.servers.map { dto ->
                    dto.toDomain().toEntity()
                }
                serverDao.insertServers(servers)
                Timber.d("Refreshed ${servers.size} servers from API")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch servers: ${response.message()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh servers")
            Result.failure(e)
        }
    }
}
