package com.easeaudio.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.easeaudio.R
import com.easeaudio.ui.theme.*
import kotlinx.coroutines.launch

private val StatusGreen = Color(0xFF10B981)
private val NeonBlue = Color(0xFF3B82F6)

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    availableGenres: List<String>,
    windowSizeClass: WindowSizeClass,
    onGenreSelected: (String) -> Unit,
    onGenresSelected: (Set<String>) -> Unit = {},
    onCountrySelected: (String) -> Unit = {},
    onRequestNotificationPermission: () -> Unit,
    onCompleteOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pageCount = 5
    val pagerState = rememberPagerState { pageCount }
    val isTv = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
    }

    var selectedGenres by remember { mutableStateOf(setOf("Chillout")) }
    var selectedCountry by remember { mutableStateOf("Global") }

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_favicon),
                    contentDescription = "Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = TextPrimary
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = true,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 70.dp, bottom = 100.dp)
        ) { page ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (page) {
                    0 -> SlideGlobalRadio()
                    1 -> SlidePodcasts()
                    2 -> SlideBackgroundControls(
                        hasPermission = hasNotificationPermission,
                        onRequestPermission = {
                            onRequestNotificationPermission()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                hasNotificationPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                            }
                        },
                        isTv = isTv
                    )
                    3 -> SlideProAudio()
                    4 -> SlideGenreDiscovery(
                        availableGenres = availableGenres,
                        selectedGenres = selectedGenres,
                        onGenreToggled = { genre ->
                            selectedGenres = if (selectedGenres.contains(genre)) {
                                if (selectedGenres.size > 1) selectedGenres - genre else selectedGenres
                            } else {
                                selectedGenres + genre
                            }
                        },
                        selectedCountry = selectedCountry,
                        onCountrySelected = { selectedCountry = it },
                        isTv = isTv
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pageCount) { index ->
                    val isSelected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(if (isSelected) 28.dp else 8.dp, label = "dotWidth")
                    val dotColor by animateColorAsState(if (isSelected) NeonCyan else TextMuted.copy(alpha = 0.4f), label = "dotColor")

                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (pagerState.currentPage > 0) {
                    var isBackFocused by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isBackFocused) NeonCyan else TextMuted
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .onFocusChanged { isBackFocused = it.isFocused }
                            .testTag("btn_onboarding_back")
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_back),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }

                var isNextFocused by remember { mutableStateOf(false) }
                val isLastPage = pagerState.currentPage == pageCount - 1

                Button(
                    onClick = {
                        if (isLastPage) {
                            onGenresSelected(selectedGenres)
                            onCountrySelected(selectedCountry)
                            onCompleteOnboarding()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isNextFocused) Color.White else NeonCyan,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .onFocusChanged { isNextFocused = it.isFocused }
                        .testTag("btn_onboarding_next")
                ) {
                    Text(
                        text = if (isLastPage) stringResource(R.string.onboarding_slide3_start_listening) else stringResource(R.string.onboarding_next),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SlideGlobalRadio() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonCyan.copy(alpha = 0.2f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .border(2.dp, NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.onboarding_slide1_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.onboarding_slide1_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BadgeItem(Icons.Filled.Radio, stringResource(R.string.onboarding_slide1_badge1))
            BadgeItem(Icons.Filled.CloudSync, stringResource(R.string.onboarding_slide1_badge2))
        }
    }
}

@Composable
private fun SlidePodcasts() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonPurple.copy(alpha = 0.2f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .border(2.dp, NeonPurple, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    tint = NeonPurple,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.onboarding_podcast_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.onboarding_podcast_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BadgeItem(Icons.Filled.DynamicFeed, stringResource(R.string.onboarding_podcast_badge1))
            BadgeItem(Icons.Filled.AutoGraph, stringResource(R.string.onboarding_podcast_badge2))
        }
    }
}

@Composable
private fun SlideBackgroundControls(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    isTv: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.media_service_label),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.notification_lockscreen_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(CircleShape)
                            .background(if (hasPermission) StatusGreen.copy(alpha = 0.2f) else DarkBackground)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (hasPermission) stringResource(R.string.status_active) else stringResource(R.string.status_ready),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (hasPermission) StatusGreen else TextMuted,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasPermission) StatusGreen.copy(alpha = 0.15f) else NeonCyan,
                        contentColor = if (hasPermission) StatusGreen else DarkBackground
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (hasPermission) Icons.Outlined.CheckCircle else Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasPermission) stringResource(R.string.onboarding_slide2_granted) else stringResource(R.string.onboarding_slide2_grant_btn),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboarding_slide2_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.onboarding_slide2_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureLine(Icons.Filled.Power, stringResource(R.string.onboarding_slide2_feature1))
            FeatureLine(Icons.Filled.Lock, stringResource(R.string.onboarding_slide2_feature2))
            FeatureLine(Icons.Filled.Autorenew, stringResource(R.string.onboarding_slide2_feature3))
        }
    }
}

