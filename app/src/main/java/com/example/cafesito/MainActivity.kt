package com.example.cafesito

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cafesito.data.SyncManager
import com.example.cafesito.ui.access.*
import com.example.cafesito.ui.detail.DetailScreen
import com.example.cafesito.ui.profile.FollowersScreen
import com.example.cafesito.ui.profile.FollowingScreen
import com.example.cafesito.ui.profile.ProfileScreen
import com.example.cafesito.ui.search.SearchScreen
import com.example.cafesito.ui.timeline.AddPostScreen
import com.example.cafesito.ui.timeline.TimelineScreen
import com.example.cafesito.ui.theme.CafesitoTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val sessionViewModel: SessionViewModel by viewModels()

    @Inject
    lateinit var syncManager: SyncManager // INYECTADO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CafesitoTheme {
                val sessionState by sessionViewModel.sessionState.collectAsState()
                
                // SINCRONIZACIÓN INICIAL: Se dispara una vez al detectar sesión activa
                LaunchedEffect(sessionState) {
                    if (sessionState is SessionState.Authenticated) {
                        syncManager.syncAll()
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (sessionState is SessionState.Loading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val startRoute = when (sessionState) {
                            is SessionState.Authenticated -> "timeline"
                            is SessionState.Registered -> "completeProfile"
                            else -> "onboarding"
                        }
                        AppNavigation(
                            startRoute = startRoute, 
                            onProfileFinished = { 
                                sessionViewModel.refreshSession()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(startRoute: String, onProfileFinished: () -> Unit) {
    val navController = rememberNavController()
    val mainScreens = listOf("timeline", "search", "profile")

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val currentRoute = currentDestination?.route?.split("?")?.firstOrNull()?.split("/")?.firstOrNull()

            if (currentRoute in mainScreens) {
                NavigationBar {
                    val navItems = listOf(
                        Triple("timeline", "Inicio", Icons.Filled.Home),
                        Triple("search", "Explorar", Icons.Filled.Coffee),
                        Triple("profile", "Perfil", Icons.Filled.Person)
                    )

                    navItems.forEach { (route, label, icon) ->
                        val fullRoute = if (route == "profile") "profile/0" else route
                        val isSelected = currentDestination?.hierarchy?.any { it.route?.startsWith(route) == true } == true

                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(fullRoute) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
            startDestination = startRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("onboarding") {
                OnboardingScreen(onFinished = { 
                    navController.navigate("login") { popUpTo("onboarding") { inclusive = true } }
                })
            }
            
            composable("login") {
                LoginScreen(onLoginSuccess = { googleId, email, name, photo ->
                    val encodedEmail = Uri.encode(email)
                    val encodedName = Uri.encode(name)
                    val encodedPhoto = Uri.encode(photo)
                    navController.navigate("completeProfile?googleId=$googleId&email=$encodedEmail&name=$encodedName&photoUrl=$encodedPhoto") {
                        popUpTo("login") { inclusive = true }
                    }
                })
            }

            composable(
                route = "completeProfile?googleId={googleId}&email={email}&name={name}&photoUrl={photoUrl}",
                arguments = listOf(
                    navArgument("googleId") { defaultValue = "" },
                    navArgument("email") { defaultValue = "" },
                    navArgument("name") { defaultValue = "" },
                    navArgument("photoUrl") { defaultValue = "" }
                )
            ) { backStackEntry ->
                CompleteProfileScreen(
                    googleId = backStackEntry.arguments?.getString("googleId") ?: "",
                    userEmail = backStackEntry.arguments?.getString("email") ?: "",
                    initialName = backStackEntry.arguments?.getString("name") ?: "",
                    initialPhoto = backStackEntry.arguments?.getString("photoUrl") ?: "",
                    onSuccess = {
                        onProfileFinished()
                        navController.navigate("timeline") { popUpTo("completeProfile") { inclusive = true } }
                    }
                )
            }

            composable("timeline") {
                TimelineScreen(
                    onUserClick = { id -> navController.navigate("profile/$id") },
                    onCoffeeClick = { id -> navController.navigate("detail/$id") },
                    onAddPostClick = { navController.navigate("addPost") }
                )
            }
            
            composable("search") { SearchScreen(onCoffeeClick = { id -> navController.navigate("detail/$id") }, onProfileClick = { id -> navController.navigate("profile/$id") }) }
            
            composable(
                route = "profile/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.IntType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getInt("userId") ?: 0
                ProfileScreen(
                    onBackClick = { navController.popBackStack() },
                    onUserClick = { id -> navController.navigate("profile/$id") },
                    onCoffeeClick = { id -> navController.navigate("detail/$id") },
                    onFollowersClick = { id -> navController.navigate("profile/$id/followers") },
                    onFollowingClick = { id -> navController.navigate("profile/$id/following") }
                )
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
            
            composable(route = "detail/{coffeeId}", arguments = listOf(navArgument("coffeeId") { type = NavType.StringType })) { DetailScreen(onBackClick = { navController.popBackStack() }) }
            composable("addPost") { AddPostScreen(onBackClick = { navController.popBackStack() }) }
        }
    }
}
