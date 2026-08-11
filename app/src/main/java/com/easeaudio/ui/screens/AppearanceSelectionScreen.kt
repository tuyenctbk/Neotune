package com.easeaudio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.easeaudio.R
import com.easeaudio.ui.theme.*

@Composable
fun AppearanceSelectionScreen(
    currentTheme: ThemePreset,
    themes: List<ThemePreset>,
    selectedLauncherIcon: String = "default",
    onSelectLauncherIcon: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onSelectTheme: (ThemePreset) -> Unit
) {
    val cafeThemeIds = listOf("espresso_bar", "bistro_warm", "fine_dining_obsidian", "garden_cafe", "wine_bar", "minimalist_cafe", "trattoria", "youth_cafe")
    val cafeThemes = themes.filter { cafeThemeIds.contains(it.id) }
    val standardThemes = themes.filter { !it.id.startsWith("youth_") && !cafeThemeIds.contains(it.id) }
    val youthThemes = themes.filter { theme -> theme.id.startsWith("youth_") && !cafeThemeIds.contains(theme.id) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var isBackFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .onFocusChanged { isBackFocused = it.isFocused }
                            .clip(CircleShape)
                            .background(if (isBackFocused) NeonCyan else Color.Transparent)
                            .testTag("btn_close_appearance")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isBackFocused) DarkBackground else NeonCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.appearance),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.appearance_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                HorizontalDivider(color = CardBorder, thickness = 1.dp, modifier = Modifier.padding(horizontal = 20.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cafe Themes Section
                    item {
                        Column {
                            Text(
                                text = stringResource(R.string.theme_section_cafe),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = NeonPink,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = stringResource(R.string.theme_section_cafe_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                    items(cafeThemes, key = { it.id }) { theme ->
                        ThemeSelectionCard(theme, currentTheme, onSelectTheme)
                    }

                    // Standard Section
                    item {
                        Column {
                            Text(
                                text = stringResource(R.string.theme_section_standard),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = NeonCyan,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                            Text(
                                text = stringResource(R.string.theme_section_standard_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                    items(standardThemes, key = { it.id }) { theme ->
                        ThemeSelectionCard(theme, currentTheme, onSelectTheme)
                    }

                    // Youth Section
                    item {
                        Column {
                            Text(
                                text = stringResource(R.string.theme_section_youth),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = NeonPurple,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                            Text(
                                text = stringResource(R.string.theme_section_youth_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                    items(youthThemes, key = { it.id }) { theme ->
                        ThemeSelectionCard(theme, currentTheme, onSelectTheme)
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeSelectionCard(
    theme: ThemePreset,
    currentTheme: ThemePreset,
    onSelectTheme: (ThemePreset) -> Unit
) {
    val isSelected = theme.id == currentTheme.id
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelectTheme(theme) }
            .border(
                width = if (isFocused) 3.dp else if (isSelected) 1.5.dp else 1.dp,
                color = if (isFocused) theme.primary else if (isSelected) theme.primary.copy(alpha = 0.6f) else CardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("theme_card_${theme.id}"),
        color = if (isFocused) DarkSurfaceVariant else if (isSelected) DarkSurfaceVariant.copy(alpha = 0.5f) else DarkSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Text Info (Left side)
            Column(modifier = Modifier.weight(1.3f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = theme.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) theme.primary else TextPrimary
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(theme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.in_use),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = theme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = theme.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Color dots info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ColorPill(label = stringResource(R.string.color_pill_bg), color = theme.background)
                    ColorPill(label = stringResource(R.string.color_pill_card), color = theme.surface)
                    ColorPill(label = stringResource(R.string.color_pill_accent), color = theme.primary)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Mini Mockup (Right side - Illustration)
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.background)
                    .border(1.dp, theme.cardBorder, RoundedCornerShape(8.dp))
                    .padding(6.dp)
            ) {
                // Mini layout mimicking NeoTune App
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tiny title "NeoTune"
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(theme.textPrimary.copy(alpha = 0.8f))
                        )
                        // Tiny dot for status/menu
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(theme.primary)
                        )
                    }

                    // Mini active card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(theme.surface)
                            .padding(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Tiny image box
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(theme.textMuted.copy(alpha = 0.4f))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            // Tiny text lines
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(theme.primary)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .height(2.dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(theme.textSecondary.copy(alpha = 0.6f))
                                )
                            }
                            // Tiny play circle button
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(theme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(theme.background)
                                )
                            }
                        }
                    }

                    // Mini Bottom bar dots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(theme.primary))
                        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(theme.textMuted.copy(alpha = 0.5f)))
                        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(theme.textMuted.copy(alpha = 0.5f)))
                    }
                }
            }
        }
    }
}

@Composable
fun ColorPill(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
                .border(0.5.dp, Color(0x33FFFFFF), CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}
