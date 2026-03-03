package com.cafesito.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.cafesito.app.data.CoffeeWithDetails
import com.cafesito.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun RecommendationCarousel(
    recommendations: List<CoffeeWithDetails>,
    onCoffeeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (recommendations.isEmpty()) return

    val context = LocalContext.current
    val imageLoader = context.imageLoader
    val scope = rememberCoroutineScope()

    // ✅ OPTIMIZACIÓN: Pre-cargar las primeras 3 imágenes
    LaunchedEffect(recommendations) {
        scope.launch {
            recommendations.take(3).forEach { item ->
                val request = ImageRequest.Builder(context)
                    .data(item.coffee.imageUrl)
                    .build()
                imageLoader.enqueue(request)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text(
            text = "Recomendados para tu paladar",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Usamos key para mejorar la fluidez del scroll y evitar recomposiciones innecesarias
            items(recommendations, key = { it.coffee.id }) { item ->
                RecommendationCard(item, onCoffeeClick)
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    item: CoffeeWithDetails,
    onClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.width(160.dp).clickable { onClick(item.coffee.id) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column {
            AsyncImage(
                model = item.coffee.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = item.coffee.nombre ?: "Café",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = (item.coffee.marca ?: "").uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(
                        text = "⭐ ${String.format("%.1f", item.averageRating)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
