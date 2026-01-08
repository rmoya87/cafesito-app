package com.example.cafesito.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cafesito.domain.Post
import com.example.cafesito.ui.components.PostCard
import com.example.cafesito.ui.components.UserSuggestionCarousel
import com.example.cafesito.ui.profile.UserReviewCard
import com.example.cafesito.ui.theme.LightGrayBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onUserClick: (Int) -> Unit,
    onCoffeeClick: (Int) -> Unit,
    onAddPostClick: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCommentSheet by remember { mutableStateOf<Post?>(null) }

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

                    if (state.items.isEmpty() && state.suggestedUsers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Sigue a más personas para ver sus publicaciones y opiniones aquí.")
                            }
                        }
                    } else {
                        itemsIndexed(items = state.items) { index, item ->
                            // Mostrar carrusel de sugerencias después de los 2 primeros elementos
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
                                    PostCard(
                                        post = item.post,
                                        onUserClick = { onUserClick(item.post.user.id) },
                                        onCommentClick = { showCommentSheet = item.post }
                                    )
                                }
                                is TimelineItem.ReviewItem -> {
                                    UserReviewCard(
                                        info = item.reviewInfo,
                                        onClick = { onCoffeeClick(item.reviewInfo.coffeeDetails.coffee.id) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        if (state.items.size <= 2 && state.suggestedUsers.isNotEmpty()) {
                            item {
                                UserSuggestionCarousel(
                                    users = state.suggestedUsers,
                                    followingIds = state.myFollowingIds,
                                    onUserClick = onUserClick,
                                    onFollowClick = { userId -> viewModel.toggleFollowSuggestion(userId) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showCommentSheet != null) {
            val postForSheet = showCommentSheet!!
            CommentsSheet(
                post = postForSheet,
                onDismiss = { showCommentSheet = null },
                onAddComment = { newCommentText ->
                    viewModel.onAddComment(postForSheet, newCommentText)
                    showCommentSheet = null
                }
            )
        }
    }
}
