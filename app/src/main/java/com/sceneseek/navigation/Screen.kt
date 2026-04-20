package com.sceneseek.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Watchlist : Screen("watchlist")
    object Detail : Screen("detail/{mediaId}/{mediaType}") {
        fun createRoute(mediaId: Int, mediaType: String) = "detail/$mediaId/$mediaType"
    }
}
