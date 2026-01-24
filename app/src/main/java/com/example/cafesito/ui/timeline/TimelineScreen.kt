package com.example.cafesito.ui.timeline

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cafesito.data.PostWithDetails
import com.example.cafesito.ui.components.*
import com.example.cafesito.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onUserClick: (Int) -> Unit,
    onCoffeeClick: (String) -> Unit,
    onAddPostClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    viewModel: TimelineViewModel = hiltViewModel()
) {
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
            if (itemsCount > 2) {
                val first = (1 until itemsCount / 2).random()
                val second = (itemsCount / 2 until itemsCount).random()
                listOf(first, second)
            } else {
                listOf(0, 1)
            }
        } else listOf(0, 1)
    }

    LaunchedEffect(Unit) { viewModel.refreshData() }

    Scaffold(
        containerColor = SoftOffWhite,
        topBar = { GlassyTopBar(title = "Cafesito", scrollBehavior = scrollBehavior) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPostClick,
                containerColor = EspressoDeep,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 90.dp, end = 8.dp)
                    .size(56.dp) // Reducido un 25% aprox (de LargeFAB 96dp o normal 56dp)
            ) { Icon(Icons.Default.Add, contentDescription = "Añadir", modifier = Modifier.size(24.dp)) }
        }
    ) { padding ->
        when (val state = uiState) {
            is TimelineUiState.Loading -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    items(5) { ShimmerItem(Modifier.fillMaxWidth().height(400.dp).padding(16.dp)) }
                }
            }
            is TimelineUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(state.message) }
            is TimelineUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    itemsIndexed(state.items) { index, item ->
                        
                        // Insertar Carrusel de Recomendaciones en posición aleatoria
                        if (index == suggestionIndices[0] && state.recommendations.isNotEmpty()) {
                            Column {
                                RecommendationCarousel(
                                    recommendations = state.recommendations,
                                    onCoffeeClick = onCoffeeClick
                                )
                                Spacer(Modifier.height(24.dp))
                            }
                        }

                        // Insertar Sugerencias de Usuarios en posición aleatoria
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
                            enter = fadeIn(animationSpec = tween(500, delayMillis = index * 100)) + 
                                    slideInVertically(initialOffsetY = { 50 })
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
