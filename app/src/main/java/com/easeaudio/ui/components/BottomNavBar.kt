package com.easeaudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.easeaudio.R
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.DirectionsCar

sealed class NavRoute(
    val route: String,
    @StringRes val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    object Home : NavRoute("home", R.string.nav_radio, Icons.Filled.Radio, Icons.Outlined.Radio, "nav_radio")
    object Radio : NavRoute("radio", R.string.nav_radio, Icons.Filled.Radio, Icons.Outlined.Radio, "nav_radio")
    object Podcast : NavRoute("podcast", R.string.nav_podcast, Icons.Filled.Mic, Icons.Outlined.Mic, "nav_podcast")
    object Favorites : NavRoute("favorites", R.string.nav_favorites, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, "nav_favorites")
    object Settings : NavRoute("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
    object Screensaver : NavRoute("screensaver", R.string.nav_dock, Icons.Filled.Bedtime, Icons.Outlined.Bedtime, "nav_screensaver")
    object Onboarding : NavRoute("onboarding", R.string.nav_onboarding, Icons.Filled.Explore, Icons.Outlined.Explore, "nav_onboarding")
    object CarMode : NavRoute("carmode", R.string.car_mode, Icons.Filled.DirectionsCar, Icons.Filled.DirectionsCar, "nav_carmode")
}

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(NavRoute.Radio, NavRoute.Podcast, NavRoute.Favorites, NavRoute.Settings)

    NavigationBar(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = NavigationBarDefaults.Elevation
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route || (item.route == "radio" && currentRoute == "home")
            val localizedTitle = stringResource(item.titleRes)
            val selectedAccent = MaterialTheme.colorScheme.primary
            var isFocused by remember { mutableStateOf(false) }
            
            NavigationBarItem(
                modifier = Modifier
                    .onFocusChanged { isFocused = it.isFocused }
                    .scale(if (isFocused) 1.08f else 1.0f)
                    .testTag(item.testTag),
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = localizedTitle
                    )
                },
                label = { Text(localizedTitle) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = selectedAccent,
                    selectedTextColor = selectedAccent,
                    indicatorColor = if (isFocused) selectedAccent.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                    unselectedIconColor = if (isFocused) selectedAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    unselectedTextColor = if (isFocused) selectedAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            )
        }
    }
}
