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
    isLightMode: Boolean = false,
    onToggleLightMode: () -> Unit = {},
    onOpenAppearance: () -> Unit,
    onOpenOnboarding: () -> Unit = {},
    onOpenBlockedDialog: () -> Unit,
    onOpenAttribution: () -> Unit,
    isAutomotive: Boolean = false,
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
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
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
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        try {
                                            val carMediaIntent = Intent("android.car.intent.action.MEDIA_TEMPLATE").apply {
                                                putExtra(
                                                    "android.car.intent.extra.MEDIA_COMPONENT",
                                                    android.content.ComponentName("com.neotune.radio", "com.easeaudio.service.RadioPlaybackService").flattenToString()
                                                )
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(carMediaIntent)
                                        } catch (e: Exception) {
                                            try {
                                                val fallbackIntent = Intent(Intent.ACTION_MAIN).apply {
                                                    addCategory(Intent.CATEGORY_APP_MUSIC)
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(fallbackIntent)
                                            } catch (ex: Exception) {
                                                Log.e("SettingsScreen", "Failed to launch Car Media Center", ex)
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.aaos_open_media_center),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
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
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
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
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                        SettingsSwitchItem(
                            icon = Icons.Filled.LightMode,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.settings_light_theme),
                            subtitle = stringResource(R.string.settings_light_theme_desc),
                            checked = isLightMode,
                            onCheckedChange = { onToggleLightMode() },
                            testTag = "setting_light_mode"
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
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://neotune.ai.studio/")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("SettingsScreen", "Failed to open NeoTune PWA link", e)
                                }
                            },
                            testTag = "setting_neotune_pwa"
                        )
                    }
                }

                // Support & Community Section
                item {
                    SettingsSectionHeader(title = stringResource(R.string.support_development_header))
                }

                item {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Filled.LocalCafe,
                            iconTint = Color(0xFFFF813F),
                            title = stringResource(R.string.donate_buymeacoffee),
                            subtitle = stringResource(R.string.donate_buymeacoffee_desc),
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/tuyenphamvn")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("SettingsScreen", "Failed to open BuyMeACoffee link", e)
                                }
                            },
                            testTag = "setting_donate_buymeacoffee"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                        SettingsItem(
                            icon = Icons.Filled.VolunteerActivism,
                            iconTint = Color(0xFF0079C1),
                            title = stringResource(R.string.donate_paypal),
                            subtitle = stringResource(R.string.donate_paypal_desc),
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://paypal.me/tuyenphamvn")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("SettingsScreen", "Failed to open PayPal donation link", e)
                                }
                            },
                            testTag = "setting_donate_paypal"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .background(if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isFocused) MaterialTheme.colorScheme.primary else iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isFocused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .background(if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) MaterialTheme.colorScheme.primary else iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isFocused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.background,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
