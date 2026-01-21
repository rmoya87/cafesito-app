package com.example.cafesito.ui.detail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.cafesito.data.PantryItemEntity
import com.example.cafesito.data.ReviewEntity
import com.example.cafesito.data.UserReviewInfo
import com.example.cafesito.ui.components.*
import com.example.cafesito.ui.theme.*
import com.example.cafesito.ui.utils.*
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBackClick: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = SoftOffWhite) {
        when (val state = uiState) {
            is DetailUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = CaramelAccent) }
            is DetailUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(state.message) }
            is DetailUiState.Success -> {
                DetailContent(
                    coffeeDetails = state.coffee,
                    userReview = state.userReview,
                    reviews = state.reviews,
                    isCustom = state.isCustom,
                    currentStock = state.currentPantryItem,
                    onBackClick = onBackClick,
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                    onUpdateStock = { t, r, n, b -> viewModel.updateStock(t, r, n, b) },
                    onReviewSubmit = { r, c, i -> viewModel.submitReview(r, c, i) }
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
    onFavoriteToggle: (Boolean) -> Unit,
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
            onSaveReview = { r, c, i -> onReviewSubmit(r, c, i) }
        )
    }

    if (showStockDialog) {
        StockEditBottomSheet(
            coffeeDetails = coffeeDetails,
            isCustom = isCustom,
            currentStock = currentStock,
            onDismiss = { showStockDialog = false },
            onSave = { t, r, n, b -> onUpdateStock(t, r, n, b) }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(450.dp).graphicsLayer {
            translationY = -scrollState.firstVisibleItemScrollOffset * 0.5f
            alpha = 1f - (scrollState.firstVisibleItemScrollOffset / 1000f).coerceIn(0f, 1f)
        }) {
            AsyncImage(model = coffee.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(
                colors = listOf(Color.Transparent, EspressoDeep.copy(alpha = 0.85f)),
                startY = 600f
            )))
            
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 60.dp, end = 100.dp)) {
                Text(text = coffee.marca.uppercase(), color = CaramelAccent, style = MaterialTheme.typography.labelLarge, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = coffee.nombre, color = Color.White, style = MaterialTheme.typography.headlineLarge, lineHeight = 38.sp)
            }

            if (!isCustom) {
                Surface(
                    modifier = Modifier.padding(end = 24.dp, bottom = 60.dp).align(Alignment.BottomEnd),
                    color = Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NOTA", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        val ratingStr = String.format(Locale.getDefault(), "%.1f", coffeeDetails.averageRating)
                        Text(text = ratingStr, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize()) {
            item { Spacer(modifier = Modifier.height(400.dp)) }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp), 
                    color = SoftOffWhite
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        
                        if (!isCustom && coffee.descripcion.isNotBlank()) {
                            Text("HISTORIA", style = MaterialTheme.typography.labelLarge, color = CaramelAccent)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = coffee.descripcion, 
                                style = MaterialTheme.typography.bodyLarge, 
                                color = EspressoDeep,
                                maxLines = if (isDescExpanded) Int.MAX_VALUE else 4,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.animateContentSize()
                            )
                            Text(
                                text = if (isDescExpanded) "LEER MENOS" else "LEER MÁS", 
                                color = CaramelAccent, 
                                fontWeight = FontWeight.Bold, 
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(vertical = 8.dp).clickable { isDescExpanded = !isDescExpanded }
                            )
                            Spacer(Modifier.height(24.dp))
                        }

                        Text("DETALLES TÉCNICOS", style = MaterialTheme.typography.labelLarge, color = CaramelAccent)
                        Spacer(Modifier.height(20.dp))
                        
                        val detailsItems = listOfNotNull(
                            coffee.paisOrigen?.takeIf { it.isNotBlank() }?.let { Triple("PAÍS", it, Icons.Default.Public) },
                            coffee.especialidad.takeIf { it.isNotBlank() }?.let { Triple("ESPECIALIDAD", it, Icons.Default.Verified) },
                            coffee.variedadTipo?.takeIf { it.isNotBlank() }?.let { Triple("VARIEDAD", it, Icons.Default.Category) },
                            coffee.tueste.takeIf { it.isNotBlank() }?.let { Triple("TUESTE", it, Icons.Default.LocalFireDepartment) },
                            coffee.proceso.takeIf { it.isNotBlank() }?.let { Triple("PROCESO", it, Icons.Default.Settings) },
                            coffee.moliendaRecomendada.takeIf { it.isNotBlank() }?.let { Triple("MOLIENDA", it, Icons.Default.Grain) }
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            maxItemsInEachRow = 2
                        ) {
                            detailsItems.forEach { (label, value, icon) ->
                                DetailPremiumBlock(label, value, icon, Modifier.weight(1f))
                            }
                        }

                        if (!isCustom) {
                            Spacer(Modifier.height(40.dp))
                            Text("PERFIL SENSORIAL", style = MaterialTheme.typography.labelLarge, color = CaramelAccent)
                            Spacer(Modifier.height(20.dp))
                            val characteristics = listOf(
                                "Aroma" to coffee.aroma,
                                "Sabor" to coffee.sabor,
                                "Cuerpo" to coffee.cuerpo,
                                "Acidez" to coffee.acidez,
                                "Dulzura" to coffee.dulzura
                            )
                            characteristics.forEach { (label, value) ->
                                PremiumCharacteristicBar(label, value)
                                Spacer(Modifier.height(16.dp))
                            }

                            if (!coffee.productUrl.isNullOrBlank()) {
                                Spacer(Modifier.height(40.dp))
                                Text("ADQUIRIR", style = MaterialTheme.typography.labelLarge, color = CaramelAccent)
                                Spacer(Modifier.height(16.dp))
                                BuyPremiumCard(coffee.productUrl) { openCustomTab(context, it) }
                            }

                            Spacer(Modifier.height(40.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("COMUNIDAD", style = MaterialTheme.typography.labelLarge, color = CaramelAccent)
                                Button(
                                    onClick = { showAddReviewDialog = true }, 
                                    colors = ButtonDefaults.buttonColors(containerColor = EspressoDeep),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (userReview != null) "EDITAR" else "AÑADIR")
                                }
                            }
                            
                            Spacer(Modifier.height(24.dp))
                            reviews.sortedByDescending { it.review.timestamp }.forEach { info ->
                                DetailReviewPremiumItem(info)
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                        Spacer(Modifier.height(120.dp))
                    }
                }
            }
        }

        // --- GLASSY ACTIONS ---
        Row(
            modifier = Modifier.statusBarsPadding().fillMaxWidth().padding(16.dp), 
            horizontalArrangement = Arrangement.SpaceBetween, 
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassyIconButton(icon = Icons.AutoMirrored.Filled.ArrowBackIos, onClick = onBackClick)
            Row {
                GlassyIconButton(icon = Icons.Default.Inventory, onClick = { showStockDialog = true })
                Spacer(Modifier.width(12.dp))
                GlassyIconButton(
                    icon = if (coffeeDetails.isFavorite) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                    iconColor = if (coffeeDetails.isFavorite) ErrorRed else EspressoDeep,
                    onClick = { onFavoriteToggle(!coffeeDetails.isFavorite) }
                )
            }
        }
    }
}

