package com.easeaudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.easeaudio.R
import com.easeaudio.data.RadioStation
import kotlinx.coroutines.delay

@Composable
fun DiagnosticsDialog(
    station: RadioStation?,
    isPlaying: Boolean,
    isLoading: Boolean,
    currentPositionMs: Long,
    totalDurationMs: Long,
    onDismiss: () -> Unit
) {
    // Dynamic buffer timer ticker
    var ticker by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(1000L)
            ticker++
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.audio_diagnostics_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.audio_diagnostics_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Status Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else if (isLoading) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isPlaying) Color(0xFF4CAF50)
                                        else if (isLoading) Color(0xFFFF9800)
                                        else Color(0xFF9E9E9E)
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isPlaying) stringResource(R.string.stream_live_active)
                                       else if (isLoading) stringResource(R.string.stream_buffering_state)
                                       else stringResource(R.string.stream_idle_state),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = if (station?.isPodcast == true) "VOD / Podcast" else "Live Audio Stream",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Diagnostic Data Grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DiagnosticRow(
                        label = stringResource(R.string.diag_station_name),
                        value = station?.name ?: stringResource(R.string.no_station_selected),
                        icon = Icons.Filled.Radio
                    )
                    DiagnosticRow(
                        label = stringResource(R.string.diag_audio_codec),
                        value = station?.codec?.ifBlank { "AAC / MP3" } ?: "AAC",
                        icon = Icons.Filled.Audiotrack
                    )
                    DiagnosticRow(
                        label = stringResource(R.string.diag_bitrate),
                        value = station?.bitrate?.ifBlank { "128 kbps" } ?: "128 kbps",
                        icon = Icons.Filled.GraphicEq
                    )
                    DiagnosticRow(
                        label = stringResource(R.string.diag_playback_engine),
                        value = "Media3 ExoPlayer v1.5",
                        icon = Icons.Filled.Memory
                    )
                    DiagnosticRow(
                        label = stringResource(R.string.diag_buffer_health),
                        value = if (isPlaying) stringResource(R.string.diag_buffer_optimal) else if (isLoading) stringResource(R.string.diag_buffer_loading) else stringResource(R.string.diag_buffer_standby),
                        icon = Icons.Filled.CheckCircle,
                        valueColor = if (isPlaying) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diag_done_btn")
                ) {
                    Text(
                        text = stringResource(R.string.done),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
    icon: ImageVector,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = valueColor
            )
        }
    }
}
