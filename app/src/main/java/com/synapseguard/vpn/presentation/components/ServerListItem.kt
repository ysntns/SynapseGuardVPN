package com.synapseguard.vpn.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synapseguard.vpn.domain.model.VpnServer
import com.synapseguard.vpn.presentation.theme.*

@Composable
fun ServerListItem(
    server: VpnServer,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    canAccess: Boolean = true // Whether user can access this server (for premium restrictions)
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = canAccess,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                SurfaceSelected
            } else if (!canAccess) {
                BackgroundSecondary.copy(alpha = 0.5f)
            } else {
                BackgroundSecondary
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, BorderAccent)
        } else if (server.isPremium && !canAccess) {
            BorderStroke(1.dp, IconYellow.copy(alpha = 0.5f))
        } else {
            BorderStroke(1.dp, BorderPrimary)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Flag + Server info
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Country flag placeholder (will be replaced with actual flag)
                Text(
                    text = getFlagEmoji(server.countryCode),
                    fontSize = 32.sp
                )

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = server.city,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canAccess) TextPrimary else TextSecondary
                        )
                        if (server.isPremium) {
                            Icon(
                                Icons.Default.WorkspacePremium,
                                contentDescription = "Premium Server",
                                tint = IconYellow,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = server.country,
                        fontSize = 14.sp,
                        color = if (canAccess) TextSecondary else TextSecondary.copy(alpha = 0.5f)
                    )
                }
            }

            // Right side: Stats + Selection indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Server load or Premium label
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    if (!canAccess && server.isPremium) {
                        Text(
                            text = "Premium",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = IconYellow
                        )
                        Text(
                            text = "Locked",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    } else {
                        Text(
                            text = "${server.load}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canAccess) getLoadColor(server.load) else getLoadColor(server.load).copy(alpha = 0.5f)
                        )
                        Text(
                            text = "${server.latency}ms",
                            fontSize = 12.sp,
                            color = if (canAccess) TextSecondary else TextSecondary.copy(alpha = 0.5f)
                        )
                    }
                }

                // Selection indicator
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = CyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Get flag emoji for country code
 */
private fun getFlagEmoji(countryCode: String): String {
    // Convert country code to flag emoji
    return countryCode.uppercase()
        .map { char -> Character.codePointAt("$char", 0) - 0x41 + 0x1F1E6 }
        .map { codePoint -> Character.toChars(codePoint) }
        .joinToString("") { String(it) }
}

/**
 * Get color based on server load
 */
private fun getLoadColor(load: Int): androidx.compose.ui.graphics.Color {
    return when {
        load < 30 -> StatusConnected // Green - Low load
        load < 70 -> StatusConnecting // Yellow - Medium load
        else -> StatusDisconnected // Red - High load
    }
}
