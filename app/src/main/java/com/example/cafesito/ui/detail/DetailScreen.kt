package com.example.cafesito.ui.detail

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.ui.components.SensoryRadarChart
import com.example.cafesito.domain.Review
import com.example.cafesito.domain.User
import com.example.cafesito.domain.currentUser
import com.example.cafesito.domain.sampleReviews
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBackClick: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is DetailUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is DetailUiState.Error -> Text("Error al cargar los detalles", modifier = Modifier.align(Alignment.Center))
            is DetailUiState.Success -> {
                val isFavorite by viewModel.isFavorite.collectAsState()
                DetailContent(state.coffee, isFavorite, onBackClick) {
                    viewModel.toggleFavorite(isFavorite)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DetailContent(
    coffeeDetails: CoffeeWithDetails,
    isFavorite: Boolean,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val scrollState = rememberLazyListState()
    val imageHeight = 350.dp
    val cardOverlap = 24.dp
    var showAddReviewDialog by remember { mutableStateOf(false) }

    val userReview = sampleReviews.value.find { it.user.id == currentUser.id }
    val averageReviewRating = if (sampleReviews.value.isNotEmpty()) sampleReviews.value.map { it.rating }.average().toFloat() else 0f
    val reviewCount = sampleReviews.value.size

    val isScrolled by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > 200
        }
    }

    if (showAddReviewDialog) {
        AddReviewDialog(
            existingReview = userReview,
            onDismissRequest = { showAddReviewDialog = false },
            onSaveReview = { rating, comment ->
                // FIXED: Passing coffeeId to Review
                val newReview = Review(currentUser, coffeeDetails.coffee.id, rating, comment)
                val currentReviews = sampleReviews.value.toMutableList()
                val index = currentReviews.indexOfFirst { it.user.id == currentUser.id }
                if (index != -1) {
                    currentReviews[index] = newReview
                } else {
                    currentReviews.add(0, newReview)
                }
                sampleReviews.value = currentReviews
                showAddReviewDialog = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = coffeeDetails.coffee.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .graphicsLayer { translationY = scrollState.firstVisibleItemScrollOffset * 0.5f }
        )

        LazyColumn(state = scrollState) {
            item { Spacer(modifier = Modifier.height(imageHeight - cardOverlap)) }
            item {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = cardOverlap, topEnd = cardOverlap))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(24.dp)
                ) {
                    Text(coffeeDetails.coffee.brandRoaster, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(coffeeDetails.coffee.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    
                    Spacer(Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ScoreView(score = "${String.format("%.2f", coffeeDetails.coffee.officialScore)}/100", label = "SCA")
                        Spacer(modifier = Modifier.weight(1f))
                        // CHANGED: "Reseñas" -> "Opiniones"
                        ScoreView(score = "${String.format("%.1f", averageReviewRating)}/5", label = "Opiniones ($reviewCount)")
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Button(
                            onClick = onFavoriteClick, 
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, 
                                contentDescription = null,
                                tint = if (isFavorite) Color.Red else Color.White
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(if (isFavorite) "Favorito" else "Añadir")
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        StatChip("Origen", coffeeDetails.origin?.countryName ?: "N/A")
                        StatChip("Tueste", coffeeDetails.coffee.roastLevel)
                        StatChip("Proceso", coffeeDetails.coffee.process)
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Text("Sobre este Café", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(coffeeDetails.coffee.description, style = MaterialTheme.typography.bodyLarge)
                    
                    Spacer(Modifier.height(24.dp))
                    
                    coffeeDetails.sensoryProfile?.let {
                        Text("Perfil Sensorial", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        SensoryRadarChart(
                            data = mapOf(
                                "Aroma" to it.aroma, "Sabor" to it.flavor, "Cuerpo" to it.body, "Acidez" to it.acidity, "Postgusto" to it.aftertaste
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Spacer(Modifier.height(24.dp))

                    ReviewsSection(reviews = sampleReviews.value, userReview = userReview, onAddReviewClick = { showAddReviewDialog = true })
                }
            }
        }

        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = if(isScrolled) MaterialTheme.colorScheme.onSurface else Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = if (isScrolled) MaterialTheme.colorScheme.surface.copy(alpha=0.9f) else Color.Transparent)
        )
    }
}

@Composable
private fun ReviewsSection(reviews: List<Review>, userReview: Review?, onAddReviewClick: () -> Unit) {
    var currentPage by remember { mutableStateOf(0) }
    val itemsPerPage = 10
    
    val sortedReviews = remember(reviews) {
        reviews.sortedByDescending { it.user.id == currentUser.id }
    }

    val totalPages = ceil(sortedReviews.size.toFloat() / itemsPerPage).toInt()
    val startIndex = currentPage * itemsPerPage
    val endIndex = (startIndex + itemsPerPage).coerceAtMost(sortedReviews.size)
    val paginatedReviews = sortedReviews.subList(startIndex, endIndex)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            // CHANGED: "Reseñas" -> "Opiniones"
            Text("${reviews.size} Opiniones", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onAddReviewClick) {
                // CHANGED: "reseña" -> "opinión"
                Icon(Icons.Default.Edit, contentDescription = if (userReview != null) "Editar mi opinión" else "Añadir opinión")
                Spacer(Modifier.size(4.dp))
                Text(if (userReview != null) "Editar mi opinión" else "Añadir opinión")
            }
        }

        Spacer(Modifier.height(16.dp))

        if (paginatedReviews.isNotEmpty()) {
            paginatedReviews.forEach {
                ReviewItem(review = it, onProfileClick = { /* TODO */ })
                Spacer(Modifier.height(16.dp))
            }

            if (totalPages > 1) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { currentPage-- }, enabled = currentPage > 0) { Text("Anterior") }
                    Spacer(Modifier.size(16.dp))
                    Text("Página ${currentPage + 1} de $totalPages")
                    Spacer(Modifier.size(16.dp))
                    Button(onClick = { currentPage++ }, enabled = currentPage < totalPages - 1) { Text("Siguiente") }
                }
            }
        } else {
            // CHANGED: "reseñas" -> "opiniones"
            Text("Todavía no hay opiniones. ¡Sé el primero!")
        }
    }
}

