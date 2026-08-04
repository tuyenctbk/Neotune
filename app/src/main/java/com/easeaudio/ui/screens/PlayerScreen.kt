package com.easeaudio.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import com.easeaudio.R
import com.easeaudio.data.RadioStation
import com.easeaudio.ui.theme.*
import com.easeaudio.viewmodel.EqPresetDisplay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.easeaudio.ui.components.NotificationPermissionReminder
import android.os.Build
import android.Manifest

@Composable
fun PlayerScreen(
    station: RadioStation?,
    windowSizeClass: WindowSizeClass,
    isPlaying: Boolean,
    isLoading: Boolean,
    streamTitle: String?,
    waveAmplitudes: List<Float>,
    volume: Float,
    sleepTimerRemaining: Int?,
    activeEqPreset: String,
    eqPresets: List<EqPresetDisplay> = emptyList(),
    playbackError: String? = null,
    hasNotificationPermission: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    onTogglePlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenTrackOptions: () -> Unit = {},
    onOpenScreensaver: () -> Unit = {},
    onRetryStream: () -> Unit = {},
    onPlayNextStation: () -> Unit = {},
    onPlayPreviousStation: () -> Unit = {},
    onSeekRelative: ((Long) -> Unit)? = null,
    onSeek: ((Long) -> Unit)? = null,
    currentPosition: Long = 0L,
    totalDuration: Long = 0L,
    playbackSpeed: Float = 1.0f,
    onPlaybackSpeedChange: ((Float) -> Unit)? = null,
    onOpenEpisodes: (() -> Unit)? = null,
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
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = stringResource(R.string.close), 
                        tint = if (isBackFocused) DarkBackground else TextPrimary
                    )
                }

                val isPodcast = station.isPodcast
                Text(
                    text = stringResource(if (isPodcast) R.string.on_demand_podcast else R.string.live_radio_broadcast),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isPodcast) NeonPurple else NeonPink,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    var isFavFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .onFocusChanged { isFavFocused = it.isFocused }
                            .clip(CircleShape)
                            .background(if (isFavFocused) FavoriteHeartColor else Color.Transparent)
                            .testTag("btn_player_favorite")
                    ) {
                        Icon(
                            imageVector = if (station.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavFocused) DarkBackground else (if (station.isFavorite) FavoriteHeartColor else TextMuted)
                        )
                    }

                    var isSleepFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onOpenSleepTimer,
                        modifier = Modifier
                            .onFocusChanged { isSleepFocused = it.isFocused }
                            .clip(CircleShape)
                            .background(if (isSleepFocused) NeonPurple else Color.Transparent)
                            .testTag("btn_player_sleep_timer")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bedtime,
                            contentDescription = "Sleep Timer",
                            tint = if (isSleepFocused) DarkBackground else (if (sleepTimerRemaining != null) NeonPurple else TextMuted)
                        )
                    }

                    var showPlayerMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showPlayerMenu = true },
                            modifier = Modifier.clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More Options",
                                tint = TextPrimary
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showPlayerMenu,
                            onDismissRequest = { showPlayerMenu = false },
                            modifier = Modifier.background(DarkSurfaceVariant)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Screensaver Mode", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Filled.Tv, contentDescription = null, tint = NeonCyan) },
                                onClick = {
                                    showPlayerMenu = false
                                    onOpenScreensaver()
                                }
                            )
                            HorizontalDivider(color = CardBorder, thickness = 1.dp)
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.track_options), color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null, tint = TextMuted) },
                                onClick = {
                                    showPlayerMenu = false
                                    onOpenTrackOptions()
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
            val isMedium = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium
            val useHorizontalLayout = isExpanded || (isMedium && maxWidth > maxHeight)
            
            if (useHorizontalLayout) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 40.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .scale(artScale)
                            .clip(RoundedCornerShape(32.dp))
                            .border(1.dp, Color(0x12FFFFFF), RoundedCornerShape(32.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = station.imageUrl,
                            contentDescription = station.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isLoading) {
                            CircularProgressIndicator(color = NeonCyan, strokeWidth = 4.dp)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        PlayerContent(
                            station = station,
                            isPlaying = isPlaying,
                            isLoading = isLoading,
                            streamTitle = streamTitle,
                            waveAmplitudes = waveAmplitudes,
                            volume = volume,
                            sleepTimerRemaining = sleepTimerRemaining,
                            activeEqPreset = activeEqPreset,
                            eqPresets = eqPresets,
                            playbackError = playbackError,
                            currentPosition = currentPosition,
                            totalDuration = totalDuration,
                            playbackSpeed = playbackSpeed,
                            onVolumeChange = onVolumeChange,
                            onTogglePlay = onTogglePlay,
                            onRetryStream = onRetryStream,
                            onPlayNextStation = onPlayNextStation,
                            onPlayPreviousStation = onPlayPreviousStation,
                            onSeek = onSeek,
                            onSeekRelative = onSeekRelative,
                            onPlaybackSpeedChange = onPlaybackSpeedChange,
                            onOpenEpisodes = onOpenEpisodes,
                            onOpenSleepTimer = onOpenSleepTimer,
                            onOpenEqualizer = onOpenEqualizer,
                            isHorizontal = true
                        )
                    }
                }
            } else {
                val maxScreenHeight = maxHeight
                val isCompactHeight = maxScreenHeight < 680.dp
                val artSize = if (isCompactHeight) 150.dp else 220.dp

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        var isPlayerReminderDismissed by remember { mutableStateOf(false) }
                        NotificationPermissionReminder(
                            visible = !isPlayerReminderDismissed,
                            onRequestPermission = onRequestNotificationPermission,
                            onDismiss = { isPlayerReminderDismissed = true },
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

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
                                    .background(DarkBackground.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = NeonCyan, strokeWidth = 3.dp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    PlayerContent(
                        station = station,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        streamTitle = streamTitle,
                        waveAmplitudes = waveAmplitudes,
                        volume = volume,
                        sleepTimerRemaining = sleepTimerRemaining,
                        activeEqPreset = activeEqPreset,
                        eqPresets = eqPresets,
                        playbackError = playbackError,
                        currentPosition = currentPosition,
                        totalDuration = totalDuration,
                        playbackSpeed = playbackSpeed,
                        onVolumeChange = onVolumeChange,
                        onTogglePlay = onTogglePlay,
                        onRetryStream = onRetryStream,
                        onPlayNextStation = onPlayNextStation,
                        onPlayPreviousStation = onPlayPreviousStation,
                        onSeek = onSeek,
                        onSeekRelative = onSeekRelative,
                        onPlaybackSpeedChange = onPlaybackSpeedChange,
                        onOpenEpisodes = onOpenEpisodes,
                        onOpenSleepTimer = onOpenSleepTimer,
                        onOpenEqualizer = onOpenEqualizer,
                        isHorizontal = false
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerContent(
    station: RadioStation,
    isPlaying: Boolean,
    isLoading: Boolean,
    streamTitle: String?,
    waveAmplitudes: List<Float>,
    volume: Float,
    sleepTimerRemaining: Int?,
    activeEqPreset: String,
    eqPresets: List<EqPresetDisplay>,
    playbackError: String?,
    currentPosition: Long,
    totalDuration: Long,
    playbackSpeed: Float,
    onVolumeChange: (Float) -> Unit,
    onTogglePlay: () -> Unit,
    onRetryStream: () -> Unit,
    onPlayNextStation: () -> Unit,
    onPlayPreviousStation: () -> Unit,
    onSeek: ((Long) -> Unit)?,
    onSeekRelative: ((Long) -> Unit)?,
    onPlaybackSpeedChange: ((Float) -> Unit)?,
    onOpenEpisodes: (() -> Unit)?,
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    isHorizontal: Boolean
) {
    val isPodcast = station.isPodcast

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isHorizontal) Modifier.verticalScroll(rememberScrollState()) else Modifier),
        horizontalAlignment = if (isHorizontal) Alignment.Start else Alignment.CenterHorizontally
    ) {
        Text(
            text = station.name,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            textAlign = if (isHorizontal) TextAlign.Start else TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (isLoading) stringResource(R.string.buffering_stream) else (streamTitle ?: station.genre),
            style = MaterialTheme.typography.titleMedium,
            color = if (isLoading) NeonCyan else TextSecondary,
            textAlign = if (isHorizontal) TextAlign.Start else TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!isPodcast) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = station.country, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                Surface(shape = RoundedCornerShape(4.dp), color = DarkSurfaceVariant) {
                    Text(
                        text = station.bitrate, 
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isPodcast && totalDuration > 0L) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { onSeek?.invoke(it.toLong()) },
                    valueRange = 0f..totalDuration.toFloat(),
                    colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = formatDuration(currentPosition), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(text = formatDuration(totalDuration), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                
                if (currentPosition > 5000L) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.resumed_at, formatDuration(currentPosition)),
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonPurple,
                        modifier = Modifier.clickable { onSeek?.invoke(0L) }
                    )
                }
            }
        } else if (!isPodcast) {
            val barColors = WaveformAnimationColors
            Row(
                modifier = Modifier.height(40.dp).fillMaxWidth(if (isHorizontal) 0.6f else 0.8f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                waveAmplitudes.forEachIndexed { index, amp ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(amp.coerceIn(0.1f, 1.0f))
                            .clip(CircleShape)
                            .background(barColors[index % barColors.size])
                    )
                }
            }
        }

        if (playbackError != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetryStream, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text("Retry Connection", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = onPlayPreviousStation) {
                Icon(Icons.Filled.SkipPrevious, null, modifier = Modifier.size(32.dp), tint = TextPrimary)
            }

            if (isPodcast && onSeekRelative != null) {
                IconButton(onClick = { onSeekRelative(-15000L) }) {
                    Icon(Icons.Filled.Replay10, null, tint = NeonPurple)
                }
            }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onTogglePlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = DarkBackground,
                    modifier = Modifier.size(40.dp)
                )
            }

            if (isPodcast && onSeekRelative != null) {
                IconButton(onClick = { onSeekRelative(30000L) }) {
                    Icon(Icons.Filled.Forward30, null, tint = NeonPurple)
                }
            }

            IconButton(onClick = onPlayNextStation) {
                Icon(Icons.Filled.SkipNext, null, modifier = Modifier.size(32.dp), tint = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.VolumeDown, null, tint = TextMuted, modifier = Modifier.size(20.dp))
            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
            )
            Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = TextMuted, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isPodcast && onOpenEpisodes != null) {
                AssistChip(
                    onClick = onOpenEpisodes,
                    label = { Text("Episodes") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, modifier = Modifier.size(16.dp)) }
                )
            }
            if (isPodcast && onPlaybackSpeedChange != null) {
                AssistChip(
                    onClick = { onPlaybackSpeedChange(if (playbackSpeed >= 2f) 1f else playbackSpeed + 0.5f) },
                    label = { Text("${playbackSpeed}x") },
                    leadingIcon = { Icon(Icons.Filled.Speed, null, modifier = Modifier.size(16.dp)) }
                )
            }
            val activePresetLabel = eqPresets.find { it.key == activeEqPreset }?.labelResId?.let { stringResource(it) } ?: activeEqPreset
            AssistChip(
                onClick = onOpenEqualizer,
                label = { Text("EQ: $activePresetLabel") },
                leadingIcon = { Icon(Icons.Filled.Tune, null, modifier = Modifier.size(16.dp)) }
            )
            AssistChip(
                onClick = onOpenSleepTimer,
                label = { Text(if (sleepTimerRemaining != null) "${sleepTimerRemaining}m" else "Timer") },
                leadingIcon = { Icon(Icons.Filled.Bedtime, null, modifier = Modifier.size(16.dp)) },
                colors = if (sleepTimerRemaining != null) AssistChipDefaults.assistChipColors(containerColor = ActivePill) else AssistChipDefaults.assistChipColors()
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        val remMinutes = minutes % 60
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, remMinutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}
