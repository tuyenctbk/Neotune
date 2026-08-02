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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.easeaudio.R
import com.easeaudio.ui.theme.DarkSurfaceVariant
import com.easeaudio.ui.theme.NeonCyan
import com.easeaudio.ui.theme.NeonPurple
import com.easeaudio.ui.theme.TextMuted

import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings

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
}

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(NavRoute.Radio, NavRoute.Podcast, NavRoute.Favorites, NavRoute.Settings)

    NavigationBar(
        modifier = Modifier
            .background(com.easeaudio.ui.theme.DarkBackground)
            .windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = com.easeaudio.ui.theme.DarkBackground,
        tonalElevation = NavigationBarDefaults.Elevation
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route || (item.route == "radio" && currentRoute == "home")
            val localizedTitle = stringResource(item.titleRes)
            val selectedAccent = if (item.route == "podcast") NeonPurple else NeonCyan
            NavigationBarItem(
                modifier = Modifier.testTag(item.testTag),
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
                    indicatorColor = DarkSurfaceVariant,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                )
            )
        }
    }
}
