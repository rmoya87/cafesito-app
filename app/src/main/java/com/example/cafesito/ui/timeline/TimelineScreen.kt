package com.example.cafesito.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cafesito.ui.components.PostCard
import com.example.cafesito.ui.components.UserReviewCard
import com.example.cafesito.ui.components.UserSuggestionCarousel
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

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPostClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir publicación")
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is TimelineUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is TimelineUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightGrayBackground)
                        .padding(padding),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    item {
                        Text(
                            text = "Inicio",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        )
                    }

                    if (state.items.isEmpty() && state.suggestedUsers.isNotEmpty()) {
                        item {
                            UserSuggestionCarousel(
                                users = state.suggestedUsers,
                                followingIds = state.myFollowingIds,
                                onUserClick = onUserClick,
                                onFollowClick = { userId -> viewModel.toggleFollowSuggestion(userId) },
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }

                    itemsIndexed(items = state.items) { index, item ->
                        if (index == 2 && state.suggestedUsers.isNotEmpty()) {
                            UserSuggestionCarousel(
                                users = state.suggestedUsers,
                                followingIds = state.myFollowingIds,
                                onUserClick = onUserClick,
                                onFollowClick = { userId -> viewModel.toggleFollowSuggestion(userId) },
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        when (item) {
                            is TimelineItem.PostItem -> {
                                val details = item.details
                                PostCard(
                                    details = details,
                                    isLiked = false, // TODO: Cargar estado real de likes de Room
                                    onUserClick = { onUserClick(details.author.id) },
                                    onCommentClick = { showCommentSheetId = details.post.id },
                                    onLikeClick = { viewModel.toggleLike(details.post.id) }
                                )
                            }
                            is TimelineItem.ReviewItem -> {
                                UserReviewCard(
                                    info = item.reviewInfo,
                                    showHeader = true,
                                    onClick = { onCoffeeClick(item.reviewInfo.coffeeDetails.coffee.id) }
                                )
                            }
                            is TimelineItem.FavoriteActionItem -> {
                                FavoriteTimelineCard(
                                    coffeeName = item.coffeeDetails.coffee.nombre,
                                    onClick = { onCoffeeClick(item.coffeeDetails.coffee.id) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                showCommentSheetId?.let { id ->
                    CommentsSheet(
                        postId = id,
                        onDismiss = { showCommentSheetId = null },
                        onAddComment = { text -> viewModel.onAddComment(id, text) },
                        onNavigateToProfile = onUserClick // Mapeamos a onUserClick para navegar al perfil
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteTimelineCard(coffeeName: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Has guardado un nuevo café", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                Text(coffeeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
