package com.synapseguard.vpn.presentation

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.synapseguard.vpn.presentation.navigation.NavGraph
import com.synapseguard.vpn.presentation.theme.SynapseGuardVPNTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // VPN permission state
    private val vpnPermissionGranted = mutableStateOf(false)
    private var pendingVpnConnection: (() -> Unit)? = null

    // VPN permission launcher
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Timber.d("VPN permission granted")
            vpnPermissionGranted.value = true
            // Execute pending connection if any
            pendingVpnConnection?.invoke()
            pendingVpnConnection = null
        } else {
            Timber.w("VPN permission denied by user")
            vpnPermissionGranted.value = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check initial VPN permission state
        checkVpnPermission()

        setContent {
            SynapseGuardVPNTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Recheck permission when app comes to foreground
        checkVpnPermission()
    }

    /**
     * Check if VPN permission is already granted
     */
    private fun checkVpnPermission() {
        val intent = VpnService.prepare(this)
        vpnPermissionGranted.value = (intent == null)
        Timber.d("VPN permission check: ${vpnPermissionGranted.value}")
    }

    /**
     * Request VPN permission and execute callback when granted
     */
    fun requestVpnPermission(onPermissionGranted: () -> Unit) {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            Timber.d("Requesting VPN permission")
            pendingVpnConnection = onPermissionGranted
            vpnPermissionLauncher.launch(intent)
        } else {
            Timber.d("VPN permission already granted")
            vpnPermissionGranted.value = true
            onPermissionGranted()
        }
    }

    companion object {
        // Static reference to MainActivity for accessing permission request
        // This is a simple approach - in production, use a better DI solution
        private var instance: MainActivity? = null

        fun getInstance(): MainActivity? = instance

        fun requestVpnPermissionStatic(onGranted: () -> Unit) {
            instance?.requestVpnPermission(onGranted)
        }
    }

    override fun onStart() {
        super.onStart()
        instance = this
    }

    override fun onStop() {
        super.onStop()
        if (instance == this) {
            instance = null
        }
    }
}
