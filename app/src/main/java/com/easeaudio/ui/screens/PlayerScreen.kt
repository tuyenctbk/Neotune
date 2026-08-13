package com.easeaudio.ui.screens

import androidx.compose.animation.*
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
import com.easeaudio.ui.components.AudioVisualizerCanvas
import com.easeaudio.ui.components.VisualizerStyle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import android.content.Intent
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
    onOpenCarMode: () -> Unit = {},
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

    val artworkPalette = rememberArtworkPalette(station.imageUrl)
    val animatedMutedColor by animateColorAsState(
        targetValue = artworkPalette.darkMutedColor.copy(alpha = 0.5f),
        animationSpec = tween(1000),
        label = "paletteMuted"
    )
    val animatedVibrantColor by animateColorAsState(
        targetValue = artworkPalette.vibrantColor.copy(alpha = 0.25f),
        animationSpec = tween(1000),
        label = "paletteVibrant"
    )

    val translucentBackground = DarkBackground.copy(alpha = 0.85f)
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(translucentBackground)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedMutedColor.copy(alpha = 0.4f),
                        translucentBackground,
                        animatedVibrantColor.copy(alpha = 0.2f),
                        translucentBackground
                    )
                )
            ),
        containerColor = Color.Transparent,
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
                    val haptic = LocalHapticFeedback.current
                    val context = LocalContext.current

                    var isFavFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleFavorite()
                        },
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

                    var isShareFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val shareText = "Listening to ${station.name} (${station.genre})\nStream: ${station.streamUrl}\nTune in live on NeoTune Radio!"
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Listen to ${station.name}")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Station"))
                        },
                        modifier = Modifier
                            .onFocusChanged { isShareFocused = it.isFocused }
                            .clip(CircleShape)
                            .background(if (isShareFocused) NeonCyan else Color.Transparent)
                            .testTag("btn_player_share")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share Station",
                            tint = if (isShareFocused) DarkBackground else TextMuted
                        )
                    }

                    var isSleepFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenSleepTimer()
                        },
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
                                text = { Text(stringResource(R.string.screensaver_mode), color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Filled.Tv, contentDescription = null, tint = NeonCyan) },
                                onClick = {
                                    showPlayerMenu = false
                                    onOpenScreensaver()
                                }
                            )
                            HorizontalDivider(color = CardBorder, thickness = 1.dp)
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.car_mode), color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = NeonCyan) },
                                onClick = {
                                    showPlayerMenu = false
                                    onOpenCarMode()
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
            
            AnimatedContent(
                targetState = station,
                transitionSpec = {
                    fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(350))
                },
                label = "StationChangeTransition",
                modifier = Modifier.fillMaxSize()
            ) { station ->
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
    var visualizerStyle by remember { mutableStateOf(VisualizerStyle.ROUNDED_BARS) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isHorizontal) Modifier.verticalScroll(rememberScrollState()) else Modifier),
        horizontalAlignment = if (isHorizontal) Alignment.Start else Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isHorizontal) Arrangement.Start else Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = station.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                textAlign = if (isHorizontal) TextAlign.Start else TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
            val detailContext = LocalContext.current
            val detailHaptic = LocalHapticFeedback.current
            IconButton(
                onClick = {
                    detailHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val shareText = "Listening to ${station.name} (${station.genre})\nStream: ${station.streamUrl}\nTune in live on NeoTune Radio!"
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Listen to ${station.name}")
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    detailContext.startActivity(Intent.createChooser(shareIntent, "Share Station"))
                },
                modifier = Modifier
                    .size(32.dp)
                    .testTag("btn_station_detail_share")
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share Station",
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        val hasLiveMetadata = !streamTitle.isNullOrBlank() && streamTitle != station.name && streamTitle != station.genre
        if (hasLiveMetadata) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NeonPurple.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f)),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NeonCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.now_playing),
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = if (isLoading) stringResource(R.string.buffering_stream) else (streamTitle ?: station.genre),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (hasLiveMetadata) FontWeight.Bold else FontWeight.Medium),
            color = if (isLoading) NeonCyan else if (hasLiveMetadata) TextPrimary else TextSecondary,
            textAlign = if (isHorizontal) TextAlign.Start else TextAlign.Center,
            maxLines = 2,
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
            val animatedPosition by animateFloatAsState(
                targetValue = currentPosition.toFloat(),
                animationSpec = if (isPlaying) tween(1000, easing = LinearEasing) else spring(),
                label = "SmoothProgress"
            )
            var draggingValue by remember { mutableStateOf<Float?>(null) }
            LaunchedEffect(station.id) {
                draggingValue = null
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = (draggingValue ?: animatedPosition).coerceIn(0f, totalDuration.toFloat()),
                    onValueChange = { draggingValue = it },
                    onValueChangeFinished = {
                        draggingValue?.let {
                            onSeek?.invoke(it.toLong())
                        }
                        draggingValue = null
                    },
                    valueRange = 0f..totalDuration.toFloat(),
                    colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = formatDuration(draggingValue?.toLong() ?: currentPosition),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
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
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Frequency Visualizer Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurfaceVariant.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth(if (isHorizontal) 0.85f else 1.0f)
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = "Audio Frequency Visualizer",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.audio_frequency_visualizer),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VisualizerStyle.entries.forEach { style ->
                            val selected = visualizerStyle == style
                            var isChipFocused by remember { mutableStateOf(false) }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) NeonCyan.copy(alpha = 0.2f) else if (isChipFocused) DarkSurfaceVariant else Color.Transparent,
                                border = BorderStroke(
                                    if (isChipFocused) 2.dp else 1.dp,
                                    if (isChipFocused) NeonCyan else if (selected) NeonCyan else Color.Transparent
                                ),
                                modifier = Modifier
                                    .onFocusChanged { isChipFocused = it.isFocused }
                                    .clickable { visualizerStyle = style }
                                    .testTag("viz_style_${style.name.lowercase()}")
                            ) {
                                Text(
                                    text = when (style) {
                                        VisualizerStyle.ROUNDED_BARS -> stringResource(R.string.viz_bars)
                                        VisualizerStyle.DUAL_MIRROR -> stringResource(R.string.viz_mirror)
                                        VisualizerStyle.WAVE_LINE -> stringResource(R.string.viz_wave)
                                        VisualizerStyle.CIRCULAR_RIPPLE -> stringResource(R.string.viz_pulse)
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected || isChipFocused) NeonCyan else TextMuted,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AudioVisualizerCanvas(
                    waveAmplitudes = waveAmplitudes,
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .height(52.dp)
                        .fillMaxWidth(),
                    style = visualizerStyle,
                    primaryColor = NeonCyan,
                    secondaryColor = NeonPurple,
                    accentColor = NeonPink
                )
            }
        }

        if (playbackError != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetryStream, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text(stringResource(R.string.retry_connection), color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val haptic = LocalHapticFeedback.current

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            var isPrevFocused by remember { mutableStateOf(false) }
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPlayPreviousStation()
                },
                modifier = Modifier
                    .onFocusChanged { isPrevFocused = it.isFocused }
                    .scale(if (isPrevFocused) 1.2f else 1.0f)
                    .background(if (isPrevFocused) NeonCyan.copy(alpha = 0.2f) else Color.Transparent, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous Station",
                    modifier = Modifier.size(32.dp),
                    tint = if (isPrevFocused) NeonCyan else TextPrimary
                )
            }

            if (isPodcast && onSeekRelative != null) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSeekRelative(-15000L)
                    }
                ) {
                    Icon(Icons.Filled.Replay10, null, tint = NeonPurple)
                }
            }

            var isPlayFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(if (isPlayFocused) 1.15f else 1.0f)
                    .onFocusChanged { isPlayFocused = it.isFocused }
                    .border(
                        width = if (isPlayFocused) 3.dp else 0.dp,
                        color = if (isPlayFocused) NeonCyan else Color.Transparent,
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .background(if (isPlayFocused) NeonCyan else PlayButtonContainer)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTogglePlay()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isPlayFocused) Color.Black else PlayButtonContent,
                    modifier = Modifier.size(40.dp)
                )
            }

            if (isPodcast && onSeekRelative != null) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSeekRelative(30000L)
                    }
                ) {
                    Icon(Icons.Filled.Forward30, null, tint = NeonPurple)
                }
            }

            var isNextFocused by remember { mutableStateOf(false) }
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPlayNextStation()
                },
                modifier = Modifier
                    .onFocusChanged { isNextFocused = it.isFocused }
                    .scale(if (isNextFocused) 1.2f else 1.0f)
                    .background(if (isNextFocused) NeonCyan.copy(alpha = 0.2f) else Color.Transparent, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next Station",
                    modifier = Modifier.size(32.dp),
                    tint = if (isNextFocused) NeonCyan else TextPrimary
                )
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
                    label = { Text(stringResource(R.string.episodes_label)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, modifier = Modifier.size(16.dp)) }
                )
            }
            if (isPodcast && onPlaybackSpeedChange != null) {
                AssistChip(
                    onClick = {
                        val nextSpeed = when (playbackSpeed) {
                            0.5f -> 1.0f
                            1.0f -> 1.25f
                            1.25f -> 1.5f
                            1.5f -> 2.0f
                            else -> 0.5f
                        }
                        onPlaybackSpeedChange(nextSpeed)
                    },
                    label = { Text("${playbackSpeed}x") },
                    leadingIcon = { Icon(Icons.Filled.Speed, null, modifier = Modifier.size(16.dp)) }
                )
            }
            val activePresetLabel = eqPresets.find { it.key == activeEqPreset }?.labelResId?.let { stringResource(it) } ?: activeEqPreset
            AssistChip(
                onClick = onOpenEqualizer,
                label = { Text(stringResource(R.string.eq_label, activePresetLabel)) },
                leadingIcon = { Icon(Icons.Filled.Tune, null, modifier = Modifier.size(16.dp)) }
            )
            AssistChip(
                onClick = onOpenSleepTimer,
                label = { Text(if (sleepTimerRemaining != null) "${sleepTimerRemaining}m" else stringResource(R.string.timer_label)) },
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
