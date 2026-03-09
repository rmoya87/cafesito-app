package com.cafesito.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.cafesito.app.data.PostWithDetails
import com.cafesito.app.data.UserEntity
import com.cafesito.app.ui.utils.formatRelativeTime
import com.cafesito.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

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
    onCoffeeClick: (String) -> Unit = {},
    onMentionClick: (String) -> Unit = {},
    resolveMentionUser: (String) -> UserEntity? = { null }
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
                                color = LocalDateMetaColor.current,
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
                    MentionText(
                        text = post.comment,
                        style = MaterialTheme.typography.bodyLarge.copy(color = postTextColor, lineHeight = 22.sp),
                        onMentionClick = onMentionClick,
                        resolveMentionUser = resolveMentionUser
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            SubcomposeAsyncImage(
                model = post.imageUrl,
                loading = { ShimmerItem(Modifier.fillMaxWidth().height(350.dp)) },
                contentDescription = "Imagen del post",
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
                    color = if (isSystemInDarkTheme()) Color(0xFF212121) else MaterialTheme.colorScheme.surface,
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
                            contentDescription = tag.coffeeName,
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
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Ver detalle del café", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Interaction para Me gusta (Color Marrón Acepto)
                    InteractionItem(
                        icon = Icons.Outlined.LocalCafe,
                        count = details.likes.size,
                        isLiked = isLiked,
                        color = postTextColor,
                        isLikeIcon = true,
                        onClick = onLikeClick
                    )
                    
                    Spacer(Modifier.width(24.dp))
                    
                    // Interaction para Comentarios
                    InteractionItem(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        count = details.comments.size,
                        color = postTextColor,
                        onClick = onCommentClick
                    )

                    if (!showHeader && isOwnPost) {
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { showOptionsSheet = true }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = "Opciones de la publicación", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractionItem(
    icon: ImageVector,
    count: Int,
    isLiked: Boolean = false,
    color: Color,
    isLikeIcon: Boolean = false,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val fillProgress = remember { Animatable(if (isLiked) 1f else 0f) }
    val fireworkProgress = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    // Estado local para respuesta inmediata
    var likedByMe by remember { mutableStateOf(isLiked) }
    
    // Sincronizar solo cuando el valor real de la DB cambia
    LaunchedEffect(isLiked) {
        likedByMe = isLiked
    }

    // Animaciones sincronizadas con el color de la app (LocalCaramelAccent.current)
    LaunchedEffect(likedByMe) {
        if (isLikeIcon) {
            if (likedByMe) {
                launch {
                    scale.animateTo(1.35f, tween(150))
                    scale.animateTo(1f, spring(dampingRatio = 0.6f))
                }
                fillProgress.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
                fireworkProgress.snapTo(0f)
                fireworkProgress.animateTo(1f, tween(500))
                fireworkProgress.snapTo(0f)
            } else {
                fillProgress.animateTo(0f, tween(250))
            }
        }
    }

    val handleIconClick = {
        if (isLikeIcon) {
            likedByMe = !likedByMe
        } else {
            scope.launch {
                scale.animateTo(1.25f, tween(100))
                scale.animateTo(1f, spring(dampingRatio = 0.6f))
            }
        }
        onClick()
    }

    val outlinedPainter = rememberVectorPainter(Icons.Outlined.LocalCafe)
    val filledPainter = rememberVectorPainter(Icons.Filled.LocalCafe)
    val activeColor = LocalCaramelAccent.current // El marrón de la app

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(
            onClick = handleIconClick,
            indication = ripple(bounded = false, radius = 24.dp),
            interactionSource = remember { MutableInteractionSource() }
        )
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    alpha = 0.95f
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLikeIcon) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // 1. Contorno (Marron si activo, sino color texto)
                    val strokeColor = if (likedByMe) activeColor else color
                    with(outlinedPainter) {
                        draw(size, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(strokeColor))
                    }
                    
                    // 2. Llenado animado
                    if (fillProgress.value > 0f) {
                        clipRect(top = size.height * (1f - fillProgress.value), bottom = size.height) {
                            with(filledPainter) {
                                draw(size, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(activeColor))
                            }
                        }
                    }
                    
                    // 3. Fuegos artificiales
                    if (fireworkProgress.value > 0f && fireworkProgress.value < 1f) {
                        val p = fireworkProgress.value
                        val alpha = (1f - p).coerceIn(0f, 1f)
                        for (i in 0 until 8) {
                            val angle = (i * 45f) * (Math.PI / 180f)
                            val dist = 36f * p
                            val px = (size.width / 2) + (cos(angle) * dist).toFloat()
                            val py = (size.height / 4) - (sin(angle) * dist).toFloat()
                            drawCircle(activeColor.copy(alpha = alpha), radius = 2.4f * (1f - p), center = Offset(px, py))
                        }
                    }
                }
            } else {
                Icon(icon, contentDescription = "Acción", tint = color, modifier = Modifier.fillMaxSize())
            }
        }

        val displayCount = if (likedByMe && !isLiked) count + 1 
                          else if (!likedByMe && isLiked) count - 1 
                          else count

        if (displayCount > 0) {
            Spacer(Modifier.width(6.dp))
            AnimatedContent(
                targetState = displayCount,
                transitionSpec = {
                    val enter = if (targetState > initialState) slideInVertically { it } + fadeIn() else slideInVertically { -it } + fadeIn()
                    val exit = if (targetState > initialState) slideOutVertically { -it } + fadeOut() else slideOutVertically { it } + fadeOut()
                    enter togetherWith exit using SizeTransform(clip = false)
                },
                label = "interactionCount"
            ) { animatedCount ->
                Text(
                    text = animatedCount.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isLikeIcon && likedByMe) activeColor else color
                )
            }
        }
    }
}
