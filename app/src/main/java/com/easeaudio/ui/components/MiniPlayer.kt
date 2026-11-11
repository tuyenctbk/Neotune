package com.easeaudio.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easeaudio.R
import com.easeaudio.data.RadioStation
import com.easeaudio.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayer(
    station: RadioStation?,
    isPlaying: Boolean,
    isLoading: Boolean,
    streamTitle: String?,
    waveAmplitudes: List<Float>,
    currentPosition: Long = 0L,
    totalDuration: Long = 0L,
    trackArtworkUrl: String? = null,
    onTogglePlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    onOpenTrackOptions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = station != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        if (station != null) {
            var isFocused by remember { mutableStateOf(false) }
            val showFocus = isFocused
            val resolvedStationUrl = remember(station.name, station.imageUrl, station.genre) {
                com.easeaudio.util.StationLogoResolver.resolveStationLogo(
                    name = station.name,
                    favicon = station.imageUrl,
                    homepage = "",
                    tags = station.genre
                )
            }
            val effectiveArtworkUrl = trackArtworkUrl?.ifBlank { null } ?: resolvedStationUrl.ifBlank { null }

            // Avatar Animations: breathing shimmer when loading, subtle scale when playing
            val loadingAlpha = if (isLoading) {
                val infiniteTransition = rememberInfiniteTransition(label = "miniAvatarAnim")
                val anim by infiniteTransition.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "miniAvatarLoadingAlpha"
                )
                anim
            } else {
                1.0f
            }
            val playingScale by animateFloatAsState(
                targetValue = if (isPlaying) 1.04f else 1.0f,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "miniAvatarScale"
            )

            val miniPlayerScale by animateFloatAsState(
                targetValue = if (showFocus) 1.025f else 1.0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "miniPlayerScale"
            )

            val haptic = LocalHapticFeedback.current

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .scale(miniPlayerScale)
                    .shadow(
                        elevation = if (showFocus) 16.dp else 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = if (showFocus) MaterialTheme.colorScheme.primary else Color.Black
                    )
                    .onFocusChanged { isFocused = it.isFocused }
                    .clip(RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -18f) {
                                onOpenFullPlayer()
                            }
                        }
                    }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            when (keyEvent.key) {
                                Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onOpenFullPlayer()
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
                    .combinedClickable(
                        onClick = { onOpenFullPlayer() },
                        onDoubleClick = { onOpenFullPlayer() }
                    )
                    .border(
                        width = if (showFocus) 2.5.dp else 1.dp,
                        color = if (showFocus) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("mini_player_bar"),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Subtle drag handle indicator indicating swipe-up to expand
                    Box(
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .width(36.dp)
                            .height(3.5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    // Station Artwork with AnimatedStationAvatar subtle pulse animation
                    AnimatedStationAvatar(
                        imageUrl = effectiveArtworkUrl,
                        contentDescription = station.name,
                        isPlaying = isPlaying,
                        stationName = station.name,
                        genre = station.genre,
                        isPodcast = station.isPodcast,
                        modifier = Modifier
                            .size(48.dp)
                            .alpha(if (isLoading) loadingAlpha else 1.0f),
                        shape = CircleShape,
                        borderWidth = 1.5.dp,
                        borderColor = if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        showVinylCenter = true,
                        enableRotation = true
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title & Stream Info
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = station.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Badge
                            val isPodcast = station.isPodcast
                            Text(
                                text = stringResource(if (isPodcast) R.string.badge_podcast else R.string.live_badge),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.background,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        if (station.isPodcast && totalDuration > 0L && !isLoading) {
                            Spacer(modifier = Modifier.height(3.dp))
                            val progress = (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
                            val animatedProgress by animateFloatAsState(
                                targetValue = progress,
                                animationSpec = spring(stiffness = Spring.StiffnessLow),
                                label = "miniPlayerPodcastProgress"
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${formatDurationShort(currentPosition)} / ${formatDurationShort(totalDuration)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            val displaySubtitle = if (isLoading) stringResource(R.string.buffering_stream) else (streamTitle ?: station.genre)
                            AnimatedContent(
                                targetState = displaySubtitle,
                                transitionSpec = {
                                    (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 3 }) togetherWith
                                        (fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 3 })
                                },
                                label = "miniPlayerSubtitleAnim"
                            ) { currentSubtitle ->
                                Text(
                                    text = currentSubtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Mini Wave Visualizer
                    if (isPlaying) {
                        AudioVisualizer(
                            waveAmplitudes = waveAmplitudes.take(5),
                            isPlaying = isPlaying,
                            modifier = Modifier
                                .width(28.dp)
                                .height(20.dp)
                                .padding(horizontal = 2.dp),
                            style = VisualizerStyle.ROUNDED_BARS,
                            primaryColor = MaterialTheme.colorScheme.primary,
                            secondaryColor = MaterialTheme.colorScheme.secondary,
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            barCount = 5
                        )
                    }

                    // Favorite Button with spring bounce on toggle
                    var isFavFocused by remember { mutableStateOf(false) }
                    val favScale by animateFloatAsState(
                        targetValue = when {
                            isFavFocused -> 1.18f
                            station.isFavorite -> 1.06f
                            else -> 1.0f
                        },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "favScale"
                    )
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleFavorite()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .scale(favScale)
                            .onFocusChanged { isFavFocused = it.isFocused }
                            .clip(CircleShape)
                            .background(if (isFavFocused) FavoriteHeartColor.copy(alpha = 0.25f) else Color.Transparent)
                            .border(
                                width = if (isFavFocused) 2.dp else 0.dp,
                                color = if (isFavFocused) FavoriteHeartColor else Color.Transparent,
                                shape = CircleShape
                            )
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyUp) {
                                    when (keyEvent.key) {
                                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onToggleFavorite()
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                            .testTag("mini_player_favorite")
                    ) {
                        Icon(
                            imageVector = if (station.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(R.string.favorites),
                            tint = if (isFavFocused) FavoriteHeartColor else (if (station.isFavorite) FavoriteHeartColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Expand to Fullscreen Button
                    var isExpandFocused by remember { mutableStateOf(false) }
                    val expandScale by animateFloatAsState(
                        targetValue = if (isExpandFocused) 1.15f else 1.0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "expandScale"
                    )
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenFullPlayer()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .scale(expandScale)
                            .onFocusChanged { isExpandFocused = it.isFocused }
                            .clip(CircleShape)
                            .background(if (isExpandFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent)
                            .border(
                                width = if (isExpandFocused) 2.dp else 0.dp,
                                color = if (isExpandFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyUp) {
                                    when (keyEvent.key) {
                                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onOpenFullPlayer()
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                            .testTag("mini_player_expand_fullscreen")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenInFull,
                            contentDescription = stringResource(R.string.fullscreen_player),
                            tint = if (isExpandFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Play/Pause Button
                    var isPlayFocused by remember { mutableStateOf(false) }
                    val playScale by animateFloatAsState(
                        targetValue = if (isPlayFocused) 1.15f else 1.0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "playScale"
                    )
                    val playBtnContainerColor = if (isPlayFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                    val playBtnContentColor = MaterialTheme.colorScheme.onPrimary

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTogglePlay()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .scale(playScale)
                            .onFocusChanged { isPlayFocused = it.isFocused }
                            .clip(CircleShape)
                            .background(playBtnContainerColor)
                            .border(
                                width = if (isPlayFocused) 2.5.dp else 0.dp,
                                color = if (isPlayFocused) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyUp) {
                                    when (keyEvent.key) {
                                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onTogglePlay()
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                            .testTag("mini_player_play_pause")
                    ) {
                        AnimatedContent(
                            targetState = if (isLoading) 0 else if (isPlaying) 1 else 2,
                            transitionSpec = {
                                (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy), initialScale = 0.7f) + fadeIn(tween(150))) togetherWith
                                    (scaleOut(tween(120), targetScale = 0.7f) + fadeOut(tween(120)))
                            },
                            label = "miniPlayerPlayIconAnim"
                        ) { playState ->
                            when (playState) {
                                0 -> CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = playBtnContentColor,
                                    strokeWidth = 2.dp
                                )
                                1 -> Icon(
                                    imageVector = Icons.Filled.Pause,
                                    contentDescription = stringResource(R.string.pause),
                                    tint = playBtnContentColor
                                )
                                else -> Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = stringResource(R.string.play),
                                    tint = playBtnContentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

private fun formatDurationShort(ms: Long): String {
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
