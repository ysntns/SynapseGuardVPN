package com.synapseguard.vpn.presentation.servers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synapseguard.vpn.domain.model.VpnProtocol
import com.synapseguard.vpn.domain.model.VpnServer
import com.synapseguard.vpn.presentation.components.ServerListItem
import com.synapseguard.vpn.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(
    onNavigateBack: () -> Unit
) {
    var selectedServer by remember { mutableStateOf<String?>(null) }

    // Mock server data - Comprehensive server list
    val mockServers = remember {
        listOf(
            // Europe
            VpnServer(
                id = "de-fra-1",
                name = "Frankfurt",
                country = "Germany",
                countryCode = "DE",
                city = "Frankfurt",
                ipAddress = "192.168.1.1",
                port = 51820,
                protocol = VpnProtocol.WIREGUARD,
                latency = 22,
                load = 15
            ),
            VpnServer(
                id = "fr-par-1",
                name = "Paris",
                country = "France",
                countryCode = "FR",
                city = "Paris",
                ipAddress = "192.168.1.2",
                port = 51820,
                protocol = VpnProtocol.WIREGUARD,
                latency = 28,
                load = 22
            ),
            VpnServer(
                id = "uk-lon-1",
                name = "London",
                country = "United Kingdom",
                countryCode = "GB",
                city = "London",
                ipAddress = "192.168.1.3",
                port = 51820,
                protocol = VpnProtocol.WIREGUARD,
                latency = 40,
                load = 35
            ),
            VpnServer(
                id = "tr-ist-1",
                name = "Istanbul",
                country = "Turkey",
                countryCode = "TR",
                city = "Istanbul",
                ipAddress = "192.168.1.4",
                port = 51820,
                protocol = VpnProtocol.WIREGUARD,
                latency = 35,
                load = 45
            ),
            // Americas
            VpnServer(
                id = "ca-tor-1",
                name = "Toronto",
                country = "Canada",
                countryCode = "CA",
                city = "Toronto",
                ipAddress = "192.168.1.5",
                port = 51820,
                protocol = VpnProtocol.WIREGUARD,
                latency = 45,
                load = 50
            ),
            VpnServer(
                id = "us-ny-1",
                name = "New York",
                country = "United States",
                countryCode = "US",
                city = "New York",
                ipAddress = "192.168.1.6",
                port = 51820,
                protocol = VpnProtocol.WIREGUARD,
                latency = 50,
                load = 60
            ),
            // Asia-Pacific
            VpnServer(
                id = "jp-tok-1",
                name = "Tokyo",
                country = "Japan",
                countryCode = "JP",
                city = "Tokyo",
                ipAddress = "192.168.1.7",
                port = 51820,
                protocol = VpnProtocol.WIREGUARD,
                latency = 88,
                load = 25
            ),
            VpnServer(
                id = "sg-sin-1",
                name = "Singapore",
                country = "Singapore",
                countryCode = "SG",
                city = "Singapore",
                ipAddress = "192.168.1.8",
                port = 51820,
                protocol = VpnProtocol.WIREGUARD,
                latency = 65,
                load = 30
            ),
            // Middle East
            VpnServer(
                id = "ae-dub-1",
                name = "Dubai",
                country = "UAE",
                countryCode = "AE",
                city = "Dubai",
                ipAddress = "192.168.1.9",
                port = 51820,
                protocol = VpnProtocol.WIREGUARD,
                latency = 55,
                load = 20
            )
        )
    }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Select Server",
                        color = TextPrimary
                    )
                },
                actions = {
                    IconButton(onClick = { /* TODO: Search */ }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = IconPrimary
                        )
                    }
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
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // AI-Optimized Selection Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = BackgroundCard
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(32.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI-Optimized Selection",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Based on latency, load, and location",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }

                        FilledTonalButton(
                            onClick = { /* TODO: Auto-select best server */ },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = CyanPrimary.copy(alpha = 0.2f),
                                contentColor = CyanPrimary
                            )
                        ) {
                            Text("Auto")
                        }
                    }
                }
            }

            // Section header
            item {
                Text(
                    text = "All Servers",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // Server list
            items(mockServers) { server ->
                ServerListItem(
                    server = server,
                    isSelected = selectedServer == server.id,
                    onClick = {
                        selectedServer = server.id
                        // TODO: Update selected server in ViewModel
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