@Composable
private fun ReviewItem(review: Review, onProfileClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        AsyncImage(
            model = review.user.avatarUrl,
            contentDescription = "Avatar de ${review.user.fullName}",
            modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.size(16.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onProfileClick() }) {
                Text(review.user.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            RatingBar(rating = review.rating, isInteractive = false)
            Spacer(Modifier.height(4.dp))
            Text(review.comment, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReviewDialog(existingReview: Review?, onDismissRequest: () -> Unit, onSaveReview: (Float, String) -> Unit) {
    var rating by remember { mutableStateOf(existingReview?.rating ?: 0f) }
    var comment by remember { mutableStateOf(existingReview?.comment ?: "") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        // CHANGED: "reseña" -> "opinión"
        title = { Text(if (existingReview != null) "Editar opinión" else "Añadir opinión") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RatingBar(rating = rating, isInteractive = true, onRatingChanged = { newRating -> rating = newRating })
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { rating = 0f }) {
                        Icon(Icons.Default.Delete, contentDescription = "Borrar Puntuación")
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comentario") },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (rating > 0f && comment.isNotBlank()) {
                        onSaveReview(rating, comment)
                    } else {
                        Toast.makeText(context, "La puntuación y el comentario son obligatorios", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun RatingBar(rating: Float, isInteractive: Boolean, starCount: Int = 5, starSize: Dp = 32.dp, onRatingChanged: ((Float) -> Unit)? = null) {
    Row {
        for (i in 1..starCount) {
            val isSelected = i <= rating
            val icon = if (isSelected) Icons.Filled.Star else Icons.Outlined.Star
            val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            val modifier = if (isInteractive && onRatingChanged != null) Modifier.clickable { onRatingChanged(i.toFloat()) } else Modifier
            Icon(icon, contentDescription = null, tint = tint, modifier = modifier.size(starSize))
        }
    }
}

@Composable
private fun ScoreView(score: String, label: String, onClick: (() -> Unit)? = null) {
    val modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(text = score, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.padding(horizontal = 8.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))) {
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
}
