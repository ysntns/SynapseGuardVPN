package com.synapseguard.vpn.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapseguard.vpn.domain.model.SubscriptionTier
import com.synapseguard.vpn.presentation.auth.AuthViewModel
import com.synapseguard.vpn.presentation.components.SettingsActionItem
import com.synapseguard.vpn.presentation.components.SettingsToggleItem
import com.synapseguard.vpn.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSplitTunnel: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        color = TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundSecondary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Security Features Section
            item {
                SectionHeader(title = "Security Features")
            }

            item {
                SettingsToggleItem(
                    title = "Kill Switch",
                    description = "Block internet if VPN disconnects",
                    icon = Icons.Default.Security,
                    iconTint = IconRed,
                    checked = uiState.settings.killSwitch,
                    onCheckedChange = { viewModel.updateKillSwitch(it) }
                )
            }

            item {
                SettingsActionItem(
                    title = "Split Tunneling",
                    description = "Choose apps to bypass VPN",
                    icon = Icons.Default.CallSplit,
                    iconTint = CyanPrimary,
                    onClick = onNavigateToSplitTunnel
                )
            }

            item {
                SettingsToggleItem(
                    title = "DNS Leak Protection",
                    description = "Prevent DNS queries from leaking",
                    icon = Icons.Default.Lock,
                    iconTint = IconBlue,
                    checked = true, // Always enabled
                    onCheckedChange = { /* Always on */ }
                )
            }

            // Connection Options Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "Connection Options")
            }

            item {
                SettingsToggleItem(
                    title = "Auto-Connect",
                    description = "Connect VPN automatically on app start",
                    icon = Icons.Default.PowerSettingsNew,
                    iconTint = IconAccent,
                    checked = uiState.settings.autoConnect,
                    onCheckedChange = { viewModel.updateAutoConnect(it) }
                )
            }

            item {
                SettingsToggleItem(
                    title = "Auto-Connect on Boot",
                    description = "Connect VPN when device starts",
                    icon = Icons.Default.PhoneAndroid,
                    iconTint = IconAccent,
                    checked = false, // TODO: Add to settings
                    onCheckedChange = { /* TODO */ }
                )
            }

            item {
                ProtocolSelectionCard(
                    currentProtocol = uiState.settings.preferredProtocol.name,
                    onProtocolClick = { /* TODO: Show protocol selector */ }
                )
            }

            // Advanced Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "Advanced")
            }

            item {
                SettingsActionItem(
                    title = "Send Diagnostic Logs",
                    description = "Help us improve the app",
                    icon = Icons.Default.Warning,
                    iconTint = IconYellow,
                    onClick = { /* TODO: Send logs */ }
                )
            }

            item {
                SettingsActionItem(
                    title = "Connection Log",
                    description = "View detailed connection history",
                    icon = Icons.Default.Storage,
                    iconTint = IconBlue,
                    onClick = { /* TODO: Show connection log */ }
                )
            }

            item {
                SettingsActionItem(
                    title = "About SynapseGuard",
                    description = "Version 1.0.0 • BCI-Optimized VPN",
                    icon = Icons.Default.Info,
                    iconTint = IconAccent,
                    onClick = { /* TODO: Show about screen */ }
                )
            }

            // Account Section
            if (authState.isAuthenticated) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(title = "Account")
                }

                // Subscription Management Card
                item {
                    SubscriptionCard(
                        isPremiumUser = authState.isPremiumUser,
                        subscriptionTier = authState.subscriptionTier,
                        onTogglePremium = { authViewModel.togglePremiumStatus() }
                    )
                }

                // User Info Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = BackgroundSecondary
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = authState.currentUser?.name ?: "User",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = authState.currentUser?.email ?: "",
                                        fontSize = 14.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            authViewModel.logout()
                            onNavigateToLogin()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusDisconnected,
                            contentColor = TextPrimary
                        )
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Logout"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logout")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun ProtocolSelectionCard(
    currentProtocol: String,
    onProtocolClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundSecondary
        ),
        onClick = onProtocolClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "VPN Protocol",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = "Current: $currentProtocol",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = IconSecondary
            )
        }
    }
}

@Composable
private fun SubscriptionCard(
    isPremiumUser: Boolean,
    subscriptionTier: SubscriptionTier,
    onTogglePremium: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPremiumUser) {
                SurfaceSelected
            } else {
                BackgroundSecondary
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isPremiumUser) Icons.Default.WorkspacePremium else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = if (isPremiumUser) IconYellow else IconSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = if (isPremiumUser) "Premium User" else "Free User",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPremiumUser) IconYellow else TextPrimary
                        )
                        Text(
                            text = "Current: ${subscriptionTier.name}",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Demo toggle switch
                Switch(
                    checked = isPremiumUser,
                    onCheckedChange = { onTogglePremium() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = IconYellow,
                        checkedTrackColor = IconYellow.copy(alpha = 0.5f),
                        uncheckedThumbColor = IconSecondary,
                        uncheckedTrackColor = BackgroundCard
                    )
                )
            }

            if (!isPremiumUser) {
                HorizontalDivider(color = BorderPrimary)

                Button(
                    onClick = { /* TODO: Navigate to subscription/purchase screen */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IconYellow,
                        contentColor = BackgroundPrimary
                    )
                ) {
                    Icon(
                        Icons.Default.Stars,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upgrade to Premium",
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "• Access to all premium servers\n• Priority connection speeds\n• Advanced security features\n• No ads",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    text = "✓ Premium features unlocked",
                    fontSize = 14.sp,
                    color = StatusConnected,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
