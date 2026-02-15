package com.cafesito.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.cafesito.app.data.PostWithDetails
import com.cafesito.app.ui.utils.formatRelativeTime
import com.cafesito.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCard(
    details: PostWithDetails, 
    onUserClick: () -> Unit,
    onCommentClick: () -> Unit,
    onLikeClick: () -> Unit,
    isLiked: Boolean,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    isOwnPost: Boolean = false,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onCoffeeClick: (String) -> Unit = {}
) {
    val post = details.post
    val author = details.author
    val postTextColor = if (isSystemInDarkTheme()) Color.White else Color.Black
    var showOptionsSheet by remember { mutableStateOf(false) }

    if (showOptionsSheet) {
        PostOptionsBottomSheet(
            onDismiss = { showOptionsSheet = false },
            onEditClick = {
                showOptionsSheet = false
                onEditClick()
            },
            onDeleteClick = {
                showOptionsSheet = false
                onDeleteClick()
            }
        )
    }

    PremiumCard(modifier = modifier.fillMaxWidth()) {
        Column {
            if (showHeader) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).clickable(onClick = onUserClick),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModernAvatar(imageUrl = author?.avatarUrl ?: "", size = 44.dp)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = author?.fullName ?: "Usuario", 
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = postTextColor
                            )
                            Text(
                                text = formatRelativeTime(post.timestamp).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    if (isOwnPost) {
                        IconButton(onClick = { showOptionsSheet = true }) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = "Opciones", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = if (showHeader) 0.dp else 20.dp)) {
                if (post.comment.isNotBlank()) {
                    Text(
                        text = post.comment,
                        style = MaterialTheme.typography.bodyLarge,
                        color = postTextColor,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            SubcomposeAsyncImage(
                model = post.imageUrl,
                loading = { ShimmerItem(Modifier.fillMaxWidth().height(350.dp)) },
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )

            details.coffeeTag?.let { tag ->
                Spacer(Modifier.height(12.dp))
                Surface(
                    onClick = { onCoffeeClick(tag.coffeeId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = tag.coffeeImageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CAFÉ ETIQUETADO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(tag.coffeeName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(tag.coffeeBrand.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                        }
                        tag.coffeeRating?.takeIf { it > 0f }?.let {
                            AssistChip(onClick = {}, enabled = false, label = { Text(String.format(java.util.Locale.getDefault(), "%.1f ★", it)) })
                            Spacer(Modifier.width(6.dp))
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    InteractionItem(
                        icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        count = details.likes.size,
                        color = if (isLiked) ElectricRed else postTextColor,
                        onClick = onLikeClick
                    )
                    
                    Spacer(Modifier.width(24.dp))
                    
                    InteractionItem(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        count = details.comments.size,
                        color = postTextColor,
                        onClick = onCommentClick
                    )

                    if (!showHeader && isOwnPost) {
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { showOptionsSheet = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.MoreHoriz, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        if (count > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
