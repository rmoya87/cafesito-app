package com.example.cafesito.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.MoreHoriz
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
        ReviewOptionsBottomSheet(
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
                    if (isOwnReview) {
                        IconButton(onClick = { showOptionsSheet = true }) {
                            Icon(Icons.Default.MoreHoriz, null, tint = EspressoDeep)
                        }
                    }
                }
            }

            // IMAGEN: Ancho completo, completa (sin recortes), sin bordes internos
            if (info.review.imageUrl != null) {
                AsyncImage(
                    model = info.review.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    contentScale = ContentScale.FillWidth
                )
            }

            Column(modifier = Modifier.padding(20.dp)) {
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

                // Bloque de Café: Estilo Minimalista
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
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = info.coffeeDetails.coffee.nombre,
                                style = MaterialTheme.typography.bodyMedium,
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = OrangeYellow, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f", info.review.rating),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = EspressoDeep
                            )
                        }
                    }
                }
            }
        }
    }
}
