package com.easeaudio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.*
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImageContent
import androidx.compose.ui.res.stringResource
import com.easeaudio.R
import com.easeaudio.data.RadioStation
import com.easeaudio.ui.components.StationCard
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

enum class ContinueListeningTab {
    Recent, MostPlayed, Featured
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
    onToggleListenLater: (RadioStation) -> Unit = {},
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
    val columns = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> 2 // Landscape tablet / Android TV
        else -> 1                           // Phone or portrait tablet: single-column list
    }
    val isTv = rememberIsTv()

    val gridState = rememberLazyGridState()
    var showCountryDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var continueListeningTab by rememberSaveable { mutableStateOf(ContinueListeningTab.Recent) }
    var userExplicitlySelectedTab by rememberSaveable { mutableStateOf(false) }
    val continueChipFocusRequester = remember { FocusRequester() }

    val featuredList = remember(uiState.selectedTab, uiState.curatedAudiophileStations, uiState.stations) {
        if (uiState.selectedTab == HomeTab.Radio) {
            uiState.curatedAudiophileStations.ifEmpty {
                uiState.stations.filter { !it.isPodcast }
            }
        } else {
            uiState.stations.filter { it.isPodcast }.ifEmpty { uiState.stations }
        }
    }

    val latestRecentList = if (uiState.selectedTab == HomeTab.Radio) uiState.recentRadioStations else uiState.recentPodcastStations
    var displayedRecentList by remember { mutableStateOf<List<RadioStation>>(emptyList()) }
    val currentRecentList by rememberUpdatedState(latestRecentList)

    val latestMostPlayedList = if (uiState.selectedTab == HomeTab.Radio) uiState.mostPlayedRadioStations else uiState.mostPlayedPodcastStations
    var displayedMostPlayedList by remember { mutableStateOf<List<RadioStation>>(emptyList()) }
    val currentMostPlayedList by rememberUpdatedState(latestMostPlayedList)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, uiState.selectedTab) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (currentRecentList.isNotEmpty()) {
                    displayedRecentList = currentRecentList
                }
                if (currentMostPlayedList.isNotEmpty()) {
                    displayedMostPlayedList = currentMostPlayedList
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        if (latestRecentList.isNotEmpty()) {
            displayedRecentList = latestRecentList
        }
        if (latestMostPlayedList.isNotEmpty()) {
            displayedMostPlayedList = latestMostPlayedList
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

    LaunchedEffect(latestMostPlayedList) {
        if (displayedMostPlayedList.isEmpty() && latestMostPlayedList.isNotEmpty()) {
            displayedMostPlayedList = latestMostPlayedList
        }
    }

    LaunchedEffect(latestRecentList, featuredList) {
        if (!userExplicitlySelectedTab) {
            if (latestRecentList.isNotEmpty()) {
                continueListeningTab = ContinueListeningTab.Recent
            } else if (featuredList.isNotEmpty() && continueListeningTab == ContinueListeningTab.Recent && latestMostPlayedList.isEmpty()) {
                continueListeningTab = ContinueListeningTab.Featured
            }
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
        containerColor = Color.Transparent,
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
                    Icon(imageVector = Icons.Filled.Add, contentDescription = stringResource(R.string.add_custom_station))
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
                columns = GridCells.Fixed(columns),
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 108.dp)
            ) {
                // ── Unified Top Bar (Mobile, Tablet, Android TV) ──────────────────
                // Single row: Brand Favicon (always visible) | Animated expanding search pill | Country Picker (hides when searching)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        var isSearchFocused by rememberSaveable { mutableStateOf(false) }
                        val isSearchExpanded = isSearchFocused || uiState.searchQuery.isNotEmpty()
                        val searchFocusRequester = remember { FocusRequester() }

                        LaunchedEffect(isSearchFocused) {
                            if (isSearchFocused) {
                                delay(60)
                                try { searchFocusRequester.requestFocus() } catch (_: Exception) {}
                                keyboardController?.show()
                            }
                        }

                        BackHandler(enabled = isSearchExpanded) {
                            isSearchFocused = false
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            if (uiState.searchQuery.isNotEmpty()) onSearchQueryChange("")
                        }

                        // Single top-bar row: Favicon (always) | Search (expands) | Country Picker (hides when searching)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ── Brand Favicon (always visible) ──
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_favicon),
                                contentDescription = stringResource(R.string.app_icon_desc),
                                tint = Color.Unspecified,
                                modifier = Modifier
                                    .size(if (isTv) 48.dp else 44.dp)
                                    .padding(horizontal = 2.dp)
                            )

                            // ── Search Area (expands to full remaining width when active) ──
                            AnimatedContent(
                                targetState = isSearchExpanded,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(200)) + expandHorizontally(expandFrom = androidx.compose.ui.Alignment.Start)) togetherWith
                                    (fadeOut(animationSpec = tween(160)) + shrinkHorizontally(shrinkTowards = androidx.compose.ui.Alignment.Start))
                                },
                                modifier = Modifier.weight(1f),
                                label = "TopBarSearchTransition"
                            ) { expanded ->
                                if (expanded) {
                                    // Full search text field (overlaps country picker)
                                    OutlinedTextField(
                                        value = uiState.searchQuery,
                                        onValueChange = onSearchQueryChange,
                                        placeholder = {
                                            Text(
                                                text = stringResource(R.string.search_placeholder),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        leadingIcon = {
                                            IconButton(
                                                onClick = {
                                                    isSearchFocused = false
                                                    focusManager.clearFocus()
                                                    keyboardController?.hide()
                                                    if (uiState.searchQuery.isNotEmpty()) onSearchQueryChange("")
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = stringResource(R.string.search_collapse_hint),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            if (uiState.searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { onSearchQueryChange("") }) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Close,
                                                        contentDescription = stringResource(R.string.clear_search),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(
                                            onSearch = {
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                                if (uiState.searchQuery.isNotBlank()) onSaveSearchQuery(uiState.searchQuery)
                                            }
                                        ),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 46.dp)
                                            .focusRequester(searchFocusRequester)
                                            .onFocusChanged { if (!it.isFocused && !isSearchExpanded) isSearchFocused = false }
                                            .testTag("input_search_stations")
                                    )
                                } else {
                                    // Collapsed search pill
                                    var isPillFocused by remember { mutableStateOf(false) }
                                    Surface(
                                        onClick = { isSearchFocused = true },
                                        shape = RoundedCornerShape(24.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        border = BorderStroke(
                                            width = if (isPillFocused) 2.dp else 1.dp,
                                            color = if (isPillFocused) MaterialTheme.colorScheme.primary else Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (isTv) 50.dp else 46.dp)
                                            .onFocusChanged { isPillFocused = it.isFocused }
                                            .testTag("input_search_stations")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Search,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = if (uiState.searchQuery.isNotEmpty()) uiState.searchQuery
                                                       else stringResource(R.string.search_placeholder),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (uiState.searchQuery.isNotEmpty()) MaterialTheme.colorScheme.onSurface
                                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            // ── Country Picker (hidden when search is active) ──
                            AnimatedVisibility(
                                visible = !isSearchExpanded && uiState.selectedTab == HomeTab.Radio,
                                enter = fadeIn(tween(180)) + expandHorizontally(),
                                exit = fadeOut(tween(140)) + shrinkHorizontally()
                            ) {
                                val currentCountryObj = uiState.availableCountries.find { it.name == uiState.selectedCountry }
                                val isGlobal = uiState.selectedCountry == "Global" || uiState.selectedCountry == "All" || currentCountryObj?.code?.isEmpty() == true
                                val flag = currentCountryObj?.flag ?: "🌐"
                                var isFlagFocused by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = { showCountryDialog = true },
                                    modifier = Modifier
                                        .onFocusChanged { isFlagFocused = it.isFocused }
                                        .border(
                                            width = if (isFlagFocused) 2.dp else 0.dp,
                                            color = if (isFlagFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .testTag("btn_header_country_picker")
                                ) {
                                    if (isGlobal) {
                                        Icon(
                                            imageVector = Icons.Filled.Language,
                                            contentDescription = stringResource(R.string.global_country),
                                            tint = if (isFlagFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(if (isTv) 28.dp else 24.dp)
                                        )
                                    } else {
                                        Text(text = flag, fontSize = if (isTv) 26.sp else 22.sp)
                                    }
                                }
                            }
                        }

                        // Search Suggestions (shown when search is active)
                        val suggestions = remember(uiState.recentSearchQueries, uiState.searchQuery) {
                            val trimmed = uiState.searchQuery.trim()
                            if (trimmed.isEmpty()) uiState.recentSearchQueries
                            else uiState.recentSearchQueries.filter { it.contains(trimmed, ignoreCase = true) }
                        }

                        if (isSearchExpanded && suggestions.isNotEmpty()) {
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
                                                contentDescription = stringResource(R.string.remove_search_suggestion),
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
                                        width = if (isPillFocused) 2.5.dp else if (isSelected) 0.dp else 1.dp,
                                        color = if (isPillFocused) {
                                            if (isSelected) Color.White else MaterialTheme.colorScheme.primary
                                        } else if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline,
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

                // ── Continue & Discover Section (Recent, Most Played & Featured) ──
                // Unified section with interactive toggle chips.
                val hasContinueOrFeatured = displayedRecentList.isNotEmpty() || displayedMostPlayedList.isNotEmpty() || featuredList.isNotEmpty()
                if (hasContinueOrFeatured && uiState.searchQuery.isEmpty() && uiState.selectedGenre == "All") {
                    val activeStreamList = when (continueListeningTab) {
                        ContinueListeningTab.Recent -> displayedRecentList
                        ContinueListeningTab.MostPlayed -> displayedMostPlayedList
                        ContinueListeningTab.Featured -> featuredList
                    }

                    // Header row: Section title + Recent / Most Played / Featured toggle chips
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        if (isExpanded) {
                            // Large screen (TV / Tablet): Title on left, filter chips on right
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.continue_listening),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.3).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ContinueTabChip(
                                        label = stringResource(R.string.tab_recent),
                                        icon = Icons.Outlined.History,
                                        isSelected = continueListeningTab == ContinueListeningTab.Recent,
                                        modifier = if (continueListeningTab == ContinueListeningTab.Recent) Modifier.focusRequester(continueChipFocusRequester) else Modifier,
                                        onClick = {
                                            userExplicitlySelectedTab = true
                                            continueListeningTab = ContinueListeningTab.Recent
                                        }
                                    )
                                    ContinueTabChip(
                                        label = stringResource(R.string.tab_most_played),
                                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                                        isSelected = continueListeningTab == ContinueListeningTab.MostPlayed,
                                        modifier = if (continueListeningTab == ContinueListeningTab.MostPlayed) Modifier.focusRequester(continueChipFocusRequester) else Modifier,
                                        onClick = {
                                            userExplicitlySelectedTab = true
                                            continueListeningTab = ContinueListeningTab.MostPlayed
                                        }
                                    )
                                    ContinueTabChip(
                                        label = stringResource(R.string.tab_featured),
                                        icon = Icons.Filled.Star,
                                        isSelected = continueListeningTab == ContinueListeningTab.Featured,
                                        modifier = if (continueListeningTab == ContinueListeningTab.Featured) Modifier.focusRequester(continueChipFocusRequester) else Modifier,
                                        onClick = {
                                            userExplicitlySelectedTab = true
                                            continueListeningTab = ContinueListeningTab.Featured
                                        }
                                    )
                                }
                            }
                        } else {
                            // Compact / Mobile phone layout: 2 clean tiers with plenty of breathing room
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.continue_listening),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.3).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ContinueTabChip(
                                        label = stringResource(R.string.tab_recent),
                                        icon = Icons.Outlined.History,
                                        isSelected = continueListeningTab == ContinueListeningTab.Recent,
                                        modifier = if (continueListeningTab == ContinueListeningTab.Recent) Modifier.focusRequester(continueChipFocusRequester) else Modifier,
                                        onClick = {
                                            userExplicitlySelectedTab = true
                                            continueListeningTab = ContinueListeningTab.Recent
                                        }
                                    )
                                    ContinueTabChip(
                                        label = stringResource(R.string.tab_most_played),
                                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                                        isSelected = continueListeningTab == ContinueListeningTab.MostPlayed,
                                        modifier = if (continueListeningTab == ContinueListeningTab.MostPlayed) Modifier.focusRequester(continueChipFocusRequester) else Modifier,
                                        onClick = {
                                            userExplicitlySelectedTab = true
                                            continueListeningTab = ContinueListeningTab.MostPlayed
                                        }
                                    )
                                    ContinueTabChip(
                                        label = stringResource(R.string.tab_featured),
                                        icon = Icons.Filled.Star,
                                        isSelected = continueListeningTab == ContinueListeningTab.Featured,
                                        modifier = if (continueListeningTab == ContinueListeningTab.Featured) Modifier.focusRequester(continueChipFocusRequester) else Modifier,
                                        onClick = {
                                            userExplicitlySelectedTab = true
                                            continueListeningTab = ContinueListeningTab.Featured
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (activeStreamList.isNotEmpty()) {
                        val resumeStation = activeStreamList.first()
                        val otherStations = activeStreamList.drop(1).take(10)

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            val badgeText = when (continueListeningTab) {
                                ContinueListeningTab.Recent -> stringResource(if (uiState.selectedTab == HomeTab.Podcast) R.string.continue_listening_podcast else R.string.continue_listening_radio)
                                ContinueListeningTab.MostPlayed -> {
                                    if (resumeStation.playCount > 0) {
                                        "${stringResource(R.string.tab_most_played)} • ${resumeStation.playCount}"
                                    } else {
                                        stringResource(R.string.tab_most_played)
                                    }
                                }
                                ContinueListeningTab.Featured -> stringResource(if (uiState.selectedTab == HomeTab.Podcast) R.string.featured_podcast else R.string.featured_station)
                            }

                            val badgeIcon = when (continueListeningTab) {
                                ContinueListeningTab.Recent -> Icons.Filled.PlayArrow
                                ContinueListeningTab.MostPlayed -> Icons.AutoMirrored.Filled.TrendingUp
                                ContinueListeningTab.Featured -> Icons.Filled.Star
                            }

                            QuickResumeCard(
                                station = resumeStation,
                                isPodcast = uiState.selectedTab == HomeTab.Podcast,
                                badgeText = badgeText,
                                badgeIcon = badgeIcon,
                                isExpanded = isExpanded,
                                isPlaying = uiState.currentStation?.id == resumeStation.id && uiState.isPlaying,
                                isLoading = uiState.currentStation?.id == resumeStation.id && uiState.isLoading,
                                isDemoted = uiState.demotedStationIds.contains(resumeStation.id),
                                modifier = Modifier.focusProperties {
                                    up = continueChipFocusRequester
                                },
                                onClick = { onStationSelect(resumeStation) },
                                onToggleFavorite = { onToggleFavorite(resumeStation) },
                                onDemoteStation = { onDemoteStation(resumeStation) },
                                onUndemoteStation = { onUndemoteStation(resumeStation) },
                                onBlockStation = { onBlockStation(resumeStation) }
                            )
                        }

                        if (otherStations.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(0.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(otherStations) { station ->
                                        RecentStationCard(
                                            station = station,
                                            isPlaying = uiState.currentStation?.id == station.id && uiState.isPlaying,
                                            isDemoted = uiState.demotedStationIds.contains(station.id),
                                            isListenLater = uiState.listenLaterItems.any { it.id == station.id },
                                            onClick = { onStationSelect(station) },
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
                    } else {
                        // Empty state for the selected tab
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = when (continueListeningTab) {
                                            ContinueListeningTab.Recent -> Icons.Outlined.History
                                            ContinueListeningTab.MostPlayed -> Icons.AutoMirrored.Filled.TrendingUp
                                            ContinueListeningTab.Featured -> Icons.Filled.Star
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = stringResource(
                                            when (continueListeningTab) {
                                                ContinueListeningTab.Recent -> R.string.no_history_yet
                                                ContinueListeningTab.MostPlayed -> R.string.no_most_played_yet
                                                ContinueListeningTab.Featured -> R.string.no_featured_yet
                                            }
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
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
                                    Spacer(modifier = Modifier.height(6.6.dp))
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
                    val isSavedToLater = uiState.listenLaterItems.any { it.id == station.id }
                    StationCard(
                        station = station,
                        isSelected = isSelected,
                        isPlaying = isSelected && uiState.isPlaying,
                        isDemoted = uiState.demotedStationIds.contains(station.id),
                        isLoading = isSelected && uiState.isLoading,
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
fun CuratedStationCard(
    station: RadioStation,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val activeAccent = MaterialTheme.colorScheme.primary
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "curated_card_focus_scale"
    )

    Box {
        Column(
            modifier = Modifier
                .width(140.dp)
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyUp) {
                        when (keyEvent.key) {
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onClick()
                                true
                            }
                            Key.Menu -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showMenu = true
                                true
                            }
                            else -> false
                        }
                    } else false
                }
                .scale(focusScale)
                .clip(RoundedCornerShape(18.dp))
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
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp, 100.dp)
                    .shadow(
                        elevation = if (isFocused) 14.dp else 0.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = activeAccent,
                        ambientColor = activeAccent.copy(alpha = 0.5f)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = if (isFocused) 3.dp else if (isPlaying) 1.5.dp else 1.dp,
                        brush = if (isFocused) {
                            Brush.horizontalGradient(
                                listOf(
                                    activeAccent,
                                    Color.White,
                                    activeAccent
                                )
                            )
                        } else if (isPlaying) {
                            Brush.horizontalGradient(
                                listOf(
                                    activeAccent.copy(alpha = 0.8f),
                                    activeAccent.copy(alpha = 0.4f)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.White.copy(alpha = 0.08f)
                                )
                            )
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                com.easeaudio.ui.components.StationArtLoader(
                    imageUrl = station.imageUrl,
                    stationName = station.name,
                    genre = station.genre,
                    isPodcast = station.isPodcast,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.65f)
                                )
                            )
                        )
                )
                // Badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = if (station.id.startsWith("curated_rp_")) stringResource(R.string.badge_flac_master) else stringResource(R.string.badge_somafm),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (station.id.startsWith("curated_rp_")) MaterialTheme.colorScheme.primary else Color(0xFFFFB74D),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(activeAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = station.name.replace("SomaFM: ", ""),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = station.genre,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = if (station.isFavorite) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites),
                        color = if (station.isFavorite) FavoriteHeartColor else MaterialTheme.colorScheme.onSurface
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (station.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = if (station.isFavorite) FavoriteHeartColor else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    onToggleFavorite()
                    showMenu = false
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickResumeCard(
    station: RadioStation,
    isPodcast: Boolean = false,
    badgeText: String? = null,
    badgeIcon: ImageVector = Icons.Filled.PlayArrow,
    isExpanded: Boolean,
    isPlaying: Boolean,
    isLoading: Boolean,
    isDemoted: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onDemoteStation: () -> Unit = {},
    onUndemoteStation: () -> Unit = {},
    onBlockStation: () -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1.0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "resume_card_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyUp) {
                        when (keyEvent.key) {
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onClick()
                                true
                            }
                            Key.Menu -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showMenu = true
                                true
                            }
                            else -> false
                        }
                    } else false
                }
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
                )
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isFocused) 0.7f else 0.55f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isFocused) 0.45f else 0.28f)
                        )
                    )
                )
                .border(
                    width = if (isFocused) 2.5.dp else 1.dp,
                    color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(16.dp)
                )
                .testTag("hero_featured_card"),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isExpanded) 96.dp else 84.dp)
                    .padding(if (isExpanded) 12.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Station Art Thumbnail
                val artSize = if (isExpanded) 72.dp else 64.dp
                Box(
                    modifier = Modifier
                        .size(artSize)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    com.easeaudio.ui.components.StationArtLoader(
                        imageUrl = station.imageUrl,
                        stationName = station.name,
                        genre = station.genre,
                        isPodcast = station.isPodcast,
                        isPlaying = isPlaying,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Station Info Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        ) {
                            Icon(
                                imageVector = badgeIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = (badgeText ?: stringResource(if (isPodcast) R.string.continue_listening_podcast else R.string.continue_listening_radio)).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.6.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = station.name,
                        style = if (isExpanded) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold) 
                                else MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${station.genre} • ${if (station.bitrate.isNotBlank()) station.bitrate else stringResource(R.string.live_badge)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Action Button (Play / Pause / Loading)
                Box(
                    modifier = Modifier
                        .size(if (isExpanded) 50.dp else 42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(if (isExpanded) 24.dp else 20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(if (isExpanded) 26.dp else 22.dp)
                        )
                    }
                }
            }
        }

        // Context / Options Dropdown Menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = if (station.isFavorite) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
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
                        text = if (isDemoted) stringResource(R.string.move_to_top) else stringResource(R.string.move_to_bottom),
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

@Composable
fun ContinueTabChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // Unmistakably distinguish Selected (persistent selection) vs Focused (remote cursor)
    val chipBackground = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isFocused -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    val chipBorderColor = when {
        isSelected && isFocused -> Color.White
        isFocused -> MaterialTheme.colorScheme.primary
        isSelected -> Color.Transparent
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    }

    val chipBorderWidth = when {
        isSelected && isFocused -> 2.5.dp
        isFocused -> 2.dp
        isSelected -> 0.dp
        else -> 1.dp
    }

    val chipTextColor = when {
        isSelected -> MaterialTheme.colorScheme.background // High-contrast black on cyan
        isFocused -> MaterialTheme.colorScheme.primary     // Cyan text on dark surface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
    }

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        shape = RoundedCornerShape(20.dp),
        color = chipBackground,
        border = if (chipBorderWidth > 0.dp) BorderStroke(chipBorderWidth, chipBorderColor) else null,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .padding(vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = chipTextColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium
                ),
                color = chipTextColor
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentStationCard(
    station: RadioStation,
    isPlaying: Boolean,
    isDemoted: Boolean = false,
    isListenLater: Boolean = false,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit = {},
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
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "recent_card_focus_scale"
    )
    
    Box {
        Column(
            modifier = Modifier
                .width(100.dp)
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyUp) {
                        when (keyEvent.key) {
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onClick()
                                true
                            }
                            Key.Menu -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showMenu = true
                                true
                            }
                            else -> false
                        }
                    } else false
                }
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
                    .size(80.dp)
                    .shadow(
                        elevation = if (isFocused) 14.dp else 0.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = activeAccent,
                        ambientColor = activeAccent.copy(alpha = 0.5f)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = if (isFocused) 3.dp else 0.5.dp,
                        brush = if (isFocused) {
                            Brush.horizontalGradient(
                                listOf(
                                    activeAccent,
                                    Color.White,
                                    activeAccent
                                )
                            )
                        } else Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                com.easeaudio.ui.components.StationArtLoader(
                    imageUrl = station.imageUrl,
                    stationName = station.name,
                    genre = station.genre,
                    isPodcast = station.isPodcast,
                    isPlaying = isPlaying,
                    shape = RoundedCornerShape(16.dp),
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
                        imageVector = if (isListenLater) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, 
                        contentDescription = null, 
                        tint = if (isListenLater) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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

