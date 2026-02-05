package com.cafesito.app.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafesito.app.data.PostWithDetails
import com.cafesito.app.data.UserReviewInfo
import com.cafesito.app.ui.components.*
import com.cafesito.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onUserClick: (Int) -> Unit,
    onCoffeeClick: (String) -> Unit,
    onFollowersClick: (Int) -> Unit,
    onFollowingClick: (Int) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("POSTS", "ADN", "FAVORITOS", "RESEÑAS")
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCommentSheetId by remember { mutableStateOf<String?>(null) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showSensoryDetail by remember { mutableStateOf(false) }

    var postToEdit by remember { mutableStateOf<PostWithDetails?>(null) }
    var reviewToEdit by remember { mutableStateOf<UserReviewInfo?>(null) }
    var itemToDelete by remember { mutableStateOf<Any?>(null) }

    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.refreshData() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GlassyTopBar(
                title = "PERFIL",
                onBackClick = if ((uiState as? ProfileUiState.Success)?.isCurrentUser == false) onBackClick else null,
                scrollBehavior = scrollBehavior,
                actions = {
                    val state = uiState as? ProfileUiState.Success
                    if (state?.isCurrentUser == true) {
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Default.Tune, contentDescription = "Ajustes", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.refreshData()
                coroutineScope.launch {
                    delay(400)
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { ProfileHeaderShimmer() }
                    items(3) { PostCardShimmer() }
                }
            }
            is ProfileUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(state.message, color = MaterialTheme.colorScheme.onSurface) }
            ProfileUiState.LoggedOut -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            }
            is ProfileUiState.Success -> {
                var username by remember { mutableStateOf(state.user.username) }
                var fullName by remember { mutableStateOf(state.user.fullName) }
                var bio by remember { mutableStateOf(state.user.bio ?: "") }
                var email by remember { mutableStateOf(state.user.email) }

                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { viewModel.onAvatarChange(it) }
                
                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    PermissionController.createRequestPermissionResultContract()
                ) { granted ->
                    if (granted.containsAll(viewModel.healthConnectRepository.permissions)) {
                        viewModel.onToggleHealthConnect(true)
                    } else {
                        viewModel.onToggleHealthConnect(false)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ModernAvatar(imageUrl = state.user.avatarUrl, size = 110.dp)

                            if (state.isEditing) {
                                TextButton(onClick = { launcher.launch("image/*") }) {
                                    Text("CAMBIAR FOTO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            if (state.isEditing) {
                                EditProfileFields(
                                    username = username, fullName = fullName, bio = bio,
                                    onUsernameChange = { username = it }, onFullNameChange = { fullName = it },
                                    onBioChange = { bio = it }, onSave = { viewModel.onSaveProfile(username, fullName, bio, email) },
                                    usernameError = state.usernameError
                                )
                            } else {
                                Text(text = state.user.fullName, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = "@${state.user.username}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                if (bio.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(text = bio, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            ProfileStatsRow(
                                posts = state.posts.size, followers = state.followers, following = state.following,
                                onFollowersClick = { onFollowersClick(state.user.id) },
                                onFollowingClick = { onFollowingClick(state.user.id) }
                            )

                            if (!state.isCurrentUser) {
                                Spacer(Modifier.height(24.dp))
                                FollowButton(isFollowing = state.isFollowing, onClick = { viewModel.toggleFollow() })
                            }
                        }
                    }

                    stickyHeader {
                        PremiumTabRow(
                            selectedTabIndex = selectedTabIndex,
                            tabs = tabs,
                            onTabSelected = { selectedTabIndex = it }
                        )
                    }

                    when (selectedTabIndex) {
                        0 -> {
                            if (state.posts.isEmpty()) {
                                item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { Text("Aún no hay publicaciones", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                            } else {
                                items(state.posts, key = { it.post.id }) { post ->
                                    Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        PostCard(
                                            details = post,
                                            isLiked = post.likes.any { it.userId == (state.activeUser?.id ?: -1) },
                                            onLikeClick = { viewModel.onToggleLike(post.post.id) },
                                            onCommentClick = { showCommentSheetId = post.post.id },
                                            onUserClick = { onUserClick(post.post.userId) },
                                            onEditClick = { postToEdit = post },
                                            onDeleteClick = { itemToDelete = post },
                                            isOwnPost = state.isCurrentUser
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            item {
                                Column(Modifier.padding(24.dp)) {
                                    Box(Modifier.clickable { showSensoryDetail = true }) {
                                        PremiumCard {
                                            Column(Modifier.padding(24.dp)) {
                                                Text("PERFIL SENSORIAL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                                Spacer(Modifier.height(16.dp))
                                                SensoryRadarChart(data = state.sensoryProfile, modifier = Modifier.fillMaxWidth().height(200.dp))
                                                Spacer(Modifier.height(16.dp))
                                                Text("Tus gustos basados en tus cafés favoritos y reseñas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            if (state.favoriteCoffees.isEmpty()) {
                                item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { Text("No hay cafés favoritos", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                            } else {
                                // Listado sin corazón; quitar de favoritos deslizando de derecha a izquierda
                                items(state.favoriteCoffees, key = { it.coffee.id }) { coffee ->
                                    Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        SwipeableFavoriteItem(
                                            coffeeDetails = coffee,
                                            onRemoveFromFavorites = { viewModel.onToggleFavorite(coffee.coffee.id, false) },
                                            onClick = { onCoffeeClick(coffee.coffee.id) }
                                        )
                                    }
                                }
                            }
                        }
                        3 -> {
                            if (state.userReviews.isEmpty()) {
                                item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { Text("No has escrito reseñas aún", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                            } else {
                                items(state.userReviews, key = { it.review.id }) { review ->
                                    Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        UserReviewCard(
                                            info = review,
                                            isOwnReview = state.isCurrentUser,
                                            onEditClick = { reviewToEdit = review },
                                            onDeleteClick = { itemToDelete = review },
                                            onClick = { onCoffeeClick(review.coffeeDetails.coffee.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Modals
                if (showSensoryDetail) {
                    SensoryDetailBottomSheet(profile = state.sensoryProfile, onDismiss = { showSensoryDetail = false })
                }

                if (showSettingsSheet) {
                    SettingsBottomSheet(
                        onDismiss = { showSettingsSheet = false },
                        onEditClick = { viewModel.toggleEditMode() },
                        onLogoutClick = { viewModel.logout() },
                        isHealthConnectAvailable = state.isHealthConnectAvailable,
                        healthConnectEnabled = state.healthConnectEnabled,
                        onHealthConnectToggle = { enabled ->
                            if (enabled) requestPermissionLauncher.launch(viewModel.healthConnectRepository.permissions)
                            else viewModel.onToggleHealthConnect(false)
                        }
                    )
                }

                itemToDelete?.let { item ->
                    DeleteConfirmationDialog(
                        onDismissRequest = { itemToDelete = null },
                        title = "Borrar",
                        text = "Una vez borrado no se puede recuperar. ¿Estás seguro?",
                        onConfirm = {
                            if (item is PostWithDetails) viewModel.deletePost(item.post.id)
                            else if (item is UserReviewInfo) viewModel.deleteReview(item.coffeeDetails.coffee.id)
                            itemToDelete = null
                        }
                    )
                }

                postToEdit?.let { details ->
                    EditPostBottomSheet(
                        initialText = details.post.comment, initialImage = details.post.imageUrl,
                        onDismiss = { postToEdit = null },
                        onConfirm = { newText, newImageUrl ->
                            viewModel.updatePost(details.post.id, newText, newImageUrl)
                            postToEdit = null
                        }
                    )
                }

                reviewToEdit?.let { info ->
                    EditReviewBottomSheet(
                        initialRating = info.review.rating, initialComment = info.review.comment, initialImage = info.review.imageUrl,
                        onDismiss = { reviewToEdit = null },
                        onConfirm = { rating, comment, imageUrl ->
                            viewModel.updateReview(info.coffeeDetails.coffee.id, rating, comment, imageUrl)
                            reviewToEdit = null
                        }
                    )
                }

                showCommentSheetId?.let { id ->
                    CommentsSheet(
                        postId = id, onDismiss = { showCommentSheetId = null },
                        onAddComment = { text -> viewModel.onAddComment(id, text) },
                        onNavigateToProfile = { userId -> showCommentSheetId = null; onUserClick(userId) }
                    )
                }
            }
        }
        }
    }
}
