package com.synapseguard.vpn.data.repository

import com.synapseguard.vpn.domain.model.VpnServer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for ServerRepositoryImpl
 *
 * Tests cover:
 * - Server list retrieval
 * - Favorites management
 * - Server filtering
 * - Latency testing
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ServerRepositoryImplTest {

    private lateinit var repository: ServerRepositoryImpl

    @Before
    fun setup() {
        repository = ServerRepositoryImpl()
    }

    @Test
    fun `getServers returns non-empty list`() = runTest {
        // When
        val servers = repository.getServers()

        // Then
        assertTrue(servers.isNotEmpty())
    }

    @Test
    fun `getServers returns correct server count`() = runTest {
        // When
        val servers = repository.getServers()

        // Then
        assertEquals(9, servers.size) // 9 servers as per project spec
    }

    @Test
    fun `getDefaultServer returns valid server`() = runTest {
        // When
        val server = repository.getDefaultServer()

        // Then
        assertNotNull(server)
        assertTrue(server.ipAddress.isNotEmpty())
    }

    @Test
    fun `getServerById returns correct server`() = runTest {
        // Given
        val servers = repository.getServers()
        val targetId = servers.first().id

        // When
        val server = repository.getServerById(targetId)

        // Then
        assertNotNull(server)
        assertEquals(targetId, server.id)
    }

    @Test
    fun `getServerById returns null for invalid id`() = runTest {
        // When
        val server = repository.getServerById("invalid_id_12345")

        // Then
        assertEquals(null, server)
    }

    @Test
    fun `setFavorite updates server favorite status`() = runTest {
        // Given
        val servers = repository.getServers()
        val server = servers.first()

        // When
        repository.setFavorite(server.id, true)
        val updatedServer = repository.getServerById(server.id)

        // Then
        assertNotNull(updatedServer)
        assertEquals(true, updatedServer.isFavorite)
    }

    @Test
    fun `getFavorites returns only favorite servers`() = runTest {
        // Given
        val servers = repository.getServers()
        repository.setFavorite(servers[0].id, true)
        repository.setFavorite(servers[1].id, true)

        // When
        val favorites = repository.getFavorites()

        // Then
        assertEquals(2, favorites.size)
        assertTrue(favorites.all { it.isFavorite })
    }

    @Test
    fun `filterByCountry returns correct servers`() = runTest {
        // Given - assuming Germany server exists
        val servers = repository.getServers()
        val germanyServers = servers.filter { it.country == "Germany" }

        // When
        val filtered = repository.filterByCountry("Germany")

        // Then
        assertEquals(germanyServers.size, filtered.size)
        assertTrue(filtered.all { it.country == "Germany" })
    }

    @Test
    fun `filterByProtocol returns correct servers`() = runTest {
        // When
        val wireguardServers = repository.filterByProtocol("WireGuard")

        // Then
        assertTrue(wireguardServers.all { it.protocol == "WireGuard" })
    }

    @Test
    fun `sortByLatency returns servers in ascending order`() = runTest {
        // Given
        val servers = repository.getServers()

        // When
        val sorted = repository.sortByLatency(servers)

        // Then
        for (i in 0 until sorted.size - 1) {
            assertTrue(sorted[i].latency <= sorted[i + 1].latency)
        }
    }

    @Test
    fun `sortByLoad returns servers in ascending order`() = runTest {
        // Given
        val servers = repository.getServers()

        // When
        val sorted = repository.sortByLoad(servers)

        // Then
        for (i in 0 until sorted.size - 1) {
            assertTrue(sorted[i].load <= sorted[i + 1].load)
        }
    }

    @Test
    fun `refreshLatencies updates server latencies`() = runTest {
        // When
        repository.refreshLatencies()
        val servers = repository.getServers()

        // Then
        assertTrue(servers.all { it.latency >= 0 })
    }

    @Test
    fun `observeServers emits server list`() = runTest {
        // When
        val servers = repository.observeServers().first()

        // Then
        assertTrue(servers.isNotEmpty())
    }
}
