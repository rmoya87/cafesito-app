package com.cafesito.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.cafesito.app.data.Coffee
import com.cafesito.app.ui.theme.Shapes
import com.cafesito.app.ui.theme.Spacing
import com.cafesito.app.ui.components.toCoffeeNameFormat

/**
 * Tarjeta de café unificada para listas (buscador, cafés probados).
 * Ver docs/GUIA_UNIFICACION_COMPONENTES_UI.md.
 *
 * @param coffee Café a mostrar
 * @param subtitle Línea debajo del nombre (ej. marca)
 * @param secondLine Línea opcional adicional (ej. "Primera vez: fecha")
 * @param imageSize Tamaño de la imagen (48.dp o 60.dp)
 * @param showChevron Si true, muestra chevron a la derecha
 * @param onClick Callback al pulsar (recibe coffee.id)
 */
@Composable
fun CoffeeListItem(
    coffee: Coffee,
    subtitle: String? = null,
    secondLine: String? = null,
    imageSize: Dp = 60.dp,
    showChevron: Boolean = false,
    onClick: (String) -> Unit
) {
    val rowHeight = when {
        imageSize >= 56.dp -> 86.dp
        else -> 72.dp
    }
    val minHeight = if (!secondLine.isNullOrBlank()) 80.dp else rowHeight
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = coffee.id.isNotBlank()) { onClick(coffee.id) },
        shape = Shapes.card,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.heightIn(min = minHeight)) {
            Row(
                modifier = Modifier
                    .padding(Spacing.space3)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (coffee.imageUrl.isNotBlank()) {
                    val optimizedImageUrl = if (coffee.imageUrl.contains("storage/v1/object/public")) {
                        "${coffee.imageUrl}?width=400&height=300&resize=contain"
                    } else {
                        coffee.imageUrl
                    }
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(optimizedImageUrl)
                            .crossfade(true)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = coffee.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(imageSize)
                            .clip(Shapes.cardSmall)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(imageSize)
                            .clip(Shapes.cardSmall)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = coffee.nombre.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(Spacing.space4))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = coffee.nombre.toCoffeeNameFormat().ifBlank { "Café" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!secondLine.isNullOrBlank()) {
                        Text(
                            text = secondLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (showChevron) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Ver café",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
