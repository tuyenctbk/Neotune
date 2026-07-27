package com.easeaudio.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.easeaudio.R
import com.easeaudio.data.RadioStation
import com.easeaudio.ui.theme.*

@Composable
fun PlayerScreen(
    station: RadioStation?,
    isPlaying: Boolean,
    isLoading: Boolean,
    streamTitle: String?,
    waveAmplitudes: List<Float>,
    volume: Float,
    sleepTimerRemaining: Int?,
    activeEqPreset: String,
    onTogglePlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onBack: () -> Unit
) {
    if (station == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.no_station_selected), color = TextSecondary)
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val artScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isPlaying) 1.03f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "artPulse"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("btn_player_back")
                ) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.close), tint = TextPrimary)
                }

                Text(
                    text = stringResource(R.string.now_playing),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("btn_player_favorite")
                ) {
                    Icon(
                        imageVector = if (station.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (station.isFavorite) NeonPink else TextMuted
                    )
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val maxScreenHeight = maxHeight
            val isCompact = maxScreenHeight < 650.dp
            val artSize = if (isCompact) 180.dp else 240.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Station Artwork Frame
                Box(
                    modifier = Modifier
                        .size(artSize)
                        .scale(artScale)
                        .clip(RoundedCornerShape(24.dp))
                        .border(2.dp, Brush.linearGradient(listOf(NeonCyan, NeonPurple)), RoundedCornerShape(24.dp))
                ) {
                    AsyncImage(
                        model = station.imageUrl,
                        contentDescription = station.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DarkBackground.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = NeonCyan)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title & Track Information
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = station.name,
                        style = if (isCompact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = streamTitle ?: station.genre,
                        style = MaterialTheme.typography.titleSmall,
                        color = NeonCyan,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${station.country} • ${station.bitrate}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Audio Waveform Animation
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .fillMaxWidth(0.7f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    waveAmplitudes.forEach { amp ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(amp.coerceIn(0.12f, 1.0f))
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(NeonCyan, NeonPurple)
                                    )
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Volume Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (volume == 0f) Icons.Filled.VolumeOff else Icons.Filled.VolumeDown,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .testTag("slider_player_volume")
                    )
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Play / Pause Transport Button
                FloatingActionButton(
                    onClick = onTogglePlay,
                    containerColor = NeonCyan,
                    contentColor = DarkBackground,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(68.dp)
                        .testTag("btn_player_toggle_play")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Shortcut Pills for Sleep Timer & Equalizer
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = onOpenEqualizer,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Equalizer,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("EQ: $activeEqPreset", color = TextPrimary, fontSize = 12.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = DarkSurfaceVariant),
                        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = CardBorder)
                    )

                    AssistChip(
                        onClick = onOpenSleepTimer,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Bedtime,
                                contentDescription = null,
                                tint = if (sleepTimerRemaining != null) NeonPurple else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = if (sleepTimerRemaining != null) stringResource(R.string.sleeping_in, sleepTimerRemaining) else stringResource(R.string.sleep_timer),
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (sleepTimerRemaining != null) ActivePill else DarkSurfaceVariant
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = if (sleepTimerRemaining != null) NeonPurple else CardBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
