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
                        targetValue = if (isFocused) 1.14f else if (isSelected) 1.05f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "rail_item_scale"
                    )

                    Box(
                        modifier = Modifier
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                            .scale(itemScale)
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusable()
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
                            .shadow(
                                elevation = if (isFocused) 12.dp else 0.dp,
                                shape = RoundedCornerShape(16.dp),
                                spotColor = selectedAccent,
                                ambientColor = selectedAccent.copy(alpha = 0.5f)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = if (isFocused) 2.5.dp else 0.dp,
                                brush = if (isFocused) {
                                    Brush.horizontalGradient(
                                        listOf(selectedAccent, Color.White, selectedAccent)
                                    )
                                } else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .testTag("nav_rail_item_${item.route}")
                    ) {
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigate(item.route)
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = localizedTitle
                                )
                            },
                            label = {
                                Text(
                                    text = localizedTitle,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = selectedAccent,
                                selectedTextColor = selectedAccent,
                                indicatorColor = if (isFocused) selectedAccent.copy(alpha = 0.35f) else selectedAccent.copy(alpha = 0.15f),
                                unselectedIconColor = if (isFocused) selectedAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                unselectedTextColor = if (isFocused) selectedAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
        }
    }
}

