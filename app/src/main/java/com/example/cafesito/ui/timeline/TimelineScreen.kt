package com.example.cafesito.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Locale

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
                PostItem(
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

@Composable
fun PostItem(post: Post, onUserClick: () -> Unit, onCommentClick: () -> Unit) {
    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableStateOf(post.initialLikes) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp), // No rounded corners
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clickable(onClick = onUserClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = post.user.avatarUrl,
                    contentDescription = "Avatar de ${post.user.name}",
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(post.user.name, fontWeight = FontWeight.Bold)
                    Text(SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(post.timestamp), style = MaterialTheme.typography.labelSmall)
                }
            }

            // Image
            AsyncImage(
                model = post.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(300.dp)
            )

            // Actions and Comment
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Comments
                    Row(modifier = Modifier.clickable(onClick = onCommentClick), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comentarios"
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(text = "${post.comments.size}", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.size(16.dp))

                    // "Like" action
                    IconButton(onClick = {
                        isLiked = !isLiked
                        likeCount = if (isLiked) likeCount + 1 else likeCount - 1
                    }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Coffee else Icons.Outlined.Coffee,
                            contentDescription = "Dar café",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(text = "$likeCount", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(post.comment)
            }
        }
    }
}
