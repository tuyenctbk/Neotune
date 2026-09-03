package com.easeaudio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easeaudio.R
import com.easeaudio.ui.theme.*

@Composable
fun SettingsScreen(
    sleepTimerRemaining: Int?,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenRadioAlarm: () -> Unit,
    selectedCountry: String = "Global",
    onOpenCountryPicker: () -> Unit = {},
    isBatterySaverEnabled: Boolean = false,
    onToggleBatterySaver: () -> Unit = {},
    isAutoPlayOnStartupEnabled: Boolean = true,
    onToggleAutoPlayOnStartup: () -> Unit = {},
    isVolumeSafetyEnabled: Boolean = false,
    onToggleVolumeSafety: () -> Unit = {},
    isNightAudioModeEnabled: Boolean = false,
    onToggleNightAudioMode: () -> Unit = {},
    todayListeningMinutes: Int = 0,
    currentStreakDays: Int = 1,
    onOpenAppearance: () -> Unit,
    onOpenOnboarding: () -> Unit = {},
    onOpenBlockedDialog: () -> Unit,
    onOpenAttribution: () -> Unit,
    onOpenBackup: () -> Unit = {},
    onOpenDiagnostics: () -> Unit = {},
    isAutomotive: Boolean = false,
    bottomPadding: androidx.compose.ui.unit.Dp = 108.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 700.dp),
                contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Automotive OS Info Banner
                if (isAutomotive) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DirectionsCar,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.aaos_title),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = stringResource(R.string.aaos_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Daily Listening Habits & Streak Banner (100% on-device)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = Color(0xFFFF7043),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.listening_stats_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFF7043).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "🔥 $currentStreakDays ${stringResource(R.string.listening_stats_streak)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF7043),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.listening_stats_today),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val hours = todayListeningMinutes / 60
                                    val mins = todayListeningMinutes % 60
                                    val formattedTime = if (hours > 0) {
                                        stringResource(R.string.listening_stats_hours_mins, hours, mins)
                                    } else {
                                        stringResource(R.string.listening_stats_minutes, mins)
                                    }
                                    Text(
                                        text = formattedTime,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(
                                    text = stringResource(R.string.listening_stats_desc),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.widthIn(max = 200.dp)
                                )
                            }
                        }
                    }
                }

                // Header
                item {
                    Text(
                        text = stringResource(R.string.nav_settings),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Audio Comfort & Protection Section
                item {
                    SettingsSectionHeader(title = stringResource(R.string.settings_section_audio_comfort))
                }

                item {
                    SettingsCard {
                        SettingsSwitchItem(
                            icon = Icons.Filled.HealthAndSafety,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.settings_volume_safety_title),
                            subtitle = stringResource(R.string.settings_volume_safety_desc),
                            checked = isVolumeSafetyEnabled,
                            onCheckedChange = { onToggleVolumeSafety() },
                            testTag = "setting_volume_safety"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                        SettingsSwitchItem(
                            icon = Icons.Filled.NightsStay,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.settings_night_mode_title),
                            subtitle = stringResource(R.string.settings_night_mode_desc),
                            checked = isNightAudioModeEnabled,
                            onCheckedChange = { onToggleNightAudioMode() },
                            testTag = "setting_night_mode"
                        )
                    }
                }

                // Audio & Playback Section
                item {
                    SettingsSectionHeader(title = stringResource(R.string.settings_section_audio_playback))
                }

                item {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Filled.Equalizer,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.equalizer),
                            subtitle = stringResource(R.string.settings_equalizer_desc),
                            onClick = onOpenEqualizer,
                            testTag = "setting_equalizer"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                        SettingsItem(
                            icon = Icons.Filled.Bedtime,
                            iconTint = if (sleepTimerRemaining != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            title = stringResource(R.string.sleep_timer),
                            subtitle = sleepTimerRemaining?.let { stringResource(R.string.sleeping_in, it) } ?: stringResource(R.string.settings_sleep_timer_desc),
                            onClick = onOpenSleepTimer,
                            testTag = "setting_sleep_timer"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                        SettingsItem(
                            icon = Icons.Filled.Alarm,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.radio_alarm),
                            subtitle = stringResource(R.string.settings_radio_alarm_desc),
                            onClick = onOpenRadioAlarm,
                            testTag = "setting_radio_alarm"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                        SettingsSwitchItem(
                            icon = Icons.Filled.BatterySaver,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.battery_saver_mode),
                            subtitle = stringResource(R.string.battery_saver_mode_desc),
                            checked = isBatterySaverEnabled,
                            onCheckedChange = { onToggleBatterySaver() },
                            testTag = "setting_battery_saver"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                        SettingsSwitchItem(
                            icon = Icons.Filled.PlayCircle,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.settings_auto_play),
                            subtitle = stringResource(R.string.settings_auto_play_desc),
                            checked = isAutoPlayOnStartupEnabled,
                            onCheckedChange = { onToggleAutoPlayOnStartup() },
                            testTag = "setting_auto_play"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                        SettingsItem(
                            icon = Icons.Filled.Speed,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.settings_diagnostics_title),
                            subtitle = stringResource(R.string.settings_diagnostics_desc),
                            onClick = onOpenDiagnostics,
                            testTag = "setting_diagnostics"
                        )
                    }
                }

                // Appearance & App Tour Section
                item {
                    SettingsSectionHeader(title = stringResource(R.string.settings_section_appearance_tour))
                }

                item {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Filled.Palette,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.appearance),
                            subtitle = stringResource(R.string.settings_appearance_desc),
                            onClick = onOpenAppearance,
                            testTag = "setting_appearance"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                        SettingsItem(
                            icon = Icons.Filled.Explore,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.onboarding_app_tour),
                            subtitle = stringResource(R.string.settings_onboarding_desc),
                            onClick = onOpenOnboarding,
                            testTag = "setting_onboarding"
                        )
                    }
                }

                // Safety & Filters Section
                item {
                    SettingsSectionHeader(title = stringResource(R.string.settings_section_content_privacy))
                }

                item {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Filled.Shield,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.content_filters_blocklist),
                            subtitle = stringResource(R.string.settings_content_filters_desc),
                            onClick = onOpenBlockedDialog,
                            testTag = "setting_blocked_stations"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                        SettingsItem(
                            icon = Icons.Filled.Info,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            title = stringResource(R.string.info),
                            subtitle = stringResource(R.string.settings_attribution_desc),
                            onClick = onOpenAttribution,
                            testTag = "setting_attribution"
                        )
                    }
                }

                // Data & Backup Section
                item {
                    SettingsSectionHeader(title = stringResource(R.string.settings_section_data_backup))
                }

                item {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Filled.Backup,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.settings_backup_title),
                            subtitle = stringResource(R.string.settings_backup_desc),
                            onClick = onOpenBackup,
                            testTag = "setting_backup_restore"
                        )
                    }
                }

                // Web & Cross-Platform Section
                item {
                    SettingsSectionHeader(title = stringResource(R.string.settings_section_web_pwa))
                }

                item {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Filled.Language,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.settings_pwa_title),
                            subtitle = stringResource(R.string.settings_pwa_desc),
                            onClick = {
                                try {
                                    val pwaIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://neotune.ai.studio"))
                                    context.startActivity(pwaIntent)
                                } catch (e: Exception) {
                                    Log.e("SettingsScreen", "Failed to open PWA link: ${e.message}")
                                }
                            },
                            testTag = "setting_open_pwa"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            content()
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isFocused) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }
    // Active icon tint: full primary when enabled, muted when disabled
    val activeIconTint = if (checked) iconTint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val activeIconBg = if (checked) iconTint.copy(alpha = 0.15f) else Color.Transparent
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(12.dp),
        color = if (isFocused) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with subtle background circle when active
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(activeIconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = activeIconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
