package com.example.cafesito

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cafesito.domain.currentUser
import com.example.cafesito.ui.detail.DetailScreen
import com.example.cafesito.ui.profile.FollowersScreen
import com.example.cafesito.ui.profile.FollowingScreen
import com.example.cafesito.ui.profile.ProfileScreen
import com.example.cafesito.ui.search.SearchScreen
import com.example.cafesito.ui.timeline.AddPostScreen
import com.example.cafesito.ui.timeline.TimelineScreen
import com.example.cafesito.ui.theme.CafesitoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CafesitoTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navItems = mapOf(
        "timeline" to "Inicio",
        "search" to "Explorar",
        "profile" to "Perfil"
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val currentRoute = currentDestination?.route
            val shouldShowBottomBar = currentRoute?.let { route ->
                val rootSegment = route.split("/").firstOrNull()
                rootSegment in navItems.keys
            } ?: false

            if (shouldShowBottomBar) {
                NavigationBar {
                    navItems.forEach { (screen, label) ->
                        val route = if (screen == "profile") "profile/${currentUser.id}" else screen
                        
                        val isSelected = currentDestination?.hierarchy?.any { 
                            it.route?.split("/")?.firstOrNull() == screen 
                        } == true

                        NavigationBarItem(
                            icon = {
                                when (screen) {
                                    "timeline" -> Icon(Icons.Filled.Home, contentDescription = label)
                                    "search" -> Icon(Icons.Filled.Coffee, contentDescription = label)
                                    "profile" -> Icon(Icons.Filled.Person, contentDescription = label)
                                }
                            },
                            label = { Text(label) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "timeline",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("timeline") {
                TimelineScreen(
                    onUserClick = { userId -> navController.navigate("profile/$userId") },
                    onCoffeeClick = { coffeeId -> navController.navigate("detail/$coffeeId") },
                    onAddPostClick = { navController.navigate("addPost") }
                )
            }
            composable("search") {
                SearchScreen(
                    onCoffeeClick = { coffeeId -> navController.navigate("detail/$coffeeId") },
                    onProfileClick = { userId -> navController.navigate("profile/$userId") }
                )
            }
            composable(
                route = "profile/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.IntType })
            ) {
                ProfileScreen(
                    onBackClick = { navController.popBackStack() },
                    onUserClick = { userId -> navController.navigate("profile/$userId") },
                    onCoffeeClick = { coffeeId -> navController.navigate("detail/$coffeeId") },
                    onFollowersClick = { userId -> navController.navigate("profile/$userId/followers") },
                    onFollowingClick = { userId -> navController.navigate("profile/$userId/following") }
                )
            }
            composable(
                route = "detail/{coffeeId}",
                arguments = listOf(navArgument("coffeeId") { type = NavType.StringType })
            ) {
                DetailScreen(onBackClick = { navController.popBackStack() })
            }
            composable("addPost") {
                // CORRECCIÓN: Vinculamos el popBackStack al callback de la pantalla
                AddPostScreen(onBackClick = { navController.popBackStack() })
            }
            composable(
                route = "profile/{userId}/followers",
                arguments = listOf(navArgument("userId") { type = NavType.IntType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getInt("userId") ?: 0
                FollowersScreen(
                    userId = userId,
                    onBackClick = { navController.popBackStack() },
                    onUserClick = { id -> navController.navigate("profile/$id") }
                )
            }
            composable(
                route = "profile/{userId}/following",
                arguments = listOf(navArgument("userId") { type = NavType.IntType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getInt("userId") ?: 0
                FollowingScreen(
                    userId = userId,
                    onBackClick = { navController.popBackStack() },
                    onUserClick = { id -> navController.navigate("profile/$id") }
                )
            }
        }
    }
}
