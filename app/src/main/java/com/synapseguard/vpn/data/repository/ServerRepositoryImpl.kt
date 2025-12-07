package com.synapseguard.vpn.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.synapseguard.vpn.data.local.dao.ServerDao
import com.synapseguard.vpn.data.local.entity.toDomain
import com.synapseguard.vpn.data.local.entity.toEntity
import com.synapseguard.vpn.data.remote.VpnApiService
import com.synapseguard.vpn.data.remote.dto.toDomain
import com.synapseguard.vpn.di.IoDispatcher
import com.synapseguard.vpn.domain.model.VpnProtocol
import com.synapseguard.vpn.domain.model.VpnServer
import com.synapseguard.vpn.domain.repository.ServerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

private val Context.serverDataStore by preferencesDataStore("server_prefs")

@Singleton
class ServerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverDao: ServerDao,
    private val apiService: VpnApiService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ServerRepository {

    private val selectedServerKey = stringPreferencesKey("selected_server_id")

    override val availableServers: Flow<List<VpnServer>> = serverDao.observeAllServers()
        .map { entities -> entities.map { it.toDomain() } }

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

    override suspend fun refreshServerLatencies(): Result<List<VpnServer>> = withContext(ioDispatcher) {
        try {
            val servers = serverDao.getAllServers()
            val updatedServers = mutableListOf<VpnServer>()

            servers.forEach { serverEntity ->
                // Simulate latency test with random values based on location
                val baseLatency = when {
                    serverEntity.country.contains("Germany") ||
                    serverEntity.country.contains("France") ||
                    serverEntity.country.contains("Netherlands") ||
                    serverEntity.country.contains("Switzerland") ||
                    serverEntity.country.contains("Belgium") -> Random.nextInt(20, 60)
                    serverEntity.country.contains("United Kingdom") ||
                    serverEntity.country.contains("Ireland") ||
                    serverEntity.country.contains("Sweden") ||
                    serverEntity.country.contains("Norway") ||
                    serverEntity.country.contains("Denmark") ||
                    serverEntity.country.contains("Finland") -> Random.nextInt(30, 70)
                    serverEntity.country.contains("United States") ||
                    serverEntity.country.contains("Canada") -> Random.nextInt(80, 150)
                    serverEntity.country.contains("Spain") ||
                    serverEntity.country.contains("Italy") ||
                    serverEntity.country.contains("Portugal") ||
                    serverEntity.country.contains("Austria") ||
                    serverEntity.country.contains("Poland") -> Random.nextInt(40, 90)
                    serverEntity.country.contains("Japan") ||
                    serverEntity.country.contains("South Korea") ||
                    serverEntity.country.contains("Singapore") ||
                    serverEntity.country.contains("Hong Kong") -> Random.nextInt(150, 250)
                    serverEntity.country.contains("Australia") -> Random.nextInt(200, 300)
                    else -> Random.nextInt(100, 250) // Far away servers
                }

                // Add some variance
                val newLatency = (baseLatency + Random.nextInt(-10, 10)).coerceAtLeast(1)

                // Simulate network delay for ping
                delay(Random.nextLong(50, 200))

                // Update in database
                serverDao.updateLatency(serverEntity.id, newLatency)

                updatedServers.add(serverEntity.toDomain().copy(latency = newLatency))

                Timber.d("Updated latency for ${serverEntity.name}: ${newLatency}ms")
            }

            Result.success(updatedServers)
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh server latencies")
            Result.failure(e)
        }
    }

    override suspend fun selectBestServer(): VpnServer? = withContext(ioDispatcher) {
        try {
            val servers = serverDao.getAllServers().map { it.toDomain() }
            servers.minByOrNull { it.latency }
        } catch (e: Exception) {
            Timber.e(e, "Failed to select best server")
            null
        }
    }

    override suspend fun saveSelectedServer(serverId: String) {
        try {
            context.serverDataStore.edit { preferences ->
                preferences[selectedServerKey] = serverId
            }
            Timber.d("Saved selected server: $serverId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save selected server")
        }
    }

    override suspend fun getSelectedServerId(): String? = withContext(ioDispatcher) {
        try {
            val preferences = context.serverDataStore.data.first()
            preferences[selectedServerKey]
        } catch (e: Exception) {
            Timber.e(e, "Failed to get selected server ID")
            null
        }
    }
}
