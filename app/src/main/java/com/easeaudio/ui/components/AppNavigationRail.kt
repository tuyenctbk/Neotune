package com.easeaudio.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.scale
import com.easeaudio.ui.theme.DarkSurfaceVariant
import com.easeaudio.ui.theme.NeonCyan
import com.easeaudio.ui.theme.NeonPurple
import com.easeaudio.ui.theme.TextMuted

@Composable
fun AppNavigationRail(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(NavRoute.Radio, NavRoute.Podcast, NavRoute.Favorites, NavRoute.Settings)

    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .width(80.dp),
        containerColor = com.easeaudio.ui.theme.DarkBackground,
        contentColor = TextMuted,
        header = {
            Spacer(modifier = Modifier.height(24.dp))
        }
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route || (item.route == "radio" && currentRoute == "home")
                val localizedTitle = stringResource(item.titleRes)
                val selectedAccent = if (item.route == "podcast") NeonPurple else NeonCyan
                var isFocused by remember { mutableStateOf(false) }
                
                NavigationRailItem(
                    modifier = Modifier
                        .onFocusChanged { isFocused = it.isFocused }
                        .scale(if (isFocused) 1.1f else 1.0f),
                    selected = isSelected,
                    onClick = { onNavigate(item.route) },
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
                        indicatorColor = if (isFocused) selectedAccent.copy(alpha = 0.25f) else DarkSurfaceVariant,
                        unselectedIconColor = if (isFocused) selectedAccent else TextMuted,
                        unselectedTextColor = if (isFocused) selectedAccent else TextMuted
                    )
                )
            }
        }
    }
}
