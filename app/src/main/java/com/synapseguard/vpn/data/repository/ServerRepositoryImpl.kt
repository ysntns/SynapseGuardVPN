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

            // Try to fetch from API
            try {
                refreshServers()
                val servers = serverDao.getAllServers().map { it.toDomain() }
                if (servers.isNotEmpty()) {
                    return@withContext Result.success(servers)
                }
            } catch (e: Exception) {
                Timber.w(e, "API fetch failed, using mock data")
            }

            // If no data, use mock servers
            val mockServers = createMockServers()
            serverDao.insertServers(mockServers.map { it.toEntity() })
            Timber.d("Inserted ${mockServers.size} mock servers")
            Result.success(mockServers)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get servers")
            Result.failure(e)
        }
    }

    private fun createMockServers(): List<VpnServer> {
        return listOf(
            // Germany
            VpnServer("de-fra-1", "Frankfurt #1", "Germany", "DE", "Frankfurt", "10.0.1.1", 51820, VpnProtocol.WIREGUARD, 25, 15, false, ""),
            VpnServer("de-fra-2", "Frankfurt #2", "Germany", "DE", "Frankfurt", "10.0.1.2", 51820, VpnProtocol.WIREGUARD, 28, 22, false, ""),
            VpnServer("de-ber-1", "Berlin #1", "Germany", "DE", "Berlin", "10.0.1.3", 51820, VpnProtocol.WIREGUARD, 32, 18, false, ""),
            // France
            VpnServer("fr-par-1", "Paris #1", "France", "FR", "Paris", "10.0.2.1", 51820, VpnProtocol.WIREGUARD, 35, 20, false, ""),
            VpnServer("fr-par-2", "Paris #2", "France", "FR", "Paris", "10.0.2.2", 51820, VpnProtocol.WIREGUARD, 38, 25, true, ""),
            // Netherlands
            VpnServer("nl-ams-1", "Amsterdam #1", "Netherlands", "NL", "Amsterdam", "10.0.3.1", 51820, VpnProtocol.WIREGUARD, 22, 12, false, ""),
            VpnServer("nl-ams-2", "Amsterdam #2", "Netherlands", "NL", "Amsterdam", "10.0.3.2", 51820, VpnProtocol.WIREGUARD, 24, 30, true, ""),
            // United Kingdom
            VpnServer("uk-lon-1", "London #1", "United Kingdom", "GB", "London", "10.0.4.1", 51820, VpnProtocol.WIREGUARD, 45, 28, false, ""),
            VpnServer("uk-lon-2", "London #2", "United Kingdom", "GB", "London", "10.0.4.2", 51820, VpnProtocol.WIREGUARD, 48, 35, true, ""),
            // United States
            VpnServer("us-nyc-1", "New York #1", "United States", "US", "New York", "10.0.5.1", 51820, VpnProtocol.WIREGUARD, 95, 22, false, ""),
            VpnServer("us-nyc-2", "New York #2", "United States", "US", "New York", "10.0.5.2", 51820, VpnProtocol.WIREGUARD, 98, 18, true, ""),
            VpnServer("us-lax-1", "Los Angeles #1", "United States", "US", "Los Angeles", "10.0.5.3", 51820, VpnProtocol.WIREGUARD, 145, 25, false, ""),
            VpnServer("us-mia-1", "Miami #1", "United States", "US", "Miami", "10.0.5.4", 51820, VpnProtocol.WIREGUARD, 120, 15, true, ""),
            // Canada
            VpnServer("ca-tor-1", "Toronto #1", "Canada", "CA", "Toronto", "10.0.6.1", 51820, VpnProtocol.WIREGUARD, 110, 20, false, ""),
            VpnServer("ca-van-1", "Vancouver #1", "Canada", "CA", "Vancouver", "10.0.6.2", 51820, VpnProtocol.WIREGUARD, 155, 18, true, ""),
            // Switzerland
            VpnServer("ch-zur-1", "Zurich #1", "Switzerland", "CH", "Zurich", "10.0.7.1", 51820, VpnProtocol.WIREGUARD, 30, 10, false, ""),
            // Japan
            VpnServer("jp-tok-1", "Tokyo #1", "Japan", "JP", "Tokyo", "10.0.8.1", 51820, VpnProtocol.WIREGUARD, 180, 22, false, ""),
            VpnServer("jp-tok-2", "Tokyo #2", "Japan", "JP", "Tokyo", "10.0.8.2", 51820, VpnProtocol.WIREGUARD, 185, 28, true, ""),
            // Singapore
            VpnServer("sg-sin-1", "Singapore #1", "Singapore", "SG", "Singapore", "10.0.9.1", 51820, VpnProtocol.WIREGUARD, 165, 15, false, ""),
            // Australia
            VpnServer("au-syd-1", "Sydney #1", "Australia", "AU", "Sydney", "10.0.10.1", 51820, VpnProtocol.WIREGUARD, 220, 18, false, ""),
            // Sweden
            VpnServer("se-sto-1", "Stockholm #1", "Sweden", "SE", "Stockholm", "10.0.11.1", 51820, VpnProtocol.WIREGUARD, 55, 12, false, ""),
            // Turkey
            VpnServer("tr-ist-1", "Istanbul #1", "Turkey", "TR", "Istanbul", "10.0.12.1", 51820, VpnProtocol.WIREGUARD, 65, 20, false, ""),
            VpnServer("tr-ist-2", "Istanbul #2", "Turkey", "TR", "Istanbul", "10.0.12.2", 51820, VpnProtocol.WIREGUARD, 68, 25, true, ""),
            // Spain
            VpnServer("es-mad-1", "Madrid #1", "Spain", "ES", "Madrid", "10.0.13.1", 51820, VpnProtocol.WIREGUARD, 50, 22, false, ""),
            // Italy
            VpnServer("it-mil-1", "Milan #1", "Italy", "IT", "Milan", "10.0.14.1", 51820, VpnProtocol.WIREGUARD, 48, 18, false, "")
        )
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
