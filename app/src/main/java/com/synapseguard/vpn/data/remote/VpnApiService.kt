package com.synapseguard.vpn.data.remote

import com.synapseguard.vpn.data.remote.dto.ServersResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface VpnApiService {

    @GET("servers")
    suspend fun getServers(): Response<ServersResponse>

    @GET("servers")
    suspend fun getServersByProtocol(
        @Query("protocol") protocol: String
    ): Response<ServersResponse>
}
