package com.synapseguard.vpn

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class SynapseGuardApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging in debug builds
        Timber.plant(Timber.DebugTree())

        Timber.d("SynapseGuard VPN Application started")
    }
}
