package com.synapseguard.vpn.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.synapseguard.vpn.di.IoDispatcher
import com.synapseguard.vpn.domain.model.VpnProtocol
import com.synapseguard.vpn.domain.model.VpnSettings
import com.synapseguard.vpn.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vpn_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SettingsRepository {

    private object PreferenceKeys {
        val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        val KILL_SWITCH = booleanPreferencesKey("kill_switch")
        val SPLIT_TUNNELING = booleanPreferencesKey("split_tunneling")
        val EXCLUDED_APPS = stringSetPreferencesKey("excluded_apps")
        val PREFERRED_PROTOCOL = stringPreferencesKey("preferred_protocol")
        val CUSTOM_DNS = stringSetPreferencesKey("custom_dns")
    }

    override fun observeSettings(): Flow<VpnSettings> {
        return context.dataStore.data.map { preferences ->
            VpnSettings(
                autoConnect = preferences[PreferenceKeys.AUTO_CONNECT] ?: false,
                killSwitch = preferences[PreferenceKeys.KILL_SWITCH] ?: false,
                splitTunneling = preferences[PreferenceKeys.SPLIT_TUNNELING] ?: false,
                excludedApps = preferences[PreferenceKeys.EXCLUDED_APPS] ?: emptySet(),
                preferredProtocol = VpnProtocol.valueOf(
                    preferences[PreferenceKeys.PREFERRED_PROTOCOL] ?: VpnProtocol.WIREGUARD.name
                ),
                customDns = preferences[PreferenceKeys.CUSTOM_DNS]?.toList() ?: emptyList()
            )
        }
    }

    override suspend fun updateSettings(settings: VpnSettings): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                context.dataStore.edit { preferences ->
                    preferences[PreferenceKeys.AUTO_CONNECT] = settings.autoConnect
                    preferences[PreferenceKeys.KILL_SWITCH] = settings.killSwitch
                    preferences[PreferenceKeys.SPLIT_TUNNELING] = settings.splitTunneling
                    preferences[PreferenceKeys.EXCLUDED_APPS] = settings.excludedApps
                    preferences[PreferenceKeys.PREFERRED_PROTOCOL] = settings.preferredProtocol.name
                    preferences[PreferenceKeys.CUSTOM_DNS] = settings.customDns.toSet()
                }
                Timber.d("Settings updated successfully")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update settings")
                Result.failure(e)
            }
        }

    override suspend fun getSettings(): VpnSettings = withContext(ioDispatcher) {
        val preferences = context.dataStore.data.map { it }.first()
        VpnSettings(
            autoConnect = preferences[PreferenceKeys.AUTO_CONNECT] ?: false,
            killSwitch = preferences[PreferenceKeys.KILL_SWITCH] ?: false,
            splitTunneling = preferences[PreferenceKeys.SPLIT_TUNNELING] ?: false,
            excludedApps = preferences[PreferenceKeys.EXCLUDED_APPS] ?: emptySet(),
            preferredProtocol = VpnProtocol.valueOf(
                preferences[PreferenceKeys.PREFERRED_PROTOCOL] ?: VpnProtocol.WIREGUARD.name
            ),
            customDns = preferences[PreferenceKeys.CUSTOM_DNS]?.toList() ?: emptyList()
        )
    }
}

// Extension function to get first value from Flow
private suspend fun <T> Flow<T>.first(): T {
    var result: T? = null
    collect {
        if (result == null) {
            result = it
        }
    }
    return result!!
}
