package com.synapseguard.vpn.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.synapseguard.vpn.domain.model.VpnProtocol
import com.synapseguard.vpn.domain.model.VpnServer

data class ServerDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("country")
    val country: String,
    @SerializedName("country_code")
    val countryCode: String,
    @SerializedName("city")
    val city: String,
    @SerializedName("ip_address")
    val ipAddress: String,
    @SerializedName("port")
    val port: Int,
    @SerializedName("protocol")
    val protocol: String,
    @SerializedName("latency")
    val latency: Int = 0,
    @SerializedName("load")
    val load: Int = 0,
    @SerializedName("is_premium")
    val isPremium: Boolean = false,
    @SerializedName("config")
    val config: String = ""
)

fun ServerDto.toDomain(): VpnServer {
    return VpnServer(
        id = id,
        name = name,
        country = country,
        countryCode = countryCode,
        city = city,
        ipAddress = ipAddress,
        port = port,
        protocol = VpnProtocol.valueOf(protocol.uppercase()),
        latency = latency,
        load = load,
        isPremium = isPremium,
        config = config
    )
}

data class ServersResponse(
    @SerializedName("servers")
    val servers: List<ServerDto>,
    @SerializedName("success")
    val success: Boolean
)
