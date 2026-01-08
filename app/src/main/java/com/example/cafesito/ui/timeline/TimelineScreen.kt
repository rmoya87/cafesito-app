package com.example.cafesito.ui.timeline

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cafesito.domain.Comment
import com.example.cafesito.domain.Post
import com.example.cafesito.domain.currentUser
import com.example.cafesito.domain.samplePosts
import com.example.cafesito.ui.components.PostCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(onUserClick: (Int) -> Unit, onAddPostClick: () -> Unit) {
    var showCommentSheet by remember { mutableStateOf<Post?>(null) }
    val posts = remember { mutableStateListOf(*samplePosts.toTypedArray()) }

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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Inicio",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
            }
            items(items = posts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    onUserClick = { onUserClick(post.user.id) },
                    onCommentClick = { showCommentSheet = post }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (showCommentSheet != null) {
            val postForSheet = showCommentSheet!!
            CommentsSheet(
                post = postForSheet,
                onDismiss = { showCommentSheet = null },
                onAddComment = { newCommentText ->
                    val postIndex = posts.indexOf(postForSheet)
                    if (postIndex != -1) {
                        val updatedComments = postForSheet.comments.toMutableList().apply {
                            add(Comment(currentUser, newCommentText))
                        }
                        val updatedPost = postForSheet.copy(comments = updatedComments)
                        posts[postIndex] = updatedPost
                        showCommentSheet = updatedPost
                    }
                }
            )
        }
    }
}
