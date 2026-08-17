package com.easeaudio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.easeaudio.R
import com.easeaudio.ui.theme.*

@Composable
fun SleepTimerDialog(
    activeTimerMinutes: Int?,
    onSelectMinutes: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    val presets = listOf(15, 30, 45, 60, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Bedtime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(stringResource(R.string.sleep_timer))
            }
        },
        text = {
            var customMinutes by remember { mutableStateOf(30f) }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (activeTimerMinutes != null) {
                    Text(
                        text = stringResource(R.string.sleeping_in, activeTimerMinutes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Button(
                        onClick = onCancelTimer,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary, 
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cancel_sleep_timer")
                    ) {
                        Text(stringResource(R.string.close))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }

                Text(
                    text = "Custom Duration: ${customMinutes.toInt()} minutes",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = customMinutes,
                    onValueChange = { customMinutes = it },
                    valueRange = 1f..120f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.testTag("sleep_timer_custom_slider")
                )
                Button(
                    onClick = { onSelectMinutes(customMinutes.toInt()) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary, 
                        contentColor = MaterialTheme.colorScheme.background
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_set_custom_timer")
                ) {
                    Text(stringResource(R.string.set_custom_timer))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    stringResource(R.string.choose_preset_timer), 
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                    style = MaterialTheme.typography.labelMedium
                )

                presets.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { mins ->
                            var isMinsFocused by remember { mutableStateOf(false) }
                            OutlinedButton(
                                onClick = { onSelectMinutes(mins) },
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { isMinsFocused = it.isFocused }
                                    .testTag("sleep_timer_$mins"),
                                border = BorderStroke(
                                    width = if (isMinsFocused) 2.dp else 1.dp,
                                    color = if (isMinsFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isMinsFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                    contentColor = if (isMinsFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(stringResource(R.string.mins_abbreviation, mins))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_sleep_timer_dialog")
            ) {
                Text(stringResource(R.string.close), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
    )
}
