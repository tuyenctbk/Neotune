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
        containerColor = DarkSurface,
        titleContentColor = TextPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Bedtime,
                    contentDescription = null,
                    tint = NeonPurple,
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
                        color = NeonCyan
                    )
                    Button(
                        onClick = onCancelTimer,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink, contentColor = DarkBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cancel_sleep_timer")
                    ) {
                        Text(stringResource(R.string.close))
                    }
                    HorizontalDivider(color = CardBorder)
                }

                Text(
                    text = "Custom Duration: ${customMinutes.toInt()} minutes",
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = customMinutes,
                    onValueChange = { customMinutes = it },
                    valueRange = 1f..120f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = CardBorder
                    ),
                    modifier = Modifier.testTag("sleep_timer_custom_slider")
                )
                Button(
                    onClick = { onSelectMinutes(customMinutes.toInt()) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_set_custom_timer")
                ) {
                    Text(stringResource(R.string.set_custom_timer))
                }

                HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp))

                Text(stringResource(R.string.choose_preset_timer), color = TextSecondary, style = MaterialTheme.typography.labelMedium)

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
                                    color = if (isMinsFocused) NeonCyan else CardBorder
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isMinsFocused) NeonCyan.copy(alpha = 0.15f) else Color.Transparent,
                                    contentColor = if (isMinsFocused) NeonCyan else TextPrimary
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
                Text(stringResource(R.string.close), color = TextMuted)
            }
        }
    )
}
