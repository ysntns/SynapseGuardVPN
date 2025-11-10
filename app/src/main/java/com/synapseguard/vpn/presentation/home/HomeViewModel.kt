package com.synapseguard.vpn.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapseguard.vpn.domain.model.ConnectionConfig
import com.synapseguard.vpn.domain.model.ConnectionStats
import com.synapseguard.vpn.domain.model.VpnServer
import com.synapseguard.vpn.domain.model.VpnState
import com.synapseguard.vpn.domain.usecase.ConnectVpnUseCase
import com.synapseguard.vpn.domain.usecase.DisconnectVpnUseCase
import com.synapseguard.vpn.domain.usecase.ObserveConnectionStatsUseCase
import com.synapseguard.vpn.domain.usecase.ObserveVpnStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class HomeUiState(
    val vpnState: VpnState = VpnState.Idle,
    val connectionStats: ConnectionStats = ConnectionStats(),
    val selectedServer: VpnServer? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val connectVpnUseCase: ConnectVpnUseCase,
    private val disconnectVpnUseCase: DisconnectVpnUseCase,
    observeVpnStateUseCase: ObserveVpnStateUseCase,
    observeConnectionStatsUseCase: ObserveConnectionStatsUseCase,
    private val settingsRepository: com.synapseguard.vpn.domain.repository.SettingsRepository,
    private val serverRepository: com.synapseguard.vpn.domain.repository.ServerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Observe VPN state
        viewModelScope.launch {
            observeVpnStateUseCase().collect { state ->
                _uiState.value = _uiState.value.copy(
                    vpnState = state,
                    isLoading = state is VpnState.Connecting || state is VpnState.Disconnecting
                )
            }
        }

        // Observe connection stats
        viewModelScope.launch {
            observeConnectionStatsUseCase().collect { stats ->
                _uiState.value = _uiState.value.copy(connectionStats = stats)
            }
        }
    }

    fun onConnectClick() {
        // Request VPN permission first
        try {
            com.synapseguard.vpn.presentation.MainActivity.requestVpnPermissionStatic {
                // Permission granted, proceed with connection
                connectToVpn()
            }
        } catch (e: Exception) {
            // Fallback: just try to connect directly
            connectToVpn()
        }
    }

    private fun connectToVpn() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val server = currentState.selectedServer ?: run {
                // Use Frankfurt server as default
                VpnServer(
                    id = "de-frankfurt",
                    name = "Germany - Frankfurt",
                    country = "Germany",
                    countryCode = "DE",
                    city = "Frankfurt",
                    ipAddress = "185.223.95.120",  // Example IP
                    port = 51820,
                    protocol = com.synapseguard.vpn.domain.model.VpnProtocol.WIREGUARD,
                    latency = 28,
                    load = 35
                )
            }

            Timber.d("Connecting to ${server.name}")

            // Get current settings from repository
            val settings = try {
                settingsRepository.getSettings()
            } catch (e: Exception) {
                Timber.w(e, "Failed to get settings, using defaults")
                com.synapseguard.vpn.domain.model.VpnSettings()
            }

            // Build connection config with settings from SettingsRepository
            val config = ConnectionConfig(
                server = server,
                enableKillSwitch = settings.killSwitch,
                enableSplitTunneling = settings.splitTunneling,
                excludedApps = settings.excludedApps.toList(),
                dns = if (settings.customDns.isNotEmpty()) settings.customDns else listOf("1.1.1.1", "1.0.0.1")
            )

            Timber.d("VPN Config: killSwitch=${config.enableKillSwitch}, splitTunneling=${config.enableSplitTunneling}, excludedApps=${config.excludedApps.size}")

            connectVpnUseCase(config).onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    error = error.message ?: "Connection failed"
                )
            }
        }
    }

    fun onDisconnectClick() {
        viewModelScope.launch {
            Timber.d("Disconnecting VPN")
            disconnectVpnUseCase().onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    error = error.message ?: "Disconnection failed"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refreshServerLatencies() {
        viewModelScope.launch {
            Timber.d("Refreshing server latencies")
            serverRepository.refreshServerLatencies()
                .onSuccess { servers ->
                    Timber.d("Successfully refreshed latencies for ${servers.size} servers")
                    // Update selected server if its latency changed
                    val currentSelectedId = _uiState.value.selectedServer?.id
                    if (currentSelectedId != null) {
                        val updatedServer = servers.find { it.id == currentSelectedId }
                        if (updatedServer != null) {
                            _uiState.value = _uiState.value.copy(selectedServer = updatedServer)
                        }
                    }
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to refresh server latencies")
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to refresh server latencies: ${error.message}"
                    )
                }
        }
    }
}
