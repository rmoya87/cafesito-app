package com.cafesito.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import com.cafesito.app.data.UserRepository
import com.cafesito.app.ui.access.*
import com.cafesito.app.ui.brewlab.*
import com.cafesito.app.ui.detail.DetailScreen
import com.cafesito.app.ui.diary.*
import com.cafesito.app.ui.profile.*
import com.cafesito.app.ui.search.SearchScreen
import com.cafesito.app.ui.theme.*
import com.cafesito.app.ui.timeline.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.hilt.navigation.compose.hiltViewModel
import android.graphics.Color as AndroidColor

import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import android.content.Intent
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val sessionViewModel: SessionViewModel by viewModels()

    @Inject
    lateinit var syncManager: SyncManager
    
    @Inject
    lateinit var userRepository: UserRepository
    
    private val notificationNavigation = mutableStateOf<NotificationNavigation?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupCoil()
        enableEdgeToEdgeConfig()

        notificationNavigation.value = NotificationNavigation.fromIntent(intent)

        setContent {
            CafesitoTheme {
                val sessionState by sessionViewModel.sessionState.collectAsState()

                LaunchedEffect(sessionState) {
                    if (sessionState is SessionState.Authenticated) {
                        syncManager.syncAll()
                        updateFcmToken()
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation(
                        sessionState = sessionState,
                        userRepository = userRepository,
                        notificationNavigation = notificationNavigation.value,
                        onNotificationConsumed = { notificationNavigation.value = null }
                    )
                }
            }
        }
    }

    private fun updateFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            lifecycleScope.launch {
                userRepository.updateFcmToken(token)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationNavigation.value = NotificationNavigation.fromIntent(intent)
    }

    private fun setupCoil() {
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
            .diskCache { DiskCache.Builder().directory(cacheDir.resolve("image_cache")).maxSizeBytes(50 * 1024 * 1024).build() }
            .respectCacheHeaders(false)
            .build()
        Coil.setImageLoader(imageLoader)
    }

    private fun enableEdgeToEdgeConfig() {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    sessionState: SessionState,
    userRepository: UserRepository,
    notificationNavigation: NotificationNavigation?,
    onNotificationConsumed: () -> Unit
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    var startRoute by rememberSaveable { mutableStateOf<String?>(null) }
    
    // IMPORTANTE: Usamos 'remember' (NO saveable). Esto se reseteará a 'false' si el sistema mata la actividad.
    // Solo redirigiremos al login si el usuario estuvo autenticado EN ESTA EJECUCIÓN ESPECÍFICA y luego deja de estarlo.
    var wasAuthenticatedInCurrentInstance by remember { mutableStateOf(false) }

    LaunchedEffect(sessionState) {
        when (sessionState) {
            is SessionState.Authenticated -> {
                wasAuthenticatedInCurrentInstance = true
                if (startRoute == null) {
                    startRoute = "timeline"
                }
            }
            is SessionState.NotAuthenticated -> {
                if (startRoute == null) {
                    startRoute = "login"
                } else if (wasAuthenticatedInCurrentInstance) {
                    // Solo redirigimos al login si ANTES estuvimos autenticados (Logout real o expiración de sesión)
                    wasAuthenticatedInCurrentInstance = false
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> {} 
        }
    }

    val finalStartRoute = startRoute
    if (finalStartRoute == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LaunchedEffect(notificationNavigation) {
        val nav = notificationNavigation ?: return@LaunchedEffect
        when (nav.type) {
            "FOLLOW" -> nav.targetId?.let { navController.navigate("profile/$it") }
            "MENTION", "COMMENT" -> nav.targetId?.let { navController.navigate("timeline?postId=$it") }
        }
        onNotificationConsumed()
    }
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""
    val navItems = remember {
        listOf(
            Triple("timeline", "Inicio", Icons.Filled.Home),
            Triple("search", "Explorar", Icons.Filled.Explore),
            Triple("brewlab", "Elabora", Icons.Filled.Science),
            Triple("diary", "Diario", Icons.Filled.Book),
            Triple("profile", "Perfil", Icons.Filled.Person)
        )
    }

    val state = remember(currentRoute) {
        val mainScreens = listOf("timeline", "search", "brewlab", "diary", "profile")
        val userIdArg = navBackStackEntry?.arguments?.getInt("userId") ?: 0
        val isOther = currentRoute.startsWith("profile/") && userIdArg != 0
        val isFollow = currentRoute.contains("/followers") || currentRoute.contains("/following") || currentRoute == "searchUsers"
        val isNotifications = currentRoute == "notifications"
        val showBottom = mainScreens.any { currentRoute.startsWith(it) } && !isOther && !isFollow && !isNotifications
        Triple(showBottom, isOther, isFollow)
    }

    val shouldShowBottomBar = state.first

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = finalStartRoute,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable("login") {
                LoginScreen(onLoginSuccess = { googleId, email, name, photo, isNewUser ->
                    if (!isNewUser) {
                        navController.navigate("timeline") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        val encodedEmail = Uri.encode(email)
                        val encodedName = Uri.encode(name)
                        val encodedPhoto = Uri.encode(photo)
                        navController.navigate("completeProfile?googleId=$googleId&email=$encodedEmail&name=$encodedName&photoUrl=$encodedPhoto") {
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
                        navController.navigate("timeline") { popUpTo("completeProfile") { inclusive = true } }
                    }
                )
            }

            composable(
                route = "timeline?postId={postId}",
                arguments = listOf(
                    navArgument("postId") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId")
                TimelineScreen(
                    onUserClick = { id -> 
                        if (id == 0) {
                            navController.navigate("profile/0") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            navController.navigate("profile/$id")
                        }
                    },
                    onCoffeeClick = { id -> navController.navigate("detail/$id") },
                    onAddPostClick = { navController.navigate("addPost") },
                    onSearchUsersClick = { navController.navigate("searchUsers") },
                    onNotificationsClick = { navController.navigate("notifications") },
                    initialPostId = postId
                )
            }

            composable(
                route = "notifications",
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300)
                    )
                }
            ) {
                val viewModel: TimelineViewModel = hiltViewModel()
                val notifications by viewModel.notifications.collectAsState()
                val unreadIds by viewModel.unreadNotificationIds.collectAsState()
                val uiState by viewModel.uiState.collectAsState()
                val followingIds = (uiState as? TimelineUiState.Success)?.myFollowingIds ?: emptySet()
                val isRefreshing by viewModel.isRefreshing.collectAsState()

                NotificationsScreen(
                    notifications = notifications,
                    unreadIds = unreadIds,
                    followingIds = followingIds,
                    onBackClick = { navController.popBackStack() },
                    onMarkAllAsRead = { viewModel.markAllAsRead() },
                    onFollowToggle = { userId -> viewModel.toggleFollowSuggestion(userId) },
                    onDeleteNotification = { notification -> viewModel.deleteNotification(notification) },
                    onNotificationClick = { notification ->
                        viewModel.markNotificationRead(notification)
                        when (notification) {
                            is TimelineNotification.Follow -> {
                                navController.navigate("profile/${notification.user.id}")
                            }
                            is TimelineNotification.Mention -> {
                                navController.navigate("timeline?postId=${notification.postId}")
                            }
                        }
                    },
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refreshData() }
                )
            }

            composable("searchUsers") {
                SearchUsersScreen(
                    onBackClick = { navController.popBackStack() },
                    onUserClick = { id -> 
                        if (id == 0) {
                            navController.navigate("profile/0") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            navController.navigate("profile/$id")
                        }
                    }
                )
            }

            composable("search") {
                SearchScreen(
                    onCoffeeClick = { id -> navController.navigate("detail/$id") },
                    onProfileClick = { id -> 
                        if (id == 0) {
                            navController.navigate("profile/0") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            navController.navigate("profile/$id")
                        }
                    }
                )
            }

            composable("brewlab") {
                BrewLabScreen(
                    onNavigateToDiary = {
                        navController.navigate("diary") {
                            popUpTo("brewlab") { inclusive = false }
                        }
                    },
                    onAddCoffeeClick = { navController.navigate("addStock?origin=brewlab") }
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

            composable(
                route = "addStock?origin={origin}",
                arguments = listOf(navArgument("origin") { defaultValue = "" })
            ) { backStackEntry ->
                val origin = backStackEntry.arguments?.getString("origin") ?: ""
                AddStockScreen(
                    onBackClick = { navController.popBackStack() },
                    onAddCustomClick = { navController.navigate("addPantryItem?onlyActivity=true&origin=$origin") },
                    onSuccess = {
                        if (origin == "brewlab") {
                            navController.popBackStack()
                        } else {
                            navController.navigate("diary?navigateTo=pantry") {
                                popUpTo("diary") { inclusive = true }
                            }
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
                route = "addPantryItem?onlyActivity={onlyActivity}&origin={origin}",
                arguments = listOf(
                    navArgument("onlyActivity") { type = NavType.BoolType; defaultValue = false },
                    navArgument("origin") { defaultValue = "" }
                )
            ) { backStackEntry ->
                val onlyActivity = backStackEntry.arguments?.getBoolean("onlyActivity") ?: false
                val origin = backStackEntry.arguments?.getString("origin") ?: ""
                AddPantryItemScreen(
                    onlyActivity = onlyActivity,
                    onBackClick = { navigateTo ->
                        if (origin == "brewlab" && navigateTo != null) {
                            navController.popBackStack("brewlab", inclusive = false)
                        } else if (navigateTo != null) {
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
                    onUserClick = { id -> 
                        if (id == 0) {
                            navController.navigate("profile/0") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            navController.navigate("profile/$id")
                        }
                    },
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
                    onUserClick = { id -> 
                        if (id == 0) {
                            navController.navigate("profile/0") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            navController.navigate("profile/$id")
                        }
                    }
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
                    onUserClick = { id -> 
                        if (id == 0) {
                            navController.navigate("profile/0") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            navController.navigate("profile/$id")
                        }
                    }
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

data class NotificationNavigation(
    val type: String,
    val targetId: String?
) {
    companion object {
        fun fromIntent(intent: Intent?): NotificationNavigation? {
            val type = intent?.getStringExtra("nav_type") ?: return null
            val targetId = intent?.getStringExtra("nav_id")
            return NotificationNavigation(type, targetId)
        }
    }
}
