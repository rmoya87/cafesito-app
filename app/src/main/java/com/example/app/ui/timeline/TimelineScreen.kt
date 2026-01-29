package com.cafesito.app.ui.timeline

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafesito.app.data.PostWithDetails
import com.cafesito.app.ui.components.*
import com.cafesito.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onUserClick: (Int) -> Unit,
    onCoffeeClick: (String) -> Unit,
    onAddPostClick: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val uiState by viewModel.uiState.collectAsState()
    var showCommentSheetId by remember { mutableStateOf<String?>(null) }
    
    var showReviewOptions by remember { mutableStateOf<TimelineItem.ReviewItem?>(null) }

    var postToEdit by remember { mutableStateOf<PostWithDetails?>(null) }
    var reviewToEdit by remember { mutableStateOf<TimelineItem.ReviewItem?>(null) }
    var itemToDelete by remember { mutableStateOf<Any?>(null) }

    // Posiciones aleatorias para sugerencias (se calculan una vez al entrar)
    val suggestionIndices = remember(uiState) {
        if (uiState is TimelineUiState.Success) {
            val itemsCount = (uiState as TimelineUiState.Success).items.size
            if (itemsCount >= 4) {
                val mid = itemsCount / 2
                val first = (1 until mid).random()
                val second = (mid until itemsCount).random()
                listOf(first, second)
            } else {
                // Si hay pocos items, usar posiciones fijas o no mostrar si es 0
                listOf(0, 1)
            }
        } else listOf(0, 1)
    }

    LaunchedEffect(Unit) { viewModel.refreshData() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { GlassyTopBar(title = "Cafesito", scrollBehavior = scrollBehavior) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPostClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 90.dp, end = 8.dp)
                    .size(56.dp) 
            ) { Icon(Icons.Default.Add, contentDescription = "Añadir", modifier = Modifier.size(24.dp)) }
        }
    ) { padding ->
        when (val state = uiState) {
            is TimelineUiState.Loading -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    items(5) { ShimmerItem(Modifier.fillMaxWidth().height(400.dp).padding(16.dp)) }
                }
            }
            is TimelineUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(state.message, color = MaterialTheme.colorScheme.onSurface) }
            is TimelineUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    itemsIndexed(
                        items = state.items,
                        key = { _, item ->
                            when(item) {
                                is TimelineItem.PostItem -> "post_${item.details.post.id}"
                                is TimelineItem.ReviewItem -> "review_${item.reviewInfo.review.id}"
                                is TimelineItem.FavoriteActionItem -> "fav_${item.timestamp}"
                            }
                        }
                    ) { index, item ->

                        // Insertar Carrusel de Recomendaciones
                        if (index == suggestionIndices[0] && state.recommendations.isNotEmpty()) {
                            Column {
                                RecommendationCarousel(
                                    recommendations = state.recommendations,
                                    onCoffeeClick = onCoffeeClick
                                )
                                Spacer(Modifier.height(24.dp))
                            }
                        }

                        // Insertar Sugerencias de Usuarios
                        if (index == suggestionIndices[1] && state.suggestedUsers.isNotEmpty()) {
                            UserSuggestionCarousel(
                                users = state.suggestedUsers,
                                followingIds = state.myFollowingIds,
                                onUserClick = onUserClick,
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
                                        onUserClick = { onUserClick(item.details.author.id) },
                                        onCommentClick = { showCommentSheetId = item.details.post.id },
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
                        onDismiss = { showCommentSheetId = null },
                        onAddComment = { viewModel.onAddComment(id, it) },
                        onNavigateToProfile = onUserClick
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
    }
}
