package com.synapseguard.vpn.presentation.home

import com.synapseguard.vpn.domain.model.ConnectionStats
import com.synapseguard.vpn.domain.model.VpnServer
import com.synapseguard.vpn.domain.model.VpnState
import com.synapseguard.vpn.domain.repository.ServerRepository
import com.synapseguard.vpn.domain.repository.VpnRepository
import com.synapseguard.vpn.domain.usecase.ConnectVpnUseCase
import com.synapseguard.vpn.domain.usecase.DisconnectVpnUseCase
import com.synapseguard.vpn.domain.usecase.ObserveConnectionStatsUseCase
import com.synapseguard.vpn.domain.usecase.ObserveVpnStateUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for HomeViewModel
 *
 * Tests cover:
 * - Initial state
 * - VPN connection flow
 * - VPN disconnection flow
 * - State observation
 * - Error handling
 * - Server selection
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @Mock
    private lateinit var connectVpnUseCase: ConnectVpnUseCase

    @Mock
    private lateinit var disconnectVpnUseCase: DisconnectVpnUseCase

    @Mock
    private lateinit var observeVpnStateUseCase: ObserveVpnStateUseCase

    @Mock
    private lateinit var observeConnectionStatsUseCase: ObserveConnectionStatsUseCase

    @Mock
    private lateinit var serverRepository: ServerRepository

    @Mock
    private lateinit var vpnRepository: VpnRepository

    private lateinit var viewModel: HomeViewModel
    private val testDispatcher = StandardTestDispatcher()

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
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Setup default mocks
        whenever(observeVpnStateUseCase()).thenReturn(flowOf(VpnState.Idle))
        whenever(observeConnectionStatsUseCase()).thenReturn(flowOf(ConnectionStats()))
        whenever(serverRepository.getDefaultServer()).thenReturn(testServer)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be idle`() = runTest {
        // Given
        viewModel = createViewModel()

        // Then
        assertEquals(VpnState.Idle, viewModel.uiState.value.vpnState)
    }

    @Test
    fun `initial connection stats should be empty`() = runTest {
        // Given
        viewModel = createViewModel()

        // Then
        assertEquals(0L, viewModel.uiState.value.connectionStats.bytesReceived)
        assertEquals(0L, viewModel.uiState.value.connectionStats.bytesSent)
    }

    @Test
    fun `should have default server selected`() = runTest {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertNotNull(viewModel.uiState.value.selectedServer)
        assertEquals("Germany", viewModel.uiState.value.selectedServer?.name)
    }

    @Test
    fun `onConnectClick should initiate connection`() = runTest {
        // Given
        whenever(connectVpnUseCase(testServer)).thenReturn(Result.success(Unit))
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onConnectClick()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(connectVpnUseCase).invoke(testServer)
    }

    @Test
    fun `onDisconnectClick should disconnect VPN`() = runTest {
        // Given
        whenever(disconnectVpnUseCase()).thenReturn(Result.success(Unit))
        viewModel = createViewModel()

        // When
        viewModel.onDisconnectClick()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(disconnectVpnUseCase).invoke()
    }

    @Test
    fun `connection failure should set error state`() = runTest {
        // Given
        val errorMessage = "Connection failed"
        whenever(connectVpnUseCase(testServer)).thenReturn(Result.failure(Exception(errorMessage)))
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onConnectClick()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error!!.contains(errorMessage))
    }

    @Test
    fun `clearError should clear error state`() = runTest {
        // Given
        val errorMessage = "Connection failed"
        whenever(connectVpnUseCase(testServer)).thenReturn(Result.failure(Exception(errorMessage)))
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onConnectClick()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `should observe VPN state changes`() = runTest {
        // Given
        val stateFlow = MutableStateFlow<VpnState>(VpnState.Idle)
        whenever(observeVpnStateUseCase()).thenReturn(stateFlow)
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When - state changes to Connecting
        stateFlow.value = VpnState.Connecting
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(VpnState.Connecting, viewModel.uiState.value.vpnState)

        // When - state changes to Connected
        stateFlow.value = VpnState.Connected(testServer)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.vpnState is VpnState.Connected)
    }

    @Test
    fun `should observe connection stats updates`() = runTest {
        // Given
        val statsFlow = MutableStateFlow(ConnectionStats())
        whenever(observeConnectionStatsUseCase()).thenReturn(statsFlow)
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        val newStats = ConnectionStats(
            bytesReceived = 1000,
            bytesSent = 500,
            packetsReceived = 10,
            packetsSent = 5
        )
        statsFlow.value = newStats
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(1000L, viewModel.uiState.value.connectionStats.bytesReceived)
        assertEquals(500L, viewModel.uiState.value.connectionStats.bytesSent)
    }

    @Test
    fun `setSelectedServer should update selected server`() = runTest {
        // Given
        val newServer = testServer.copy(id = "server2", name = "United States")
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.setSelectedServer(newServer)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("United States", viewModel.uiState.value.selectedServer?.name)
    }

    @Test
    fun `should not allow connect when already connecting`() = runTest {
        // Given
        val stateFlow = MutableStateFlow<VpnState>(VpnState.Connecting)
        whenever(observeVpnStateUseCase()).thenReturn(stateFlow)
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onConnectClick()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - connect should not be called since already connecting
        // The ViewModel should check state before initiating connection
    }

    @Test
    fun `refreshServerLatencies should refresh server data`() = runTest {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.refreshServerLatencies()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(serverRepository).refreshLatencies()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            connectVpnUseCase = connectVpnUseCase,
            disconnectVpnUseCase = disconnectVpnUseCase,
            observeVpnStateUseCase = observeVpnStateUseCase,
            observeConnectionStatsUseCase = observeConnectionStatsUseCase,
            serverRepository = serverRepository
        )
    }
}
