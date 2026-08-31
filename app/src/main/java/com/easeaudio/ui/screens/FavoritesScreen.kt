package com.easeaudio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.easeaudio.R
import com.easeaudio.data.ListenLaterItem
import com.easeaudio.data.RadioStation
import com.easeaudio.ui.components.StationCard
import com.easeaudio.ui.theme.*

enum class LibraryTab {
    FAVORITES,
    LISTEN_LATER
}

enum class LibraryFilter {
    ALL, RADIO, PODCASTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favoriteStations: List<RadioStation>,
    listenLaterItems: List<ListenLaterItem> = emptyList(),
    currentStation: RadioStation?,
    isPlaying: Boolean,
    isLoading: Boolean = false,
    demotedStationIds: Set<String> = emptySet(),
    failedStationIds: Set<String> = emptySet(),
    onStationSelect: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    onToggleListenLater: (RadioStation) -> Unit = {},
    onClearListenLater: () -> Unit = {},
    onBlockStation: (RadioStation) -> Unit = {},
    onDemoteStation: (RadioStation) -> Unit = {},
    onUndemoteStation: (RadioStation) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(LibraryTab.FAVORITES) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(LibraryFilter.ALL) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Determine grid column count at composable scope (cannot be called inside non-composable lambda)
    val context = LocalContext.current
    val favColumns = remember(context) {
        val metrics = context.resources.displayMetrics
        val widthDp = metrics.widthPixels / metrics.density
        when {
            widthDp >= 840f -> 2  // Landscape tablet / Android TV: 2 columns
            else -> 1             // Phone or portrait tablet: single-column list
        }
    }

    val listenLaterAsStations = remember(listenLaterItems) {
        listenLaterItems.map { item ->
            RadioStation(
                id = item.id,
                name = item.name,
                genre = item.genre,
                country = item.country,
                streamUrl = item.streamUrl,
                imageUrl = item.imageUrl,
                bitrate = if (item.isPodcast && !item.bitrate.equals("Podcast", ignoreCase = true) && !item.id.startsWith("itunes_", ignoreCase = true)) "Podcast" else item.bitrate,
                codec = item.codec,
                isCustom = item.isCustom,
                isPodcast = item.isPodcast, // BUG-FIX: was missing — podcast items played as radio
                isFavorite = favoriteStations.any { it.id == item.id }
            )
        }
    }

    val activeStationList = if (selectedTab == LibraryTab.FAVORITES) favoriteStations else listenLaterAsStations

    val filteredList = remember(activeStationList, searchQuery, selectedFilter) {
        activeStationList.filter { station ->
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                station.name.contains(searchQuery, ignoreCase = true) ||
                station.genre.contains(searchQuery, ignoreCase = true)
            }
            val matchesFilter = when (selectedFilter) {
                LibraryFilter.ALL -> true
                LibraryFilter.RADIO -> !station.isPodcast
                LibraryFilter.PODCASTS -> station.isPodcast
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
            ) {
                // Header Segmented Tabs (Favorites vs Listen Later)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            // Favorites Tab
                            val isFavSelected = selectedTab == LibraryTab.FAVORITES
                            var isFavFocused by remember { mutableStateOf(false) }
                            Button(
                                onClick = { selectedTab = LibraryTab.FAVORITES },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFavSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (isFavSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .onFocusChanged { isFavFocused = it.isFocused }
                                    .scale(if (isFavFocused) 1.05f else 1.0f)
                                    .border(
                                        width = if (isFavFocused) 2.dp else 0.dp,
                                        color = if (isFavFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .testTag("tab_library_favorites")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = if (isFavSelected) MaterialTheme.colorScheme.onPrimary else FavoriteHeartColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${stringResource(R.string.tab_favorites)} (${favoriteStations.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Listen Later Tab
                            val isLaterSelected = selectedTab == LibraryTab.LISTEN_LATER
                            var isLaterFocused by remember { mutableStateOf(false) }
                            Button(
                                onClick = { selectedTab = LibraryTab.LISTEN_LATER },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLaterSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (isLaterSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .onFocusChanged { isLaterFocused = it.isFocused }
                                    .scale(if (isLaterFocused) 1.05f else 1.0f)
                                    .border(
                                        width = if (isLaterFocused) 2.dp else 0.dp,
                                        color = if (isLaterFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .testTag("tab_library_listen_later")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bookmark,
                                    contentDescription = null,
                                    tint = if (isLaterSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${stringResource(R.string.tab_listen_later)} (${listenLaterItems.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (selectedTab == LibraryTab.LISTEN_LATER && listenLaterItems.isNotEmpty()) {
                        IconButton(
                            onClick = onClearListenLater,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("btn_clear_listen_later")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = stringResource(R.string.clear_listen_later),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Search Bar and Filter pills
                if (activeStationList.isNotEmpty()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.search_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            errorBorderColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 2.dp)
                            .testTag("favorites_search_input")
                    )

                    // Filter Category Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LibraryFilter.entries.forEach { filter ->
                            val isSelected = selectedFilter == filter
                            var isPillFocused by remember { mutableStateOf(false) }
                            val label = when (filter) {
                                LibraryFilter.ALL -> stringResource(R.string.all)
                                LibraryFilter.RADIO -> stringResource(R.string.nav_radio)
                                LibraryFilter.PODCASTS -> stringResource(R.string.nav_podcast)
                            }
                            val bgColors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else if (isPillFocused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else if (isPillFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Button(
                                onClick = { selectedFilter = filter },
                                colors = bgColors,
                                shape = RoundedCornerShape(100.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .onFocusChanged { isPillFocused = it.isFocused }
                                    .scale(if (isPillFocused) 1.08f else 1.0f)
                                    .border(
                                        width = if (isPillFocused) 2.dp else 0.dp,
                                        color = if (isPillFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(100.dp)
                                    )
                                    .testTag("lib_filter_${filter.name.lowercase()}")
                            ) {
                                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (activeStationList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (selectedTab == LibraryTab.FAVORITES) Icons.Outlined.FavoriteBorder else Icons.Filled.BookmarkBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (selectedTab == LibraryTab.FAVORITES) stringResource(R.string.no_saved_favorites) else stringResource(R.string.listen_later_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            if (selectedTab == LibraryTab.LISTEN_LATER) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.listen_later_empty_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    }
                } else if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.no_matching_favorites),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(favColumns),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 80.dp)
                    ) {
                        items(filteredList, key = { it.id }) { station ->
                            val isSelected = currentStation?.id == station.id
                            val isUnreachable = failedStationIds.contains(station.id)
                            val isSavedToLater = listenLaterItems.any { it.id == station.id }
                            StationCard(
                                station = station,
                                isSelected = isSelected,
                                isPlaying = isSelected && isPlaying,
                                isDemoted = demotedStationIds.contains(station.id),
                                isLoading = isSelected && isLoading,
                                isUnreachable = isUnreachable,
                                isListenLater = isSavedToLater,
                                onSelect = { onStationSelect(station) },
                                onToggleFavorite = { onToggleFavorite(station) },
                                onToggleListenLater = { onToggleListenLater(station) },
                                onBlockStation = { onBlockStation(station) },
                                onDemoteStation = { onDemoteStation(station) },
                                onUndemoteStation = { onUndemoteStation(station) }
                            )
                        }
                    }
                }
            }
        }
    }
}
