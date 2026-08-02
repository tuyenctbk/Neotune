package com.easeaudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.easeaudio.R
import com.easeaudio.data.PodcastEpisode
import com.easeaudio.data.RadioStation
import com.easeaudio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastEpisodesSheet(
    show: RadioStation,
    episodes: List<PodcastEpisode>,
    currentEpisode: PodcastEpisode?,
    isPlaying: Boolean,
    isLoading: Boolean,
    onSelectEpisode: (PodcastEpisode) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkBackground,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted) },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Top Header: Show Cover & Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = show.imageUrl,
                    contentDescription = show.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = show.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = show.genre,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonPurple.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.episodes_available, episodes.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = TextMuted
                    )
                }
            }

            HorizontalDivider(color = DarkSurfaceVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Episodes List
            if (episodes.isEmpty() && isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NeonPurple, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.loading_podcast_episodes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            } else if (episodes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_episodes_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(episodes, key = { it.id }) { episode ->
                        val isSelected = currentEpisode?.id == episode.id || (currentEpisode == null && episode.audioUrl == show.streamUrl)
                        val isEpisodePlaying = isSelected && isPlaying

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectEpisode(episode) }
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) NeonPurple else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .testTag("episode_item_${episode.id}"),
                            color = if (isSelected) DarkSurfaceVariant else DarkSurface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Play / Pause indicator button
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) NeonPurple else DarkSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected && isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = DarkBackground,
                                            strokeWidth = 2.5.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isEpisodePlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = if (isEpisodePlaying) "Pause" else "Play",
                                            tint = if (isSelected) DarkBackground else TextPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Episode details
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = episode.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 15.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                        ),
                                        color = if (isSelected) NeonPurple else TextPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (episode.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = episode.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (episode.pubDate.isNotBlank()) {
                                            Text(
                                                text = episode.pubDate,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }
                                        if (episode.pubDate.isNotBlank() && episode.durationMs > 0L) {
                                            Text(" • ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        }
                                        if (episode.durationMs > 0L) {
                                            Icon(
                                                imageVector = Icons.Filled.Schedule,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = formatDurationMs(episode.durationMs),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
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
    }
}

private fun formatDurationMs(ms: Long): String {
    val totalSecs = ms / 1000L
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    val hours = mins / 60
    val remMins = mins % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, remMins, secs)
    } else {
        "%d:%02d".format(remMins, secs)
    }
}
