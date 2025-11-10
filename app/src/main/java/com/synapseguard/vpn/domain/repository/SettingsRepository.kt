package com.synapseguard.vpn.domain.repository

import com.synapseguard.vpn.domain.model.VpnSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<VpnSettings>
    suspend fun updateSettings(settings: VpnSettings): Result<Unit>
    suspend fun getSettings(): VpnSettings
}
