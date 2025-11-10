package com.synapseguard.vpn.domain.usecase

import com.synapseguard.vpn.domain.repository.VpnRepository
import javax.inject.Inject

class DisconnectVpnUseCase @Inject constructor(
    private val vpnRepository: VpnRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return vpnRepository.disconnect()
    }
}
