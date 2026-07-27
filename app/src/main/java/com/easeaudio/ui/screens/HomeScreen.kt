package com.easeaudio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.easeaudio.R
import com.easeaudio.ads.AdMobBanner
import com.easeaudio.data.RadioStation
import com.easeaudio.firebase.AppRemoteConfig
import com.easeaudio.network.NetworkStatus
import com.easeaudio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    stations: List<RadioStation>,
    recentStations: List<RadioStation>,
    currentStation: RadioStation?,
    isPlaying: Boolean,
    searchQuery: String,
    selectedGenre: String,
    availableGenres: List<String>,
    sleepTimerRemaining: Int?,
    networkStatus: NetworkStatus = NetworkStatus(),
    remoteConfig: AppRemoteConfig = AppRemoteConfig(),
    isLoadingMore: Boolean = false,
    canLoadMore: Boolean = true,
    onSearchQueryChange: (String) -> Unit,
    onGenreSelect: (String) -> Unit,
    onStationSelect: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    onOpenAddStation: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenNetworkConfig: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val isFabVisible by remember(searchQuery, stations) {
        derivedStateOf {
            searchQuery.isNotBlank() && stations.isEmpty()
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItemsCount = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 4
        }
    }

    LaunchedEffect(shouldLoadMore, canLoadMore, isLoadingMore) {
        if (shouldLoadMore && canLoadMore && !isLoadingMore) {
            onLoadMore()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        floatingActionButton = {
            AnimatedVisibility(
                visible = isFabVisible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.imePadding()
            ) {
                FloatingActionButton(
                    onClick = onOpenAddStation,
                    containerColor = NeonCyan,
                    contentColor = DarkBackground,
                    shape = CircleShape,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("fab_add_station")
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Custom Station")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // AdMob Banner (Configured via Firebase Remote Config)
            if (remoteConfig.adsEnabled) {
                item {
                    AdMobBanner(
                        adUnitId = remoteConfig.bannerAdUnitId,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // App Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Radio,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = stringResource(R.string.app_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }

                        // Action Icons (Equalizer, Sleep Timer)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onOpenEqualizer,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(DarkSurfaceVariant)
                                    .testTag("btn_open_equalizer")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Equalizer,
                                    contentDescription = stringResource(R.string.equalizer),
                                    tint = NeonCyan
                                )
                            }

                            IconButton(
                                onClick = onOpenSleepTimer,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (sleepTimerRemaining != null) ActivePill else DarkSurfaceVariant)
                                    .testTag("btn_open_sleep_timer")
                                ) {
                                Icon(
                                    imageVector = Icons.Filled.Bedtime,
                                    contentDescription = stringResource(R.string.sleep_timer),
                                    tint = if (sleepTimerRemaining != null) NeonPurple else TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.search_placeholder), color = TextMuted) },
                    leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .testTag("input_search_stations")
                )
            }

            // Genre Filter Pills
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableGenres) { genre ->
                        val isSelected = genre == selectedGenre
                        FilterChip(
                            selected = isSelected,
                            onClick = { onGenreSelect(genre) },
                            label = {
                                Text(
                                    text = genre,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = DarkBackground,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = CardBorder,
                                selectedBorderColor = NeonCyan
                            ),
                            modifier = Modifier.testTag("genre_chip_$genre")
                        )
                    }
                }
            }

            // Featured Station Hero Banner
            if (stations.isNotEmpty() && searchQuery.isEmpty() && selectedGenre == "All") {
                val featured = stations.first()
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onStationSelect(featured) }
                            .testTag("hero_featured_card"),
                        color = DarkSurface
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                            AsyncImage(
                                model = featured.imageUrl,
                                contentDescription = featured.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Gradient Overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, DarkBackground.copy(alpha = 0.95f))
                                        )
                                    )
                            )
                            // Content
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "FEATURED STATION",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = featured.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${featured.genre} • ${featured.bitrate}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }

                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = CircleShape,
                                    color = NeonCyan
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (currentStation?.id == featured.id && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = "Play Featured",
                                            tint = DarkBackground
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section Title
            item {
                Text(
                    text = stringResource(R.string.live_radio_stations),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }

            // Empty State
            if (stations.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.no_stations_found), style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(stringResource(R.string.add_station_prompt), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        }
                    }
                }
            }

            // Station Cards List
            items(stations, key = { it.id }) { station ->
                val isSelected = currentStation?.id == station.id
                StationCard(
                    station = station,
                    isSelected = isSelected,
                    isPlaying = isSelected && isPlaying,
                    onSelect = { onStationSelect(station) },
                    onToggleFavorite = { onToggleFavorite(station) }
                )
            }

            if (isLoadingMore) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = NeonCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.loading_more_stations),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StationCard(
    station: RadioStation,
    isSelected: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) NeonCyan else CardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onSelect() }
            .testTag("station_card_${station.id}"),
        color = if (isSelected) DarkSurfaceVariant else DarkSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Station Image
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                            .background(DarkBackground.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = NeonCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) NeonCyan else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = station.genre,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(" • ", color = TextMuted)
                    Text(
                        text = station.country,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }

            // Favorite Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("favorite_button_${station.id}")
            ) {
                Icon(
                    imageVector = if (station.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (station.isFavorite) NeonPink else TextMuted
                )
            }
        }
    }
}
