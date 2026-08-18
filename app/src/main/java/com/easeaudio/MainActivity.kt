package com.easeaudio

import android.os.Bundle
import android.os.Build
import android.util.Log
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.content.Context
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.onPreviewKeyEvent
import kotlinx.coroutines.delay
import com.easeaudio.viewmodel.RadioViewModel
import com.easeaudio.network.QualityLevel
import com.easeaudio.service.FirebaseManager
import com.easeaudio.ui.components.*
import com.easeaudio.ui.screens.FavoritesScreen
import com.easeaudio.ui.screens.HomeScreen
import com.easeaudio.ui.screens.OnboardingScreen
import com.easeaudio.ui.screens.PlayerScreen
import com.easeaudio.ui.screens.ScreensaverScreen
import com.easeaudio.ui.screens.AppearanceSelectionScreen
import com.easeaudio.ui.screens.CarModeScreen
import com.easeaudio.ui.theme.TuneveTheme
import com.easeaudio.ui.theme.AppThemeState
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class MainActivity : ComponentActivity() {

    private val viewModel: RadioViewModel by viewModels()

    private val isPermissionGrantedState = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        Log.d("MainActivity", "Notification permission granted: $isGranted")
        isPermissionGrantedState.value = isGranted
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        AppThemeState.loadTheme(applicationContext)
        enableEdgeToEdge()

        // Safe Firebase Initializer
        FirebaseManager.initialize(applicationContext)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            TuneveTheme {
                MainAppContent(
                    viewModel = viewModel,
                    windowSizeClass = windowSizeClass,
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK -> {
                viewModel.togglePlayPause()
                true
            }
            KeyEvent.KEYCODE_MEDIA_STOP -> {
                viewModel.playerManager.stopPlayer()
                true
            }
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_CHANNEL_UP,
            KeyEvent.KEYCODE_PAGE_UP -> {
                viewModel.playNextStation()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_CHANNEL_DOWN,
            KeyEvent.KEYCODE_PAGE_DOWN -> {
                viewModel.playPreviousStation()
                true
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                viewModel.playerManager.skipForward(30000L)
                true
            }
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                viewModel.playerManager.skipBackward(15000L)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}

@Composable
fun MainAppContent(
    viewModel: RadioViewModel,
    windowSizeClass: WindowSizeClass,
    onRequestNotificationPermission: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("neotune_prefs", Context.MODE_PRIVATE) }
    val isOnboardingCompleted = remember { mutableStateOf(prefs.getBoolean("is_onboarding_completed", false)) }

    val uiState by viewModel.homeUiState.collectAsState()

    val isAutomotive = remember {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? android.app.UiModeManager
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE) ||
            uiModeManager?.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_CAR
    }
    
    val startDestination = when {
        !isOnboardingCompleted.value -> NavRoute.Onboarding.route
        isAutomotive -> NavRoute.CarMode.route
        else -> NavRoute.Home.route
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Update notification permission state
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }
                // Refresh recent streams when app comes to foreground
                viewModel.refreshRecentStations()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var isPlaytimePermissionDismissed by remember { mutableStateOf(false) }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startDestination

    LaunchedEffect(currentRoute) {
        FirebaseManager.logScreenView(currentRoute)
    }

    val completeOnboarding = {
        prefs.edit().putBoolean("is_onboarding_completed", true).apply()
        isOnboardingCompleted.value = true
        navController.navigate(NavRoute.Home.route) {
            popUpTo(NavRoute.Onboarding.route) { inclusive = true }
        }
    }

    val syncedCurrentStation = uiState.currentStation?.let { st ->
        st.copy(isFavorite = uiState.favoriteStations.any { it.id == st.id })
    }

    var isFullPlayerVisible by remember { mutableStateOf(value = false) }

    // Keep screen awake while radio is playing (perfect for background TV listening)
    val activity = context as? android.app.Activity
    DisposableEffect(uiState.isPlaying) {
        if (uiState.isPlaying) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Android TV detection and auto screensaver activation
    val isTv = remember {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? android.app.UiModeManager
        uiModeManager?.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(uiState.isPlaying, currentRoute, lastInteractionTime) {
        if (isTv && uiState.isPlaying && currentRoute != NavRoute.Screensaver.route && currentRoute != NavRoute.Onboarding.route) {
            delay(15000L) // Auto launch screensaver after 15 seconds of inactivity
            isFullPlayerVisible = false
            navController.navigate(NavRoute.Screensaver.route)
        }
    }

    var showTrackActionSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect UI notifications/snackbars (e.g., Favorites, Recently Played)
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
        }
    }

    // Show error toast/snackbar if stream error occurs
    LaunchedEffect(uiState.playbackError, uiState.playbackErrorDetails) {
        uiState.playbackErrorDetails?.let { details ->
            Log.e("MainActivity", "Stream Error Captured: ${details.toUserSummary()} (Code: ${details.errorCodeName}, HTTP: ${details.httpStatusCode ?: "N/A"})")
            FirebaseManager.recordException(Exception("Stream Error: ${details.errorCodeName}"))
        }
        uiState.playbackError?.let { err ->
            snackbarHostState.showSnackbar(err)
        }
    }

    val showBottomBar = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact && currentRoute != NavRoute.Onboarding.route && currentRoute != NavRoute.CarMode.route && currentRoute != NavRoute.Screensaver.route && !isFullPlayerVisible
    val showNavigationRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact && currentRoute != NavRoute.Onboarding.route && currentRoute != NavRoute.CarMode.route && currentRoute != NavRoute.Screensaver.route && !isFullPlayerVisible

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        lastInteractionTime = System.currentTimeMillis()
                    }
                }
            }
            .onPreviewKeyEvent {
                lastInteractionTime = System.currentTimeMillis()
                false
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (showNavigationRail) {
                AppNavigationRail(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route == NavRoute.Radio.route || route == NavRoute.Home.route) {
                            viewModel.setSelectedTab(com.easeaudio.ui.screens.HomeTab.Radio)
                        } else if (route == NavRoute.Podcast.route) {
                            viewModel.setSelectedTab(com.easeaudio.ui.screens.HomeTab.Podcast)
                        }
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            
            Scaffold(
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        )
                    }
                },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (showBottomBar) {
                        BottomNavBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                if (route == NavRoute.Radio.route || route == NavRoute.Home.route) {
                                    viewModel.setSelectedTab(com.easeaudio.ui.screens.HomeTab.Radio)
                                } else if (route == NavRoute.Podcast.route) {
                                    viewModel.setSelectedTab(com.easeaudio.ui.screens.HomeTab.Podcast)
                                }
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination
                        ) {
                        composable(NavRoute.Onboarding.route) {
                            OnboardingScreen(
                                availableGenres = uiState.availableGenres.map { it.key },
                                windowSizeClass = windowSizeClass,
                                onGenreSelected = { genre -> viewModel.setSelectedGenre(genre) },
                                onGenresSelected = { genres -> viewModel.setPreferredGenres(genres) },
                                onCountrySelected = { country -> viewModel.setSelectedCountry(country) },
                                onRequestNotificationPermission = onRequestNotificationPermission,
                                onCompleteOnboarding = completeOnboarding
                            )
                        }

                        composable(NavRoute.Home.route) {
                            HomeScreen(
                                uiState = uiState,
                                windowSizeClass = windowSizeClass,
                                onPlayPause = { viewModel.togglePlayPause() },
                                onNextStation = { viewModel.playNextStation() },
                                onPreviousStation = { viewModel.playPreviousStation() },
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onSaveSearchQuery = { viewModel.saveSearchQuery(it) },
                                onDeleteSearchQuery = { viewModel.deleteSearchQuery(it) },
                                onClearSearchHistory = { viewModel.clearSearchHistory() },
                                onGenreSelect = { viewModel.setSelectedGenre(it) },
                                onCountrySelect = { viewModel.setSelectedCountry(it) },
                                onStationSelect = { station -> 
                                    FirebaseManager.logEvent("play_station", Bundle().apply { putString("station_name", station.name) })
                                    viewModel.playStation(station) 
                                },
                                onToggleFavorite = { station -> 
                                    FirebaseManager.logEvent("toggle_favorite", Bundle().apply { putString("station_name", station.name) })
                                    viewModel.toggleFavorite(station) 
                                },
                                onBlockStation = { station -> viewModel.blockStation(station.id) },
                                onDemoteStation = { station -> viewModel.demoteStation(station.id) },
                                onUndemoteStation = { station -> viewModel.undemoteStation(station.id) },
                                onOpenAddStation = { viewModel.setShowAddStationDialog(true) },
                                onLoadMore = { viewModel.loadMoreStations() },
                                onRefresh = { viewModel.refreshStations() },
                                onRetryDiscovery = { viewModel.retryDiscovery() }
                            )
                        }

                        composable(NavRoute.Radio.route) {
                            HomeScreen(
                                uiState = uiState,
                                windowSizeClass = windowSizeClass,
                                onPlayPause = { viewModel.togglePlayPause() },
                                onNextStation = { viewModel.playNextStation() },
                                onPreviousStation = { viewModel.playPreviousStation() },
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onSaveSearchQuery = { viewModel.saveSearchQuery(it) },
                                onDeleteSearchQuery = { viewModel.deleteSearchQuery(it) },
                                onClearSearchHistory = { viewModel.clearSearchHistory() },
                                onGenreSelect = { viewModel.setSelectedGenre(it) },
                                onCountrySelect = { viewModel.setSelectedCountry(it) },
                                onStationSelect = { station -> 
                                    FirebaseManager.logEvent("play_station", Bundle().apply { putString("station_name", station.name) })
                                    viewModel.playStation(station) 
                                },
                                onToggleFavorite = { station -> 
                                    FirebaseManager.logEvent("toggle_favorite", Bundle().apply { putString("station_name", station.name) })
                                    viewModel.toggleFavorite(station) 
                                },
                                onBlockStation = { station -> viewModel.blockStation(station.id) },
                                onDemoteStation = { station -> viewModel.demoteStation(station.id) },
                                onUndemoteStation = { station -> viewModel.undemoteStation(station.id) },
                                onOpenAddStation = { viewModel.setShowAddStationDialog(true) },
                                onLoadMore = { viewModel.loadMoreStations() },
                                onRefresh = { viewModel.refreshStations() },
                                onRetryDiscovery = { viewModel.retryDiscovery() }
                            )
                        }

                        composable(NavRoute.Podcast.route) {
                            HomeScreen(
                                uiState = uiState,
                                windowSizeClass = windowSizeClass,
                                onPlayPause = { viewModel.togglePlayPause() },
                                onNextStation = { viewModel.playNextStation() },
                                onPreviousStation = { viewModel.playPreviousStation() },
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onSaveSearchQuery = { viewModel.saveSearchQuery(it) },
                                onDeleteSearchQuery = { viewModel.deleteSearchQuery(it) },
                                onClearSearchHistory = { viewModel.clearSearchHistory() },
                                onGenreSelect = { viewModel.setSelectedGenre(it) },
                                onCountrySelect = { viewModel.setSelectedCountry(it) },
                                onStationSelect = { station -> 
                                    FirebaseManager.logEvent("play_station", Bundle().apply { putString("station_name", station.name) })
                                    viewModel.playStation(station) 
                                },
                                onToggleFavorite = { station -> 
                                    FirebaseManager.logEvent("toggle_favorite", Bundle().apply { putString("station_name", station.name) })
                                    viewModel.toggleFavorite(station) 
                                },
                                onBlockStation = { station -> viewModel.blockStation(station.id) },
                                onDemoteStation = { station -> viewModel.demoteStation(station.id) },
                                onUndemoteStation = { station -> viewModel.undemoteStation(station.id) },
                                onOpenAddStation = { viewModel.setShowAddStationDialog(true) },
                                onLoadMore = { viewModel.loadMoreStations() },
                                onRefresh = { viewModel.refreshStations() },
                                onRetryDiscovery = { viewModel.retryDiscovery() }
                            )
                        }

                        composable(NavRoute.Favorites.route) {
                            FavoritesScreen(
                                favoriteStations = uiState.favoriteStations,
                                currentStation = syncedCurrentStation,
                                isPlaying = uiState.isPlaying,
                                isLoading = uiState.isLoading,
                                demotedStationIds = uiState.demotedStationIds,
                                failedStationIds = uiState.failedStationIds,
                                onStationSelect = { station -> 
                                    FirebaseManager.logEvent("play_station", Bundle().apply { putString("station_name", station.name) })
                                    viewModel.playStation(station) 
                                },
                                onToggleFavorite = { station -> 
                                    FirebaseManager.logEvent("toggle_favorite", Bundle().apply { putString("station_name", station.name) })
                                    viewModel.toggleFavorite(station) 
                                },
                                onBlockStation = { station -> viewModel.blockStation(station.id) },
                                onDemoteStation = { station -> viewModel.demoteStation(station.id) },
                                onUndemoteStation = { station -> viewModel.undemoteStation(station.id) }
                            )
                        }

                        composable(NavRoute.Settings.route) {
                            var showAlarmDialog by remember { mutableStateOf(false) }

                            com.easeaudio.ui.screens.SettingsScreen(
                                sleepTimerRemaining = uiState.sleepTimerRemaining,
                                selectedCountry = uiState.selectedCountry,
                                isBatterySaverEnabled = uiState.isBatterySaverEnabled,
                                onToggleBatterySaver = { viewModel.toggleBatterySaver() },
                                isAutoPlayOnStartupEnabled = uiState.isAutoPlayOnStartupEnabled,
                                onToggleAutoPlayOnStartup = { viewModel.toggleAutoPlayOnStartup() },
                                isLightMode = AppThemeState.isLightMode,
                                onToggleLightMode = { AppThemeState.setLightMode(context, !AppThemeState.isLightMode) },
                                onOpenEqualizer = { viewModel.setShowEqualizerDialog(true) },
                                onOpenSleepTimer = { viewModel.setShowSleepTimerDialog(true) },
                                onOpenRadioAlarm = { showAlarmDialog = true },
                                onOpenAppearance = { viewModel.setShowAppearanceDialog(true) },
                                onOpenOnboarding = { navController.navigate(NavRoute.Onboarding.route) },
                                onOpenBlockedDialog = { viewModel.setShowBlockedDialog(true) },
                                onOpenAttribution = { viewModel.setShowAttributionDialog(true) }
                            )

                            if (showAlarmDialog) {
                                com.easeaudio.ui.components.AlarmDialog(
                                    currentStation = syncedCurrentStation,
                                    onDismiss = { showAlarmDialog = false }
                                )
                            }
                        }

                        composable(NavRoute.Screensaver.route) {
                            ScreensaverScreen(
                                currentStation = syncedCurrentStation,
                                isPlaying = uiState.isPlaying,
                                streamTitle = uiState.streamTitle,
                                waveAmplitudes = uiState.waveAmplitudes,
                                sleepTimerRemaining = uiState.sleepTimerRemaining,
                                onTogglePlay = { viewModel.togglePlayPause() },
                                onOpenSleepTimer = { viewModel.setShowSleepTimerDialog(true) },
                                onToggleFavorite = { syncedCurrentStation?.let { viewModel.toggleFavorite(it) } }
                            )
                        }

                        composable(NavRoute.CarMode.route) {
                            CarModeScreen(
                                uiState = uiState,
                                onPlayPause = { viewModel.togglePlayPause() },
                                onNextStation = { viewModel.playNextStation() },
                                onPreviousStation = { viewModel.playPreviousStation() },
                                onSelectStation = { viewModel.playStation(it) },
                                onEpisodeSelect = { show, episode -> viewModel.playEpisode(show, episode) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onTabSelect = { viewModel.setSelectedTab(it) },
                                onGenreSelect = { viewModel.setSelectedGenre(it) },
                                onCountrySelect = { viewModel.setSelectedCountry(it) },
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onLoadMore = { viewModel.loadMoreStations() },
                                onSeekRelative = { offsetMs -> viewModel.playerManager.seekRelative(offsetMs) },
                                onSeek = { posMs -> viewModel.playerManager.seekTo(posMs) },
                                onPlaybackSpeedChange = { speed -> viewModel.setPlaybackSpeed(speed) },
                                onOpenSleepTimer = { viewModel.setShowSleepTimerDialog(true) },
                                onOpenEqualizer = { viewModel.setShowEqualizerDialog(true) },
                                onOpenEpisodes = { viewModel.setShowEpisodesSheet(true) },
                                eqPresets = viewModel.eqPresets,
                                onExitCarMode = {
                                    if (!navController.popBackStack()) {
                                        navController.navigate(NavRoute.Home.route) {
                                            popUpTo(NavRoute.CarMode.route) { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // Mini Player
                    if (!isFullPlayerVisible && currentRoute != NavRoute.Screensaver.route && currentRoute != NavRoute.CarMode.route && currentRoute != NavRoute.Onboarding.route) {
                        Column(
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            com.easeaudio.ui.components.NotificationPermissionReminder(
                                visible = (uiState.currentStation != null && (uiState.isPlaying || uiState.isLoading) && !hasNotificationPermission && !isPlaytimePermissionDismissed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU),
                                onRequestPermission = onRequestNotificationPermission,
                                onDismiss = { isPlaytimePermissionDismissed = true }
                            )

                            MiniPlayer(
                                station = syncedCurrentStation,
                                isPlaying = uiState.isPlaying,
                                isLoading = uiState.isLoading,
                                streamTitle = uiState.streamTitle,
                                waveAmplitudes = uiState.waveAmplitudes,
                                currentPosition = uiState.currentPlaybackPosition,
                                totalDuration = uiState.totalPlaybackDuration,
                                onTogglePlay = { viewModel.togglePlayPause() },
                                onToggleFavorite = { syncedCurrentStation?.let { viewModel.toggleFavorite(it) } },
                                onOpenFullPlayer = { isFullPlayerVisible = true },
                                onOpenTrackOptions = { showTrackActionSheet = true }
                            )
                        }
                    }

                    if (showTrackActionSheet && !uiState.streamTitle.isNullOrBlank()) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        com.easeaudio.ui.components.TrackActionSheet(
                            trackTitle = uiState.streamTitle!!,
                            stationName = syncedCurrentStation?.name ?: "Radio",
                            stationGenre = syncedCurrentStation?.genre ?: "",
                            isFavorite = syncedCurrentStation?.isFavorite ?: false,
                            onToggleFavorite = syncedCurrentStation?.let { { viewModel.toggleFavorite(it) } },
                            onSetAsAlarmStation = syncedCurrentStation?.let { st ->
                                {
                                    com.easeaudio.alarm.RadioAlarmManager.setWakeUpStation(context, st.id, st.name, st.streamUrl)
                                    android.widget.Toast.makeText(context, context.getString(R.string.alarm_station_saved), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            onBlockStation = { syncedCurrentStation?.let { viewModel.blockStation(it.id) } },
                            onDismiss = { showTrackActionSheet = false }
                        )
                    }
                }
            }
        }
        }

        // Full Player Modal with Window-Level Focus Isolation
        if (isFullPlayerVisible) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { isFullPlayerVisible = false },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                PlayerScreen(
                    station = syncedCurrentStation,
                    windowSizeClass = windowSizeClass,
                    isPlaying = uiState.isPlaying,
                    isLoading = uiState.isLoading,
                    streamTitle = uiState.streamTitle,
                    waveAmplitudes = uiState.waveAmplitudes,
                    volume = uiState.volume,
                    sleepTimerRemaining = uiState.sleepTimerRemaining,
                    activeEqPreset = uiState.activeEqPreset,
                    eqPresets = viewModel.eqPresets,
                    playbackError = uiState.playbackError,
                    hasNotificationPermission = hasNotificationPermission,
                    currentPosition = uiState.currentPlaybackPosition,
                    totalDuration = uiState.totalPlaybackDuration,
                    playbackSpeed = uiState.playbackSpeed,
                    onPlaybackSpeedChange = { speed -> viewModel.playerManager.setPlaybackSpeed(speed) },
                    onOpenEpisodes = { viewModel.setShowEpisodesSheet(true) },
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onTogglePlay = { viewModel.togglePlayPause() },
                    onToggleFavorite = { syncedCurrentStation?.let { viewModel.toggleFavorite(it) } },
                    onVolumeChange = { viewModel.playerManager.setVolume(it) },
                    onOpenSleepTimer = { viewModel.setShowSleepTimerDialog(true) },
                    onOpenEqualizer = { viewModel.setShowEqualizerDialog(true) },
                    onOpenTrackOptions = { showTrackActionSheet = true },
                    onOpenScreensaver = {
                        isFullPlayerVisible = false
                        navController.navigate(NavRoute.Screensaver.route)
                    },
                    onOpenCarMode = {
                        isFullPlayerVisible = false
                        navController.navigate(NavRoute.CarMode.route)
                    },
                    onRetryStream = { viewModel.retryCurrentStation() },
                    onPlayNextStation = { viewModel.playNextStation() },
                    onPlayPreviousStation = { viewModel.playPreviousStation() },
                    onSeekRelative = { offsetMs -> viewModel.playerManager.seekRelative(offsetMs) },
                    onSeek = { posMs -> viewModel.playerManager.seekTo(posMs) },
                    onBack = { isFullPlayerVisible = false }
                )
            }
        }

        val activePrompt by viewModel.smartEngagementManager.activePrompt.collectAsState()
        val updateInfo by viewModel.smartEngagementManager.updateInfo.collectAsState()

        // Dialogs
        if (uiState.showSleepTimerDialog) {
            SleepTimerDialog(
                activeTimerMinutes = uiState.sleepTimerRemaining,
                onSelectMinutes = { mins -> viewModel.setSleepTimer(mins) },
                onCancelTimer = { viewModel.cancelSleepTimer() },
                onDismiss = { viewModel.setShowSleepTimerDialog(false) }
            )
        }

        if (uiState.showEqualizerDialog) {
            EqualizerDialog(
                activePreset = uiState.activeEqPreset,
                presets = viewModel.eqPresets,
                isAudioBoosterEnabled = uiState.isAudioBoosterEnabled,
                onToggleAudioBooster = { enabled -> viewModel.setAudioBoosterEnabled(enabled) },
                onSelectPreset = { preset -> viewModel.setEqPreset(preset) },
                onDismiss = { viewModel.setShowEqualizerDialog(false) }
            )
        }

        if (uiState.showAddStationDialog) {
            AddStationDialog(
                onAddStation = { name, url, genre -> viewModel.addCustomStation(name, url, genre) },
                onDismiss = { viewModel.setShowAddStationDialog(false) }
            )
        }

        if (uiState.showBlockedDialog) {
            com.easeaudio.ui.components.BlockedStationsDialog(
                filterConfig = uiState.filterConfig,
                blockedStations = uiState.blockedStations,
                onToggleAdultFilter = { enabled -> viewModel.setFilterAdultContent(enabled) },
                onTogglePoliticsFilter = { enabled -> viewModel.setFilterPoliticsContent(enabled) },
                onToggleReligiousFilter = { enabled -> viewModel.setFilterReligiousContent(enabled) },
                onAddCustomKeyword = { keyword -> viewModel.addCustomFilterKeyword(keyword) },
                onRemoveCustomKeyword = { keyword -> viewModel.removeCustomFilterKeyword(keyword) },
                onClearCustomKeywords = { viewModel.clearCustomFilterKeywords() },
                onUnblockStation = { stationId -> viewModel.unblockStation(stationId) },
                onClearAllBlocked = { viewModel.clearAllBlockedStations() },
                onDismiss = { viewModel.setShowBlockedDialog(false) }
            )
        }

        if (uiState.showEpisodesSheet && syncedCurrentStation != null) {
            com.easeaudio.ui.components.PodcastEpisodesSheet(
                show = syncedCurrentStation,
                episodes = uiState.currentEpisodesList,
                currentEpisode = uiState.currentEpisode,
                isPlaying = uiState.isPlaying,
                isLoading = uiState.isLoading || uiState.isLoadingEpisodes,
                currentPlaybackPosition = uiState.currentPlaybackPosition,
                onSelectEpisode = { episode ->
                    viewModel.playEpisode(syncedCurrentStation, episode)
                },
                onSeek = { posMs ->
                    viewModel.playerManager.seekTo(posMs)
                },
                onDismiss = { viewModel.setShowEpisodesSheet(false) }
            )
        }

        if (uiState.showAppearanceDialog) {
            AppearanceSelectionScreen(
                currentTheme = AppThemeState.currentTheme,
                themes = AppThemeState.ThemePresets,
                selectedLauncherIcon = uiState.selectedLauncherIcon,
                onSelectLauncherIcon = { viewModel.setLauncherIcon(it) },
                onDismiss = { viewModel.setShowAppearanceDialog(false) },
                onSelectTheme = { theme ->
                    AppThemeState.saveTheme(context, theme.id)
                }
            )
        }

        if (uiState.showAttributionDialog) {
            AttributionDialog(onDismiss = { viewModel.setShowAttributionDialog(false) })
        }

        // Smart Engagement Dialogs
        when (activePrompt) {
            com.easeaudio.engagement.EngagementPromptType.RATE_5_STARS -> {
                RateAppDialog(
                    onRateSubmitted = { stars -> viewModel.smartEngagementManager.onRatingCompleted(stars) },
                    onDismiss = { viewModel.smartEngagementManager.onRatingDismissed() }
                )
            }
            com.easeaudio.engagement.EngagementPromptType.SHARE_APP -> {
                ShareAppDialog(
                    onShareConfirmed = { viewModel.smartEngagementManager.onShareCompleted() },
                    onDismiss = { viewModel.smartEngagementManager.onShareDismissed() }
                )
            }
            com.easeaudio.engagement.EngagementPromptType.UPDATE_APP -> {
                UpdateAppDialog(
                    updateInfo = updateInfo,
                    onUpdateConfirmed = { viewModel.smartEngagementManager.onUpdateConfirmed() },
                    onDismiss = { viewModel.smartEngagementManager.onUpdateDismissed() }
                )
            }
            com.easeaudio.engagement.EngagementPromptType.NONE -> {}
        }
    }
}
