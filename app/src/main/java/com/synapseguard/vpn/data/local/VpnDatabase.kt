package com.synapseguard.vpn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.synapseguard.vpn.data.local.dao.ServerDao
import com.synapseguard.vpn.data.local.entity.ServerEntity

@Database(
    entities = [ServerEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VpnDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
}
