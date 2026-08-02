package com.easeaudio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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
    onOpenAppearance: () -> Unit,
    onOpenOnboarding: () -> Unit = {},
    onOpenBlockedDialog: () -> Unit,
    onOpenAttribution: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
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
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Audio & Playback Section
            item {
                SettingsSectionHeader(title = "Audio & Region")
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Filled.Language,
                        iconTint = NeonCyan,
                        title = "Country & Region",
                        subtitle = "Active region: $selectedCountry",
                        onClick = onOpenCountryPicker,
                        testTag = "setting_country_region"
                    )
                    HorizontalDivider(color = DarkSurfaceVariant, thickness = 1.dp)
                    SettingsItem(
                        icon = Icons.Filled.Equalizer,
                        iconTint = NeonCyan,
                        title = stringResource(R.string.equalizer),
                        subtitle = "Customize bass, treble & audio presets",
                        onClick = onOpenEqualizer,
                        testTag = "setting_equalizer"
                    )
                    HorizontalDivider(color = DarkSurfaceVariant, thickness = 1.dp)
                    SettingsItem(
                        icon = Icons.Filled.Bedtime,
                        iconTint = if (sleepTimerRemaining != null) NeonPurple else TextMuted,
                        title = stringResource(R.string.sleep_timer),
                        subtitle = sleepTimerRemaining?.let { stringResource(R.string.sleeping_in, it) } ?: "Set automatic sleep timer",
                        onClick = onOpenSleepTimer,
                        testTag = "setting_sleep_timer"
                    )
                    HorizontalDivider(color = DarkSurfaceVariant, thickness = 1.dp)
                    SettingsItem(
                        icon = Icons.Filled.Alarm,
                        iconTint = NeonCyan,
                        title = stringResource(R.string.radio_alarm),
                        subtitle = "Wake up to your favorite live stream",
                        onClick = onOpenRadioAlarm,
                        testTag = "setting_radio_alarm"
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
                        iconTint = NeonPink,
                        title = stringResource(R.string.appearance),
                        subtitle = "Themes, accent colors & languages",
                        onClick = onOpenAppearance,
                        testTag = "setting_appearance"
                    )
                    HorizontalDivider(color = DarkSurfaceVariant, thickness = 1.dp)
                    SettingsItem(
                        icon = Icons.Filled.Explore,
                        iconTint = NeonCyan,
                        title = stringResource(R.string.onboarding_app_tour),
                        subtitle = "Replay onboarding walkthrough & setup",
                        onClick = onOpenOnboarding,
                        testTag = "setting_onboarding"
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
                        iconTint = NeonCyan,
                        title = stringResource(R.string.content_filters_blocklist),
                        subtitle = "Manage hidden stations & safety filters",
                        onClick = onOpenBlockedDialog,
                        testTag = "setting_blocked_stations"
                    )
                    HorizontalDivider(color = DarkSurfaceVariant, thickness = 1.dp)
                    SettingsItem(
                        icon = Icons.Filled.Info,
                        iconTint = TextMuted,
                        title = stringResource(R.string.info),
                        subtitle = "Data providers & API attributions",
                        onClick = onOpenAttribution,
                        testTag = "setting_attribution"
                    )
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
        color = NeonCyan,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}
