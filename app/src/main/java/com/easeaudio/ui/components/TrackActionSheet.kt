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
import com.easeaudio.R
import com.easeaudio.ui.theme.*
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionSheet(
    trackTitle: String,
    stationName: String,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onSetAsAlarmStation: (() -> Unit)? = null,
    onBlockStation: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        scrimColor = DarkBackground.copy(alpha = 0.7f),
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
                            .background(NeonCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.track_options),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = stationName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Currently Playing Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = DarkBackground.copy(alpha = 0.8f)
            ) {
                Text(
                    text = trackTitle,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = NeonCyan,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Item 1: Add/Remove Favorites
            if (onToggleFavorite != null) {
                TrackActionItem(
                    icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    label = stringResource(if (isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites),
                    tint = if (isFavorite) FavoriteHeartColor else TextPrimary
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
                    tint = NeonCyan
                ) {
                    onDismiss()
                    onSetAsAlarmStation()
                }
            }

            // Action Item 3: Search on YouTube
            TrackActionItem(
                icon = Icons.Filled.PlayArrow,
                label = stringResource(R.string.search_on_youtube),
                tint = NeonCyan
            ) {
                onDismiss()
                val query = URLEncoder.encode(trackTitle, "UTF-8")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$query"))
                context.startActivity(intent)
            }

            // Action Item 4: Share Track (with App Download Link)
            val shareTrackMessage = stringResource(R.string.share_track_message, trackTitle, stationName, "https://play.google.com/store/apps/details?id=${context.packageName}")
            val shareTrackChooserText = stringResource(R.string.share_track_chooser)
            TrackActionItem(
                icon = Icons.Filled.Share,
                label = stringResource(R.string.share_track),
                tint = TextPrimary
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = DarkSurfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = TextPrimary
            )
        }
    }
}
