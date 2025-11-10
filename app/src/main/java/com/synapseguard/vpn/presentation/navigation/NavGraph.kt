package com.synapseguard.vpn.presentation.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.synapseguard.vpn.presentation.home.HomeScreen
import com.synapseguard.vpn.presentation.servers.ServersScreen
import com.synapseguard.vpn.presentation.settings.SettingsScreen
import com.synapseguard.vpn.presentation.splash.SplashScreen
import com.synapseguard.vpn.presentation.splittunnel.SplitTunnelScreen
import com.synapseguard.vpn.presentation.stats.StatsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Screen.Home.route) {
                        // Remove splash from back stack
                        popUpTo(Screen.Splash.route) {
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
                snackbarHostState = snackbarHostState
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
