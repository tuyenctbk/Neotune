package com.easeaudio.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
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
import com.easeaudio.ui.components.AudioVisualizerCanvas
import com.easeaudio.ui.components.VisualizerStyle
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
    var previewTheme by remember(currentTheme) { mutableStateOf(currentTheme) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var isBackFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .onFocusChanged { isBackFocused = it.isFocused }
                            .clip(CircleShape)
                            .background(if (isBackFocused) previewTheme.primary else Color.Transparent)
                            .testTag("btn_close_appearance")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isBackFocused) MaterialTheme.colorScheme.background else previewTheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Palette,
                                contentDescription = null,
                                tint = previewTheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.appearance),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = stringResource(R.string.appearance_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Studio Hero Preview Banner
                AnimatedContent(
                    targetState = previewTheme,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "theme_studio_preview"
                ) { target ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = target.primary),
                        color = target.surface,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, target.primary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (target.tagline.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(target.primary.copy(alpha = 0.2f))
                                            .border(1.dp, target.primary.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = target.tagline,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.sp
                                            ),
                                            color = target.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                Text(
                                    text = target.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = target.textPrimary
                                )
                                Text(
                                    text = target.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = target.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Mini Live Equalizer Preview inside Studio Hero
                            Box(
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(target.background)
                                    .border(1.dp, target.cardBorder, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AudioVisualizerCanvas(
                                    waveAmplitudes = listOf(0.4f, 0.7f, 0.9f, 0.6f, 0.85f, 0.5f, 0.95f, 0.4f),
                                    isPlaying = true,
                                    style = VisualizerStyle.ROUNDED_BARS,
                                    primaryColor = target.primary,
                                    secondaryColor = target.secondary,
                                    accentColor = target.accent,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

                // Adaptive Grid for 8 curated themes
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 340.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(themes, key = { it.id }) { theme ->
                        ThemeSelectionCard(
                            theme = theme,
                            currentTheme = currentTheme,
                            onHoverPreview = { previewTheme = theme },
                            onSelectTheme = {
                                previewTheme = theme
                                onSelectTheme(theme)
                            }
                        )
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
    onHoverPreview: () -> Unit = {},
    onSelectTheme: (ThemePreset) -> Unit
) {
    val isSelected = theme.id == currentTheme.id
    var isFocused by remember { mutableStateOf(false) }
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "theme_focus_scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onHoverPreview()
            }
            .scale(focusScale)
            .shadow(
                elevation = if (isFocused) 14.dp else 0.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = theme.primary,
                ambientColor = theme.primary.copy(alpha = 0.4f)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable { onSelectTheme(theme) }
            .border(
                width = if (isFocused) 3.dp else if (isSelected) 2.dp else 1.dp,
                brush = if (isFocused) {
                    Brush.horizontalGradient(
                        listOf(
                            theme.primary,
                            Color.White,
                            theme.primary
                        )
                    )
                } else if (isSelected) {
                    Brush.horizontalGradient(
                        listOf(
                            theme.primary,
                            theme.primary.copy(alpha = 0.5f)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )
                },
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("theme_card_${theme.id}"),
        color = if (isFocused) MaterialTheme.colorScheme.surfaceVariant else if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Text Info (Left side)
            Column(modifier = Modifier.weight(1.2f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = theme.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isSelected || isFocused) FontWeight.Black else FontWeight.Bold
                        ),
                        color = if (isFocused) Color.White else if (isSelected) theme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(theme.primary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.background,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = stringResource(R.string.in_use),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp
                                    ),
                                    color = MaterialTheme.colorScheme.background
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = theme.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isFocused) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
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

            Spacer(modifier = Modifier.width(14.dp))

            // Mini Mockup (Right side - Realistic App Simulation)
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.background)
                    .border(1.dp, theme.cardBorder, RoundedCornerShape(10.dp))
                    .padding(6.dp)
            ) {
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
                        Box(
                            modifier = Modifier
                                .width(34.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(theme.textPrimary.copy(alpha = 0.85f))
                        )
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(theme.primary)
                        )
                    }

                    // Mini active card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(theme.surface)
                            .padding(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(theme.textMuted.copy(alpha = 0.4f))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(theme.primary)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .height(2.dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(theme.textSecondary.copy(alpha = 0.6f))
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
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
                .border(0.5.dp, Color(0x44FFFFFF), CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
