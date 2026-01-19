package com.example.cafesito.ui.detail

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.data.PantryItemEntity
import com.example.cafesito.data.ReviewEntity
import com.example.cafesito.data.UserReviewInfo
import com.example.cafesito.ui.components.SemicircleRatingBar
import com.example.cafesito.ui.components.RatingBar
import com.example.cafesito.ui.theme.CoffeeBrown
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// --- FUNCIONES DE UTILIDAD ---
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
    } catch (e: Exception) {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBackClick: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        when (val state = uiState) {
            is DetailUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is DetailUiState.Error -> Text("Error: ${state.message}", modifier = Modifier.align(Alignment.Center))
            is DetailUiState.Success -> {
                DetailContent(
                    coffeeDetails = state.coffee,
                    userReview = state.userReview,
                    reviews = state.reviews,
                    isCustom = state.isCustom,
                    currentStock = state.currentPantryItem,
                    onBackClick = onBackClick,
                    onFavoriteToggle = { viewModel.toggleFavorite(state.coffee.isFavorite) },
                    onUpdateStock = { total, remaining, name, brand -> viewModel.updateStock(total, remaining, name, brand) },
                    onReviewSubmit = { rating, comment, imageUri -> 
                        viewModel.submitReview(rating, comment, imageUri) 
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    coffeeDetails: CoffeeWithDetails,
    userReview: ReviewEntity?,
    reviews: List<UserReviewInfo>,
    isCustom: Boolean,
    currentStock: PantryItemEntity?,
    onBackClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onUpdateStock: (Int, Int, String?, String?) -> Unit,
    onReviewSubmit: (Float, String, Uri?) -> Unit
) {
    val scrollState = rememberLazyListState()
    val coffee = coffeeDetails.coffee
    val context = LocalContext.current
    var showAddReviewDialog by remember { mutableStateOf(false) }
    var showStockDialog by remember { mutableStateOf(false) }
    var isDescExpanded by remember { mutableStateOf(false) }

    if (showAddReviewDialog) {
        ReviewBottomSheet(
            existingReview = userReview,
            onDismissRequest = { showAddReviewDialog = false },
            onSaveReview = { rating, comment, imageUri ->
                onReviewSubmit(rating, comment, imageUri)
                showAddReviewDialog = false
            }
        )
    }

    if (showStockDialog) {
        StockEditBottomSheet(
            coffeeDetails = coffeeDetails,
            isCustom = isCustom,
            currentStock = currentStock,
            onDismiss = { showStockDialog = false },
            onSave = { total, remaining, name, brand -> 
                onUpdateStock(total, remaining, name, brand)
                showStockDialog = false
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

            if (!isCustom) {
                Surface(modifier = Modifier.padding(end = 20.dp, bottom = 48.dp).align(Alignment.BottomEnd), color = Color.White.copy(alpha = 0.95f), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Nota Media", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(text = String.format(Locale.getDefault(), "%.1f", coffeeDetails.averageRating), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize()) {
            item { Spacer(modifier = Modifier.height(360.dp)) }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), 
                    color = Color.White
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        
                        if (!isCustom && coffee.descripcion.isNotBlank()) {
                            Text(
                                "Descripción", 
                                style = MaterialTheme.typography.titleLarge, 
                                fontWeight = FontWeight.Normal,
                                color = Color.Black
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(text = coffee.descripcion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = if (isDescExpanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.animateContentSize())
                            Text(text = if (isDescExpanded) "Leer menos" else "Leer más", color = CoffeeBrown, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 4.dp).clickable { isDescExpanded = !isDescExpanded })
                            Spacer(Modifier.height(24.dp))
                        }

                        Text(
                            "Detalles", 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.Normal,
                            color = Color.Black
                        )
                        Spacer(Modifier.height(20.dp))
                        
                        val detailsItems = listOfNotNull(
                            coffee.paisOrigen?.takeIf { it.isNotBlank() }?.let { Triple("País", it.capitalizeWords(), Icons.Default.Public) },
                            coffee.especialidad.takeIf { it.isNotBlank() }?.let { Triple("Especialidad", it.capitalizeWords(), Icons.Default.Verified) },
                            coffee.variedadTipo?.takeIf { it.isNotBlank() }?.let { Triple("Variedad", it.capitalizeWords(), Icons.Default.Category) },
                            coffee.tueste.takeIf { it.isNotBlank() }?.let { Triple("Tueste", it.capitalizeWords(), Icons.Default.LocalFireDepartment) },
                            coffee.proceso.takeIf { it.isNotBlank() }?.let { Triple("Proceso", it.capitalizeWords(), Icons.Default.Settings) },
                            coffee.ratioRecomendado?.takeIf { it.isNotBlank() }?.let { Triple("Ratio", it, Icons.Default.Scale) },
                            coffee.moliendaRecomendada.takeIf { it.isNotBlank() }?.let { Triple("Molienda", it, Icons.Default.Grain) },
                            coffee.cafeina.takeIf { it.isNotBlank() }?.let { Triple("Cafeína", it, Icons.Default.Bolt) }
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            maxItemsInEachRow = 2
                        ) {
                            detailsItems.forEach { (label, value, icon) ->
                                DetailStatBlock(label, value, icon, Modifier.weight(1f))
                            }
                            if (detailsItems.size % 2 != 0) {
                                Spacer(Modifier.weight(1f))
                            }
                        }

                        if (!isCustom) {
                            Spacer(Modifier.height(32.dp))
                            Text(
                                "Características", 
                                style = MaterialTheme.typography.titleLarge, 
                                fontWeight = FontWeight.Normal,
                                color = Color.Black
                            )
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

                            if (!coffee.productUrl.isNullOrBlank()) {
                                Spacer(Modifier.height(32.dp))
                                Text(
                                    "Donde comprar", 
                                    style = MaterialTheme.typography.titleLarge, 
                                    fontWeight = FontWeight.Normal,
                                    color = Color.Black
                                )
                                Spacer(Modifier.height(16.dp))
                                Surface(
                                    onClick = { openCustomTab(context, coffee.productUrl) },
                                    color = Color(0xFFF8F8F8),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val domain = coffee.productUrl
                                            .removePrefix("https://")
                                            .removePrefix("http://")
                                            .removePrefix("www.")
                                            .substringBefore("/")
                                        Text(
                                            text = domain, 
                                            style = MaterialTheme.typography.bodyLarge, 
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Black
                                        )
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight, 
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
                                Text(
                                    opinionsTitle, 
                                    style = MaterialTheme.typography.titleLarge, 
                                    fontWeight = FontWeight.Normal,
                                    color = Color.Black
                                )
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
                            }
                        }
                        
                        Spacer(Modifier.height(100.dp))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.statusBarsPadding().fillMaxWidth().padding(16.dp), 
            horizontalArrangement = Arrangement.SpaceBetween, 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(onClick = onBackClick, color = Color.White.copy(alpha = 0.9f), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(24.dp)) }
            }
            Row {
                Surface(
                    onClick = { showStockDialog = true }, 
                    color = Color.White.copy(alpha = 0.9f), 
                    shape = RoundedCornerShape(12.dp), 
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) { 
                        Icon(Icons.Default.Inventory, contentDescription = "Gestionar Stock", tint = CoffeeBrown, modifier = Modifier.size(24.dp)) 
                    }
                }
                Spacer(Modifier.width(12.dp))
                Surface(onClick = onFavoriteToggle, color = Color.White.copy(alpha = 0.9f), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = if (coffeeDetails.isFavorite) Icons.Filled.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (coffeeDetails.isFavorite) Color.Red else Color.Gray, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockEditBottomSheet(
    coffeeDetails: CoffeeWithDetails,
    isCustom: Boolean,
    currentStock: PantryItemEntity?,
    onDismiss: () -> Unit,
    onSave: (Int, Int, String?, String?) -> Unit
) {
    var totalGrams by remember { mutableFloatStateOf(currentStock?.totalGrams?.toFloat() ?: 600f) }
    var remainingGrams by remember { mutableFloatStateOf(currentStock?.gramsRemaining?.toFloat() ?: totalGrams) }
    
    var editName by remember { mutableStateOf(coffeeDetails.coffee.nombre) }
    var editBrand by remember { mutableStateOf(coffeeDetails.coffee.marca) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = if (isCustom) "Editar Café y Stock" else "Gestionar Stock", 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Bold
                )
            }

            if (isCustom) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Nombre") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editBrand,
                            onValueChange = { editBrand = it },
                            label = { Text("Marca") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }

            item {
                StockSliderSection(
                    label = "Total de la bolsa",
                    value = totalGrams,
                    onValueChange = { 
                        totalGrams = it
                        if (remainingGrams > it) remainingGrams = it
                    }
                )
            }

            item {
                StockSliderSection(
                    label = "Gramos restantes",
                    value = remainingGrams,
                    maxValue = totalGrams,
                    onValueChange = { remainingGrams = it }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Text("CANCELAR", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { 
                            onSave(
                                totalGrams.roundToInt(), 
                                remainingGrams.roundToInt(),
                                if (isCustom) editName else null,
                                if (isCustom) editBrand else null
                            ) 
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("GUARDAR", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewBottomSheet(
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

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if (existingReview != null) "Editar opinión" else "Añadir opinión", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            
            Text("Puntuación", style = MaterialTheme.typography.labelLarge, color = CoffeeBrown)
            Spacer(Modifier.height(8.dp))
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
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(model = selectedImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else if (!existingReview?.imageUrl.isNullOrBlank()) {
                    AsyncImage(model = existingReview?.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = CoffeeBrown)
                        Text("Añadir foto", style = MaterialTheme.typography.labelMedium, color = CoffeeBrown)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Text("CANCELAR", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onSaveReview(rating, comment, selectedImageUri) }, 
                    enabled = rating > 0 && comment.isNotBlank(),
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("GUARDAR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StockSliderSection(label: String, value: Float, maxValue: Float = 1000f, onValueChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Color.Gray)
        Text(
            text = "${value.roundToInt()} g", 
            style = MaterialTheme.typography.headlineSmall, 
            fontWeight = FontWeight.Black, 
            color = CoffeeBrown
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..(if(maxValue > 0) maxValue else 1000f),
            colors = SliderDefaults.colors(thumbColor = CoffeeBrown, activeTrackColor = CoffeeBrown)
        )
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
private fun DetailStatBlock(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xFFF8F8F8),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CoffeeBrown.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = CoffeeBrown, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
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
