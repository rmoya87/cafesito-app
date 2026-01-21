package com.example.cafesito.ui.timeline

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cafesito.data.PostWithDetails
import com.example.cafesito.ui.components.*
import com.example.cafesito.ui.theme.*
import com.example.cafesito.ui.timeline.CommentsSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onUserClick: (Int) -> Unit,
    onCoffeeClick: (String) -> Unit,
    onAddPostClick: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCommentSheetId by remember { mutableStateOf<String?>(null) }
    
    var postToEdit by remember { mutableStateOf<PostWithDetails?>(null) }
    var reviewToEdit by remember { mutableStateOf<TimelineItem.ReviewItem?>(null) }
    var itemToDelete by remember { mutableStateOf<Any?>(null) }

    LaunchedEffect(Unit) { viewModel.refreshData() }

    Scaffold(
        containerColor = SoftOffWhite,
        topBar = { GlassyTopBar(title = "Cafesito") },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onAddPostClick,
                containerColor = EspressoDeep,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
            ) { Icon(Icons.Default.Add, contentDescription = "Añadir", modifier = Modifier.size(30.dp)) }
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
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    if (state.recommendations.isNotEmpty()) {
                        item {
                            Text(
                                "DESCUBRE TU PRÓXIMO CAFÉ",
                                style = MaterialTheme.typography.labelLarge,
                                color = CaramelAccent,
                                modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp)
                            )
                            RecommendationCarousel(
                                recommendations = state.recommendations,
                                onCoffeeClick = onCoffeeClick
                            )
                            Spacer(Modifier.height(24.dp))
                        }
                    }

                    if (state.suggestedUsers.isNotEmpty()) {
                        item {
                            UserSuggestionCarousel(
                                users = state.suggestedUsers,
                                followingIds = state.myFollowingIds,
                                onUserClick = onUserClick,
                                onFollowClick = { viewModel.toggleFollowSuggestion(it) }
                            )
                            Spacer(Modifier.height(32.dp))
                        }
                    }

                    item {
                        Text(
                            "FEED",
                            style = MaterialTheme.typography.labelLarge,
                            color = CaramelAccent,
                            modifier = Modifier.padding(start = 24.dp, bottom = 16.dp)
                        )
                    }

                    itemsIndexed(state.items) { index, item ->
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
                    AlertDialog(
                        onDismissRequest = { itemToDelete = null },
                        title = { Text("Eliminar publicación", fontWeight = FontWeight.Bold) },
                        text = { Text("Esta acción es irreversible.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val item = itemToDelete
                                    if (item is PostWithDetails) viewModel.deletePost(item.post.id)
                                    else if (item is TimelineItem.ReviewItem) viewModel.deleteReview(item.reviewInfo.coffeeDetails.coffee.id)
                                    itemToDelete = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                            ) { Text("ELIMINAR") }
                        },
                        dismissButton = { TextButton(onClick = { itemToDelete = null }) { Text("CANCELAR", color = Color.Gray) } },
                        shape = RoundedCornerShape(28.dp),
                        containerColor = Color.White
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
                
                postToEdit?.let { details ->
                    EditPostDialog(
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
                    EditReviewDialog(
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
