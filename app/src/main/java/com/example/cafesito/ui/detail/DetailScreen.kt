package com.example.cafesito.ui.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.data.ReviewEntity
import com.example.cafesito.ui.profile.UserReviewInfo
import com.example.cafesito.ui.theme.CoffeeBrown
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lógica de tiempo relativo unificada para toda la app.
 */
fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val weeks = days / 7

    return when {
        minutes < 5 -> "hace unos minutos"
        minutes < 60 -> "hace $minutes min"
        hours < 24 -> "hace $hours h"
        days < 7 -> "hace $days d"
        weeks < 52 -> "hace $weeks sem"
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

fun String.capitalizeWords(): String = if (this.isBlank()) "" else {
    this.lowercase().split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBackClick: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        when (val state = uiState) {
            is DetailUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is DetailUiState.Error -> Text("Error: ${state.message}", modifier = Modifier.align(Alignment.Center))
            is DetailUiState.Success -> {
                DetailContent(
                    coffeeDetails = state.coffee,
                    userReview = state.userReview,
                    reviews = state.reviews,
                    onBackClick = onBackClick,
                    onFavoriteToggle = { viewModel.toggleFavorite(state.coffee.isFavorite) },
                    onReviewSubmit = { rating, comment -> viewModel.submitReview(rating, comment) }
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    coffeeDetails: CoffeeWithDetails,
    userReview: ReviewEntity?,
    reviews: List<UserReviewInfo>,
    onBackClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onReviewSubmit: (Float, String) -> Unit
) {
    val scrollState = rememberLazyListState()
    val coffee = coffeeDetails.coffee
    var showAddReviewDialog by remember { mutableStateOf(false) }
    var isDescExpanded by remember { mutableStateOf(false) }

    if (showAddReviewDialog) {
        AddReviewDialog(
            existingReview = userReview,
            onDismissRequest = { showAddReviewDialog = false },
            onSaveReview = { rating, comment ->
                onReviewSubmit(rating, comment)
                showAddReviewDialog = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(400.dp).graphicsLayer {
            translationY = -scrollState.firstVisibleItemScrollOffset * 0.4f
            alpha = 1f - (scrollState.firstVisibleItemScrollOffset / 800f).coerceIn(0f, 1f)
        }) {
            AsyncImage(model = coffee.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)), startY = 600f)))
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 48.dp, end = 100.dp)) {
                Text(text = coffee.marca, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                Text(text = coffee.nombre, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
            }
            Surface(modifier = Modifier.padding(end = 20.dp, bottom = 48.dp).align(Alignment.BottomEnd), color = Color.White.copy(alpha = 0.95f), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nota Media", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(text = String.format(Locale.getDefault(), "%.1f", coffeeDetails.averageRating), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize()) {
            item { Spacer(modifier = Modifier.height(360.dp)) }
            item {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Descripción", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Text(text = coffee.descripcion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = if (isDescExpanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.animateContentSize())
                        Text(text = if (isDescExpanded) "Leer menos" else "Leer más", color = CoffeeBrown, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 4.dp).clickable { isDescExpanded = !isDescExpanded })

                        Spacer(Modifier.height(32.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Opiniones", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Button(onClick = { showAddReviewDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown), shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (userReview != null) "Editar" else "Añadir")
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        reviews.sortedByDescending { it.review.timestamp }.forEach { info ->
                            DetailReviewItem(info)
                            Spacer(Modifier.height(16.dp))
                        }
                        
                        Spacer(Modifier.height(100.dp))
                    }
                }
            }
        }

        Row(modifier = Modifier.statusBarsPadding().fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = onBackClick, color = Color.White.copy(alpha = 0.9f), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(24.dp)) }
            }
            Surface(onClick = onFavoriteToggle, color = Color.White.copy(alpha = 0.9f), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = if (coffeeDetails.isFavorite) Icons.Filled.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (coffeeDetails.isFavorite) Color.Red else Color.Gray, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailReviewItem(info: UserReviewInfo) {
    Row(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = info.authorAvatarUrl,
            contentDescription = null,
            modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = info.authorName ?: "Usuario", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = formatRelativeTime(info.review.timestamp), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Text(text = info.review.comment, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AddReviewDialog(existingReview: ReviewEntity?, onDismissRequest: () -> Unit, onSaveReview: (Float, String) -> Unit) {
    var rating by remember { mutableStateOf(existingReview?.rating ?: 0f) }
    var comment by remember { mutableStateOf(existingReview?.comment ?: "") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = { onSaveReview(rating, comment) }, enabled = rating > 0) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancelar") }
        },
        title = { Text(if (existingReview != null) "Editar opinión" else "Añadir opinión") },
        text = {
            Column {
                Slider(value = rating, onValueChange = { rating = it }, valueRange = 0f..5f, steps = 4)
                Text("Nota: ${String.format("%.1f", rating)}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("Comentario") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            }
        }
    )
}

@Composable
private fun DetailStatBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CharacteristicBar(label: String, value: Float) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = value.toString(), fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(progress = { value / 10f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = CoffeeBrown, trackColor = Color.LightGray.copy(alpha = 0.3f))
    }
}
