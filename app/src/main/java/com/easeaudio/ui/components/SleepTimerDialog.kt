package com.easeaudio.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

                Text(stringResource(R.string.sleep_timer), color = TextSecondary)

                presets.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { mins ->
                            OutlinedButton(
                                onClick = { onSelectMinutes(mins) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sleep_timer_$mins"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = TextPrimary
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
