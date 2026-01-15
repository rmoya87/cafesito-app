package com.example.cafesito.ui.detail

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.data.ReviewEntity
import com.example.cafesito.data.UserReviewInfo
import com.example.cafesito.ui.components.SemicircleRatingBar
import com.example.cafesito.ui.components.RatingBar
import com.example.cafesito.ui.theme.CoffeeBrown
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

private fun openCustomTab(context: Context, url: String) {
    try {
        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setToolbarColor(CoffeeBrown.toArgb())
            .build()
        intent.launchUrl(context, Uri.parse(url))
    } catch (e: Exception) {
        // Fallback si falla
    }
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
                    onReviewSubmit = { rating, comment, imageUri -> 
                        viewModel.submitReview(rating, comment, imageUri) 
                    }
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
    onReviewSubmit: (Float, String, Uri?) -> Unit
) {
    val scrollState = rememberLazyListState()
    val context = LocalContext.current
    val coffee = coffeeDetails.coffee
    var showAddReviewDialog by remember { mutableStateOf(false) }
    var isDescExpanded by remember { mutableStateOf(false) }

    if (showAddReviewDialog) {
        AddReviewDialog(
            existingReview = userReview,
            onDismissRequest = { showAddReviewDialog = false },
            onSaveReview = { rating, comment, imageUri ->
                onReviewSubmit(rating, comment, imageUri)
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
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 48.dp, end = 120.dp)) {
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
                        if (coffee.descripcion.isNotBlank()) {
                            Text(text = coffee.descripcion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = if (isDescExpanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.animateContentSize())
                            Text(text = if (isDescExpanded) "Leer menos" else "Leer más", color = CoffeeBrown, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 4.dp).clickable { isDescExpanded = !isDescExpanded })
                        }

                        Spacer(Modifier.height(32.dp))

                        Text("Detalles", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        
                        val detailsItems = listOfNotNull(
                            coffee.paisOrigen?.takeIf { it.isNotBlank() }?.let { "País" to it.capitalizeWords() },
                            coffee.especialidad.takeIf { it.isNotBlank() }?.let { "Especialidad" to it.capitalizeWords() },
                            coffee.variedadTipo?.takeIf { it.isNotBlank() }?.let { "Variedad" to it.capitalizeWords() },
                            coffee.tueste.takeIf { it.isNotBlank() }?.let { "Tueste" to it.capitalizeWords() },
                            coffee.proceso.takeIf { it.isNotBlank() }?.let { "Proceso" to it.capitalizeWords() },
                            coffee.ratioRecomendado?.takeIf { it.isNotBlank() }?.let { "Ratio" to it },
                            coffee.moliendaRecomendada.takeIf { it.isNotBlank() }?.let { "Molienda" to it },
                            coffee.cafeina.takeIf { it.isNotBlank() }?.let { "Cafeína" to it }
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            detailsItems.chunked(3).forEach { rowItems ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    rowItems.forEach { (label, value) ->
                                        DetailStatBlock(label, value, Modifier.weight(1f))
                                    }
                                    repeat(3 - rowItems.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(32.dp))

                        Text("Características", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        val characteristics = listOf(
                            "Aroma" to coffee.aroma,
                            "Sabor" to coffee.sabor,
                            "Cuerpo" to coffee.cuerpo,
                            "Acidez" to coffee.acidez,
                            "Retrogusto" to coffee.retrogusto,
                            "Dulzura" to coffee.dulzura,
                            "Uniformidad" to coffee.uniformidad
                        )
                        characteristics.forEach { (label, value) ->
                            CharacteristicBar(label, value)
                            Spacer(Modifier.height(12.dp))
                        }

                        if (coffee.productUrl.isNotBlank()) {
                            Spacer(Modifier.height(32.dp))
                            Text("Donde encontrarlo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            
                            val domain = remember(coffee.productUrl) {
                                try {
                                    val host = Uri.parse(coffee.productUrl).host ?: ""
                                    host.removePrefix("www.")
                                } catch (e: Exception) {
                                    ""
                                }
                            }

                            Surface(
                                onClick = { openCustomTab(context, coffee.productUrl) },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8F8F8),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = domain.ifBlank { "Ver en tienda" },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Color.Gray
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(32.dp))
                        
                        val reviewCount = reviews.size
                        val opinionsTitle = when {
                            reviewCount == 0 -> "Opiniones"
                            reviewCount == 1 -> "1 Opinión"
                            else -> "$reviewCount Opiniones"
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(opinionsTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Button(onClick = { showAddReviewDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown), shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (userReview != null) "Editar" else "Añadir")
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        if (reviews.isNotEmpty()) {
                            reviews.sortedByDescending { it.review.timestamp }.forEachIndexed { index, info ->
                                DetailReviewItem(info)
                                if (index < reviews.size - 1) {
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
            RatingBar(rating = info.review.rating, starSize = 14.dp, isInteractive = false)
            Spacer(Modifier.height(8.dp))
            Text(text = info.review.comment, style = MaterialTheme.typography.bodyMedium)
            if (!info.review.imageUrl.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = info.review.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun AddReviewDialog(
    existingReview: ReviewEntity?, 
    onDismissRequest: () -> Unit, 
    onSaveReview: (Float, String, Uri?) -> Unit
) {
    var rating by remember { mutableStateOf(existingReview?.rating ?: 0f) }
    var comment by remember { mutableStateOf(existingReview?.comment ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = { onSaveReview(rating, comment, selectedImageUri) }, 
                enabled = rating > 0 && comment.isNotBlank(), 
                colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown)
            ) { 
                Text("Guardar") 
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancelar") }
        },
        title = { Text(if (existingReview != null) "Editar opinión" else "Añadir opinión", fontWeight = FontWeight.Bold) },
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
                Spacer(Modifier.height(16.dp))
                
                // SECCIÓN DE IMAGEN
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = Color.White)
                        }
                    } else if (!existingReview?.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = existingReview?.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            "Toca para cambiar",
                            modifier = Modifier.align(Alignment.BottomCenter).background(Color.Black.copy(alpha = 0.5f)).fillMaxWidth().padding(4.dp),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = CoffeeBrown)
                            Text("Añadir foto", style = MaterialTheme.typography.labelMedium, color = CoffeeBrown)
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun DetailStatBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CharacteristicBar(label: String, value: Float) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(text = "${String.format("%.1f", value)}/10", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { value / 10f }, 
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), 
            color = CoffeeBrown, 
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
