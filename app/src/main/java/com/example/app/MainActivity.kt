package com.cafesito.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cafesito.app.data.SyncManager
import com.cafesito.app.ui.access.*
import com.cafesito.app.ui.brewlab.*
import com.cafesito.app.ui.detail.DetailScreen
import com.cafesito.app.ui.diary.*
import com.cafesito.app.ui.profile.*
import com.cafesito.app.ui.search.SearchScreen
import com.cafesito.app.ui.theme.*
import com.cafesito.app.ui.timeline.AddPostScreen
import com.cafesito.app.ui.timeline.TimelineScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.hilt.navigation.compose.hiltViewModel
import android.graphics.Color as AndroidColor

import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val sessionViewModel: SessionViewModel by viewModels()

    @Inject
    lateinit var syncManager: SyncManager

    override fun onCreate(savedInstanceState: Bundle?) {

        // ✅ OPTIMIZACIÓN: Configuración Global de Coil
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
        Coil.setImageLoader(imageLoader)

        // ✅ Edge-to-edge REAL: barras del sistema transparentes
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT
            )
        )

        // ✅ Fallback (algunas ROMs/phones lo requieren)
        @Suppress("DEPRECATION")
        window.statusBarColor = AndroidColor.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = AndroidColor.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)

        setContent {
            CafesitoTheme {
                val sessionState by sessionViewModel.sessionState.collectAsState()

                LaunchedEffect(sessionState.userId) {
                    if (sessionState is SessionState.Authenticated) {
                        syncManager.syncAll()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var initialLoadDone by rememberSaveable { mutableStateOf(false) }
                    
                    if (sessionState !is SessionState.Loading) {
                        initialLoadDone = true
                    }

                    if (!initialLoadDone) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val startRoute = rememberSaveable {
                            when (sessionState) {
                                is SessionState.Authenticated -> "timeline"
                                else -> "onboarding"
                            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(startRoute: String, onProfileFinished: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route ?: ""

    // ✅ Redirección global por estado de sesión
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val sessionState by sessionViewModel.sessionState.collectAsState()

    LaunchedEffect(sessionState) {
        if (sessionState is SessionState.NotAuthenticated) {
            val route = navController.currentDestination?.route ?: ""
            if (route != "login" && route != "onboarding" && !route.startsWith("completeProfile")) {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    val state = remember(currentRoute) {
        val mainScreens = listOf("timeline", "search", "brewlab", "diary", "profile")
        val userIdArg = navBackStackEntry?.arguments?.getInt("userId") ?: 0
        val isOther = currentRoute.startsWith("profile/") && userIdArg != 0
        val isFollow = currentRoute.contains("/followers") || currentRoute.contains("/following")
        val showBottom = mainScreens.any { currentRoute.startsWith(it) } && !isOther && !isFollow
        Triple(showBottom, isOther, isFollow)
    }

    val shouldShowBottomBar = state.first

    val navItems = remember {
        listOf(
            Triple("timeline", "Inicio", Icons.Filled.Home),
            Triple("search", "Explorar", Icons.Filled.Explore),
            Triple("brewlab", "Elabora", Icons.Filled.Science),
            Triple("diary", "Diario", Icons.Filled.Book),
            Triple("profile", "Perfil", Icons.Filled.Person)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.fillMaxSize()
        ) {
            composable("onboarding") {
                OnboardingScreen(onFinished = {
                    navController.navigate("login") { popUpTo("onboarding") { inclusive = true } }
                })
            }

            composable("login") {
                LoginScreen(onLoginSuccess = { googleId, email, name, photo, isNewUser ->
                    if (isNewUser) {
                        val encodedEmail = Uri.encode(email)
                        val encodedName = Uri.encode(name)
                        val encodedPhoto = Uri.encode(photo)
                        navController.navigate("completeProfile?googleId=$googleId&email=$encodedEmail&name=$encodedName&photoUrl=$encodedPhoto") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        onProfileFinished()
                        navController.navigate("timeline") {
                            popUpTo("login") { inclusive = true }
                        }
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

            composable("search") {
                SearchScreen(
                    onCoffeeClick = { id -> navController.navigate("detail/$id") },
                    onProfileClick = { id -> navController.navigate("profile/$id") }
                )
            }

            composable("brewlab") {
                BrewLabScreen(
                    onNavigateToDiary = {
                        navController.navigate("diary") {
                            popUpTo("brewlab") { inclusive = false }
                        }
                    }
                )
            }

            composable(
                route = "diary?navigateTo={navigateTo}",
                arguments = listOf(navArgument("navigateTo") { defaultValue = "" })
            ) { backStackEntry ->
                val navigateTo = backStackEntry.arguments?.getString("navigateTo") ?: ""
                DiaryScreen(
                    navigateTo = navigateTo,
                    onCoffeeClick = { id -> navController.navigate("detail/$id") },
                    onAddWaterClick = { navController.navigate("addDiaryEntry?type=WATER") },
                    onAddCoffeeClick = { navController.navigate("addDiaryEntry?type=COFFEE") },
                    onAddStockClick = { navController.navigate("addStock") },
                    onEditStockClick = { id, isCustom ->
                        if (isCustom) navController.navigate("editCustomCoffee/$id")
                        else navController.navigate("editNormalStock/$id")
                    }
                )
            }

            composable("addStock") {
                AddStockScreen(
                    onBackClick = { navController.popBackStack() },
                    onAddCustomClick = { navController.navigate("addPantryItem?onlyActivity=true") },
                    onSuccess = {
                        navController.navigate("diary?navigateTo=pantry") {
                            popUpTo("diary") { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = "editCustomCoffee/{coffeeId}",
                arguments = listOf(navArgument("coffeeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val coffeeId = backStackEntry.arguments?.getString("coffeeId")
                AddPantryItemScreen(
                    coffeeId = coffeeId,
                    onBackClick = { navigateTo ->
                        if (navigateTo != null) {
                            navController.navigate("diary?navigateTo=$navigateTo") {
                                popUpTo("diary") { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable(
                route = "editNormalStock/{coffeeId}",
                arguments = listOf(navArgument("coffeeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val coffeeId = backStackEntry.arguments?.getString("coffeeId") ?: ""
                EditNormalStockScreen(
                    coffeeId = coffeeId,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = "addDiaryEntry?type={type}",
                arguments = listOf(navArgument("type") { defaultValue = "" })
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: ""
                AddDiaryEntryScreen(
                    initialType = type,
                    onBackClick = { navController.popBackStack() },
                    onAddNotFoundClick = { navController.navigate("addPantryItem?onlyActivity=true") }
                )
            }

            composable(
                route = "addPantryItem?onlyActivity={onlyActivity}",
                arguments = listOf(navArgument("onlyActivity") { type = NavType.BoolType; defaultValue = false })
            ) { backStackEntry ->
                val onlyActivity = backStackEntry.arguments?.getBoolean("onlyActivity") ?: false
                AddPantryItemScreen(
                    onlyActivity = onlyActivity,
                    onBackClick = { navigateTo ->
                        if (navigateTo != null) {
                            navController.navigate("diary?navigateTo=$navigateTo") {
                                popUpTo("diary") { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                )
            }

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

            composable(
                route = "detail/{coffeeId}",
                arguments = listOf(navArgument("coffeeId") { type = NavType.StringType })
            ) {
                DetailScreen(onBackClick = { navController.popBackStack() })
            }

            composable("addPost") {
                AddPostScreen(onBackClick = { navController.popBackStack() })
            }
        }

        if (shouldShowBottomBar) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(32.dp),
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        modifier = Modifier.height(64.dp)
                    ) {
                        navItems.forEach { (route, label, icon) ->
                            val isSelected = currentRoute.startsWith(route)

                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) icon else when (label) {
                                            "Inicio" -> Icons.Outlined.Home
                                            "Explorar" -> Icons.Outlined.Explore
                                            "Elabora" -> Icons.Outlined.Science
                                            "Diario" -> Icons.Outlined.Book
                                            "Perfil" -> Icons.Outlined.Person
                                            else -> icon
                                        },
                                        contentDescription = label
                                    )
                                },
                                selected = isSelected,
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color.Transparent,
                                    selectedIconColor = MaterialTheme.colorScheme.secondary,
                                    unselectedIconColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                ),
                                onClick = {
                                    val previousRoute = navController.previousBackStackEntry?.destination?.route
                                    if (route == "profile") {
                                        navController.navigate("profile/0") {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = previousRoute == route
                                        }
                                    } else {
                                        navController.navigate(route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = previousRoute == route
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
