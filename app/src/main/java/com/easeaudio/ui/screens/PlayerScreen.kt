package com.easeaudio.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import coil.request.ImageRequest
import com.easeaudio.ui.components.AnimatedStationAvatar
import com.easeaudio.ui.components.AudioVisualizer
import com.easeaudio.ui.components.VisualizerStyle
import com.easeaudio.ui.components.pulseOnPlaying
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
import com.easeaudio.data.SongLyrics
import com.easeaudio.ui.components.LyricsBottomSheet
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
    trackArtworkUrl: String? = null,
    currentLyrics: SongLyrics? = null,
    isLoadingLyrics: Boolean = false,
    hasNotificationPermission: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    onTogglePlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    isListenLater: Boolean = false,
    onToggleListenLater: () -> Unit = {},
    onVolumeChange: (Float) -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onFetchLyrics: () -> Unit = {},
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
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.no_station_selected), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val effectiveImageUrl = trackArtworkUrl?.ifBlank { null } ?: station.imageUrl
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

    val artworkPalette = rememberArtworkPalette(effectiveImageUrl)
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

    var showLyricsSheet by remember { mutableStateOf(false) }

    if (showLyricsSheet) {
        LyricsBottomSheet(
            lyrics = currentLyrics,
            isLoading = isLoadingLyrics,
            streamTitle = streamTitle,
            currentPositionMs = currentPosition,
            onDismiss = { showLyricsSheet = false }
        )
    }

    val solidBackground = MaterialTheme.colorScheme.background
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(solidBackground)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedMutedColor.copy(alpha = 0.35f),
                        solidBackground,
                        animatedVibrantColor.copy(alpha = 0.15f),
                        solidBackground
                    )
                )
            ),
        containerColor = solidBackground,
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
                        .background(if (isBackFocused) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .testTag("btn_player_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = stringResource(R.string.close), 
                        tint = if (isBackFocused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }

                val isPodcast = station.isPodcast
                Text(
                    text = stringResource(if (isPodcast) R.string.on_demand_podcast else R.string.live_radio_broadcast),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
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
                            tint = if (isFavFocused) MaterialTheme.colorScheme.onPrimary else (if (station.isFavorite) FavoriteHeartColor else MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    var isBookmarkFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleListenLater()
                        },
                        modifier = Modifier
                            .onFocusChanged { isBookmarkFocused = it.isFocused }
                            .clip(CircleShape)
                            .background(if (isBookmarkFocused) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .testTag("btn_player_listen_later")
                    ) {
                        Icon(
                            imageVector = if (isListenLater) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (isListenLater) "Remove from Listen Later" else "Save to Listen Later",
                            tint = if (isBookmarkFocused) MaterialTheme.colorScheme.onPrimary else (if (isListenLater) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
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
                            .background(if (isShareFocused) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .testTag("btn_player_share")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share Station",
                            tint = if (isShareFocused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
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
                            .background(if (isSleepFocused) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .testTag("btn_player_sleep_timer")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bedtime,
                            contentDescription = "Sleep Timer",
                            tint = if (isSleepFocused) MaterialTheme.colorScheme.onPrimary else (if (sleepTimerRemaining != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    var isMenuFocused by remember { mutableStateOf(false) }
                    var showPlayerMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showPlayerMenu = true },
                            modifier = Modifier
                                .onFocusChanged { isMenuFocused = it.isFocused }
                                .clip(CircleShape)
                                .background(if (isMenuFocused) MaterialTheme.colorScheme.primary else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More Options",
                                tint = if (isMenuFocused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showPlayerMenu,
                            onDismissRequest = { showPlayerMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.screensaver_mode), color = MaterialTheme.colorScheme.onSurface) },
                                leadingIcon = { Icon(Icons.Filled.Tv, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showPlayerMenu = false
                                    onOpenScreensaver()
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.car_mode), color = MaterialTheme.colorScheme.onSurface) },
                                leadingIcon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showPlayerMenu = false
                                    onOpenCarMode()
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.lyrics_title), color = MaterialTheme.colorScheme.onSurface) },
                                leadingIcon = { Icon(Icons.Filled.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showPlayerMenu = false
                                    onFetchLyrics()
                                    showLyricsSheet = true
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.track_options), color = MaterialTheme.colorScheme.onSurface) },
                                leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                        StationArtworkCard(
                            station = station,
                            isPlaying = isPlaying,
                            trackArtworkUrl = trackArtworkUrl,
                            isLoading = isLoading,
                            artScale = artScale,
                            cornerRadius = 32.dp,
                            paletteVibrant = artworkPalette.vibrantColor,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )

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
                                onOpenLyrics = {
                                    onFetchLyrics()
                                    showLyricsSheet = true
                                },
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

                        StationArtworkCard(
                            station = station,
                            isPlaying = isPlaying,
                            trackArtworkUrl = trackArtworkUrl,
                            isLoading = isLoading,
                            artScale = artScale,
                            cornerRadius = 24.dp,
                            paletteVibrant = artworkPalette.vibrantColor,
                            modifier = Modifier.size(artSize)
                        )

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
                            onOpenLyrics = {
                                onFetchLyrics()
                                showLyricsSheet = true
                            },
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
    onOpenLyrics: () -> Unit = {},
    isHorizontal: Boolean
) {
    val isPodcast = station.isPodcast
    var visualizerStyle by remember { mutableStateOf(VisualizerStyle.ROUNDED_BARS) }

    val playPauseFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(120)
        try {
            playPauseFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

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
                color = MaterialTheme.colorScheme.onSurface,
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        val hasLiveMetadata = !streamTitle.isNullOrBlank() && streamTitle != station.name && streamTitle != station.genre
        if (hasLiveMetadata) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
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
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.now_playing),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = if (isLoading) stringResource(R.string.buffering_stream) else (streamTitle ?: station.genre),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (hasLiveMetadata) FontWeight.Bold else FontWeight.Medium),
            color = if (isLoading) MaterialTheme.colorScheme.primary else if (hasLiveMetadata) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
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
                Text(text = station.country, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        text = station.bitrate, 
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = formatDuration(draggingValue?.toLong() ?: currentPosition),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(text = formatDuration(totalDuration), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
                
                if (currentPosition > 5000L) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.resumed_at, formatDuration(currentPosition)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onSeek?.invoke(0L) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Frequency Visualizer Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.audio_frequency_visualizer),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else if (isChipFocused) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                border = BorderStroke(
                                    if (isChipFocused) 2.dp else 1.dp,
                                    if (isChipFocused) MaterialTheme.colorScheme.primary else if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
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
                                        VisualizerStyle.NEON_RIBBON -> stringResource(R.string.viz_ribbon)
                                        VisualizerStyle.DOT_MATRIX -> stringResource(R.string.viz_matrix)
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected || isChipFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AudioVisualizer(
                    waveAmplitudes = waveAmplitudes,
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .height(52.dp)
                        .fillMaxWidth(),
                    style = visualizerStyle,
                    primaryColor = MaterialTheme.colorScheme.primary,
                    secondaryColor = MaterialTheme.colorScheme.secondary,
                    accentColor = MaterialTheme.colorScheme.tertiary
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
                    .background(if (isPrevFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous Station",
                    modifier = Modifier.size(32.dp),
                    tint = if (isPrevFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            if (isPodcast && onSeekRelative != null) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSeekRelative(-15000L)
                    }
                ) {
                    Icon(Icons.Filled.Replay10, null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            var isPlayFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .focusRequester(playPauseFocusRequester)
                    .size(72.dp)
                    .scale(if (isPlayFocused) 1.15f else 1.0f)
                    .onFocusChanged { isPlayFocused = it.isFocused }
                    .border(
                        width = if (isPlayFocused) 3.dp else 0.dp,
                        color = if (isPlayFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .background(if (isPlayFocused) MaterialTheme.colorScheme.primary else PlayButtonContainer)
                    .focusable()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTogglePlay()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isPlayFocused) MaterialTheme.colorScheme.background else PlayButtonContent,
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
                    Icon(Icons.Filled.Forward30, null, tint = MaterialTheme.colorScheme.primary)
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
                    .background(if (isNextFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next Station",
                    modifier = Modifier.size(32.dp),
                    tint = if (isNextFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.VolumeDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
            )
            Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
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
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
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
                    label = { Text(stringResource(R.string.speed_format, playbackSpeed.toString())) },
                    leadingIcon = { Icon(Icons.Filled.Speed, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                )
            }
            val activePresetLabel = eqPresets.find { it.key == activeEqPreset }?.labelResId?.let { stringResource(it) } ?: activeEqPreset
            AssistChip(
                onClick = onOpenEqualizer,
                label = { Text(stringResource(R.string.eq_label, activePresetLabel)) },
                leadingIcon = { Icon(Icons.Filled.Tune, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
            )
            AssistChip(
                onClick = onOpenSleepTimer,
                label = { Text(if (sleepTimerRemaining != null) "${sleepTimerRemaining}m" else stringResource(R.string.timer_label)) },
                leadingIcon = { Icon(Icons.Filled.Bedtime, null, modifier = Modifier.size(16.dp), tint = if (sleepTimerRemaining != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary) },
                colors = if (sleepTimerRemaining != null) AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else AssistChipDefaults.assistChipColors()
            )
            AssistChip(
                onClick = onOpenLyrics,
                label = { Text(stringResource(R.string.lyrics_label)) },
                leadingIcon = { Icon(Icons.Filled.Mic, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
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

@Composable
private fun StationArtworkCard(
    station: RadioStation,
    isPlaying: Boolean = false,
    trackArtworkUrl: String? = null,
    isLoading: Boolean,
    artScale: Float,
    cornerRadius: androidx.compose.ui.unit.Dp,
    paletteVibrant: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectiveUrl = trackArtworkUrl?.ifBlank { null } ?: station.imageUrl
    val imageRequest = remember(effectiveUrl) {
        ImageRequest.Builder(context)
            .data(effectiveUrl?.ifBlank { null })
            .crossfade(true)
            .error(R.drawable.ic_favicon)
            .placeholder(R.drawable.ic_favicon)
            .build()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "playerArtAnim")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "playerArtRotation"
    )

    Box(
        modifier = modifier
            .scale(artScale)
            .pulseOnPlaying(isPlaying = isPlaying, pulseTargetScale = 1.05f)
            .clip(RoundedCornerShape(cornerRadius))
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.05f),
                        paletteVibrant.copy(alpha = 0.25f)
                    )
                ),
                RoundedCornerShape(cornerRadius)
            )
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        // Layer 1: Ambient Blurred Glow Background from the Station Artwork
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 24.dp)
                .alpha(if (isPlaying) 0.55f else 0.35f)
        )

        // Radial depth vignette overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.82f)
                        )
                    )
                )
        )

        // Layer 2: Clean Inner Artwork Frame (prevents pixelation of low-res favicons)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedStationAvatar(
                imageUrl = effectiveUrl,
                contentDescription = station.name,
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(cornerRadius - 8.dp),
                borderWidth = 0.5.dp,
                borderColor = Color.White.copy(alpha = 0.15f),
                showVinylCenter = true,
                enableRotation = true
            )
        }

        // Layer 3: Glass top gloss reflection
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.20f)
                        )
                    )
                )
        )

        // Layer 4: Buffering / Loading Indicator
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.5.dp,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}
