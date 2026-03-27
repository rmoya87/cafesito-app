package com.cafesito.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.cafesito.app.R
import com.cafesito.app.data.UserReviewInfo
import com.cafesito.app.ui.utils.formatRelativeTime
import com.cafesito.app.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserReviewCard(
    info: UserReviewInfo, 
    showHeader: Boolean = true,
    isOwnReview: Boolean = false,
    onClick: () -> Unit
) {
    // Se eliminan showOptionsSheet y ReviewOptionsBottomSheet
    
    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column {
            if (showHeader) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModernAvatar(imageUrl = info.authorAvatarUrl, size = 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = info.authorName ?: stringResource(id = R.string.diary_barista),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatRelativeTime(info.review.timestamp).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = LocalDateMetaColor.current,
                            letterSpacing = 1.sp
                        )
                    }
                    // Se ha eliminado el IconButton de opciones (MoreHoriz)
                }
            }

            if (info.review.comment.isNotBlank()) {
                Text(
                    text = info.review.comment,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = if (showHeader) 0.dp else 20.dp)
                )
                Spacer(Modifier.height(16.dp))
            }

            if (info.review.imageUrl != null) {
                AsyncImage(
                    model = info.review.imageUrl,
                    contentDescription = "Imagen de la reseña",
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    contentScale = ContentScale.FillWidth
                )
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = Shapes.pill
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = info.coffeeDetails.coffee.imageUrl,
                            contentDescription = info.coffeeDetails.coffee.nombre,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(Shapes.cardSmall),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = info.coffeeDetails.coffee.nombre,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = info.coffeeDetails.coffee.marca.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = "Valoración", tint = OrangeYellow, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${info.review.rating.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
