package com.synapseguard.vpn.domain.usecase

import com.synapseguard.vpn.domain.model.ConnectionStats
import com.synapseguard.vpn.domain.repository.VpnRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveConnectionStatsUseCase @Inject constructor(
    private val vpnRepository: VpnRepository
) {
    operator fun invoke(): Flow<ConnectionStats> {
        return vpnRepository.observeConnectionStats()
    }
}
