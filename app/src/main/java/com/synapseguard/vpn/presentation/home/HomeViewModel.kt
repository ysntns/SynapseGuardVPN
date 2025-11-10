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
    observeConnectionStatsUseCase: ObserveConnectionStatsUseCase
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
        viewModelScope.launch {
            val currentState = _uiState.value
            val server = currentState.selectedServer ?: run {
                // Use a default server for demo purposes
                VpnServer(
                    id = "demo-1",
                    name = "Demo Server",
                    country = "United States",
                    countryCode = "US",
                    city = "New York",
                    ipAddress = "192.168.1.1",
                    port = 51820,
                    protocol = com.synapseguard.vpn.domain.model.VpnProtocol.WIREGUARD
                )
            }

            Timber.d("Connecting to ${server.name}")
            val config = ConnectionConfig(server = server)
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
}
