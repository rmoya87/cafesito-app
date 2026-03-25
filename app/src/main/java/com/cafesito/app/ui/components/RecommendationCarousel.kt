package com.cafesito.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
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
    val context = LocalContext.current
    val imageLoader = context.imageLoader
    val scope = rememberCoroutineScope()

    val cards = remember(recommendations) { recommendations.take(9).chunked(3) }

    LaunchedEffect(recommendations) {
        if (recommendations.isNotEmpty()) {
            scope.launch {
                recommendations.take(9).forEach { item ->
                    val request = ImageRequest.Builder(context)
                        .data(item.coffee.imageUrl)
                        .build()
                    imageLoader.enqueue(request)
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text(
            text = "Cafés recomendados",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (recommendations.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(72.dp),
                shape = Shapes.card,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Añade favoritos o reseñas para ver recomendaciones.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cards.size) { index ->
                    RecommendationCard3x3(items = cards[index], onClick = onCoffeeClick)
                }
            }
        }
    }
}

/** Una card con hasta 3 cafés: imagen a la izquierda, nombre + marca a la derecha (2 líneas máx para nombre). Misma altura por fila. */
@Composable
private fun RecommendationCard3x3(
    items: List<CoffeeWithDetails>,
    onClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .heightIn(min = 180.dp),
        shape = Shapes.cardSmall,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            items.forEachIndexed { i, item ->
                if (i > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable(enabled = item.coffee.id.isNotBlank()) { onClick(item.coffee.id) },
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    if (!item.coffee.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.coffee.imageUrl,
                            contentDescription = item.coffee.nombre.ifBlank { "Café" },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(Shapes.small),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(Shapes.small)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Icon(Icons.Default.Coffee, contentDescription = "Café", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = item.coffee.nombre.ifBlank { "Café" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.coffee.marca.ifBlank { "Café" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
