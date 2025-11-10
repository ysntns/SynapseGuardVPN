package com.synapseguard.vpn.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Servers : Screen("servers")
    object Settings : Screen("settings")
}
