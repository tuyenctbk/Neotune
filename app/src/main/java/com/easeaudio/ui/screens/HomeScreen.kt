package com.easeaudio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.easeaudio.R
import com.easeaudio.ads.AdMobBanner
import com.easeaudio.data.RadioStation
import com.easeaudio.ui.theme.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import com.easeaudio.viewmodel.HomeUiState
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle

enum class HomeTab {
    Radio, Podcast
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    windowSizeClass: WindowSizeClass,
    onPlayPause: () -> Unit,
    onNextStation: () -> Unit,
    onPreviousStation: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSaveSearchQuery: (String) -> Unit = {},
    onDeleteSearchQuery: (String) -> Unit = {},
    onClearSearchHistory: () -> Unit = {},
    onGenreSelect: (String) -> Unit,
    onCountrySelect: (String) -> Unit,
    onStationSelect: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    onBlockStation: (RadioStation) -> Unit = {},
    onDemoteStation: (RadioStation) -> Unit = {},
    onUndemoteStation: (RadioStation) -> Unit = {},
    onOpenAddStation: () -> Unit,
    onLoadMore: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onRetryDiscovery: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val isMedium = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium
    val columns = when {
        isExpanded -> 4 // Optimize for TV and Large Tablets
        isMedium -> 2
        else -> 1
    }

