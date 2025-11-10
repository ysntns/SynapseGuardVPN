package com.synapseguard.vpn.domain.usecase

import com.synapseguard.vpn.domain.model.VpnServer
import com.synapseguard.vpn.domain.repository.ServerRepository
import javax.inject.Inject

class GetServersUseCase @Inject constructor(
    private val serverRepository: ServerRepository
) {
    suspend operator fun invoke(): Result<List<VpnServer>> {
        return serverRepository.getServers()
    }
}
