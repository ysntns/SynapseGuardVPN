package com.synapseguard.vpn.di

import android.content.Context
import androidx.room.Room
import com.synapseguard.vpn.data.local.VpnDatabase
import com.synapseguard.vpn.data.local.dao.ServerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideVpnDatabase(@ApplicationContext context: Context): VpnDatabase {
        return Room.databaseBuilder(
            context,
            VpnDatabase::class.java,
            "synapseguard_vpn_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideServerDao(database: VpnDatabase): ServerDao {
        return database.serverDao()
    }
}