    val gridState = rememberLazyGridState()
    var showCountryDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val latestRecentList = if (uiState.selectedTab == HomeTab.Radio) uiState.recentRadioStations else uiState.recentPodcastStations
    var displayedRecentList by remember { mutableStateOf<List<RadioStation>>(emptyList()) }
    val currentRecentList by rememberUpdatedState(latestRecentList)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, uiState.selectedTab) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (currentRecentList.isNotEmpty()) {
                    displayedRecentList = currentRecentList
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        if (latestRecentList.isNotEmpty()) {
            displayedRecentList = latestRecentList
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(latestRecentList) {
        if (displayedRecentList.isEmpty() && latestRecentList.isNotEmpty()) {
            displayedRecentList = latestRecentList
        }
    }
    
    val isFabVisible by remember(uiState.searchQuery, uiState.stations) {
        derivedStateOf {
            uiState.searchQuery.isNotBlank() && uiState.stations.isEmpty() && !uiState.isDiscoveringOnline
        }
    }

    val shouldLoadMore by remember(gridState) {
        derivedStateOf {
            val totalItemsCount = gridState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore, uiState.canLoadMore, uiState.isLoadingMore) {
        if (shouldLoadMore && uiState.canLoadMore && !uiState.isLoadingMore) {
            onLoadMore()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            AnimatedVisibility(
                visible = isFabVisible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.imePadding()
            ) {
                var isFabFocused by remember { mutableStateOf(false) }
                FloatingActionButton(
                    onClick = onOpenAddStation,
                    containerColor = if (isFabFocused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.background,
                    shape = CircleShape,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .onFocusChanged { isFabFocused = it.isFocused }
                        .border(
                            width = if (isFabFocused) 3.dp else 0.dp,
                            color = if (isFabFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .testTag("fab_add_station")
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Custom Station")
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isDiscoveringOnline,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 80.dp)
            ) {
                // AdMob Banner
                if (uiState.remoteConfig.adsEnabled) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        AdMobBanner(
                            adUnitId = uiState.remoteConfig.bannerAdUnitId,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // App Header
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_favicon),
                                    contentDescription = "NeoTune Logo",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(if (isExpanded) 48.dp else 36.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = (if (isExpanded) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium).copy(
                                        fontSize = if (isExpanded) 36.sp else 28.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.8).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (uiState.selectedTab == HomeTab.Radio) {
                                val currentCountryObj = uiState.availableCountries.find { it.name == uiState.selectedCountry }
                                val isGlobal = uiState.selectedCountry == "Global" || uiState.selectedCountry == "All" || currentCountryObj?.code?.isEmpty() == true
                                val flag = currentCountryObj?.flag ?: "🌐"

                                IconButton(
                                    onClick = { showCountryDialog = true },
                                    modifier = Modifier.testTag("btn_header_country_picker")
                                ) {
                                    if (isGlobal) {
                                        Icon(
                                            imageVector = Icons.Filled.Language,
                                            contentDescription = "Global",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Text(text = flag, fontSize = 22.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Search Bar & Suggestions
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text(stringResource(R.string.search_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                            leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
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
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                    if (uiState.searchQuery.isNotBlank()) {
                                        onSaveSearchQuery(uiState.searchQuery)
                                    }
                                }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_search_stations")
                        )

                        // Search Suggestions from Room Database
                        val suggestions = remember(uiState.recentSearchQueries, uiState.searchQuery) {
                            val trimmed = uiState.searchQuery.trim()
                            if (trimmed.isEmpty()) {
                                uiState.recentSearchQueries
                            } else {
                                uiState.recentSearchQueries.filter {
                                    it.contains(trimmed, ignoreCase = true)
                                }
                            }
                        }

                        if (suggestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.recent_searches),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.clear_history),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable { onClearSearchHistory() }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(suggestions) { query ->
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                        modifier = Modifier.clickable {
                                            onSearchQueryChange(query)
                                            onSaveSearchQuery(query)
                                            keyboardController?.hide()
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.History,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = query,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Remove search suggestion",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { onDeleteSearchQuery(query) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Genre Filter Pills
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val activeGenreList = if (uiState.selectedTab == HomeTab.Podcast) uiState.availablePodcastTopics else uiState.availableGenres
                    val haptic = LocalHapticFeedback.current
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentPadding = PaddingValues(0.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeGenreList) { genre ->
                            val isSelected = genre.key == uiState.selectedGenre
                            var isPillFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .onFocusChanged { isPillFocused = it.isFocused }
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary 
                                        else if (isPillFocused) MaterialTheme.colorScheme.surfaceVariant 
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = if (isPillFocused) 2.dp else if (isSelected) 0.dp else 1.dp,
                                        color = if (isPillFocused) MaterialTheme.colorScheme.primary else if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onGenreSelect(genre.key)
                                    }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                                    .testTag("genre_chip_${genre.key}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(genre.labelResId),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected || isPillFocused) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.background else if (isPillFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // Recent Streams Section (Tab-filtered, Top 5)
                val activeRecentList = displayedRecentList
                if (activeRecentList.isNotEmpty() && uiState.searchQuery.isEmpty() && uiState.selectedGenre == "All") {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(R.string.recent_streams),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(0.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(activeRecentList.take(10)) { station ->
                                RecentStationCard(
                                    station = station,
                                    isPlaying = uiState.currentStation?.id == station.id && uiState.isPlaying,
                                    isDemoted = uiState.demotedStationIds.contains(station.id),
                                    onClick = { onStationSelect(station) },
                                    onToggleFavorite = { onToggleFavorite(station) },
                                    onBlockStation = { onBlockStation(station) },
                                    onDemoteStation = { onDemoteStation(station) },
                                    onUndemoteStation = { onUndemoteStation(station) }
                                )
                            }
                        }
                    }
                }

                // Featured Station Hero Banner
                if (uiState.stations.isNotEmpty() && uiState.searchQuery.isEmpty() && uiState.selectedGenre == "All") {
                    val featured = uiState.stations.first()
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        var isHeroFocused by remember { mutableStateOf(false) }
                        var showHeroMenu by remember { mutableStateOf(false) }
                        val isFeaturedDemoted = uiState.demotedStationIds.contains(featured.id)
                        val heroScale by animateFloatAsState(
                            targetValue = if (isHeroFocused) 1.03f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                            label = "hero_focus_scale"
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { isHeroFocused = it.isFocused }
                                    .scale(heroScale)
                                    .shadow(
                                        elevation = if (isHeroFocused) 16.dp else 0.dp,
                                        shape = RoundedCornerShape(20.dp),
                                        spotColor = MaterialTheme.colorScheme.primary,
                                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                    .clip(RoundedCornerShape(20.dp))
                                    .combinedClickable(
                                        onClick = { onStationSelect(featured) },
                                        onLongClick = { showHeroMenu = true }
                                    )
                                    .border(
                                        width = if (isHeroFocused) 3.5.dp else 0.dp,
                                        brush = if (isHeroFocused) {
                                            Brush.horizontalGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    Color.White,
                                                    MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        } else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .testTag("hero_featured_card"),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().height(if (isExpanded) 220.dp else 160.dp)) {
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
                                                    listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                                                )
                                            )
                                    )
                                    // Content
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(if (isExpanded) 24.dp else 16.dp),
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stringResource(R.string.featured_station),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = featured.name,
                                                style = if (isExpanded) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${featured.genre} • ${featured.bitrate}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(if (isExpanded) 64.dp else 48.dp)
                                                .clip(CircleShape)
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val isFeaturedSelected = uiState.currentStation?.id == featured.id
                                            if (isFeaturedSelected && uiState.isLoading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(if (isExpanded) 32.dp else 24.dp),
                                                    color = MaterialTheme.colorScheme.background,
                                                    strokeWidth = 2.5.dp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = if (isFeaturedSelected && uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                    contentDescription = "Play Featured",
                                                    tint = MaterialTheme.colorScheme.background,
                                                    modifier = Modifier.size(if (isExpanded) 32.dp else 24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            DropdownMenu(
                                expanded = showHeroMenu,
                                onDismissRequest = { showHeroMenu = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (featured.isFavorite) "Remove from Favorites" else "Add to Favorites", color = MaterialTheme.colorScheme.onSurface) },
                                    leadingIcon = { 
                                        Icon(
                                            imageVector = if (featured.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, 
                                            contentDescription = null, 
                                            tint = if (featured.isFavorite) FavoriteHeartColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        ) 
                                    },
                                    onClick = {
                                        showHeroMenu = false
                                        onToggleFavorite(featured)
                                    }
                                )
                                
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            if (isFeaturedDemoted) "Move to Top (Show More)" else "Move to Bottom (Show Less)", 
                                            color = MaterialTheme.colorScheme.onSurface
                                        ) 
                                    },
                                    leadingIcon = { 
                                        Icon(
                                            imageVector = if (isFeaturedDemoted) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward, 
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.primary
                                        ) 
                                    },
                                    onClick = {
                                        showHeroMenu = false
                                        if (isFeaturedDemoted) onUndemoteStation(featured) else onDemoteStation(featured)
                                    }
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.block_this_station), color = Color(0xFFEF5350)) },
                                    leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null, tint = Color(0xFFEF5350)) },
                                    onClick = {
                                        showHeroMenu = false
                                        onBlockStation(featured)
                                    }
                                )
                            }
                        }
                    }
                }

                // Section Title
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = if (uiState.selectedTab == HomeTab.Radio) stringResource(R.string.live_radio_stations) else stringResource(R.string.podcasts_and_shows),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }

                // Empty or Initial Loading State
                if (uiState.stations.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isDiscoveringOnline) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = stringResource(R.string.loading_more_stations),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else if (uiState.isDiscoveryError) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = stringResource(R.string.network_error_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = onRetryDiscovery,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(stringResource(R.string.retry_discovery), color = MaterialTheme.colorScheme.background)
                                    }
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(stringResource(R.string.no_stations_found), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(stringResource(R.string.add_station_prompt), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }

                // Station Cards List
                items(uiState.stations, key = { it.id }) { station ->
                    val isSelected = uiState.currentStation?.id == station.id
                    val isUnreachable = uiState.failedStationIds.contains(station.id)
                    StationCard(
                        station = station,
                        isSelected = isSelected,
                        isPlaying = isSelected && uiState.isPlaying,
                        isDemoted = uiState.demotedStationIds.contains(station.id),
                        isLoading = isSelected && uiState.isLoading,
                        isUnreachable = isUnreachable,
                        onSelect = { onStationSelect(station) },
                        onToggleFavorite = { onToggleFavorite(station) },
                        onBlockStation = { onBlockStation(station) },
                        onDemoteStation = { onDemoteStation(station) },
                        onUndemoteStation = { onUndemoteStation(station) }
                    )
                }

                if (uiState.isLoadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.loading_more_stations),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCountryDialog) {
        com.easeaudio.ui.components.CountrySelectionDialog(
            selectedCountry = uiState.selectedCountry,
            countries = uiState.availableCountries,
            onSelectCountry = { 
                onCountrySelect(it)
                showCountryDialog = false 
            },
            onDismiss = { showCountryDialog = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentStationCard(
    station: RadioStation,
    isPlaying: Boolean,
    isDemoted: Boolean = false,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit = {},
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
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "recent_card_focus_scale"
    )
    
    Box {
        Column(
            modifier = Modifier
                .width(112.dp)
                .onFocusChanged { isFocused = it.isFocused }
                .scale(focusScale)
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .shadow(
                        elevation = if (isFocused) 14.dp else 0.dp,
                        shape = RoundedCornerShape(18.dp),
                        spotColor = activeAccent,
                        ambientColor = activeAccent.copy(alpha = 0.5f)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = if (isFocused) 3.5.dp else 0.dp,
                        brush = if (isFocused) {
                            Brush.horizontalGradient(
                                listOf(
                                    activeAccent,
                                    Color.White,
                                    activeAccent
                                )
                            )
                        } else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
                        shape = RoundedCornerShape(18.dp)
                    )
            ) {
                AsyncImage(
                    model = station.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = null,
                            tint = activeAccent,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = station.name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isFocused) FontWeight.Black else FontWeight.Medium
                ),
                color = if (isFocused) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            DropdownMenuItem(
                text = { Text(if (station.isFavorite) "Remove from Favorites" else "Add to Favorites", color = MaterialTheme.colorScheme.onSurface) },
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StationCard(
    station: RadioStation,
    isSelected: Boolean,
    isPlaying: Boolean,
    isDemoted: Boolean = false,
    isLoading: Boolean = false,
    isUnreachable: Boolean = false,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
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
                    // Station Image with sleek border when focused
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

                    // Text Info
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
                                val secondary = station.country.ifBlank { station.bitrate }
                                if (secondary.isNotBlank()) {
                                    Text(" • ", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = secondary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isFocused) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                            }
                        }
                    }

                    // Action Button on right: On TV Focus, show a vibrant PLAY / TUNE IN pill!
                    if (isFocused) {
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
