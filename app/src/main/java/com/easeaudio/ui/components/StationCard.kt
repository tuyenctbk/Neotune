package com.easeaudio.ui.components

import android.content.Intent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.easeaudio.R
import com.easeaudio.data.RadioStation
import com.easeaudio.ui.theme.FavoriteHeartColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StationCard(
    station: RadioStation,
    isSelected: Boolean,
    isPlaying: Boolean,
    isDemoted: Boolean = false,
    isLoading: Boolean = false,
    isUnreachable: Boolean = false,
    isListenLater: Boolean = false,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleListenLater: () -> Unit = {},
    onBlockStation: () -> Unit = {},
    onDemoteStation: () -> Unit = {},
    onUndemoteStation: () -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val activeAccent = MaterialTheme.colorScheme.primary
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "station_card_focus_scale"
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
                .scale(focusScale)
                .shadow(
                    elevation = if (isFocused) 16.dp else 0.dp,
                    shape = RoundedCornerShape(18.dp),
                    spotColor = activeAccent,
                    ambientColor = activeAccent.copy(alpha = 0.5f)
                )
                .clip(RoundedCornerShape(18.dp))
                .combinedClickable(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelect()
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                )
                .border(
                    width = if (isFocused) 3.5.dp else if (isSelected) 1.5.dp else 1.dp,
                    brush = if (isFocused) {
                        Brush.horizontalGradient(
                            listOf(
                                activeAccent,
                                Color.White,
                                activeAccent
                            )
                        )
                    } else if (isSelected) {
                        Brush.horizontalGradient(
                            listOf(
                                activeAccent.copy(alpha = 0.8f),
                                activeAccent.copy(alpha = 0.4f)
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(18.dp)
                )
                .testTag("station_card_${station.id}"),
            color = if (isFocused) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f) else if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surface
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isFocused) {
                            Modifier.background(
                                Brush.horizontalGradient(
                                    listOf(
                                        activeAccent.copy(alpha = 0.22f),
                                        Color.Transparent
                                    )
                                )
                            )
                        } else Modifier
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (isFocused) 2.dp else 0.dp,
                                color = if (isFocused) activeAccent else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        AsyncImage(
                            model = station.imageUrl,
                            contentDescription = station.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = activeAccent,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = activeAccent,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = station.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (isFocused || isSelected) FontWeight.Black else FontWeight.Bold,
                                fontSize = if (isFocused) 16.sp else 15.sp
                            ),
                            color = if (isFocused) Color.White else if (isSelected) activeAccent else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isSelected && isLoading) {
                            Text(
                                text = stringResource(R.string.buffering_stream),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = activeAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (isUnreachable) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB74D),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.stream_unreachable),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFFB74D),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val genreText = station.genre.ifBlank { "Radio" }
                                Text(
                                    text = genreText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = if (isFocused) activeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (station.bitrate.isNotBlank()) {
                                    Text(
                                        text = " • ${station.bitrate}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isFocused) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = activeAccent,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.background,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPlaying) "PLAYING" else "TUNE IN",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.background
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleFavorite()
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .focusProperties { canFocus = false }
                                .testTag("favorite_button_${station.id}")
                        ) {
                            Icon(
                                imageVector = if (station.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (station.isFavorite) FavoriteHeartColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            DropdownMenuItem(
                text = { Text(if (station.isFavorite) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites), color = MaterialTheme.colorScheme.onSurface) },
                leadingIcon = { 
                    Icon(
                        imageVector = if (station.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, 
                        contentDescription = null, 
                        tint = if (station.isFavorite) FavoriteHeartColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    ) 
                },
                onClick = {
                    showMenu = false
                    onToggleFavorite()
                }
            )

            DropdownMenuItem(
                text = { Text(if (isListenLater) stringResource(R.string.remove_from_listen_later) else stringResource(R.string.add_to_listen_later), color = MaterialTheme.colorScheme.onSurface) },
                leadingIcon = { 
                    Icon(
                        imageVector = if (isListenLater) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary
                    ) 
                },
                onClick = {
                    showMenu = false
                    onToggleListenLater()
                }
            )
            
            DropdownMenuItem(
                text = { 
                    Text(
                        if (isDemoted) stringResource(R.string.move_to_top) else stringResource(R.string.move_to_bottom), 
                        color = MaterialTheme.colorScheme.onSurface
                    ) 
                },
                leadingIcon = { 
                    Icon(
                        imageVector = if (isDemoted) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary
                    ) 
                },
                onClick = {
                    showMenu = false
                    if (isDemoted) onUndemoteStation() else onDemoteStation()
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

            DropdownMenuItem(
                text = { Text(stringResource(R.string.share_station), color = MaterialTheme.colorScheme.onSurface) },
                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = {
                    showMenu = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val shareText = "Listening to ${station.name} (${station.genre})\nStream: ${station.streamUrl}\nTune in live on NeoTune Radio!"
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Listen to ${station.name}")
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_station)))
                }
            )

            DropdownMenuItem(
                text = { Text(stringResource(R.string.block_this_station), color = Color(0xFFEF5350)) },
                leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null, tint = Color(0xFFEF5350)) },
                onClick = {
                    showMenu = false
                    onBlockStation()
                }
            )
        }
    }
}
