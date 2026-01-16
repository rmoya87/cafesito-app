package com.example.cafesito.ui.timeline

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.cafesito.ui.components.PostCard
import com.example.cafesito.ui.components.RatingBar
import com.example.cafesito.ui.components.UserReviewCard
import com.example.cafesito.ui.components.UserSuggestionCarousel
import com.example.cafesito.ui.components.RecommendationCarousel
import com.example.cafesito.ui.theme.CoffeeBrown
import com.example.cafesito.ui.theme.LightGrayBackground

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

    // Refrescar cada vez que se entra en la pantalla
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    Scaffold(
        containerColor = Color(0xFFF8F8F8),
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPostClick, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White) {
                Icon(Icons.Default.Add, contentDescription = "Añadir")
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is TimelineUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            is TimelineUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item { 
                        Text(
                            text = "Inicio", 
                            style = MaterialTheme.typography.headlineMedium, 
                            color = Color.Black, // CAMBIADO A NEGRO
                            modifier = Modifier.padding(16.dp)
                        ) 
                    }

                    // SECCIÓN DE RECOMENDACIONES SENSORIALES
                    if (state.recommendations.isNotEmpty()) {
                        item {
                            RecommendationCarousel(
                                recommendations = state.recommendations,
                                onCoffeeClick = onCoffeeClick
                            )
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
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    itemsIndexed(state.items) { index, item ->
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
                        Spacer(Modifier.height(16.dp))
                    }
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

                itemToDelete?.let { item ->
                    AlertDialog(
                        onDismissRequest = { itemToDelete = null },
                        title = { Text("¿Borrar publicación?") },
                        text = { Text("Esta acción no se puede deshacer.") },
                        confirmButton = {
                            TextButton(onClick = {
                                if (item is PostWithDetails) viewModel.deletePost(item.post.id)
                                else if (item is TimelineItem.ReviewItem) viewModel.deleteReview(item.reviewInfo.coffeeDetails.coffee.id)
                                itemToDelete = null
                            }) { Text("Borrar", color = Color.Red) }
                        },
                        dismissButton = { TextButton(onClick = { itemToDelete = null }) { Text("Cancelar") } }
                    )
                }

                showCommentSheetId?.let { id ->
                    CommentsSheet(postId = id, onDismiss = { showCommentSheetId = null }, onAddComment = { viewModel.onAddComment(id, it) }, onNavigateToProfile = onUserClick)
                }
            }
        }
    }
}

@Composable
fun EditPostDialog(
    initialText: String, 
    initialImage: String,
    onDismiss: () -> Unit, 
    onConfirm: (String, String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var imageUrl by remember { mutableStateOf(initialImage) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { imageUrl = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Publicación") },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Cambiar foto", color = Color.White, modifier = Modifier.padding(4.dp), fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text, 
                    onValueChange = { text = it }, 
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Descripción") }
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(text, imageUrl) }, colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown)) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun EditReviewDialog(
    initialRating: Float, 
    initialComment: String, 
    initialImage: String?,
    onDismiss: () -> Unit, 
    onConfirm: (Float, String, String?) -> Unit
) {
    var rating by remember { mutableFloatStateOf(initialRating) }
    var comment by remember { mutableStateOf(initialComment) }
    var imageUrl by remember { mutableStateOf(initialImage) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { imageUrl = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Opinión") },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl, 
                            contentDescription = null, 
                            modifier = Modifier.fillMaxSize(), 
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, null, tint = Color.Gray)
                            Text("Añadir foto", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                RatingBar(
                    rating = rating, 
                    isInteractive = true, 
                    onRatingChanged = { rating = it }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = comment, 
                    onValueChange = { comment = it }, 
                    label = { Text("Tu opinión") }, 
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(rating, comment, imageUrl) }, colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown)) { Text("Actualizar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
