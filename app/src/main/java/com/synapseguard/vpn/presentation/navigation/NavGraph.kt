package com.synapseguard.vpn.presentation.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.synapseguard.vpn.presentation.auth.AuthViewModel
import com.synapseguard.vpn.presentation.auth.LoginScreen
import com.synapseguard.vpn.presentation.home.HomeScreen
import com.synapseguard.vpn.presentation.servers.ServersScreen
import com.synapseguard.vpn.presentation.settings.SettingsScreen
import com.synapseguard.vpn.presentation.splash.SplashScreen
import com.synapseguard.vpn.presentation.splittunnel.SplitTunnelScreen
import com.synapseguard.vpn.presentation.stats.StatsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    vpnPermissionGranted: Boolean,
    notificationPermissionGranted: Boolean,
    onRequestVpnPermission: (onPermissionGranted: () -> Unit) -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            SplashScreen(
                onSplashFinished = { isAuthenticated ->
                    val destination = if (isAuthenticated) Screen.Home.route else Screen.Login.route
                    navController.navigate(destination) {
                        // Remove splash from back stack
                        popUpTo(Screen.Splash.route) {
                            inclusive = true
                        }
                    }
                },
                authViewModel = authViewModel
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        // Remove login from back stack
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToServers = {
                    navController.navigate(Screen.Servers.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToStats = {
                    navController.navigate(Screen.Stats.route)
                },
                snackbarHostState = snackbarHostState,
                vpnPermissionGranted = vpnPermissionGranted,
                notificationPermissionGranted = notificationPermissionGranted,
                onRequestVpnPermission = onRequestVpnPermission,
                onRequestNotificationPermission = onRequestNotificationPermission
            )
        }

        composable(Screen.Servers.route) {
            ServersScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSplitTunnel = {
                    navController.navigate(Screen.SplitTunnel.route)
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Stats.route) {
            StatsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.SplitTunnel.route) {
            SplitTunnelScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
