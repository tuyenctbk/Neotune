package com.easeaudio.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.foundation.border
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.easeaudio.R
import com.easeaudio.ui.theme.*
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionSheet(
    trackTitle: String,
    stationName: String,
    stationGenre: String = "",
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onSetAsAlarmStation: (() -> Unit)? = null,
    onBlockStation: (() -> Unit)? = null,
    onOpenLyrics: (() -> Unit)? = null,
    onOpenQrCode: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isPodcast = stationGenre.contains("Podcast", ignoreCase = true) ||
            stationGenre.contains("Talk", ignoreCase = true) ||
            stationGenre.contains("Audiobook", ignoreCase = true) ||
            stationGenre.contains("Story", ignoreCase = true) ||
            stationGenre.contains("Drama", ignoreCase = true) ||
            stationGenre.contains("Interview", ignoreCase = true) ||
            stationGenre.contains("Speech", ignoreCase = true) ||
            stationGenre.contains("Spoken", ignoreCase = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPodcast) Icons.Filled.MusicNote else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(if (isPodcast) R.string.podcast_options else R.string.station_options),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stationName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Currently Playing Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
            ) {
                Text(
                    text = trackTitle,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Item 1: Add/Remove Favorites
            if (onToggleFavorite != null) {
                TrackActionItem(
                    icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    label = stringResource(if (isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites),
                    tint = if (isFavorite) FavoriteHeartColor else MaterialTheme.colorScheme.onSurface
                ) {
                    onDismiss()
                    onToggleFavorite()
                }
            }

            // Action Item 2: Set as Radio Alarm Station
            if (onSetAsAlarmStation != null) {
                TrackActionItem(
                    icon = Icons.Filled.Alarm,
                    label = stringResource(R.string.set_as_alarm_station),
                    tint = MaterialTheme.colorScheme.primary
                ) {
                    onDismiss()
                    onSetAsAlarmStation()
                }
            }

            // Action Item: View Live Lyrics
            if (onOpenLyrics != null) {
                TrackActionItem(
                    icon = Icons.Filled.Mic,
                    label = stringResource(R.string.lyrics_title),
                    tint = MaterialTheme.colorScheme.primary
                ) {
                    onDismiss()
                    onOpenLyrics()
                }
            }

            // Action Item 3: Search on YouTube
            TrackActionItem(
                icon = Icons.Filled.PlayArrow,
                label = stringResource(R.string.search_on_youtube),
                tint = MaterialTheme.colorScheme.primary
            ) {
                onDismiss()
                val query = URLEncoder.encode(trackTitle, "UTF-8")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$query"))
                context.startActivity(intent)
            }

            // Action Item: Scan & Share QR Code
            if (onOpenQrCode != null) {
                TrackActionItem(
                    icon = Icons.Filled.QrCode2,
                    label = stringResource(R.string.show_qr_code),
                    tint = MaterialTheme.colorScheme.primary
                ) {
                    onDismiss()
                    onOpenQrCode()
                }
            }

            // Action Item 4: Share Track (with App Download Link)
            val shareTrackMessage = stringResource(R.string.share_track_message, trackTitle, stationName, "https://play.google.com/store/apps/details?id=${context.packageName}")
            val shareTrackChooserText = stringResource(R.string.share_track_chooser)
            TrackActionItem(
                icon = Icons.Filled.Share,
                label = stringResource(R.string.share_track),
                tint = MaterialTheme.colorScheme.onSurface
            ) {
                onDismiss()
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareTrackMessage)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, shareTrackChooserText))
            }

            // Action Item 5: Block This Station
            if (onBlockStation != null) {
                TrackActionItem(
                    icon = Icons.Filled.Block,
                    label = stringResource(R.string.block_this_station),
                    tint = androidx.compose.ui.graphics.Color(0xFFFF5252)
                ) {
                    onDismiss()
                    onBlockStation()
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TrackActionItem(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        color = if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) MaterialTheme.colorScheme.primary else tint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
