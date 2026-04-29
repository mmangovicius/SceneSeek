package com.sceneseek.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(val screen: Screen, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(
        screen = Screen.Home,
        label = "Home",
        icon = Icons.Default.Home,
    ),
    BottomNavItem(
        screen = Screen.Search,
        label = "Search",
        icon = Icons.Default.Search,
    ),
    BottomNavItem(
        screen = Screen.Watchlist,
        label = "Watchlist",
        icon = Icons.Default.Star,
    ),
)
