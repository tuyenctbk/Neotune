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
import androidx.compose.ui.res.stringResource
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

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
    val activeEqPreset by viewModel.activeEqPreset.collectAsState()

    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val isDiscoveringOnline by viewModel.isDiscoveringOnline.collectAsState()
    val isDiscoveryError by viewModel.isDiscoveryError.collectAsState()
    val canLoadMore by viewModel.canLoadMore.collectAsState()
    val failedStationIds by viewModel.failedStationIds.collectAsState()

    val showSleepTimerDialog by viewModel.showSleepTimerDialog.collectAsState()
    val showEqualizerDialog by viewModel.showEqualizerDialog.collectAsState()
    val showAddStationDialog by viewModel.showAddStationDialog.collectAsState()

    var isFullPlayerVisible by remember { mutableStateOf(value = false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Network notification banner state
    var previousConnected by remember { mutableStateOf<Boolean?>(null) }
    var activeBanner by remember { mutableStateOf(NetworkBannerType.NONE) }

    LaunchedEffect(networkStatus) {
        val connected = networkStatus.isConnected
        val quality = networkStatus.qualityLevel
        
        if (previousConnected != null) {
            if (!connected) {
                activeBanner = NetworkBannerType.OFFLINE
            } else if (previousConnected == false) {
                activeBanner = NetworkBannerType.BACK_ONLINE
                kotlinx.coroutines.delay(3.seconds)
                if (activeBanner == NetworkBannerType.BACK_ONLINE) {
                    activeBanner = NetworkBannerType.NONE
                }
            } else if (quality == QualityLevel.SAVER_SMOOTH) {
                activeBanner = NetworkBannerType.WEAK_CONNECTION
                kotlinx.coroutines.delay(3.seconds)
                if (activeBanner == NetworkBannerType.WEAK_CONNECTION) {
                    activeBanner = NetworkBannerType.NONE
                }
            } else {
                if (activeBanner == NetworkBannerType.WEAK_CONNECTION) {
                    activeBanner = NetworkBannerType.NONE
                }
            }
        } else {
            if (!connected) {
                activeBanner = NetworkBannerType.OFFLINE
            }
        }
        previousConnected = connected
    }

    // Show error toast/snackbar if stream error occurs
    LaunchedEffect(playbackError, playbackErrorDetails) {
        playbackErrorDetails?.let { details ->
            Log.e("MainActivity", "Stream Error Captured: ${details.toUserSummary()} (Code: ${details.errorCodeName}, HTTP: ${details.httpStatusCode ?: "N/A"})")
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
                            onRequestNotificationPermission = onRequestNotificationPermission,
                            onCompleteOnboarding = completeOnboarding
                        )
                    }

                    composable(NavRoute.Home.route) {
                        HomeScreen(
                            stations = stations,
                            recentStations = recentStations,
                            currentStation = currentStation,
                            isPlaying = isPlaying,
                            isLoading = isLoading,
                            isDiscoveringOnline = isDiscoveringOnline,
                            failedStationIds = failedStationIds,
                            searchQuery = searchQuery,
                            selectedGenre = selectedGenre,
                            availableGenres = viewModel.availableGenres,
                            sleepTimerRemaining = sleepTimerRemaining,
                            networkStatus = networkStatus,
                            remoteConfig = remoteConfig,
                            isLoadingMore = isLoadingMore,
                            canLoadMore = canLoadMore,
                            isDiscoveryError = isDiscoveryError,
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onGenreSelect = { viewModel.setSelectedGenre(it) },
                            onStationSelect = { station -> viewModel.playStation(station) },
                            onToggleFavorite = { station -> viewModel.toggleFavorite(station) },
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
                            currentStation = currentStation,
                            isPlaying = isPlaying,
                            isLoading = isLoading,
                            failedStationIds = failedStationIds,
                            onStationSelect = { station -> viewModel.playStation(station) },
                            onToggleFavorite = { station -> viewModel.toggleFavorite(station) }
                        )
                    }

                    composable(NavRoute.Screensaver.route) {
                        ScreensaverScreen(
                            currentStation = currentStation,
                            isPlaying = isPlaying,
                            streamTitle = streamTitle,
                            waveAmplitudes = waveAmplitudes,
                            sleepTimerRemaining = sleepTimerRemaining,
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onOpenSleepTimer = { viewModel.setShowSleepTimerDialog(true) },
                            onToggleFavorite = { currentStation?.let { viewModel.toggleFavorite(it) } }
                        )
                    }
                }

                // Mini Player floating bar (shown if station is selected and full player is collapsed)
                if (!isFullPlayerVisible && currentRoute != NavRoute.Screensaver.route && currentRoute != NavRoute.Onboarding.route) {
                    MiniPlayer(
                        station = currentStation,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        streamTitle = streamTitle,
                        waveAmplitudes = waveAmplitudes,
                        onTogglePlay = { viewModel.togglePlayPause() },
                        onToggleFavorite = { currentStation?.let { viewModel.toggleFavorite(it) } },
                        onOpenFullPlayer = { isFullPlayerVisible = true },
                        modifier = Modifier.align(Alignment.BottomCenter)
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
                station = currentStation,
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
                onToggleFavorite = { currentStation?.let { viewModel.toggleFavorite(it) } },
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

        // Floating Network Status Overlay Banner (Graceful non-blocking overlay)
        AnimatedVisibility(
            visible = activeBanner != NetworkBannerType.NONE,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 72.dp)
                .zIndex(99f),
        ) {
            val config = when (activeBanner) {
                NetworkBannerType.OFFLINE -> BannerUIConfig(
                    text = stringResource(R.string.banner_offline),
                    bgColor = Color(0xFFD32F2F),
                    icon = Icons.Filled.WifiOff,
                    iconColor = Color.White
                )
                NetworkBannerType.BACK_ONLINE -> BannerUIConfig(
                    text = stringResource(R.string.banner_back_online),
                    bgColor = Color(0xFF388E3C),
                    icon = Icons.Filled.Wifi,
                    iconColor = Color.White
                )
                NetworkBannerType.WEAK_CONNECTION -> BannerUIConfig(
                    text = stringResource(R.string.banner_weak_connection),
                    bgColor = Color(0xFFF57C00),
                    icon = Icons.Filled.Warning,
                    iconColor = Color.White
                )
                else -> BannerUIConfig("", Color.Transparent, Icons.Filled.Wifi, Color.White)
            }

            if (config.text.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = config.bgColor),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .testTag("network_status_overlay_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = config.icon,
                            contentDescription = null,
                            tint = config.iconColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = config.text,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(
                            onClick = { activeBanner = NetworkBannerType.NONE },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Dismiss",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class NetworkBannerType {
    OFFLINE, BACK_ONLINE, WEAK_CONNECTION, NONE
}

private data class BannerUIConfig(
    val text: String,
    val bgColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconColor: Color
)
