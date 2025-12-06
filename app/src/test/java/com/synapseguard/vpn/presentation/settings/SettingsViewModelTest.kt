package com.synapseguard.vpn.presentation.settings

import com.synapseguard.vpn.domain.model.VpnProtocol
import com.synapseguard.vpn.domain.model.VpnSettings
import com.synapseguard.vpn.domain.repository.SettingsRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for SettingsViewModel
 *
 * Tests cover:
 * - Settings loading
 * - Kill switch toggle
 * - Split tunneling toggle
 * - Protocol selection
 * - DNS settings
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val defaultSettings = VpnSettings(
        killSwitchEnabled = false,
        splitTunnelingEnabled = false,
        selectedProtocol = VpnProtocol.WIREGUARD,
        customDns = listOf("1.1.1.1", "1.0.0.1"),
        autoConnect = false,
        excludedApps = emptyList()
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        whenever(settingsRepository.observeSettings()).thenReturn(flowOf(defaultSettings))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads settings from repository`() = runTest {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(false, viewModel.uiState.value.killSwitchEnabled)
        assertEquals(false, viewModel.uiState.value.splitTunnelingEnabled)
        assertEquals(VpnProtocol.WIREGUARD, viewModel.uiState.value.selectedProtocol)
    }

    @Test
    fun `toggleKillSwitch updates setting`() = runTest {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.toggleKillSwitch(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(settingsRepository).setKillSwitchEnabled(true)
    }

    @Test
    fun `toggleSplitTunneling updates setting`() = runTest {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.toggleSplitTunneling(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(settingsRepository).setSplitTunnelingEnabled(true)
    }

    @Test
    fun `setProtocol updates selected protocol`() = runTest {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.setProtocol(VpnProtocol.OPENVPN)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(settingsRepository).setSelectedProtocol(VpnProtocol.OPENVPN)
    }

    @Test
    fun `setCustomDns updates DNS servers`() = runTest {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        val newDns = listOf("8.8.8.8", "8.8.4.4")

        // When
        viewModel.setCustomDns(newDns)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(settingsRepository).setCustomDns(newDns)
    }

    @Test
    fun `toggleAutoConnect updates setting`() = runTest {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.toggleAutoConnect(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(settingsRepository).setAutoConnect(true)
    }

    @Test
    fun `addExcludedApp updates excluded apps list`() = runTest {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.addExcludedApp("com.example.app")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(settingsRepository).addExcludedApp("com.example.app")
    }

    @Test
    fun `removeExcludedApp updates excluded apps list`() = runTest {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.removeExcludedApp("com.example.app")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(settingsRepository).removeExcludedApp("com.example.app")
    }

    @Test
    fun `available protocols contains all protocols`() = runTest {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val protocols = viewModel.uiState.value.availableProtocols
        assertTrue(protocols.contains(VpnProtocol.WIREGUARD))
        assertTrue(protocols.contains(VpnProtocol.OPENVPN))
        assertTrue(protocols.contains(VpnProtocol.V2RAY))
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(settingsRepository)
    }
}
