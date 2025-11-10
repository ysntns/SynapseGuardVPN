package com.synapseguard.vpn.domain.usecase

import com.synapseguard.vpn.domain.model.VpnState
import com.synapseguard.vpn.domain.repository.VpnRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveVpnStateUseCase @Inject constructor(
    private val vpnRepository: VpnRepository
) {
    operator fun invoke(): Flow<VpnState> {
        return vpnRepository.observeVpnState()
    }
}
