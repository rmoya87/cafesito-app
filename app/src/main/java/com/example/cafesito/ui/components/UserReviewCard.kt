package com.example.cafesito.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.cafesito.ui.components.RatingBar // FIX: Import corregido
import com.example.cafesito.ui.detail.formatRelativeTime
import com.example.cafesito.ui.profile.UserReviewInfo
import com.example.cafesito.ui.theme.CoffeeBrown

@Composable
fun UserReviewCard(
    info: UserReviewInfo, 
    showHeader: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            
            // 1. CABECERA DE AUTOR (Imagen Izquierda, Info Derecha)
            if (showHeader) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = info.authorAvatarUrl, // USAMOS EL AVATAR REAL
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = info.authorName ?: "Barista",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatRelativeTime(info.review.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "ha opinado",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // 2. TEXTO DE LA OPINIÓN (Ahora va ANTES que el café)
            if (info.review.comment.isNotBlank()) {
                Text(
                    text = info.review.comment,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))
            }

            // 3. FOTO DE LA OPINIÓN (Si existe)
            if (info.review.imageUrl != null) {
                AsyncImage(
                    model = info.review.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.FillWidth
                )
                Spacer(Modifier.height(16.dp))
            }

            // 4. TARJETA DEL CAFÉ (Al final como contexto)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8F8F8), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = info.coffeeDetails.coffee.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = info.coffeeDetails.coffee.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = info.coffeeDetails.coffee.marca,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format("%.1f", info.review.rating),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = CoffeeBrown
                    )
                    RatingBar(rating = info.review.rating, isInteractive = false, starSize = 10.dp)
                }
            }
        }
    }
}
