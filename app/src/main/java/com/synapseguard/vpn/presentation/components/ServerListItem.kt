package com.synapseguard.vpn.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    canAccess: Boolean = true
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = canAccess,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
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
            BorderStroke(2.dp, CyanPrimary)
        } else if (server.isPremium && !canAccess) {
            BorderStroke(1.dp, IconYellow.copy(alpha = 0.5f))
        } else {
            BorderStroke(1.dp, BorderPrimary)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Flag + Server info
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Country flag with background
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BackgroundCard),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getFlagEmoji(server.countryCode),
                        fontSize = 24.sp
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${server.city}, ${server.country}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) CyanPrimary else if (canAccess) TextPrimary else TextSecondary
                        )
                        if (server.isPremium) {
                            Icon(
                                Icons.Default.WorkspacePremium,
                                contentDescription = "Premium Server",
                                tint = IconYellow,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Right side: Stats + Selection indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!canAccess && server.isPremium) {
                    // Premium locked label
                    Text(
                        text = "Premium",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = IconYellow
                    )
                } else {
                    // Load and Ping info
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Load: ${server.load}%",
                            fontSize = 12.sp,
                            color = getLoadColor(server.load)
                        )
                        Text(
                            text = "Ping: ${server.latency} ms",
                            fontSize = 12.sp,
                            color = TextSecondary
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
