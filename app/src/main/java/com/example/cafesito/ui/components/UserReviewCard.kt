package com.example.cafesito.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.cafesito.domain.currentUser
import com.example.cafesito.ui.detail.RatingBar
import com.example.cafesito.ui.detail.formatRelativeTime
import com.example.cafesito.ui.profile.UserReviewInfo
import com.example.cafesito.ui.theme.CoffeeBrown
import java.util.Locale

@Composable
fun UserReviewCard(
    info: UserReviewInfo, 
    showHeader: Boolean = true,
    onClick: () -> Unit
) {
    val isMe = info.review.userId == currentUser.id
    val authorLabel = if (isMe) "Has opinado" else "${info.authorName ?: "Alguien"} ha opinado"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            // CABECERA MARRÓN: Solo en Inicio/Timeline
            if (showHeader) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CoffeeBrown.copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = authorLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatRelativeTime(info.review.timestamp), // LÓGICA FECHA HUMANA
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                // EL COMENTARIO: Primero en Perfil si no hay cabecera
                if (!showHeader && info.review.comment.isNotBlank()) {
                    Text(
                        text = info.review.comment,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // DATOS DEL CAFÉ + PUNTUACIÓN
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = info.coffeeDetails.coffee.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Fecha sutil en Perfil debajo de la marca
                        if (!showHeader) {
                            Text(
                                text = formatRelativeTime(info.review.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format("%.1f", info.review.rating),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        RatingBar(rating = info.review.rating, isInteractive = false, starSize = 12.dp)
                    }
                }

                // EL COMENTARIO: Al final en Inicio si hay cabecera
                if (showHeader && info.review.comment.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = info.review.comment,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
