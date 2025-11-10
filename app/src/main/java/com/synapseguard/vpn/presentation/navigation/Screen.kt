package com.synapseguard.vpn.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Servers : Screen("servers")
    object Settings : Screen("settings")
    object Stats : Screen("stats")
    object SplitTunnel : Screen("split_tunnel")
}
