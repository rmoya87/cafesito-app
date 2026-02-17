package com.cafesito.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.cafesito.app.data.PostWithDetails
import com.cafesito.app.ui.utils.formatRelativeTime
import com.cafesito.app.ui.theme.*
import kotlinx.coroutines.launch

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
    val postTextColor = if (isSystemInDarkTheme()) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black
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
                        icon = if (isLiked) Icons.Filled.LocalCafe else Icons.Outlined.LocalCafe,
                        count = details.likes.size,
                        color = if (isLiked) ElectricRed else postTextColor,
                        type = InteractionType.FAVORITE,
                        onClick = onLikeClick
                    )
                    
                    Spacer(Modifier.width(24.dp))
                    
                    InteractionItem(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        count = details.comments.size,
                        color = postTextColor,
                        type = InteractionType.COMMENT,
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
    color: androidx.compose.ui.graphics.Color,
    type: InteractionType,
    onClick: () -> Unit
) {
    val iconScale = remember { Animatable(1f) }
    val iconRotation = remember { Animatable(0f) }
    val iconGlow = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val onInteractionClick = {
        scope.launch {
            iconScale.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = 520
                    1.34f at 120
                    0.9f at 260
                    1.12f at 380
                    1f at 520
                }
            )
        }
        scope.launch {
            val target = if (type == InteractionType.FAVORITE) 22f else 14f
            iconRotation.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 420
                    -target at 100
                    target * 0.7f at 190
                    -target * 0.4f at 290
                    0f at 420
                }
            )
        }
        scope.launch {
            iconGlow.snapTo(0f)
            iconGlow.animateTo(1f, animationSpec = tween(180, easing = FastOutSlowInEasing))
            iconGlow.animateTo(0f, animationSpec = tween(360, easing = FastOutSlowInEasing))
        }
        onClick()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onInteractionClick)
    ) {
        Icon(
            icon,
            null,
            tint = color,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = iconScale.value
                    scaleY = iconScale.value
                    rotationZ = iconRotation.value
                    shadowElevation = 20f * iconGlow.value
                    alpha = 0.82f + (0.18f * iconGlow.value)
                }
        )
        if (count > 0) {
            Spacer(Modifier.width(6.dp))
            AnimatedContent(
                targetState = count,
                transitionSpec = {
                    val enter = if (targetState > initialState) {
                        slideInVertically(animationSpec = tween(240)) { it } +
                            fadeIn(tween(180)) +
                            scaleIn(initialScale = 0.72f, animationSpec = spring(dampingRatio = 0.65f, stiffness = 520f))
                    } else {
                        slideInVertically(animationSpec = tween(240)) { -it } +
                            fadeIn(tween(180)) +
                            scaleIn(initialScale = 0.72f, animationSpec = spring(dampingRatio = 0.65f, stiffness = 520f))
                    }
                    val exit = if (targetState > initialState) {
                        slideOutVertically(animationSpec = tween(240)) { -it } +
                            fadeOut(tween(180)) +
                            scaleOut(targetScale = 1.28f, animationSpec = tween(220))
                    } else {
                        slideOutVertically(animationSpec = tween(240)) { it } +
                            fadeOut(tween(180)) +
                            scaleOut(targetScale = 1.28f, animationSpec = tween(220))
                    }
                    enter togetherWith exit using SizeTransform(clip = false)
                },
                label = "interactionCount"
            ) { animatedCount ->
                Text(
                    text = animatedCount.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.clickable(onClick = onInteractionClick)
                )
            }
        }
    }
}

private enum class InteractionType {
    FAVORITE,
    COMMENT
}
