package com.example.cafesito.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cafesito.domain.Post
import com.example.cafesito.ui.detail.formatRelativeTime
import java.util.Locale

@Composable
fun PostCard(
    post: Post,
    onUserClick: () -> Unit,
    onCommentClick: () -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true
) {
    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableStateOf(post.initialLikes) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            if (showHeader) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onUserClick)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = post.user.avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(post.user.fullName, fontWeight = FontWeight.Bold)
                        Text(
                            text = formatRelativeTime(post.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // AJUSTE: Imagen a ancho completo sin recortes ni fondos laterales (espacios negros)
            AsyncImage(
                model = post.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.FillWidth, // Llena el ancho respetando el aspecto
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight() // Altura dinámica según la foto
            )

            // Actions and Comment
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.clickable(onClick = onCommentClick),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Outlined.ChatBubbleOutline, contentDescription = "Comentarios")
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(text = "${post.comments.size}", fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.size(16.dp))
                    
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
