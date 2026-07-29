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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    onGenreSelected: (String) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onCompleteOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { 3 }
    val isTv = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
    }

    // Selected genre state for slide 3
    var selectedGenre by remember { mutableStateOf("Chillout") }

    // Permission state check for slide 2
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

    // Sync permission state when screen is displayed or when app resumes from permission dialog
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
        // Top Bar with App Title and Skip Button
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
                    contentDescription = "NeoTune Logo",
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

        val canScrollForward = if (pagerState.currentPage == 1) hasNotificationPermission else true

        // Pager Content
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = canScrollForward,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 70.dp, bottom = 100.dp)
        ) { page ->
            when (page) {
                0 -> SlideGlobalRadio()
                1 -> SlideBackgroundControls(
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
                2 -> SlideGenreDiscovery(
                    availableGenres = availableGenres,
                    selectedGenre = selectedGenre,
                    onGenreSelected = { selectedGenre = it },
                    isTv = isTv
                )
            }
        }

        // Bottom Navigation Bar with Page Indicators & Next/Start Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Page Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
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

            // Action Buttons (Back & Next / Get Started)
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
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(
                                listOf(
                                    if (isBackFocused) NeonCyan else TextMuted,
                                    TextMuted
                                )
                            )
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
                val isLastPage = pagerState.currentPage == 2
                val isNextEnabled = if (pagerState.currentPage == 1) hasNotificationPermission else true

                Button(
                    onClick = {
                        if (isLastPage) {
                            onGenreSelected(selectedGenre)
                            onCompleteOnboarding()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    enabled = isNextEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isNextFocused) Color.White else NeonCyan,
                        contentColor = DarkBackground,
                        disabledContainerColor = DarkSurfaceVariant,
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .onFocusChanged { isNextFocused = it.isFocused }
                        .border(
                            width = if (isNextEnabled && isNextFocused) 3.dp else 0.dp,
                            color = if (isNextEnabled) NeonCyan else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("btn_onboarding_next")
                ) {
                    Text(
                        text = if (isLastPage) stringResource(R.string.onboarding_slide3_start_listening) else stringResource(R.string.onboarding_next),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
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
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glowing Visual Hero Card
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.25f),
                            NeonBlue.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Pulse rings
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .border(2.dp, NeonCyan.copy(alpha = 0.4f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .border(2.dp, NeonCyan.copy(alpha = 0.7f), CircleShape)
            )

            // Inner icon badge
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                DarkSurfaceVariant,
                                DarkSurface
                            )
                        )
                    )
                    .border(2.dp, NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Public,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.onboarding_slide1_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                letterSpacing = (-0.5).sp
            ),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.onboarding_slide1_subtitle),
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 22.sp,
                fontSize = 15.sp
            ),
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Feature Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            BadgeItem(
                icon = Icons.Filled.Radio,
                text = stringResource(R.string.onboarding_slide1_badge1)
            )
            Spacer(modifier = Modifier.width(10.dp))
            BadgeItem(
                icon = Icons.Filled.CloudSync,
                text = stringResource(R.string.onboarding_slide1_badge2)
            )
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
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Visual Hero Preview Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                                text = "Background Media Service",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "System Notification & Lockscreen",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (hasPermission) StatusGreen.copy(alpha = 0.2f) else DarkBackground)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (hasPermission) "ACTIVE" else "READY",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (hasPermission) StatusGreen else TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Permission Trigger
                var isBtnFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasPermission) StatusGreen.copy(alpha = 0.15f) else NeonCyan,
                        contentColor = if (hasPermission) StatusGreen else DarkBackground
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isBtnFocused = it.isFocused }
                        .border(
                            width = if (isBtnFocused) 3.dp else 0.dp,
                            color = if (hasPermission) StatusGreen else NeonCyan,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .testTag("btn_grant_notification")
                ) {
                    Icon(
                        imageVector = if (hasPermission) Icons.Outlined.CheckCircle else Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasPermission)
                            stringResource(R.string.onboarding_slide2_granted)
                        else
                            stringResource(R.string.onboarding_slide2_grant_btn),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.onboarding_slide2_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                letterSpacing = (-0.5).sp
            ),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.onboarding_slide2_subtitle),
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 22.sp,
                fontSize = 15.sp
            ),
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FeatureLine(icon = Icons.Filled.Power, text = stringResource(R.string.onboarding_slide2_feature1))
            FeatureLine(icon = Icons.Filled.Lock, text = stringResource(R.string.onboarding_slide2_feature2))
            FeatureLine(icon = Icons.Filled.Autorenew, text = stringResource(R.string.onboarding_slide2_feature3))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlideGenreDiscovery(
    availableGenres: List<String>,
    selectedGenre: String,
    onGenreSelected: (String) -> Unit,
    isTv: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                letterSpacing = (-0.5).sp
            ),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_slide3_subtitle),
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 22.sp,
                fontSize = 15.sp
            ),
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.onboarding_slide3_select_genres),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Multi-genre chips flow layout
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val genres = remember(availableGenres) {
                val list = availableGenres.filter { it != "Custom" }.toMutableList()
                if (!list.contains("Chillout")) list.add(1, "Chillout")
                list
            }

            genres.forEach { genre ->
                val isSelected = selectedGenre.equals(genre, ignoreCase = true)
                var isChipFocused by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) NeonCyan else DarkSurfaceVariant
                        )
                        .border(
                            width = if (isSelected || isChipFocused) 2.dp else 1.dp,
                            color = if (isSelected) NeonCyan else if (isChipFocused) Color.White else TextMuted.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onGenreSelected(genre) }
                        .onFocusChanged { isChipFocused = it.isFocused }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("chip_genre_$genre")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = DarkBackground,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) DarkBackground else TextPrimary
                        )
                    }
                }
            }
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary
        )
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = TextPrimary
        )
    }
}
