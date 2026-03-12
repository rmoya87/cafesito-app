package com.cafesito.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cafesito.app.data.CoffeeWithDetails
import com.cafesito.app.data.PantryItemWithDetails
import com.cafesito.app.ui.theme.LocalCaramelAccent
import com.cafesito.app.ui.theme.PureBlack
import com.cafesito.app.ui.theme.PureWhite

@Composable
fun PantryPremiumMiniCard(
    item: PantryItemWithDetails,
    onClick: () -> Unit,
    onOptionsClick: ((String) -> Unit)? = null
) {
    val totalGrams = item.pantryItem.totalGrams.coerceAtLeast(0)
    val remainingGrams = item.pantryItem.gramsRemaining.coerceIn(0, totalGrams)
    val progress = if (totalGrams > 0) remainingGrams.toFloat() / totalGrams.toFloat() else 0f

    PremiumCard(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = item.coffee.imageUrl,
                    contentDescription = item.coffee.nombre,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                if (onOptionsClick != null) {
                    val isDark = isSystemInDarkTheme()
                    val optionsIconTint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    val optionsBgColor = if (isDark) Color.Black else Color.White.copy(alpha = 0.82f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(optionsBgColor, CircleShape)
                            .clickable { onOptionsClick(item.pantryItem.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "Opciones", tint = optionsIconTint, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = item.coffee.nombre.toCoffeeNameFormat(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                @Suppress("DEPRECATION")
                Text(
                    text = "${remainingGrams}/${totalGrams}g",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 8.sp
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = LocalCaramelAccent.current,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
            }
        }
    }
}

@Composable
fun CoffeePremiumRowItem(coffee: CoffeeWithDetails, onClick: () -> Unit) {
    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = coffee.coffee.imageUrl,
                contentDescription = coffee.coffee.nombre,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = coffee.coffee.nombre.toCoffeeNameFormat(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                @Suppress("DEPRECATION")
                Text(
                    text = coffee.coffee.marca.toCoffeeBrandFormat(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun PantryAddActionCard(onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val cardBackground = if (isDark) MaterialTheme.colorScheme.surface else PureWhite
    val borderColor = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    val plusCircleColor = if (isDark) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val plusColor = if (isDark) MaterialTheme.colorScheme.onSurface else PureBlack

    Surface(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = plusCircleColor,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir café a la despensa", tint = plusColor, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
