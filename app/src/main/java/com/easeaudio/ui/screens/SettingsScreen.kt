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
                    SettingsSectionHeader(title = "Audio & Playback")
                }

                item {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Filled.Equalizer,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.equalizer),
                            subtitle = "Customize bass, treble & audio presets",
                            onClick = onOpenEqualizer,
                            testTag = "setting_equalizer"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                        SettingsItem(
                            icon = Icons.Filled.Bedtime,
                            iconTint = if (sleepTimerRemaining != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            title = stringResource(R.string.sleep_timer),
                            subtitle = sleepTimerRemaining?.let { stringResource(R.string.sleeping_in, it) } ?: "Set automatic sleep timer",
                            onClick = onOpenSleepTimer,
                            testTag = "setting_sleep_timer"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                        SettingsItem(
                            icon = Icons.Filled.Alarm,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.radio_alarm),
                            subtitle = "Wake up to your favorite live stream",
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
                            title = "Auto-Play on Startup",
                            subtitle = "Resume the last played station/podcast on app launch",
                            checked = isAutoPlayOnStartupEnabled,
                            onCheckedChange = { onToggleAutoPlayOnStartup() },
                            testTag = "setting_auto_play"
                        )
                    }
                }

                // Appearance & App Tour Section
                item {
                    SettingsSectionHeader(title = "Appearance & Tour")
                }

                item {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Filled.Palette,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.appearance),
                            subtitle = "Themes, accent colors & languages",
                            onClick = onOpenAppearance,
                            testTag = "setting_appearance"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                        SettingsItem(
                            icon = Icons.Filled.Explore,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.onboarding_app_tour),
                            subtitle = "Replay onboarding walkthrough & setup",
                            onClick = onOpenOnboarding,
                            testTag = "setting_onboarding"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                        SettingsSwitchItem(
                            icon = Icons.Filled.LightMode,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Light Theme",
                            subtitle = "Toggle comfortable light theme for long playback sessions",
                            checked = isLightMode,
                            onCheckedChange = { onToggleLightMode() },
                            testTag = "setting_light_mode"
                        )
                    }
                }

                // Safety & Filters Section
                item {
                    SettingsSectionHeader(title = "Content & Privacy")
                }

                item {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Filled.Shield,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.content_filters_blocklist),
                            subtitle = "Manage hidden stations & safety filters",
                            onClick = onOpenBlockedDialog,
                            testTag = "setting_blocked_stations"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                        SettingsItem(
                            icon = Icons.Filled.Info,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            title = stringResource(R.string.info),
                            subtitle = "Data providers & API attributions",
                            onClick = onOpenAttribution,
                            testTag = "setting_attribution"
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
                            icon = Icons.Filled.Favorite,
                            iconTint = Color(0xFFEA4AAA),
                            title = stringResource(R.string.donate_github_sponsors),
                            subtitle = stringResource(R.string.donate_github_sponsors_desc),
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sponsors/tuyenphamvn")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("SettingsScreen", "Failed to open GitHub Sponsors link", e)
                                }
                            },
                            testTag = "setting_donate_github"
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
