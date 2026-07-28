package com.easeaudio.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
    playbackError: String? = null,
    onTogglePlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onRetryStream: () -> Unit = {},
    onPlayNextStation: () -> Unit = {},
    onPlayPreviousStation: () -> Unit = {},
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
                var isBackFocused by remember { mutableStateOf(false) }
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .onFocusChanged { isBackFocused = it.isFocused }
                        .clip(CircleShape)
                        .background(if (isBackFocused) NeonCyan else Color.Transparent)
                        .testTag("btn_player_back")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack, 
                        contentDescription = stringResource(R.string.close), 
                        tint = if (isBackFocused) DarkBackground else TextPrimary
                    )
                }

                Text(
                    text = stringResource(R.string.now_playing),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                var isFavFocused by remember { mutableStateOf(false) }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .onFocusChanged { isFavFocused = it.isFocused }
                        .clip(CircleShape)
                        .background(if (isFavFocused) NeonPink else Color.Transparent)
                        .testTag("btn_player_favorite")
                ) {
                    Icon(
                        imageVector = if (station.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavFocused) DarkBackground else (if (station.isFavorite) NeonPink else TextMuted)
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
                    .fillMaxHeight()
                    .widthIn(max = 500.dp)
                    .align(Alignment.TopCenter)
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
                        .border(1.dp, Color(0x12FFFFFF), RoundedCornerShape(24.dp))
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
                                .background(DarkBackground.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = NeonCyan,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.buffering_stream),
                                    color = NeonCyan,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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
                        text = if (isLoading) stringResource(R.string.buffering_stream) else (streamTitle ?: station.genre),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary,
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
                                .background(Color.White)
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

                if (playbackError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        color = Color(0x22FF5252),
                        border = BorderStroke(1.dp, Color(0xFFFF5252))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.stream_offline_notice),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onRetryStream,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.retry_stream), fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = onPlayNextStation,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                    border = BorderStroke(1.dp, NeonCyan),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.next_station), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Transport Row (Previous, Play/Pause, Next)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    var isPrevFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onPlayPreviousStation,
                        modifier = Modifier
                            .size(48.dp)
                            .onFocusChanged { isPrevFocused = it.isFocused }
                            .clip(CircleShape)
                            .background(if (isPrevFocused) NeonCyan else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous Station",
                            tint = if (isPrevFocused) DarkBackground else TextPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Play / Pause Transport Button
                    var isPlayFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .onFocusChanged { isPlayFocused = it.isFocused }
                            .clip(CircleShape)
                            .background(if (isPlayFocused) NeonCyan else Color.White)
                            .clickable(onClick = onTogglePlay)
                            .border(
                                width = if (isPlayFocused) 3.dp else 0.dp,
                                color = if (isPlayFocused) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .testTag("btn_player_toggle_play"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
                                color = if (isPlayFocused) NeonCyan else DarkBackground,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = DarkBackground,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    var isNextFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onPlayNextStation,
                        modifier = Modifier
                            .size(48.dp)
                            .onFocusChanged { isNextFocused = it.isFocused }
                            .clip(CircleShape)
                            .background(if (isNextFocused) NeonCyan else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next Station",
                            tint = if (isNextFocused) DarkBackground else TextPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Shortcut Pills for Sleep Timer & Equalizer
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var isEqFocused by remember { mutableStateOf(false) }
                    AssistChip(
                        onClick = onOpenEqualizer,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Equalizer,
                                contentDescription = null,
                                tint = if (isEqFocused) DarkBackground else NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("EQ: $activeEqPreset", color = if (isEqFocused) DarkBackground else TextPrimary, fontSize = 12.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isEqFocused) NeonCyan else DarkSurfaceVariant
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = if (isEqFocused) Color.White else CardBorder
                        ),
                        modifier = Modifier.onFocusChanged { isEqFocused = it.isFocused }
                    )

                    var isSleepFocused by remember { mutableStateOf(false) }
                    AssistChip(
                        onClick = onOpenSleepTimer,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Bedtime,
                                contentDescription = null,
                                tint = if (isSleepFocused) DarkBackground else (if (sleepTimerRemaining != null) DarkBackground else TextMuted),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = if (sleepTimerRemaining != null) stringResource(R.string.sleeping_in, sleepTimerRemaining) else stringResource(R.string.sleep_timer),
                                color = if (isSleepFocused) DarkBackground else (if (sleepTimerRemaining != null) DarkBackground else TextPrimary),
                                fontSize = 12.sp
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isSleepFocused) NeonCyan else (if (sleepTimerRemaining != null) ActivePill else DarkSurfaceVariant)
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = if (isSleepFocused) Color.White else (if (sleepTimerRemaining != null) Color.White else CardBorder)
                        ),
                        modifier = Modifier.onFocusChanged { isSleepFocused = it.isFocused }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
