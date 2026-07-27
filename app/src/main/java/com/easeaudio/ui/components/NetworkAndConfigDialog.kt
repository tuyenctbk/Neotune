package com.easeaudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easeaudio.firebase.AppRemoteConfig
import com.easeaudio.network.NetworkStatus
import com.easeaudio.ui.theme.*

@Composable
fun NetworkAndConfigDialog(
    networkStatus: NetworkStatus,
    remoteConfig: AppRemoteConfig,
    onToggleSimulatedAds: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        titleContentColor = TextPrimary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CloudDone,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Adaptive Engine & Firebase",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Network Condition Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceVariant)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (networkStatus.isWifi) Icons.Filled.Wifi else Icons.Filled.CellTower,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Network: ${networkStatus.label}",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Auto-adaptive buffer: ${networkStatus.minBufferMs / 1000}s - ${networkStatus.maxBufferMs / 1000}s capacity to guarantee continuous streaming without buffering stalls.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // 2. Firebase Remote Config Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceVariant)
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "Firebase Remote Config",
                            color = NeonPurple,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Source: ${remoteConfig.configSource}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "AdMob Advertisements",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Always 100% silent (Muted video/audio)",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }

                            Switch(
                                checked = remoteConfig.adsEnabled,
                                onCheckedChange = { onToggleSimulatedAds(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DarkBackground,
                                    checkedTrackColor = NeonCyan,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = CardBorder
                                ),
                                modifier = Modifier.testTag("switch_admob_ads")
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}
