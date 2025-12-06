package com.synapseguard.vpn.presentation.servers

import com.synapseguard.vpn.domain.model.VpnServer
import com.synapseguard.vpn.domain.repository.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for ServersViewModel
 *
 * Tests cover:
 * - Server list loading
 * - Server selection
 * - Filtering
 * - Sorting
 * - Favorites
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ServersViewModelTest {

    @Mock
    private lateinit var serverRepository: ServerRepository

    private lateinit var viewModel: ServersViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val testServers = listOf(
        VpnServer(
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
        ),
        VpnServer(
            id = "server2",
            name = "United States",
            country = "United States",
            city = "New York",
            ipAddress = "10.0.0.2",
            port = 51820,
            protocol = "WireGuard",
            load = 60,
            latency = 80,
            isFavorite = true,
            isPremium = false
        ),
        VpnServer(
            id = "server3",
            name = "Japan",
            country = "Japan",
            city = "Tokyo",
            ipAddress = "10.0.0.3",
            port = 51820,
            protocol = "OpenVPN",
            load = 30,
            latency = 150,
            isFavorite = false,
            isPremium = true
        )
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        whenever(serverRepository.observeServers()).thenReturn(flowOf(testServers))
        whenever(serverRepository.getServers()).thenReturn(testServers)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads servers from repository`() = runTest {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(3, viewModel.uiState.value.servers.size)
    }

    @Test
    fun `selectServer updates selected server`() = runTest {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.selectServer(testServers[1])

        // Then
        assertEquals(testServers[1], viewModel.uiState.value.selectedServer)
    }

    @Test
    fun `filterByCountry shows only matching servers`() = runTest {
        // Given
        whenever(serverRepository.filterByCountry("Germany")).thenReturn(
            testServers.filter { it.country == "Germany" }
        )
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.filterByCountry("Germany")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val filteredServers = viewModel.uiState.value.filteredServers
        assertTrue(filteredServers.all { it.country == "Germany" })
    }

    @Test
    fun `sortByLatency orders servers correctly`() = runTest {
        // Given
        val sortedServers = testServers.sortedBy { it.latency }
        whenever(serverRepository.sortByLatency(testServers)).thenReturn(sortedServers)
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.sortByLatency()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val servers = viewModel.uiState.value.filteredServers
        for (i in 0 until servers.size - 1) {
            assertTrue(servers[i].latency <= servers[i + 1].latency)
        }
    }

    @Test
    fun `sortByLoad orders servers correctly`() = runTest {
        // Given
        val sortedServers = testServers.sortedBy { it.load }
        whenever(serverRepository.sortByLoad(testServers)).thenReturn(sortedServers)
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.sortByLoad()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val servers = viewModel.uiState.value.filteredServers
        for (i in 0 until servers.size - 1) {
            assertTrue(servers[i].load <= servers[i + 1].load)
        }
    }

    @Test
    fun `toggleFavorite updates server favorite status`() = runTest {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.toggleFavorite(testServers[0])
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(serverRepository).setFavorite(testServers[0].id, true)
    }

    @Test
    fun `showFavoritesOnly filters to favorite servers`() = runTest {
        // Given
        val favorites = testServers.filter { it.isFavorite }
        whenever(serverRepository.getFavorites()).thenReturn(favorites)
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.showFavoritesOnly(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val filteredServers = viewModel.uiState.value.filteredServers
        assertTrue(filteredServers.all { it.isFavorite })
    }

    @Test
    fun `searchServers filters by name`() = runTest {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.searchServers("Germany")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val filteredServers = viewModel.uiState.value.filteredServers
        assertTrue(filteredServers.any { it.name.contains("Germany", ignoreCase = true) })
    }

    @Test
    fun `refreshServers updates server list`() = runTest {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.refreshServers()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(serverRepository).refreshLatencies()
    }

    @Test
    fun `clearFilters shows all servers`() = runTest {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.filterByCountry("Germany")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearFilters()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(testServers.size, viewModel.uiState.value.filteredServers.size)
    }

    private fun createViewModel(): ServersViewModel {
        return ServersViewModel(serverRepository)
    }
}
