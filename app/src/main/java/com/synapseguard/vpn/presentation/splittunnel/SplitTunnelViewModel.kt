package com.synapseguard.vpn.presentation.splittunnel

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapseguard.vpn.di.IoDispatcher
import com.synapseguard.vpn.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val isEnabled: Boolean = false
)

data class SplitTunnelUiState(
    val apps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = ""
)

@HiltViewModel
class SplitTunnelViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplitTunnelUiState())
    val uiState: StateFlow<SplitTunnelUiState> = _uiState.asStateFlow()

    private val enabledApps = mutableSetOf<String>()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch(ioDispatcher) {
            try {
                val settings = settingsRepository.getSettings()
                enabledApps.clear()
                enabledApps.addAll(settings.excludedApps)
                loadInstalledApps()
            } catch (e: Exception) {
                loadInstalledApps()
            }
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(ioDispatcher) {
            try {
                val packageManager = context.packageManager
                val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { app ->
                        // Filter out system apps (optional, can be configured)
                        (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                    }
                    .map { app ->
                        AppInfo(
                            packageName = app.packageName,
                            appName = app.loadLabel(packageManager).toString(),
                            isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                            isEnabled = enabledApps.contains(app.packageName)
                        )
                    }
                    .sortedBy { it.appName }

                _uiState.value = _uiState.value.copy(
                    apps = installedApps,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false
                )
            }
        }
    }

    fun toggleApp(packageName: String) {
        if (enabledApps.contains(packageName)) {
            enabledApps.remove(packageName)
        } else {
            enabledApps.add(packageName)
        }

        // Update UI state
        _uiState.value = _uiState.value.copy(
            apps = _uiState.value.apps.map { app ->
                if (app.packageName == packageName) {
                    app.copy(isEnabled = !app.isEnabled)
                } else {
                    app
                }
            }
        )

        persistSelection()
    }

    private fun persistSelection() {
        viewModelScope.launch(ioDispatcher) {
            try {
                val settings = settingsRepository.getSettings()
                val updated = settings.copy(
                    splitTunneling = true,
                    excludedApps = enabledApps.toSet()
                )
                settingsRepository.updateSettings(updated)
            } catch (_: Exception) {
                // Ignore persistence errors for now but keep UI responsive
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun getFilteredApps(): List<AppInfo> {
        val query = _uiState.value.searchQuery.lowercase()
        return if (query.isEmpty()) {
            _uiState.value.apps
        } else {
            _uiState.value.apps.filter {
                it.appName.lowercase().contains(query) ||
                it.packageName.lowercase().contains(query)
            }
        }
    }
}
