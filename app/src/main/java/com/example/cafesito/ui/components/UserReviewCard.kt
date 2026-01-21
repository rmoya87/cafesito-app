package com.example.cafesito.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.cafesito.data.UserReviewInfo
import com.example.cafesito.ui.detail.formatRelativeTime
import com.example.cafesito.ui.theme.CoffeeBrown

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
        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet = false },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                ListItem(
                    headlineContent = { Text("Editar información") },
                    leadingContent = { Icon(Icons.Default.Edit, null) },
                    modifier = Modifier.clickable { 
                        showOptionsSheet = false
                        onEditClick() 
                    }
                )
                ListItem(
                    headlineContent = { Text("Eliminar opinión", color = Color.Red) },
                    leadingContent = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                    modifier = Modifier.clickable { 
                        showOptionsSheet = false
                        onDeleteClick() 
                    }
                )
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            
            if (showHeader) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = info.authorAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = info.authorName ?: "Barista",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatRelativeTime(info.review.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray.copy(alpha = 0.7f)
                            )
                        }
                    }

                    if (isOwnReview) {
                        Box {
                            IconButton(onClick = { showOptionsSheet = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (info.review.comment.isNotBlank()) {
                Text(
                    text = info.review.comment,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))
            }

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
                    
                    // Solo si estamos en perfil (sin cabecera), mostramos fecha y menu aquí
                    if (!showHeader && isOwnReview) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Coffee, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = formatRelativeTime(info.review.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (!showHeader && isOwnReview) {
                        Box {
                            IconButton(onClick = { showOptionsSheet = true }, modifier = Modifier.size(32.dp).offset(x = 12.dp, y = (-12).dp)) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
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
