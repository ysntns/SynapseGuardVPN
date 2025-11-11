package com.synapseguard.vpn.presentation.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapseguard.vpn.domain.model.VpnServer
import com.synapseguard.vpn.domain.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class ServersUiState(
    val servers: List<VpnServer> = emptyList(),
    val selectedServer: VpnServer? = null,
    val isLoading: Boolean = false,
    val isRefreshingLatencies: Boolean = false,
    val sortOrder: ServerSortOrder = ServerSortOrder.DEFAULT,
    val error: String? = null
)

enum class ServerSortOrder {
    DEFAULT,
    LATENCY_ASC,
    LATENCY_DESC,
    NAME_ASC,
    LOAD_ASC
}

@HiltViewModel
class ServersViewModel @Inject constructor(
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServersUiState())
    val uiState: StateFlow<ServersUiState> = _uiState.asStateFlow()

    init {
        loadServers()
        observeServers()
        loadSelectedServer()
    }

    private fun observeServers() {
        viewModelScope.launch {
            serverRepository.availableServers.collect { servers ->
                val sortedServers = applySorting(servers, _uiState.value.sortOrder)
                _uiState.value = _uiState.value.copy(servers = sortedServers)
            }
        }
    }

    private fun loadServers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            serverRepository.getServers()
                .onSuccess { servers ->
                    Timber.d("Loaded ${servers.size} servers")
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to load servers")
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to load servers"
                    )
                }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun loadSelectedServer() {
        viewModelScope.launch {
            val selectedServerId = serverRepository.getSelectedServerId()
            if (selectedServerId != null) {
                val server = _uiState.value.servers.find { it.id == selectedServerId }
                _uiState.value = _uiState.value.copy(selectedServer = server)
            }
        }
    }

    fun selectServer(server: VpnServer) {
        viewModelScope.launch {
            serverRepository.saveSelectedServer(server.id)
            _uiState.value = _uiState.value.copy(selectedServer = server)
            Timber.d("Selected server: ${server.name}")
        }
    }

    fun refreshLatencies() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshingLatencies = true)
            serverRepository.refreshServerLatencies()
                .onSuccess { servers ->
                    Timber.d("Refreshed latencies for ${servers.size} servers")
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to refresh latencies")
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to refresh latencies"
                    )
                }
            _uiState.value = _uiState.value.copy(isRefreshingLatencies = false)
        }
    }

    fun selectBestServer() {
        viewModelScope.launch {
            val bestServer = serverRepository.selectBestServer()
            if (bestServer != null) {
                selectServer(bestServer)
                Timber.d("Auto-selected best server: ${bestServer.name} (${bestServer.latency}ms)")
            }
        }
    }

    fun setSortOrder(order: ServerSortOrder) {
        val sortedServers = applySorting(_uiState.value.servers, order)
        _uiState.value = _uiState.value.copy(
            sortOrder = order,
            servers = sortedServers
        )
        Timber.d("Changed sort order to: $order")
    }

    private fun applySorting(servers: List<VpnServer>, order: ServerSortOrder): List<VpnServer> {
        return when (order) {
            ServerSortOrder.DEFAULT -> servers
            ServerSortOrder.LATENCY_ASC -> servers.sortedBy { it.latency }
            ServerSortOrder.LATENCY_DESC -> servers.sortedByDescending { it.latency }
            ServerSortOrder.NAME_ASC -> servers.sortedBy { it.name }
            ServerSortOrder.LOAD_ASC -> servers.sortedBy { it.load }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
