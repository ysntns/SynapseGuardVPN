package com.synapseguard.vpn.presentation.servers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapseguard.vpn.presentation.auth.AuthViewModel
import com.synapseguard.vpn.presentation.components.ServerListItem
import com.synapseguard.vpn.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(
    onNavigateBack: () -> Unit,
    viewModel: ServersViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar for errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            // Snackbar will be handled by MainActivity's SnackbarHost
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = BackgroundPrimary,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Select Server",
                            color = TextPrimary
                        )
                        if (uiState.servers.isNotEmpty()) {
                            Text(
                                "${uiState.servers.size} servers available",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                actions = {
                    // Loading indicator
                    AnimatedVisibility(visible = uiState.isRefreshingLatencies) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = CyanPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Refresh latencies button
                    IconButton(onClick = { viewModel.refreshLatencies() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh Latencies",
                            tint = IconPrimary
                        )
                    }

                    // Sort menu
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                Icons.Default.Sort,
                                contentDescription = "Sort",
                                tint = IconPrimary
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(BackgroundSecondary)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Default",
                                        color = if (uiState.sortOrder == ServerSortOrder.DEFAULT) CyanPrimary else TextPrimary
                                    )
                                },
                                onClick = {
                                    viewModel.setSortOrder(ServerSortOrder.DEFAULT)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Latency (Low to High)",
                                        color = if (uiState.sortOrder == ServerSortOrder.LATENCY_ASC) CyanPrimary else TextPrimary
                                    )
                                },
                                onClick = {
                                    viewModel.setSortOrder(ServerSortOrder.LATENCY_ASC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Latency (High to Low)",
                                        color = if (uiState.sortOrder == ServerSortOrder.LATENCY_DESC) CyanPrimary else TextPrimary
                                    )
                                },
                                onClick = {
                                    viewModel.setSortOrder(ServerSortOrder.LATENCY_DESC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Name (A-Z)",
                                        color = if (uiState.sortOrder == ServerSortOrder.NAME_ASC) CyanPrimary else TextPrimary
                                    )
                                },
                                onClick = {
                                    viewModel.setSortOrder(ServerSortOrder.NAME_ASC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Load (Low to High)",
                                        color = if (uiState.sortOrder == ServerSortOrder.LOAD_ASC) CyanPrimary else TextPrimary
                                    )
                                },
                                onClick = {
                                    viewModel.setSortOrder(ServerSortOrder.LOAD_ASC)
                                    showSortMenu = false
                                }
                            )
                        }
                    }

                    // Search button
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
                            onClick = { viewModel.selectBestServer() },
                            enabled = !uiState.isRefreshingLatencies && uiState.servers.isNotEmpty(),
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
            items(
                items = uiState.servers,
                key = { server -> server.id }
            ) { server ->
                val canAccess = !server.isPremium || authState.isPremiumUser

                ServerListItem(
                    server = server,
                    isSelected = uiState.selectedServer?.id == server.id,
                    canAccess = canAccess,
                    onClick = {
                        if (canAccess) {
                            viewModel.selectServer(server)
                        } else {
                            // Show snackbar for premium servers
                            kotlinx.coroutines.GlobalScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Upgrade to Premium to access this server",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                )
            }

            // Empty state
            if (uiState.servers.isEmpty() && !uiState.isLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No servers available",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Pull to refresh",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
