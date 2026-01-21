package com.example.cafesito.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreHoriz
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
import com.example.cafesito.data.UserReviewInfo
import com.example.cafesito.ui.utils.formatRelativeTime
import com.example.cafesito.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserReviewCard(
    info: UserReviewInfo, 
    showHeader: Boolean = true,
    isOwnReview: Boolean = false,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onClick: () -> Unit
) {
    var showOptionsSheet by remember { mutableStateOf(false) }

    if (showOptionsSheet) {
        SettingsBottomSheet(
            onDismiss = { showOptionsSheet = false },
            onEditClick = onEditClick,
            onLogoutClick = onDeleteClick
        )
    }

    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            
            if (showHeader) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModernAvatar(imageUrl = info.authorAvatarUrl, size = 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = info.authorName ?: "Barista",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = EspressoDeep
                            )
                            Text(
                                text = formatRelativeTime(info.review.timestamp).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = CaramelAccent,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    if (isOwnReview) {
                        IconButton(onClick = { showOptionsSheet = true }) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = "Opciones", tint = EspressoDeep)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (info.review.comment.isNotBlank()) {
                Text(
                    text = info.review.comment,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp,
                    color = EspressoDeep,
                    fontWeight = FontWeight.Light
                )
                Spacer(Modifier.height(16.dp))
            }

            if (info.review.imageUrl != null) {
                AsyncImage(
                    model = info.review.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(16.dp))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CreamLight,
                shape = RoundedCornerShape(24.dp),
                border = borderLight()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = info.coffeeDetails.coffee.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = info.coffeeDetails.coffee.nombre,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = info.coffeeDetails.coffee.marca.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = CaramelAccent,
                            fontSize = 10.sp
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = CaramelAccent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f", info.review.rating),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = EspressoDeep
                            )
                        }
                    }
                }
            }
            
            if (!showHeader && isOwnReview) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatRelativeTime(info.review.timestamp).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = { showOptionsSheet = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreHoriz, null, tint = Color.LightGray)
                    }
                }
            }
        }
    }
}
