package com.easeaudio.ui.components

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.easeaudio.R
import com.easeaudio.alarm.RadioAlarmManager
import com.easeaudio.data.RadioStation
import com.easeaudio.ui.theme.*
import java.util.Locale

@Composable
fun AlarmDialog(
    currentStation: RadioStation?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentConfig = remember { RadioAlarmManager.getAlarmConfig(context) }

    var isEnabled by remember { mutableStateOf(currentConfig.isEnabled) }
    var hour by remember { mutableStateOf(currentConfig.hour) }
    var minute by remember { mutableStateOf(currentConfig.minute) }
    var targetStationName by remember { mutableStateOf(currentConfig.stationName.ifBlank { currentStation?.name ?: "Current Station" }) }
    var targetStationUrl by remember { mutableStateOf(currentConfig.stationUrl.ifBlank { currentStation?.streamUrl ?: "" }) }
    var targetStationId by remember { mutableStateOf(currentConfig.stationId.ifBlank { currentStation?.id ?: "" }) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Row
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
                                imageVector = Icons.Filled.Alarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.radio_alarm),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
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

                Spacer(modifier = Modifier.height(16.dp))

                // Time Display Card
                val timeString = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            TimePickerDialog(
                                context,
                                { _, selectedHour, selectedMinute ->
                                    hour = selectedHour
                                    minute = selectedMinute
                                },
                                hour,
                                minute,
                                true
                            ).show()
                        },
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = stringResource(R.string.alarm_time), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                Text(text = timeString, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text(text = stringResource(R.string.tap_to_change), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Target Station Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Filled.Radio, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = stringResource(R.string.wake_station), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            Text(
                                text = if (currentStation != null) currentStation.name else targetStationName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Enable Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.enable_alarm),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.background,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val activeStationName = currentStation?.name ?: targetStationName.ifBlank { "BBC World Service" }
                            val activeStationUrl = currentStation?.streamUrl ?: targetStationUrl.ifBlank { "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service" }
                            val activeStationId = currentStation?.id ?: targetStationId.ifBlank { "bbc_world_service" }

                            RadioAlarmManager.setAlarm(
                                context = context,
                                enabled = isEnabled,
                                hour = hour,
                                minute = minute,
                                stationId = activeStationId,
                                stationName = activeStationName,
                                stationUrl = activeStationUrl
                            )

                            val msg = if (isEnabled) {
                                context.getString(R.string.alarm_set_for, String.format(Locale.getDefault(), "%02d:%02d", hour, minute))
                            } else {
                                context.getString(R.string.alarm_off)
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.background),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = stringResource(R.string.save), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
