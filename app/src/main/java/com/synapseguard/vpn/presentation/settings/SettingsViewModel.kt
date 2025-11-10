package com.synapseguard.vpn.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapseguard.vpn.domain.model.VpnSettings
import com.synapseguard.vpn.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class SettingsUiState(
    val settings: VpnSettings = VpnSettings(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
            }
        }
    }

    fun updateAutoConnect(enabled: Boolean) {
        updateSettings { it.copy(autoConnect = enabled) }
    }

    fun updateKillSwitch(enabled: Boolean) {
        updateSettings { it.copy(killSwitch = enabled) }
    }

    fun updateSplitTunneling(enabled: Boolean) {
        updateSettings { it.copy(splitTunneling = enabled) }
    }

    private fun updateSettings(transform: (VpnSettings) -> VpnSettings) {
        viewModelScope.launch {
            val currentSettings = _uiState.value.settings
            val newSettings = transform(currentSettings)
            settingsRepository.updateSettings(newSettings).onFailure { error ->
                Timber.e(error, "Failed to update settings")
                _uiState.value = _uiState.value.copy(
                    error = error.message ?: "Failed to update settings"
                )
            }
        }
    }
}
