package com.synapseguard.vpn.presentation

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.synapseguard.vpn.presentation.navigation.NavGraph
import com.synapseguard.vpn.presentation.navigation.Screen
import com.synapseguard.vpn.presentation.theme.*
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
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                MainScaffold(
                    navController = navController,
                    snackbarHostState = snackbarHostState
                )
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

/**
 * Main scaffold with bottom navigation and snackbar support
 */
@Composable
private fun MainScaffold(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    // Define screens that should show bottom navigation
    val bottomNavScreens = listOf(
        Screen.Home.route,
        Screen.Servers.route,
        Screen.Settings.route
    )

    // Show bottom nav only on main screens
    val showBottomNav = currentRoute in bottomNavScreens

    Scaffold(
        containerColor = BackgroundPrimary,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = BackgroundSecondary,
                        contentColor = TextPrimary,
                        actionColor = CyanPrimary
                    )
                }
            )
        },
        bottomBar = {
            if (showBottomNav) {
                VpnBottomNavigationBar(
                    navController = navController,
                    currentDestination = currentDestination
                )
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = BackgroundPrimary
        ) {
            NavGraph(
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }
    }
}

/**
 * Bottom navigation bar for main screens
 */
@Composable
private fun VpnBottomNavigationBar(
    navController: NavHostController,
    currentDestination: androidx.navigation.NavDestination?
) {
    NavigationBar(
        containerColor = BackgroundSecondary,
        contentColor = TextPrimary
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(item.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CyanPrimary,
                    selectedTextColor = CyanPrimary,
                    unselectedIconColor = IconSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = SurfaceSelected
                )
            )
        }
    }
}

/**
 * Bottom navigation item data class
 */
private data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Bottom navigation items
 */
private val bottomNavItems = listOf(
    BottomNavItem(
        route = Screen.Home.route,
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    BottomNavItem(
        route = Screen.Servers.route,
        label = "Servers",
        selectedIcon = Icons.Filled.Storage,
        unselectedIcon = Icons.Outlined.Storage
    ),
    BottomNavItem(
        route = Screen.Settings.route,
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)
