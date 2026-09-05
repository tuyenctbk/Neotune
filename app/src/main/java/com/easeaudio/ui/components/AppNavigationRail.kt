package com.easeaudio.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AppNavigationRail(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = true
) {
    val items = listOf(NavRoute.Radio, NavRoute.Podcast, NavRoute.Favorites, NavRoute.Settings)
    val haptic = LocalHapticFeedback.current

    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) 84.dp else 0.dp,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "rail_width"
    )

    val railAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "rail_alpha"
    )

    if (railWidth > 0.dp) {
        NavigationRail(
            modifier = modifier
                .fillMaxHeight()
                .width(railWidth)
                .alpha(railAlpha)
                .testTag("app_navigation_rail"),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            header = {
                Spacer(modifier = Modifier.height(28.dp))
            }
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route || (item.route == "radio" && currentRoute == "home")
                    val localizedTitle = stringResource(item.titleRes)
                    val selectedAccent = MaterialTheme.colorScheme.primary
                    var isFocused by remember { mutableStateOf(false) }

                    val itemScale by animateFloatAsState(
                        targetValue = if (isFocused) 1.05f else if (isSelected) 1.02f else 1.0f,
                        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
                        label = "rail_item_scale"
                    )

                    Column(
                        modifier = Modifier
                            .width(72.dp)
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                            .scale(itemScale)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                color = when {
                                    isSelected && isFocused -> selectedAccent.copy(alpha = 0.28f)
                                    isSelected -> selectedAccent.copy(alpha = 0.16f)
                                    isFocused -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> Color.Transparent
                                },
                                shape = RoundedCornerShape(14.dp)
                            )
                            .border(
                                width = when {
                                    isSelected && isFocused -> 2.5.dp
                                    isFocused -> 2.dp
                                    else -> 0.dp
                                },
                                color = when {
                                    isSelected && isFocused -> Color.White
                                    isFocused -> selectedAccent
                                    else -> Color.Transparent
                                },
                                shape = RoundedCornerShape(14.dp)
                            )
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusable()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigate(item.route)
                            }
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyUp) {
                                    when (keyEvent.key) {
                                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onNavigate(item.route)
                                            true
                                        }
                                        else -> false
                                    }
                                } else {
                                    false
                                }
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                            .testTag("nav_rail_item_${item.route}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = localizedTitle,
                            tint = if (isSelected || isFocused) selectedAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = localizedTitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected || isFocused) selectedAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