@Composable
fun GlassyIconButton(icon: ImageVector, iconColor: Color = EspressoDeep, onClick: () -> Unit) {
    Surface(
        onClick = onClick, 
        color = Color.White.copy(alpha = 0.8f), 
        shape = RoundedCornerShape(16.dp), 
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun DetailPremiumBlock(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    PremiumCard(modifier = modifier) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(CaramelAccent.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = CaramelAccent, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 8.sp)
                Text(text = value.uppercase(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun PremiumCharacteristicBar(label: String, value: Float) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label.uppercase(), style = MaterialTheme.typography.labelSmall, color = EspressoDeep, fontWeight = FontWeight.Bold)
            val ratingStr = String.format(Locale.getDefault(), "%.1f", value)
            Text(text = "$ratingStr/10", style = MaterialTheme.typography.labelSmall, color = CaramelAccent)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { value / 10f }, 
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), 
            color = CaramelAccent, 
            trackColor = BorderLight
        )
    }
}

@Composable
fun BuyPremiumCard(url: String, onClick: (String) -> Unit) {
    PremiumCard(modifier = Modifier.clickable { onClick(url) }) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Storefront, null, tint = EspressoDeep)
            Spacer(Modifier.width(16.dp))
            val domain = url.removePrefix("https://").removePrefix("www.").substringBefore("/")
            Text(domain.uppercase(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = CaramelAccent)
        }
    }
}

