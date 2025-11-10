package com.synapseguard.vpn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.synapseguard.vpn.domain.model.VpnProtocol
import com.synapseguard.vpn.domain.model.VpnServer

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val country: String,
    val countryCode: String,
    val city: String,
    val ipAddress: String,
    val port: Int,
    val protocol: String,
    val latency: Int,
    val load: Int,
    val isPremium: Boolean,
    val config: String,
    val isFavorite: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

fun ServerEntity.toDomain(): VpnServer {
    return VpnServer(
        id = id,
        name = name,
        country = country,
        countryCode = countryCode,
        city = city,
        ipAddress = ipAddress,
        port = port,
        protocol = VpnProtocol.valueOf(protocol),
        latency = latency,
        load = load,
        isPremium = isPremium,
        config = config
    )
}

fun VpnServer.toEntity(isFavorite: Boolean = false): ServerEntity {
    return ServerEntity(
        id = id,
        name = name,
        country = country,
        countryCode = countryCode,
        city = city,
        ipAddress = ipAddress,
        port = port,
        protocol = protocol.name,
        latency = latency,
        load = load,
        isPremium = isPremium,
        config = config,
        isFavorite = isFavorite
    )
}
