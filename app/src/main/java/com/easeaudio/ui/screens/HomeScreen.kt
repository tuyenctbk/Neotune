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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.easeaudio.ui.components.AttributionDialog
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
    isLoading: Boolean = false,
    isDiscoveringOnline: Boolean = false,
    failedStationIds: Set<String> = emptySet(),
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
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var showAttributionDialog by remember { mutableStateOf(false) }
    val isFabVisible by remember(searchQuery, stations) {
        derivedStateOf {
            searchQuery.isNotBlank() && stations.isEmpty() && !isDiscoveringOnline
        }
    }

    val shouldLoadMore by remember(listState) {
        derivedStateOf {
            val totalItemsCount = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 3
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
        PullToRefreshBox(
            isRefreshing = isDiscoveringOnline,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 800.dp)
                    .align(Alignment.TopCenter),
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
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_favicon),
                                    contentDescription = "NeoTune Logo",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.app_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }

                        // Dropdown overflow menu button for multiple secondary actions (Solution 1)
                        var showMenu by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier.wrapContentSize(Alignment.TopEnd)
                        ) {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(DarkSurfaceVariant)
                                    .testTag("btn_open_overflow_menu")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "More Options",
                                    tint = NeonCyan
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(DarkSurfaceVariant)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Equalizer", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Equalizer,
                                            contentDescription = "Equalizer",
                                            tint = NeonCyan
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onOpenEqualizer()
                                    },
                                    modifier = Modifier.testTag("menu_item_equalizer")
                                )

                                DropdownMenuItem(
                                    text = {
                                        val label = if (sleepTimerRemaining != null) "Sleep Timer (Active)" else "Sleep Timer"
                                        Text(label, color = TextPrimary)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Bedtime,
                                            contentDescription = "Sleep Timer",
                                            tint = if (sleepTimerRemaining != null) NeonPurple else TextMuted
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onOpenSleepTimer()
                                    },
                                    modifier = Modifier.testTag("menu_item_sleep_timer")
                                )

                                DropdownMenuItem(
                                    text = { Text("Info", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Info,
                                            contentDescription = "Info",
                                            tint = NeonCyan
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showAttributionDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_item_attribution")
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
                                        val isFeaturedSelected = currentStation?.id == featured.id
                                        if (isFeaturedSelected && isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = DarkBackground,
                                                strokeWidth = 2.5.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (isFeaturedSelected && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
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

            // Empty or Initial Loading State
            if (stations.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDiscoveringOnline) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = NeonCyan,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.loading_more_stations),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.no_stations_found), style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(stringResource(R.string.add_station_prompt), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                            }
                        }
                    }
                }
            }

            // Station Cards List
            items(stations, key = { it.id }) { station ->
                val isSelected = currentStation?.id == station.id
                val isUnreachable = failedStationIds.contains(station.id)
                StationCard(
                    station = station,
                    isSelected = isSelected,
                    isPlaying = isSelected && isPlaying,
                    isLoading = isSelected && isLoading,
                    isUnreachable = isUnreachable,
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

    if (showAttributionDialog) {
        AttributionDialog(onDismiss = { showAttributionDialog = false })
    }
}

@Composable
fun StationCard(
    station: RadioStation,
    isSelected: Boolean,
    isPlaying: Boolean,
    isLoading: Boolean = false,
    isUnreachable: Boolean = false,
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
                            .background(DarkBackground.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = NeonCyan,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = NeonCyan
                            )
                        }
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
                if (isSelected && isLoading) {
                    Text(
                        text = stringResource(R.string.buffering_stream),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                } else if (isUnreachable) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
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
