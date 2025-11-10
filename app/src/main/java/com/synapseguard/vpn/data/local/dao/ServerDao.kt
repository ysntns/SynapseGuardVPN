package com.synapseguard.vpn.data.local.dao

import androidx.room.*
import com.synapseguard.vpn.data.local.entity.ServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {

    @Query("SELECT * FROM servers")
    suspend fun getAllServers(): List<ServerEntity>

    @Query("SELECT * FROM servers WHERE protocol = :protocol")
    suspend fun getServersByProtocol(protocol: String): List<ServerEntity>

    @Query("SELECT * FROM servers WHERE isFavorite = 1")
    fun getFavoriteServers(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE id = :serverId")
    suspend fun getServerById(serverId: String): ServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServers(servers: List<ServerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerEntity)

    @Update
    suspend fun updateServer(server: ServerEntity)

    @Query("UPDATE servers SET isFavorite = :isFavorite WHERE id = :serverId")
    suspend fun updateFavoriteStatus(serverId: String, isFavorite: Boolean)

    @Delete
    suspend fun deleteServer(server: ServerEntity)

    @Query("DELETE FROM servers")
    suspend fun deleteAllServers()
}
