package com.cafesito.app.navigation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafesito.app.data.UserRepository
import com.cafesito.app.ui.components.ModernAvatar
import com.cafesito.app.ui.access.*
import com.cafesito.app.ui.brewlab.*
import com.cafesito.app.ui.detail.DetailScreen
import com.cafesito.app.ui.diary.*
import com.cafesito.app.ui.profile.*
import com.cafesito.app.ui.search.SearchScreen
import com.cafesito.app.ui.timeline.*
import com.cafesito.app.analytics.AnalyticsHelper
import androidx.navigation.NavGraph.Companion.findStartDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    sessionState: SessionState,
    userRepository: UserRepository,
    notificationNavigation: NotificationNavigation?,
    onNotificationConsumed: () -> Unit,
    shortcutAction: String?,
    onShortcutConsumed: () -> Unit,
    analyticsHelper: AnalyticsHelper
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var startRoute by rememberSaveable { mutableStateOf<String?>(null) }
    var wasAuthenticatedInCurrentInstance by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(sessionState) {
        when (sessionState) {
            is SessionState.Authenticated -> {
                wasAuthenticatedInCurrentInstance = true
                if (startRoute == null) startRoute = "timeline"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permission = Manifest.permission.POST_NOTIFICATIONS
                    if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                        notificationPermissionLauncher.launch(permission)
                    }
                }
            }
            is SessionState.NotAuthenticated -> {
                if (startRoute == null) {
                    startRoute = "login"
                } else if (wasAuthenticatedInCurrentInstance) {
                    wasAuthenticatedInCurrentInstance = false
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
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
            "NOTIFICATIONS" -> navController.navigate("notifications")
            "FOLLOW" -> nav.targetId?.let { navController.navigate("profile/$it") }
            "MENTION", "COMMENT" -> nav.targetId?.let { postId ->
                val route = if (nav.commentId != null) "timeline?postId=$postId&commentId=${nav.commentId}" else "timeline?postId=$postId"
                navController.navigate(route)
            }
        }
        onNotificationConsumed()
    }

    LaunchedEffect(shortcutAction) {
        when (shortcutAction) {
            "SEARCH" -> navController.navigate("search")
            "NEW_POST" -> navController.navigate("addPost")
            "BREWLAB" -> navController.navigate("brewlab")
            "DIARY" -> navController.navigate("diary")
            else -> return@LaunchedEffect
        }
        onShortcutConsumed()
    }
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""
    val activeUser by userRepository.getActiveUserFlow().collectAsState(initial = null)

    LaunchedEffect(currentRoute) {
        if (currentRoute.isNotEmpty()) analyticsHelper.trackScreenView(currentRoute)
    }
    
    val navItems = remember {
        listOf(
            Triple("timeline", "Inicio", Icons.Filled.Home),
            Triple("search", "Explorar", Icons.Filled.Explore),
            Triple("brewlab", "Elabora", Icons.Filled.Science),
            Triple("diary", "Diario", Icons.Filled.Book),
            Triple("profile", "Perfil", Icons.Filled.Person)
        )
    }

    val shouldShowBottomBar = remember(currentRoute) {
        val mainScreens = listOf("timeline", "search", "brewlab", "diary", "profile")
        val userIdArg = navBackStackEntry?.arguments?.getInt("userId") ?: 0
        val isOther = currentRoute.startsWith("profile/") && userIdArg != 0
        val isFollow = currentRoute.contains("/followers") || currentRoute.contains("/following") || currentRoute == "searchUsers"
        val isNotifications = currentRoute == "notifications"
        mainScreens.any { currentRoute.startsWith(it) } && !isOther && !isFollow && !isNotifications
    }

    val useNavRail = LocalConfiguration.current.screenWidthDp >= 840

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (shouldShowBottomBar && useNavRail) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight().width(88.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    navItems.forEach { (route, label, icon) ->
                        val isSelected = currentRoute.startsWith(route)
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) return@NavigationRailItem
                                val destination = if (route == "profile") "profile/0" else route
                                navController.navigate(destination) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (route == "profile" && !activeUser?.avatarUrl.isNullOrBlank()) {
                                    ModernAvatar(imageUrl = activeUser?.avatarUrl, size = 24.dp)
                                } else {
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
                                }
                            }
                        )
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = finalStartRoute,
                modifier = Modifier.fillMaxSize().weight(1f)
            ) {
                composable("login") {
                    LoginScreen(onLoginSuccess = { googleId, email, name, photo, isNewUser ->
                        analyticsHelper.trackEvent("login_success", bundleOf("is_new_user" to isNewUser))
                        if (!isNewUser) {
                            navController.navigate("timeline") { popUpTo("login") { inclusive = true } }
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
                            analyticsHelper.trackEvent("profile_completed")
                            navController.navigate("timeline") { popUpTo("completeProfile") { inclusive = true } }
                        }
                    )
                }

                composable(
                    route = "timeline?postId={postId}&commentId={commentId}",
                    arguments = listOf(
                        navArgument("postId") { type = NavType.StringType; nullable = true; defaultValue = null },
                        navArgument("commentId") { type = NavType.IntType; defaultValue = -1 }
                    )
                ) { backStackEntry ->
                    val postId = backStackEntry.arguments?.getString("postId")
                    val commentIdArg = backStackEntry.arguments?.getInt("commentId") ?: -1
                    val commentId = commentIdArg.takeIf { it >= 0 }
                    val publishingPending by backStackEntry.savedStateHandle.getStateFlow("publishing_pending", false).collectAsState()
                    TimelineScreen(
                        onUserClick = { id -> 
                            if (id == 0) {
                                navController.navigate("profile/0") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else navController.navigate("profile/$id")
                        },
                        onCoffeeClick = { id -> navController.navigate("detail/$id") },
                        onAddPostClick = { navController.navigate("addPost") },
                        onSearchUsersClick = { navController.navigate("searchUsers") },
                        onNotificationsClick = { navController.navigate("notifications") },
                        initialPostId = postId,
                        initialCommentId = commentId,
                        publishingPending = publishingPending,
                        onPublishingPendingConsumed = { backStackEntry.savedStateHandle["publishing_pending"] = false }
                    )
                }

                composable("notifications") {
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
                        onReplyToNotification = { notification ->
                            when (notification) {
                                is TimelineNotification.Mention -> {
                                    if (notification.commentId >= 0) {
                                        navController.navigate("timeline?postId=${notification.postId}&commentId=${notification.commentId}")
                                    } else {
                                        navController.navigate("timeline?postId=${notification.postId}")
                                    }
                                }
                                is TimelineNotification.Comment -> navController.navigate("timeline?postId=${notification.postId}&commentId=${notification.commentId}")
                                else -> Unit
                            }
                        },
                        onSavePostFromNotification = { notification -> viewModel.savePostFromNotification(notification) },
                        onNotificationClick = { notification ->
                            viewModel.markNotificationRead(notification)
                            when (notification) {
                                is TimelineNotification.Follow -> navController.navigate("profile/${notification.user.id}")
                                is TimelineNotification.Mention -> {
                                    if (notification.commentId >= 0) {
                                        navController.navigate("timeline?postId=${notification.postId}&commentId=${notification.commentId}")
                                    } else {
                                        navController.navigate("timeline?postId=${notification.postId}")
                                    }
                                }
                                is TimelineNotification.Comment -> navController.navigate("timeline?postId=${notification.postId}&commentId=${notification.commentId}")
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
                            } else navController.navigate("profile/$id")
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
                            } else navController.navigate("profile/$id")
                        }
                    )
                }

                composable("brewlab") { backStackEntry ->
                    val createdCoffeeId by backStackEntry.savedStateHandle.getStateFlow<String?>("brewlab_created_coffee_id", null).collectAsState()
                    BrewLabScreen(
                        onNavigateToDiary = {
                            navController.navigate("diary") { popUpTo("brewlab") { inclusive = false } }
                        },
                        onAddCoffeeClick = { navController.navigate("addPantryItem?origin=brewlab") },
                        createdCoffeeId = createdCoffeeId,
                        onCreatedCoffeeConsumed = { backStackEntry.savedStateHandle["brewlab_created_coffee_id"] = null }
                    )
                }

                composable(
                    route = "diary?navigateTo={navigateTo}",
                    arguments = listOf(navArgument("navigateTo") { defaultValue = "" })
                ) { backStackEntry ->
                    val navigateTo = backStackEntry.arguments?.getString("navigateTo") ?: ""
                    val refreshSignal by backStackEntry.savedStateHandle.getStateFlow<Long?>("diary_refresh_signal", null).collectAsState()
                    val forcePantry by backStackEntry.savedStateHandle.getStateFlow("diary_force_pantry", false).collectAsState()
                    val pendingPantryPlaceholder by backStackEntry.savedStateHandle.getStateFlow("pending_pantry_placeholder", false).collectAsState()
                    DiaryScreen(
                        navigateTo = navigateTo,
                        refreshSignal = refreshSignal,
                        forcePantry = forcePantry,
                        showPendingPantryPlaceholder = pendingPantryPlaceholder,
                        onConsumeForcePantry = { backStackEntry.savedStateHandle["diary_force_pantry"] = false },
                        onConsumePendingPantryPlaceholder = { backStackEntry.savedStateHandle["pending_pantry_placeholder"] = false },
                        onRefreshSignalConsumed = { backStackEntry.savedStateHandle["diary_refresh_signal"] = null },
                        onCoffeeClick = { id -> navController.navigate("detail/$id") },
                        onAddWaterClick = { navController.navigate("addDiaryEntry?type=WATER") },
                        onAddCoffeeClick = { navController.navigate("addDiaryEntry?type=COFFEE") },
                        onAddStockClick = { navController.navigate("addStock") },
                        onEditStockClick = { id, isCustom ->
                            if (isCustom) navController.navigate("editCustomCoffee/$id")
                            else navController.navigate("editNormalStock/$id")
                        },
                        onEditCoffeeClick = { id -> navController.navigate("editCustomCoffee/$id") }
                    )
                }

                composable(
                    route = "addStock?origin={origin}",
                    arguments = listOf(navArgument("origin") { defaultValue = "" })
                ) { backStackEntry ->
                    val origin = backStackEntry.arguments?.getString("origin") ?: ""
                    AddStockScreen(
                        onBackClick = { navController.popBackStack() },
                        onAddCustomClick = { navController.navigate("addPantryItem?onlyActivity=true") },
                        onSuccess = {
                            if (origin == "brewlab") navController.popBackStack()
                            else navController.navigate("diary?navigateTo=pantry") { popUpTo("diary") { inclusive = true } }
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
                                navController.navigate("diary?navigateTo=$navigateTo") { popUpTo("diary") { inclusive = true } }
                            } else navController.popBackStack()
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
                    route = "addDiaryEntry?type={type}&quick={quick}",
                    arguments = listOf(
                        navArgument("type") { defaultValue = "" },
                        navArgument("quick") { type = NavType.BoolType; defaultValue = false }
                    )
                ) { backStackEntry ->
                    val type = backStackEntry.arguments?.getString("type") ?: ""
                    val quick = backStackEntry.arguments?.getBoolean("quick") ?: false
                    val createdCoffeeId by backStackEntry.savedStateHandle.getStateFlow<String?>("diary_created_coffee_id", null).collectAsState()
                    AddDiaryEntryScreen(
                        initialType = type,
                        quickStart = quick,
                        createdCoffeeId = createdCoffeeId,
                        onCreatedCoffeeConsumed = { backStackEntry.savedStateHandle["diary_created_coffee_id"] = null },
                        onBackClick = {
                            navController.previousBackStackEntry?.savedStateHandle?.set("diary_refresh_signal", System.currentTimeMillis())
                            navController.popBackStack()
                        },
                        onAddNotFoundClick = { navController.navigate("addPantryItem?onlyActivity=true&origin=diary_entry") }
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
                        diaryEntryFlow = origin == "diary_entry",
                        brewLabFlow = origin == "brewlab",
                        onCoffeeCreatedForDiary = { id -> navController.previousBackStackEntry?.savedStateHandle?.set("diary_created_coffee_id", id) },
                        onCoffeeCreatedForBrewLab = { id -> navController.previousBackStackEntry?.savedStateHandle?.set("brewlab_created_coffee_id", id) },
                        onBackClick = { navigateTo ->
                            if (origin == "brewlab" && navigateTo != null) {
                                navController.popBackStack("brewlab", inclusive = false)
                            } else if (origin == "diary_entry" && navigateTo == "pantry_loading") {
                                val diaryEntry = navController.getBackStackEntry("diary")
                                diaryEntry.savedStateHandle["diary_force_pantry"] = true
                                diaryEntry.savedStateHandle["pending_pantry_placeholder"] = true
                                navController.popBackStack("diary", inclusive = false)
                            } else if (origin == "diary_entry") {
                                navController.popBackStack()
                            } else if (navigateTo != null) {
                                navController.navigate("diary?navigateTo=$navigateTo") { popUpTo("diary") { inclusive = true } }
                            } else navController.popBackStack()
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
                            } else navController.navigate("profile/$id")
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
                            } else navController.navigate("profile/$id")
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
                            } else navController.navigate("profile/$id")
                        }
                    )
                }

                composable(
                    route = "detail/{coffeeId}",
                    arguments = listOf(navArgument("coffeeId") { type = NavType.StringType })
                ) { DetailScreen(onBackClick = { navController.popBackStack() }) }

                composable(
                    route = "addPost?postType={postType}",
                    arguments = listOf(navArgument("postType") { type = NavType.StringType; defaultValue = "PUBLICATION" })
                ) { backStackEntry ->
                    val postTypeArg = backStackEntry.arguments?.getString("postType")
                    AddPostScreen(
                        onBackClick = { navController.popBackStack() },
                        onPublishSuccess = { navController.previousBackStackEntry?.savedStateHandle?.set("publishing_pending", true) },
                        initialPostType = if (postTypeArg.equals("OPINION", ignoreCase = true)) PostType.OPINION else PostType.PUBLICATION
                    )
                }
            }
        }

        if (shouldShowBottomBar && !useNavRail) {
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
                                    if (route == "profile" && !activeUser?.avatarUrl.isNullOrBlank()) {
                                        ModernAvatar(imageUrl = activeUser?.avatarUrl, size = 24.dp)
                                    } else {
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
                                    }
                                },
                                selected = isSelected,
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color.Transparent,
                                    selectedIconColor = MaterialTheme.colorScheme.secondary,
                                    unselectedIconColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                ),
                                onClick = {
                                    if (isSelected) return@NavigationBarItem
                                    val destination = if (route == "profile") "profile/0" else route
                                    navController.navigate(destination) {
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
        }
    }
}

