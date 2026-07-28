package com.easeaudio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.easeaudio.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.easeaudio.data.RadioStation
import com.easeaudio.ui.theme.*

@Composable
fun MiniPlayer(
    station: RadioStation?,
    isPlaying: Boolean,
    isLoading: Boolean,
    streamTitle: String?,
    waveAmplitudes: List<Float>,
    onTogglePlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = station != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        if (station != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenFullPlayer() }
                    .testTag("mini_player_bar"),
                color = DarkSurfaceVariant,
                tonalElevation = 8.dp,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Station Artwork
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        AsyncImage(
                            model = station.imageUrl,
                            contentDescription = station.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title & Stream Info
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = station.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Badge
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelMedium,
                                color = DarkBackground,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonPink)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        Text(
                            text = if (isLoading) stringResource(R.string.buffering_stream) else (streamTitle ?: station.genre),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isLoading) NeonCyan else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Mini Wave Visualizer
                    if (isPlaying) {
                        Row(
                            modifier = Modifier
                                .height(20.dp)
                                .padding(horizontal = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            waveAmplitudes.take(5).forEach { amp ->
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .fillMaxHeight(amp.coerceIn(0.2f, 1.0f))
                                        .clip(CircleShape)
                                        .background(NeonCyan)
                                )
                            }
                        }
                    }

                    // Favorite Button
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.testTag("mini_player_favorite")
                    ) {
                        Icon(
                            imageVector = if (station.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (station.isFavorite) NeonPink else TextMuted
                        )
                    }

                    // Play/Pause Button
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(NeonCyan, NeonPurple)
                                )
                            )
                            .testTag("mini_player_play_pause")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = DarkBackground,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = DarkBackground
                            )
                        }
                    }
                }
            }
        }
    }
}