@Composable
private fun SlideProAudio() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonCyan.copy(alpha = 0.2f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .border(2.dp, NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Equalizer,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.onboarding_audio_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.onboarding_audio_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        FeatureLine(Icons.Filled.Tune, stringResource(R.string.five_band_equalizer))
        Spacer(modifier = Modifier.height(8.dp))
        FeatureLine(Icons.Filled.Speed, stringResource(R.string.custom_playback_speed))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlideGenreDiscovery(
    availableGenres: List<String>,
    selectedGenres: Set<String>,
    onGenreToggled: (String) -> Unit,
    selectedCountry: String,
    onCountrySelected: (String) -> Unit,
    isTv: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(NeonCyan.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.onboarding_slide3_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_slide3_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        val allCountriesList = remember {
            listOf(
                com.easeaudio.viewmodel.CountryDisplay("Global", "🌐", ""),
                com.easeaudio.viewmodel.CountryDisplay("Vietnam", "🇻🇳", "VN"),
                com.easeaudio.viewmodel.CountryDisplay("United States", "🇺🇸", "US"),
                com.easeaudio.viewmodel.CountryDisplay("United Kingdom", "🇬🇧", "GB"),
                com.easeaudio.viewmodel.CountryDisplay("Canada", "🇨🇦", "CA"),
                com.easeaudio.viewmodel.CountryDisplay("Australia", "🇦🇺", "AU"),
                com.easeaudio.viewmodel.CountryDisplay("Germany", "🇩🇪", "DE"),
                com.easeaudio.viewmodel.CountryDisplay("Japan", "🇯🇵", "JP"),
                com.easeaudio.viewmodel.CountryDisplay("Brazil", "🇧🇷", "BR")
            )
        }

        var showCountryDialog by remember { mutableStateOf(false) }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allCountriesList.forEach { country ->
                val isSelected = selectedCountry.equals(country.name, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkSurfaceVariant)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) NeonCyan else TextMuted.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onCountrySelected(country.name) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isGlobal = country.name.equals("Global", ignoreCase = true) || country.code.isBlank()
                        if (isGlobal) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(text = country.flag, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = country.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                            color = if (isSelected) NeonCyan else TextPrimary
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .clickable { showCountryDialog = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.more_countries_label),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = NeonCyan
                    )
                }
            }
        }

        if (showCountryDialog) {
            com.easeaudio.ui.components.CountrySelectionDialog(
                selectedCountry = selectedCountry,
                countries = allCountriesList, // Simplified for brevity in onboarding
                onSelectCountry = { onCountrySelected(it) },
                onDismiss = { showCountryDialog = false }
            )
        }
    }
}

@Composable
private fun BadgeItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, TextMuted.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
    }
}

@Composable
private fun FeatureLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = TextPrimary)
    }
}
