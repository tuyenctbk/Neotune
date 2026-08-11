package com.easeaudio.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.easeaudio.R
import com.easeaudio.data.RadioStation
import com.easeaudio.ui.theme.*

enum class LibraryFilter {
    ALL, RADIO, PODCASTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favoriteStations: List<RadioStation>,
    currentStation: RadioStation?,
    isPlaying: Boolean,
    isLoading: Boolean = false,
    demotedStationIds: Set<String> = emptySet(),
    failedStationIds: Set<String> = emptySet(),
    onStationSelect: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    onBlockStation: (RadioStation) -> Unit = {},
    onDemoteStation: (RadioStation) -> Unit = {},
    onUndemoteStation: (RadioStation) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(LibraryFilter.ALL) }

    val filteredFavorites = remember(favoriteStations, searchQuery, selectedFilter) {
        favoriteStations.filter { station ->
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
        containerColor = DarkBackground
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
                    .widthIn(max = 800.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = FavoriteHeartColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.your_favorite_stations),
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${favoriteStations.size} ${stringResource(R.string.favorites)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                // Only show Search bar and Filter pills if overall favorites list is NOT empty
                if (favoriteStations.isNotEmpty()) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.search_placeholder), color = TextMuted) },
                        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Clear search",
                                        tint = TextMuted
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            errorBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .testTag("favorites_search_input")
                    )

                    // Filter Category Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LibraryFilter.values().forEach { filter ->
                            val isSelected = selectedFilter == filter
                            val label = when (filter) {
                                LibraryFilter.ALL -> "All"
                                LibraryFilter.RADIO -> "Radio"
                                LibraryFilter.PODCASTS -> "Podcasts"
                            }
                            val bgColors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) NeonCyan else DarkSurface,
                                contentColor = if (isSelected) DarkBackground else TextPrimary
                            )
                            Button(
                                onClick = { selectedFilter = filter },
                                colors = bgColors,
                                shape = RoundedCornerShape(100.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("lib_filter_${filter.name.lowercase()}")
                            ) {
                                Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (favoriteStations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.no_saved_favorites),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (filteredFavorites.isEmpty()) {
                    // Show "No matches" if search/filter leaves the list empty
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
                                tint = TextMuted,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.no_matching_favorites),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredFavorites, key = { it.id }) { station ->
                            val isSelected = currentStation?.id == station.id
                            val isUnreachable = failedStationIds.contains(station.id)
                            StationCard(
                                station = station,
                                isSelected = isSelected,
                                isPlaying = isSelected && isPlaying,
                                isDemoted = demotedStationIds.contains(station.id),
                                isLoading = isSelected && isLoading,
                                isUnreachable = isUnreachable,
                                onSelect = { onStationSelect(station) },
                                onToggleFavorite = { onToggleFavorite(station) },
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
