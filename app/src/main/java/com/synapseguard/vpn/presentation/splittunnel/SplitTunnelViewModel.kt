package com.synapseguard.vpn.presentation.splittunnel

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    private val packageManager: PackageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplitTunnelUiState())
    val uiState: StateFlow<SplitTunnelUiState> = _uiState.asStateFlow()

    private val enabledApps = mutableSetOf<String>()

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            try {
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

        // TODO: Persist to DataStore
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
