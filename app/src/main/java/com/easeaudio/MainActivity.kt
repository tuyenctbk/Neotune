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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.easeaudio.network.QualityLevel
import com.easeaudio.service.FirebaseManager
import com.easeaudio.ui.components.*
import com.easeaudio.ui.screens.FavoritesScreen
import com.easeaudio.ui.screens.HomeScreen
import com.easeaudio.ui.screens.OnboardingScreen
import com.easeaudio.ui.screens.PlayerScreen
import com.easeaudio.ui.screens.ScreensaverScreen
import com.easeaudio.ui.theme.TuneveTheme
import com.easeaudio.ui.theme.AppThemeState
import com.easeaudio.viewmodel.RadioViewModel
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {

    private val viewModel: RadioViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        Log.d("MainActivity", "Notification permission granted: $isGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        AppThemeState.loadTheme(applicationContext)
        enableEdgeToEdge()

        // Safe Firebase Initializer
        FirebaseManager.initialize(applicationContext)

        setContent {
            TuneveTheme {
                MainAppContent(
                    viewModel = viewModel,
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MainAppContent(
    viewModel: RadioViewModel,
    onRequestNotificationPermission: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("neotune_prefs", Context.MODE_PRIVATE) }
    val isOnboardingCompleted = remember { mutableStateOf(prefs.getBoolean("is_onboarding_completed", false)) }
    val startDestination = if (isOnboardingCompleted.value) NavRoute.Home.route else NavRoute.Onboarding.route

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

    val stations by viewModel.stations.collectAsState()
    val favoriteStations by viewModel.favoriteStations.collectAsState()
    val recentStations by viewModel.recentStations.collectAsState()

    val currentStation by viewModel.playerManager.currentStation.collectAsState()
    val isCurrentStationFavorite = favoriteStations.any { it.id == currentStation?.id }
    val syncedCurrentStation = currentStation?.copy(isFavorite = isCurrentStationFavorite)
    val isPlaying by viewModel.playerManager.isPlaying.collectAsState()
    val isLoading by viewModel.playerManager.isLoading.collectAsState()
    val playbackError by viewModel.playerManager.playbackError.collectAsState()
    val playbackErrorDetails by viewModel.playerManager.playbackErrorDetails.collectAsState()
    val streamTitle by viewModel.playerManager.streamTitle.collectAsState()
    val waveAmplitudes by viewModel.playerManager.waveAmplitudes.collectAsState()
    val volume by viewModel.playerManager.volume.collectAsState()
    val sleepTimerRemaining by viewModel.playerManager.sleepTimerMinutesRemaining.collectAsState()

    val networkStatus by viewModel.networkStatus.collectAsState()
    val remoteConfig by viewModel.remoteConfig.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val activeEqPreset by viewModel.activeEqPreset.collectAsState()

    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val isDiscoveringOnline by viewModel.isDiscoveringOnline.collectAsState()
    val isDiscoveryError by viewModel.isDiscoveryError.collectAsState()
    val canLoadMore by viewModel.canLoadMore.collectAsState()
    val failedStationIds by viewModel.failedStationIds.collectAsState()
    val availableCountries by viewModel.availableCountries.collectAsState()
    val isLoadingCountries by viewModel.isLoadingCountries.collectAsState()

    val showSleepTimerDialog by viewModel.showSleepTimerDialog.collectAsState()
    val showEqualizerDialog by viewModel.showEqualizerDialog.collectAsState()
    val showAddStationDialog by viewModel.showAddStationDialog.collectAsState()

    var isFullPlayerVisible by remember { mutableStateOf(value = false) }
    var showTrackActionSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error toast/snackbar if stream error occurs
    LaunchedEffect(playbackError, playbackErrorDetails) {
        playbackErrorDetails?.let { details ->
            Log.e("MainActivity", "Stream Error Captured: ${details.toUserSummary()} (Code: ${details.errorCodeName}, HTTP: ${details.httpStatusCode ?: "N/A"})")
            FirebaseManager.recordException(Exception("Stream Error: ${details.errorCodeName}"))
        }
        playbackError?.let { err ->
            snackbarHostState.showSnackbar(err)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!isFullPlayerVisible && currentRoute != NavRoute.Onboarding.route) {
                    BottomNavBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable(NavRoute.Onboarding.route) {
                        OnboardingScreen(
                            availableGenres = viewModel.availableGenres.map { it.key },
                            onGenreSelected = { genre -> viewModel.setSelectedGenre(genre) },
                            onGenresSelected = { genres -> viewModel.setPreferredGenres(genres) },
                            onCountrySelected = { country -> viewModel.setSelectedCountry(country) },
                            onRequestNotificationPermission = onRequestNotificationPermission,
                            onCompleteOnboarding = completeOnboarding
                        )
                    }

                    composable(NavRoute.Home.route) {
                        HomeScreen(
                            stations = stations,
                            recentStations = recentStations,
                            currentStation = syncedCurrentStation,
                            isPlaying = isPlaying,
                            isLoading = isLoading,
                            isDiscoveringOnline = isDiscoveringOnline,
                            failedStationIds = failedStationIds,
                            searchQuery = searchQuery,
                            selectedGenre = selectedGenre,
                            availableGenres = viewModel.availableGenres,
                            selectedCountry = selectedCountry,
                            availableCountries = availableCountries,
                            isLoadingCountries = isLoadingCountries,
                            sleepTimerRemaining = sleepTimerRemaining,
                            networkStatus = networkStatus,
                            remoteConfig = remoteConfig,
                            isLoadingMore = isLoadingMore,
                            canLoadMore = canLoadMore,
                            isDiscoveryError = isDiscoveryError,
                            streamTitle = streamTitle,
                            onPlayPause = { viewModel.togglePlayPause() },
                            onNextStation = { viewModel.playNextStation() },
                            onPreviousStation = { viewModel.playPreviousStation() },
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
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
                            onOpenAddStation = { viewModel.setShowAddStationDialog(true) },
                            onOpenSleepTimer = { viewModel.setShowSleepTimerDialog(true) },
                            onOpenEqualizer = { viewModel.setShowEqualizerDialog(true) },
                            onOpenOnboarding = { navController.navigate(NavRoute.Onboarding.route) },
                            onLoadMore = { viewModel.loadMoreStations() },
                            onRefresh = { viewModel.refreshStations() },
                            onRetryDiscovery = { viewModel.retryDiscovery() }
                        )
                    }

                    composable(NavRoute.Favorites.route) {
                        FavoritesScreen(
                            favoriteStations = favoriteStations,
                            currentStation = syncedCurrentStation,
                            isPlaying = isPlaying,
                            isLoading = isLoading,
                            failedStationIds = failedStationIds,
                            onStationSelect = { station -> 
                                FirebaseManager.logEvent("play_station", Bundle().apply { putString("station_name", station.name) })
                                viewModel.playStation(station) 
                            },
                            onToggleFavorite = { station -> 
                                FirebaseManager.logEvent("toggle_favorite", Bundle().apply { putString("station_name", station.name) })
                                viewModel.toggleFavorite(station) 
                            }
                        )
                    }

                    composable(NavRoute.Screensaver.route) {
                        ScreensaverScreen(
                            currentStation = syncedCurrentStation,
                            isPlaying = isPlaying,
                            streamTitle = streamTitle,
                            waveAmplitudes = waveAmplitudes,
                            sleepTimerRemaining = sleepTimerRemaining,
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onOpenSleepTimer = { viewModel.setShowSleepTimerDialog(true) },
                            onToggleFavorite = { syncedCurrentStation?.let { viewModel.toggleFavorite(it) } }
                        )
                    }
                }

                // Mini Player floating bar (shown if station is selected and full player is collapsed)
                if (!isFullPlayerVisible && currentRoute != NavRoute.Screensaver.route && currentRoute != NavRoute.Onboarding.route) {
                    MiniPlayer(
                        station = syncedCurrentStation,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        streamTitle = streamTitle,
                        waveAmplitudes = waveAmplitudes,
                        onTogglePlay = { viewModel.togglePlayPause() },
                        onToggleFavorite = { syncedCurrentStation?.let { viewModel.toggleFavorite(it) } },
                        onOpenFullPlayer = { isFullPlayerVisible = true },
                        onOpenTrackOptions = { showTrackActionSheet = true },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                if (showTrackActionSheet && !streamTitle.isNullOrBlank()) {
                    com.easeaudio.ui.components.TrackActionSheet(
                        trackTitle = streamTitle!!,
                        stationName = syncedCurrentStation?.name ?: "Radio",
                        onDismiss = { showTrackActionSheet = false }
                    )
                }
            }
        }

        // Full Player Modal Overlay
        AnimatedVisibility(
            visible = isFullPlayerVisible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerScreen(
                station = syncedCurrentStation,
                isPlaying = isPlaying,
                isLoading = isLoading,
                streamTitle = streamTitle,
                waveAmplitudes = waveAmplitudes,
                volume = volume,
                sleepTimerRemaining = sleepTimerRemaining,
                activeEqPreset = activeEqPreset,
                eqPresets = viewModel.eqPresets,
                playbackError = playbackError,
                onTogglePlay = { viewModel.togglePlayPause() },
                onToggleFavorite = { syncedCurrentStation?.let { viewModel.toggleFavorite(it) } },
                onVolumeChange = { viewModel.playerManager.setVolume(it) },
                onOpenSleepTimer = { viewModel.setShowSleepTimerDialog(true) },
                onOpenEqualizer = { viewModel.setShowEqualizerDialog(true) },
                onRetryStream = { viewModel.retryCurrentStation() },
                onPlayNextStation = { viewModel.playNextStation() },
                onPlayPreviousStation = { viewModel.playPreviousStation() },
                onBack = { isFullPlayerVisible = false }
            )
        }

        // Dialogs
        if (showSleepTimerDialog) {
            SleepTimerDialog(
                activeTimerMinutes = sleepTimerRemaining,
                onSelectMinutes = { mins -> viewModel.setSleepTimer(mins) },
                onCancelTimer = { viewModel.cancelSleepTimer() },
                onDismiss = { viewModel.setShowSleepTimerDialog(false) }
            )
        }

        if (showEqualizerDialog) {
            EqualizerDialog(
                activePreset = activeEqPreset,
                presets = viewModel.eqPresets,
                onSelectPreset = { preset -> viewModel.setEqPreset(preset) },
                onDismiss = { viewModel.setShowEqualizerDialog(false) }
            )
        }

        if (showAddStationDialog) {
            AddStationDialog(
                onAddStation = { name, url, genre -> viewModel.addCustomStation(name, url, genre) },
                onDismiss = { viewModel.setShowAddStationDialog(false) }
            )
        }
    }
}
