package com.easeaudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.easeaudio.R
import com.easeaudio.ui.theme.DarkSurface
import com.easeaudio.ui.theme.DarkSurfaceVariant
import com.easeaudio.ui.theme.NeonCyan
import com.easeaudio.ui.theme.TextMuted

sealed class NavRoute(
    val route: String,
    @StringRes val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    object Home : NavRoute("home", R.string.nav_tuner, Icons.Filled.Radio, Icons.Outlined.Radio, "nav_tuner")
    object Favorites : NavRoute("favorites", R.string.nav_favorites, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, "nav_favorites")
    object Screensaver : NavRoute("screensaver", R.string.nav_dock, Icons.Filled.Bedtime, Icons.Outlined.Bedtime, "nav_screensaver")
}

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(NavRoute.Home, NavRoute.Favorites, NavRoute.Screensaver)

    NavigationBar(
        modifier = Modifier
            .background(com.easeaudio.ui.theme.DarkBackground)
            .windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = com.easeaudio.ui.theme.DarkBackground,
        tonalElevation = NavigationBarDefaults.Elevation
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            val localizedTitle = stringResource(item.titleRes)
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
                    selectedIconColor = NeonCyan,
                    selectedTextColor = NeonCyan,
                    indicatorColor = DarkSurfaceVariant,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                )
            )
        }
    }
}
