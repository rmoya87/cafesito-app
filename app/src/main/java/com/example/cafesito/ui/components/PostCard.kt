package com.example.cafesito.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.BrokenImage
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
import coil.compose.SubcomposeAsyncImage
import com.example.cafesito.data.PostWithDetails
import com.example.cafesito.ui.detail.formatRelativeTime

@Composable
fun PostCard(
    details: PostWithDetails, 
    onUserClick: () -> Unit,
    onCommentClick: () -> Unit,
    onLikeClick: () -> Unit,
    isLiked: Boolean,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true
) {
    val post = details.post
    val author = details.author

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
                    SubcomposeAsyncImage(
                        model = author.avatarUrl,
                        loading = { Box(Modifier.fillMaxSize().background(Color.LightGray)) },
                        error = { Icon(Icons.Default.BrokenImage, contentDescription = null, tint = Color.Gray) },
                        contentDescription = "Avatar",
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(author.fullName, fontWeight = FontWeight.Bold)
                        Text(
                            text = formatRelativeTime(post.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            SubcomposeAsyncImage(
                model = post.imageUrl,
                loading = { 
                    Box(modifier = Modifier.fillMaxWidth().height(250.dp).background(Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                },
                error = { 
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.BrokenImage, contentDescription = "Error de carga", tint = Color.LightGray)
                    }
                },
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
            )

            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.clickable(onClick = onCommentClick),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Outlined.ChatBubbleOutline, contentDescription = "Comentarios")
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(text = "${details.comments.size}", fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.size(16.dp))
                    
                    IconButton(onClick = onLikeClick) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Coffee else Icons.Outlined.Coffee,
                            contentDescription = "Me gusta",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(text = "${details.likes.size}", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(post.comment)
            }
        }
    }
}
