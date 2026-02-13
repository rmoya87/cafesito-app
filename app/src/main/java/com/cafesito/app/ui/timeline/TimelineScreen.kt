package com.cafesito.app.ui.timeline

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import android.Manifest
import android.os.Build
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafesito.app.data.PostWithDetails
import com.cafesito.app.ui.components.*
import kotlin.math.max
import kotlin.random.Random
import com.cafesito.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onUserClick: (Int) -> Unit,
    onCoffeeClick: (String) -> Unit,
    onAddPostClick: () -> Unit,
    onSearchUsersClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    initialPostId: String? = null,
    initialCommentId: Int? = null,
    publishingPending: Boolean = false,
    onPublishingPendingConsumed: () -> Unit = {},
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isPublishingContent by viewModel.isPublishingContent.collectAsState()
    
    var showCommentSheetId by remember { mutableStateOf<String?>(null) }
    var highlightedCommentId by remember { mutableStateOf<Int?>(null) }
    var hasHandledInitialDeepLink by rememberSaveable { mutableStateOf(false) }
    
    val unreadCount by viewModel.unreadCount.collectAsState()
    val newDeviceNotifications by viewModel.newUnreadNotifications.collectAsState()
    val context = LocalContext.current
    
    val listState = rememberLazyListState()
    var showReviewOptions by remember { mutableStateOf<TimelineItem.ReviewItem?>(null) }

    var postToEdit by remember { mutableStateOf<PostWithDetails?>(null) }
    var reviewToEdit by remember { mutableStateOf<TimelineItem.ReviewItem?>(null) }
    var itemToDelete by remember { mutableStateOf<Any?>(null) }

    val suggestionIndices = remember(uiState) {
        val state = uiState as? TimelineUiState.Success ?: return@remember emptyList()
        val itemsCount = state.items.size
        if (itemsCount < 3) return@remember emptyList()
        val random = Random(max(itemsCount, 1) + state.activeUser.id)
        val first = random.nextInt(1, itemsCount)
        var second = random.nextInt(1, itemsCount)
        if (second == first) second = (second + 1).coerceAtMost(itemsCount - 1)
        listOf(first, second)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        viewModel.refreshData()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }

    LaunchedEffect(publishingPending) {
        if (publishingPending) {
            viewModel.startPublishingContent()
            onPublishingPendingConsumed()
        }
    }

    var lastItemCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(uiState) {
        val currentState = uiState
        if (currentState is TimelineUiState.Success) {
            val currentCount = currentState.items.size
            if (currentCount > lastItemCount && lastItemCount != 0) {
                listState.animateScrollToItem(0)
            }
            lastItemCount = currentCount
        }
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && lastItemCount > 0) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(uiState, initialPostId, initialCommentId) {
        if (hasHandledInitialDeepLink) return@LaunchedEffect
        val postId = initialPostId ?: return@LaunchedEffect
        val successState = uiState as? TimelineUiState.Success ?: return@LaunchedEffect

        val index = successState.items.indexOfFirst { item ->
            (item as? TimelineItem.PostItem)?.details?.post?.id == postId
        }
        if (index >= 0) {
            listState.animateScrollToItem(index)
            showCommentSheetId = postId
            highlightedCommentId = initialCommentId
            hasHandledInitialDeepLink = true
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { 
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "CAFESITO",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onSearchUsersClick) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar Usuarios", modifier = Modifier.size(24.dp))
                    }
                },
                actions = {
                    IconButton(onClick = onNotificationsClick) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge { Text(unreadCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notificaciones", modifier = Modifier.size(24.dp))
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                    scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPostClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 100.dp, end = 8.dp)
                    .size(56.dp) 
            ) { Icon(Icons.Default.Add, contentDescription = "Añadir", modifier = Modifier.size(24.dp)) }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshData() },
                modifier = Modifier.fillMaxSize()
            ) {
                when (val state = uiState) {
                is TimelineUiState.Loading -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(5) { ShimmerItem(Modifier.fillMaxWidth().height(400.dp).padding(16.dp)) }
                    }
                }
                is TimelineUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(state.message, color = MaterialTheme.colorScheme.onSurface) }
                is TimelineUiState.Success -> {
                    if (state.items.isEmpty()) {
                        TimelineEmptyState(
                            suggestedUsers = state.suggestedUsers,
                            topics = state.recommendedTopics,
                            onFollowClick = { viewModel.toggleFollowSuggestion(it) },
                            onUserClick = { userId ->
                                if (userId == state.activeUser.id) onUserClick(0)
                                else onUserClick(userId)
                            },
                            onAddPostClick = onAddPostClick
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 120.dp)
                        ) {
                            itemsIndexed(
                                items = state.items,
                                key = { _, item -> item.stableKey }
                            ) { index, item ->
                                viewModel.loadMoreIfNeeded(index)
                                if (suggestionIndices.isNotEmpty() && index == suggestionIndices[0] && state.recommendations.isNotEmpty()) {
                                    Column {
                                        RecommendationCarousel(
                                            recommendations = state.recommendations,
                                            onCoffeeClick = onCoffeeClick
                                        )
                                        Spacer(Modifier.height(24.dp))
                                    }
                                }
                                if (suggestionIndices.size > 1 && index == suggestionIndices[1] && state.suggestedUsers.isNotEmpty()) {
                                    UserSuggestionCarousel(
                                        users = state.suggestedUsers,
                                        followingIds = state.myFollowingIds,
                                        onUserClick = { userId ->
                                            if (userId == state.activeUser.id) onUserClick(0)
                                            else onUserClick(userId)
                                        },
                                        onFollowClick = { viewModel.toggleFollowSuggestion(it) }
                                    )
                                    Spacer(Modifier.height(32.dp))
                                }
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(animationSpec = tween(500, delayMillis = index * 50)) +
                                            slideInVertically(initialOffsetY = { 20 })
                                ) {
                                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                        when (item) {
                                            is TimelineItem.PostItem -> PostCard(
                                                details = item.details,
                                                isLiked = item.details.likes.any { it.userId == state.activeUser.id },
                                                onUserClick = {
                                                    val userId = item.details.post.userId
                                                    if (userId == state.activeUser.id) onUserClick(0)
                                                    else onUserClick(userId)
                                                },
                                                onCommentClick = {
                                                    highlightedCommentId = null
                                                    showCommentSheetId = item.details.post.id
                                                },
                                                onLikeClick = { viewModel.toggleLike(item.details.post.id) },
                                                isOwnPost = item.details.post.userId == state.activeUser.id,
                                                onEditClick = { postToEdit = item.details },
                                                onDeleteClick = { itemToDelete = item.details }
                                            )
                                            is TimelineItem.ReviewItem -> UserReviewCard(
                                                info = item.reviewInfo,
                                                isOwnReview = item.reviewInfo.review.userId == state.activeUser.id,
                                                onEditClick = { reviewToEdit = item },
                                                onDeleteClick = { itemToDelete = item },
                                                onClick = { onCoffeeClick(item.reviewInfo.coffeeDetails.coffee.id) }
                                            )
                                            else -> {}
                                        }
                                    }
                                }
                            }
                            if (state.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }

            AnimatedVisibility(
                visible = isPublishingContent,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = "Publicando tu contenido...",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        if (itemToDelete != null) {
            DeleteConfirmationDialog(
                onDismissRequest = { itemToDelete = null },
                onConfirm = {
                    val item = itemToDelete
                    if (item is PostWithDetails) viewModel.deletePost(item.post.id)
                    else if (item is TimelineItem.ReviewItem) viewModel.deleteReview(item.reviewInfo.coffeeDetails.coffee.id)
                    itemToDelete = null
                },
                title = "Borrar",
                text = "Una vez borrado no se puede recuperar. ¿Estás seguro?"
            )
        }
        showCommentSheetId?.let { id ->
            CommentsSheet(
                postId = id,
                onDismiss = {
                    showCommentSheetId = null
                    highlightedCommentId = null
                },
                onAddComment = { viewModel.onAddComment(id, it) },
                onNavigateToProfile = { userId ->
                    val activeId = (uiState as? TimelineUiState.Success)?.activeUser?.id
                    if (userId == activeId) onUserClick(0)
                    else onUserClick(userId)
                },
                highlightedCommentId = highlightedCommentId
            )
        }
        showReviewOptions?.let { review ->
            ReviewOptionsBottomSheet(
                onDismiss = { showReviewOptions = null },
                onEditClick = { 
                    showReviewOptions = null
                    reviewToEdit = review 
                },
                onDeleteClick = { 
                    showReviewOptions = null
                    itemToDelete = review 
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
        reviewToEdit?.let { item ->
            EditReviewBottomSheet(
                initialRating = item.reviewInfo.review.rating,
                initialComment = item.reviewInfo.review.comment,
                initialImage = item.reviewInfo.review.imageUrl,
                onDismiss = { reviewToEdit = null },
                onConfirm = { rating, comment, imageUrl ->
                    viewModel.updateReview(item.reviewInfo.coffeeDetails.coffee.id, rating, comment, imageUrl)
                    reviewToEdit = null
                }
            )
        }
    }
}
