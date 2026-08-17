package com.easeaudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.easeaudio.R
import com.easeaudio.ui.theme.*
import com.easeaudio.viewmodel.EqPresetDisplay

@Composable
fun EqualizerDialog(
    activePreset: String,
    presets: List<EqPresetDisplay>,
    isAudioBoosterEnabled: Boolean = true,
    onToggleAudioBooster: (Boolean) -> Unit = {},
    onSelectPreset: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Equalizer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(stringResource(R.string.equalizer), color = MaterialTheme.colorScheme.onSurface)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Audio Booster / Loudness Normalizer Card
                var isBoosterFocused by remember { mutableStateOf(false) }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isBoosterFocused = it.isFocused }
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (isBoosterFocused) 2.dp else 0.dp,
                            color = if (isBoosterFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.loudness_booster),
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.normalize_stream_volume),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Switch(
                            checked = isAudioBoosterEnabled,
                            onCheckedChange = onToggleAudioBooster,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.background,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("switch_audio_booster")
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                Text(stringResource(R.string.select_tuning_profile), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)

                // Presets
                presets.forEach { preset ->
                    val isSelected = preset.key == activePreset
                    EqPresetItem(
                        preset = preset,
                        isSelected = isSelected,
                        onClick = { onSelectPreset(preset.key) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done), color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
fun EqPresetItem(
    preset: EqPresetDisplay,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .testTag("eq_preset_${preset.key}"),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else if (isFocused) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(preset.labelResId),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer)
                )
            }
        }
    }
}
