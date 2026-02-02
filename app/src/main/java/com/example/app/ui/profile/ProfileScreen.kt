package com.cafesito.app.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    val postsListState = rememberLazyListState()
    val adnListState = rememberLazyListState()
    val favoritesListState = rememberLazyListState()
    val reviewsListState = rememberLazyListState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCommentSheetId by remember { mutableStateOf<String?>(null) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showSensoryDetail by remember { mutableStateOf(false) }

    var postToEdit by remember { mutableStateOf<PostWithDetails?>(null) }
    var reviewToEdit by remember { mutableStateOf<UserReviewInfo?>(null) }
    var itemToDelete by remember { mutableStateOf<Any?>(null) }

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
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    item { ProfileHeaderShimmer() }
                    items(3) { PostCardShimmer() }
                }
            }
            is ProfileUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(state.message, color = MaterialTheme.colorScheme.onSurface) }
            ProfileUiState.LoggedOut -> {
                // Dejamos que la redirección global en MainActivity maneje la navegación a Login
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

                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp)
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
                                        username = username,
                                        fullName = fullName,
                                        bio = bio,
                                        onUsernameChange = { username = it },
                                        onFullNameChange = { fullName = it },
                                        onBioChange = { bio = it },
                                        onSave = { viewModel.onSaveProfile(username, fullName, bio, email) },
                                        usernameError = state.usernameError
                                    )
                                } else {
                                    Text(
                                        text = state.user.fullName,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "@${state.user.username}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (bio.isNotBlank()) {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = bio,
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(24.dp))

                                ProfileStatsRow(
                                    posts = state.posts.size,
                                    followers = state.followers,
                                    following = state.following,
                                    onFollowersClick = { onFollowersClick(state.user.id) },
                                    onFollowingClick = { onFollowingClick(state.user.id) }
                                )

                                if (!state.isCurrentUser) {
                                    Spacer(Modifier.height(24.dp))
                                    FollowButton(
                                        isFollowing = state.isFollowing,
                                        onClick = { viewModel.toggleFollow() }
                                    )
                                }
                            }
                        }

                        stickyHeader {
                            PremiumTabRow(
                                selectedTabIndex = pagerState.currentPage,
                                tabs = tabs,
                                onTabSelected = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(it) }
                                }
                            )
                        }

                        item {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillParentMaxHeight(),
                                verticalAlignment = Alignment.Top,
                            ) {
                                when (it) {
                                    0 -> ProfilePosts(
                                        posts = state.posts,
                                        isCurrentUser = state.isCurrentUser,
                                        activeUser = state.activeUser,
                                        viewModel = viewModel,
                                        onUserClick = onUserClick,
                                        listState = postsListState,
                                        onCommentClick = { c -> showCommentSheetId = c },
                                        onEditClick = { post -> postToEdit = post },
                                        onDeleteClick = { post -> itemToDelete = post }
                                    )
                                    1 -> ProfileAdn(state, listState = adnListState) { showSensoryDetail = true }
                                    2 -> ProfileFavorites(state.favoriteCoffees, viewModel, onCoffeeClick, listState = favoritesListState)
                                    3 -> ProfileReviews(
                                        userReviews = state.userReviews,
                                        isCurrentUser = state.isCurrentUser,
                                        onCoffeeClick = onCoffeeClick,
                                        listState = reviewsListState,
                                        onEditClick = { review -> reviewToEdit = review },
                                        onDeleteClick = { review -> itemToDelete = review }
                                    )
                                }
                            }
                        }
                    }
                }

                // Modals & Dialogs
                if (showSensoryDetail) {
                    SensoryDetailBottomSheet(
                        profile = state.sensoryProfile,
                        onDismiss = { showSensoryDetail = false }
                    )
                }

                if (showSettingsSheet) {
                    SettingsBottomSheet(
                        onDismiss = { showSettingsSheet = false },
                        onEditClick = { viewModel.toggleEditMode() },
                        onLogoutClick = { viewModel.logout() },
                        isHealthConnectAvailable = state.isHealthConnectAvailable,
                        healthConnectEnabled = state.healthConnectEnabled,
                        onHealthConnectToggle = { enabled ->
                            if (enabled) {
                                coroutineScope.launch {
                                    if (viewModel.healthConnectRepository.hasPermissions()) {
                                        viewModel.onToggleHealthConnect(true)
                                    } else {
                                        requestPermissionLauncher.launch(viewModel.healthConnectRepository.permissions)
                                    }
                                }
                            } else {
                                viewModel.onToggleHealthConnect(false)
                            }
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
                        initialText = details.post.comment,
                        initialImage = details.post.imageUrl,
                        onDismiss = { postToEdit = null },
                        onConfirm = { newText, newImageUrl ->
                            viewModel.updatePost(details.post.id, newText, newImageUrl)
                            postToEdit = null
                        }
                    )
                }

                reviewToEdit?.let { info ->
                    EditReviewBottomSheet(
                        initialRating = info.review.rating,
                        initialComment = info.review.comment,
                        initialImage = info.review.imageUrl,
                        onDismiss = { reviewToEdit = null },
                        onConfirm = { rating, comment, imageUrl ->
                            viewModel.updateReview(info.coffeeDetails.coffee.id, rating, comment, imageUrl)
                            reviewToEdit = null
                        }
                    )
                }

                showCommentSheetId?.let { id ->
                    CommentsSheet(
                        postId = id,
                        onDismiss = { showCommentSheetId = null },
                        onAddComment = { text -> viewModel.onAddComment(id, text) },
                        onNavigateToProfile = { userId ->
                            showCommentSheetId = null
                            onUserClick(userId)
                        }
                    )
                }
            }
        }
    }
}
