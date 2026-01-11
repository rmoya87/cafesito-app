package com.example.cafesito.ui.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.StarHalf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.data.ReviewEntity
import com.example.cafesito.domain.allUsers
import com.example.cafesito.domain.currentUser
import com.example.cafesito.ui.components.UserReviewCard
import com.example.cafesito.ui.theme.CoffeeBrown
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Lógica de tiempo relativo unificada:
 * - < 5 min: "hace unos minutos"
 * - 5 min a 1 h: "hace X min"
 * - 1 h a 24 h: "hace X h"
 * - 1 d a 7 d: "hace X d"
 * - 1 sem a 1 año: "hace X sem"
 * - > 1 año: dd/MM/yyyy
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
            is DetailUiState.Error -> Text("Error al cargar los detalles", modifier = Modifier.align(Alignment.Center))
            is DetailUiState.Success -> {
                val isFavorite by viewModel.isFavorite.collectAsState()
                DetailContent(
                    coffeeDetails = state.coffee,
                    userReview = state.userReview,
                    isFavorite = isFavorite,
                    onBackClick = onBackClick,
                    onFavoriteToggle = { viewModel.toggleFavorite(isFavorite) },
                    onReviewSubmit = { rating, comment -> viewModel.submitReview(rating, comment) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    coffeeDetails: CoffeeWithDetails,
    userReview: ReviewEntity?,
    isFavorite: Boolean,
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
                    Text("Opinión", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(text = String.format(Locale.getDefault(), "%.1f", coffeeDetails.averageRating), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize()) {
            item { Spacer(modifier = Modifier.height(360.dp)) }
            item {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Detalles del Lote", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Text(text = coffee.descripcion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = if (isDescExpanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.animateContentSize())
                        Text(text = if (isDescExpanded) "Leer menos" else "Leer más", color = CoffeeBrown, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 4.dp).clickable { isDescExpanded = !isDescExpanded })

                        Spacer(Modifier.height(32.dp))
                        Text("DETALLES TÉCNICOS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                DetailStatBlock("País", coffee.paisOrigen.capitalizeWords(), Modifier.weight(1f))
                                DetailStatBlock("Especialidad", coffee.especialidad.capitalizeWords(), Modifier.weight(1f))
                                DetailStatBlock("Variedad", coffee.variedadTipo.capitalizeWords(), Modifier.weight(1f))
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                DetailStatBlock("Tueste", coffee.tueste.capitalizeWords(), Modifier.weight(1f))
                                DetailStatBlock("Proceso", coffee.proceso.capitalizeWords(), Modifier.weight(1f))
                                DetailStatBlock("Molienda", coffee.moliendaRecomendada.capitalizeWords(), Modifier.weight(1f))
                            }
                        }

                        Spacer(Modifier.height(32.dp))
                        Text("Características del grano", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        listOf("Aroma" to coffee.aroma, "Sabor" to coffee.sabor, "Cuerpo" to coffee.cuerpo, "Acidez" to coffee.acidez, "Retrogusto" to coffee.retrogusto).forEach { (label, value) ->
                            CharacteristicBar(label, value)
                            Spacer(Modifier.height(12.dp))
                        }

                        Spacer(Modifier.height(32.dp))
                        val reviewCount = coffeeDetails.reviews.size
                        val opinionsTitle = when {
                            reviewCount == 0 -> "Opiniones"
                            reviewCount == 1 -> "1 Opinión"
                            else -> "$reviewCount Opiniones"
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(opinionsTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { showAddReviewDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (userReview != null) "Editar" else "Añadir")
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        if (coffeeDetails.reviews.isNotEmpty()) {
                            val sortedReviews = coffeeDetails.reviews.sortedByDescending { it.timestamp }
                            sortedReviews.forEachIndexed { index, review ->
                                DetailReviewItem(review)
                                if (index < sortedReviews.size - 1) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        } else {
                            Text(
                                text = "Sin opiniones todavía.\n¿Quieres ser el primero?",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                            )
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
                    Icon(imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (isFavorite) Color.Red else Color.Gray, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailReviewItem(review: ReviewEntity) {
    val user = allUsers.find { it.id == review.userId }
    Row(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(model = user?.avatarUrl, contentDescription = null, modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentScale = ContentScale.Crop)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = user?.fullName ?: "Usuario", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = formatRelativeTime(review.timestamp), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            RatingBar(rating = review.rating, starSize = 14.dp, isInteractive = false)
            Spacer(Modifier.height(8.dp))
            Text(text = review.comment, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun CharacteristicBar(label: String, value: Float, maxValue: Float = 10f) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("${value.toInt()}/10", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(progress = { value / maxValue }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = CoffeeBrown, trackColor = MaterialTheme.colorScheme.surfaceVariant)
    }
}

@Composable
private fun DetailStatBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReviewDialog(existingReview: ReviewEntity?, onDismissRequest: () -> Unit, onSaveReview: (Float, String) -> Unit) {
    var rating by remember { mutableStateOf(existingReview?.rating ?: 0f) }
    var comment by remember { mutableStateOf(existingReview?.comment ?: "") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(if (existingReview != null) "Actualizar opinión" else "Añadir opinión", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Puntuación", style = MaterialTheme.typography.labelLarge, color = CoffeeBrown)
                Spacer(Modifier.height(16.dp))
                SemicircleRatingBar(rating = rating, onRatingChanged = { rating = it })
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Tu comentario (obligatorio)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSaveReview(rating, comment) }, enabled = rating > 0 && comment.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown)) {
                Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancelar") } }
    )
}

@Composable
fun SemicircleRatingBar(rating: Float, onRatingChanged: (Float) -> Unit, modifier: Modifier = Modifier) {
    val starSize = 42.dp
    val radius = 110.dp
    Box(
        modifier = modifier.fillMaxWidth().height(140.dp).pointerInput(Unit) {
            detectDragGestures { change, _ ->
                val x = change.position.x - (size.width / 2)
                val y = change.position.y - size.height
                val angle = Math.toDegrees(atan2(-y.toDouble(), x.toDouble()))
                val star = ((160 - angle) / (140 / 4)).toFloat() + 1
                onRatingChanged(star.coerceIn(1f, 5f).let { Math.round(it).toFloat() })
            }
        },
        contentAlignment = Alignment.BottomCenter
    ) {
        for (i in 1..5) {
            val angleDeg = 160f - (i - 1) * (140f / 4f)
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val xOffset = (radius.value * cos(angleRad)).dp
            val yOffset = -(radius.value * sin(angleRad)).dp
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Default.StarOutline,
                contentDescription = null,
                tint = if (i <= rating) Color(0xFFFFD700) else Color.Gray,
                modifier = Modifier.size(starSize).offset(x = xOffset, y = yOffset).clickable { onRatingChanged(i.toFloat()) }
            )
        }
    }
}

@Composable
fun RatingBar(rating: Float, isInteractive: Boolean = true, onRatingChanged: (Float) -> Unit = {}, starSize: Dp = 32.dp) {
    Row {
        for (i in 1..5) {
            val starIcon = if (i <= rating) Icons.Filled.Star else if (i - 0.5f <= rating) Icons.AutoMirrored.Filled.StarHalf else Icons.Default.StarOutline
            Icon(
                imageVector = starIcon,
                contentDescription = null,
                tint = if (i <= rating + 0.5f) Color(0xFFFFD700) else Color.Gray,
                modifier = Modifier.size(starSize).clickable(enabled = isInteractive) { onRatingChanged(i.toFloat()) }
            )
        }
    }
}
