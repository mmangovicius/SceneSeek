package com.sceneseek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sceneseek.feature.detail.presentation.DetailScreen
import com.sceneseek.feature.home.presentation.HomeScreen
import com.sceneseek.feature.search.presentation.SearchScreen
import com.sceneseek.feature.watchlist.presentation.WatchlistScreen
import com.sceneseek.navigation.Screen
import com.sceneseek.navigation.bottomNavItems
import com.sceneseek.uicore.theme.SceneSeekTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SceneSeekTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            bottomNavItems.forEach { item ->
                                NavigationBarItem(
                                    selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                                    onClick = {
                                        navController.navigate(item.screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                onNavigateToDetail = { id, type ->
                                    navController.navigate(Screen.Detail.createRoute(id, type))
                                }
                            )
                        }
                        composable(Screen.Search.route) {
                            SearchScreen(
                                onNavigateToDetail = { id, type ->
                                    navController.navigate(Screen.Detail.createRoute(id, type))
                                }
                            )
                        }
                        composable(Screen.Watchlist.route) {
                            WatchlistScreen(
                                onNavigateToDetail = { id, type ->
                                    navController.navigate(Screen.Detail.createRoute(id, type))
                                }
                            )
                        }
                        composable(
                            route = Screen.Detail.route,
                            arguments = listOf(
                                androidx.navigation.navArgument("mediaId") { type = androidx.navigation.NavType.IntType },
                                androidx.navigation.navArgument("mediaType") { type = androidx.navigation.NavType.StringType },
                            ),
                        ) {
                            DetailScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToDetail = { id, type ->
                                    navController.navigate(Screen.Detail.createRoute(id, type))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