@Composable
fun DetailReviewPremiumItem(info: UserReviewInfo) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ModernAvatar(imageUrl = info.authorAvatarUrl, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(info.authorName ?: "Usuario", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(formatRelativeTime(info.review.timestamp).uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Spacer(Modifier.weight(1f))
            val ratingStr = String.format(Locale.getDefault(), "%.1f", info.review.rating)
            Text(text = ratingStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = CaramelAccent)
        }
        Spacer(Modifier.height(12.dp))
        Text(info.review.comment, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
        if (!info.review.imageUrl.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            AsyncImage(
                model = info.review.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewBottomSheet(existingReview: ReviewEntity?, onDismissRequest: () -> Unit, onSaveReview: (Float, String, Uri?) -> Unit) {
    var rating by remember { mutableFloatStateOf(existingReview?.rating ?: 0f) }
    var comment by remember { mutableStateOf(existingReview?.comment ?: "") }
    var uri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri = it }

    ModalBottomSheet(onDismissRequest = onDismissRequest, containerColor = Color.White, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TU OPINIÓN", style = MaterialTheme.typography.labelLarge, color = CaramelAccent)
            Spacer(Modifier.height(24.dp))
            SemicircleRatingBar(rating = rating, onRatingChanged = { rating = it })
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(
                value = comment, onValueChange = { comment = it }, label = { Text("¿Qué te ha parecido?") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), minLines = 3
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { onSaveReview(rating, comment, uri) }, enabled = rating > 0 && comment.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = EspressoDeep), shape = RoundedCornerShape(28.dp)
            ) { Text("GUARDAR RESEÑA", fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockEditBottomSheet(coffeeDetails: CoffeeWithDetails, isCustom: Boolean, currentStock: PantryItemEntity?, onDismiss: () -> Unit, onSave: (Int, Int, String?, String?) -> Unit) {
    var total by remember { mutableFloatStateOf(currentStock?.totalGrams?.toFloat() ?: 600f) }
    var rem by remember { mutableFloatStateOf(currentStock?.gramsRemaining?.toFloat() ?: total) }
    var name by remember { mutableStateOf(coffeeDetails.coffee.nombre) }
    var brand by remember { mutableStateOf(coffeeDetails.coffee.marca) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GESTIÓN DE STOCK", style = MaterialTheme.typography.labelLarge, color = CaramelAccent)
            Spacer(Modifier.height(24.dp))
            if (isCustom) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Marca") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                Spacer(Modifier.height(24.dp))
            }
            StockSliderSection("TOTAL BOLSA", total, 1000f) { total = it; if (rem > it) rem = it }
            Spacer(Modifier.height(24.dp))
            StockSliderSection("RESTANTE", rem, total) { rem = it }
            Spacer(Modifier.height(40.dp))
            Button(onClick = { onSave(total.roundToInt(), rem.roundToInt(), if(isCustom) name else null, if(isCustom) brand else null) }, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = EspressoDeep), shape = RoundedCornerShape(28.dp)) { Text("ACTUALIZAR", fontWeight = FontWeight.Bold) }
        }
    }
}
