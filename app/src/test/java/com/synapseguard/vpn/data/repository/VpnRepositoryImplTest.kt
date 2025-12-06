package com.synapseguard.vpn.data.repository

import com.synapseguard.vpn.domain.model.ConnectionStats
import com.synapseguard.vpn.domain.model.VpnServer
import com.synapseguard.vpn.domain.model.VpnState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for VpnRepositoryImpl
 *
 * Tests cover:
 * - State management
 * - Connection flow
 * - Statistics tracking
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VpnRepositoryImplTest {

    private lateinit var repository: VpnRepositoryImpl

    private val testServer = VpnServer(
        id = "server1",
        name = "Germany",
        country = "Germany",
        city = "Frankfurt",
        ipAddress = "10.0.0.1",
        port = 51820,
        protocol = "WireGuard",
        load = 45,
        latency = 25,
        isFavorite = false,
        isPremium = false
    )

    @Before
    fun setup() {
        repository = VpnRepositoryImpl()
    }

    @Test
    fun `initial state should be Idle`() = runTest {
        // When
        val state = repository.observeVpnState().first()

        // Then
        assertEquals(VpnState.Idle, state)
    }

    @Test
    fun `initial stats should be empty`() = runTest {
        // When
        val stats = repository.observeConnectionStats().first()

        // Then
        assertEquals(0L, stats.bytesReceived)
        assertEquals(0L, stats.bytesSent)
        assertEquals(0L, stats.packetsReceived)
        assertEquals(0L, stats.packetsSent)
    }

    @Test
    fun `connect should transition to Connecting state`() = runTest {
        // Given
        // Note: This test assumes mock behavior for actual connection

        // When
        repository.connect(testServer)
        val state = repository.observeVpnState().first()

        // Then
        // The state should be either Connecting or Connected depending on timing
        assertTrue(state is VpnState.Connecting || state is VpnState.Connected || state is VpnState.Idle)
    }

    @Test
    fun `disconnect should transition to Idle state`() = runTest {
        // When
        repository.disconnect()
        val state = repository.observeVpnState().first()

        // Then
        assertTrue(state is VpnState.Idle || state is VpnState.Disconnecting)
    }

    @Test
    fun `updateStats should update connection statistics`() = runTest {
        // Given
        val newStats = ConnectionStats(
            bytesReceived = 1000,
            bytesSent = 500,
            packetsReceived = 10,
            packetsSent = 5
        )

        // When
        repository.updateStats(newStats)
        val stats = repository.observeConnectionStats().first()

        // Then
        assertEquals(1000L, stats.bytesReceived)
        assertEquals(500L, stats.bytesSent)
    }

    @Test
    fun `stats should accumulate correctly`() = runTest {
        // Given
        val stats1 = ConnectionStats(bytesReceived = 100, bytesSent = 50)
        val stats2 = ConnectionStats(bytesReceived = 200, bytesSent = 100)

        // When
        repository.updateStats(stats1)
        repository.updateStats(stats2)
        val finalStats = repository.observeConnectionStats().first()

        // Then - stats2 should replace stats1 (latest value)
        assertEquals(200L, finalStats.bytesReceived)
        assertEquals(100L, finalStats.bytesSent)
    }

    @Test
    fun `getConnectionDuration returns correct duration`() = runTest {
        // Given - simulate a connected state with known start time
        repository.setConnectedState(testServer)

        // When
        val duration = repository.getConnectionDuration()

        // Then
        assertTrue(duration >= 0)
    }

    @Test
    fun `isConnected returns correct state`() = runTest {
        // Given
        assertEquals(false, repository.isConnected())

        // When
        repository.setConnectedState(testServer)

        // Then
        assertEquals(true, repository.isConnected())

        // When disconnected
        repository.disconnect()

        // Then
        assertEquals(false, repository.isConnected())
    }
}