data class NotificationNavigation(
    val type: String,
    val targetId: String?,
    val commentId: Int? = null
) {
    companion object {
        fun fromIntent(intent: Intent?): NotificationNavigation? {
            val navType = intent?.getStringExtra("nav_type")
            val timelineType = intent?.getStringExtra(TimelineNotificationSystem.EXTRA_TYPE)?.uppercase()
            val timelinePostId = intent?.getStringExtra(TimelineNotificationSystem.EXTRA_POST_ID)
            val timelineCommentId = intent?.getIntExtra(TimelineNotificationSystem.EXTRA_COMMENT_ID, -1)?.takeIf { it >= 0 }
            val targetId = intent?.getStringExtra("nav_id") ?: timelinePostId
            val commentId = intent?.getIntExtra("nav_comment_id", -1)?.takeIf { it >= 0 } ?: timelineCommentId

            val type = when {
                !navType.isNullOrBlank() -> navType.uppercase()
                timelineType == "FOLLOW" -> "FOLLOW"
                timelineType == "MENTION" -> "MENTION"
                timelineType == "COMMENT" -> "COMMENT"
                timelineType != null -> "NOTIFICATIONS"
                else -> null
            }
            return type?.let { NotificationNavigation(it, targetId, commentId) }
        }
    }
}
