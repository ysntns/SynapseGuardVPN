package com.synapseguard.vpn.domain.usecase

import com.synapseguard.vpn.domain.model.ConnectionConfig
import com.synapseguard.vpn.domain.repository.VpnRepository
import javax.inject.Inject

class ConnectVpnUseCase @Inject constructor(
    private val vpnRepository: VpnRepository
) {
    suspend operator fun invoke(config: ConnectionConfig): Result<Unit> {
        return vpnRepository.connect(config)
    }
}
