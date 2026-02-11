package com.cafesito.app.ui.detail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.cafesito.app.data.CoffeeWithDetails
import com.cafesito.app.data.PantryItemEntity
import com.cafesito.app.data.ReviewEntity
import com.cafesito.app.data.UserEntity
import com.cafesito.app.data.UserReviewInfo
import com.cafesito.app.ui.components.*
import com.cafesito.app.ui.theme.*
import com.cafesito.app.ui.timeline.CommentsViewModel
import com.cafesito.app.ui.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBackClick: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.loadInitialIfNeeded()
                scope.launch {
                    delay(400)
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            when (val state = uiState) {
                is DetailUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                is DetailUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(state.message, color = MaterialTheme.colorScheme.onSurface) }
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

    if (showAddReviewDialog) {
        ReviewBottomSheet(
            existingReview = userReview,
            onDismissRequest = { showAddReviewDialog = false },
            onSaveReview = { r, c, i -> 
                onReviewSubmit(r, c, i)
                showAddReviewDialog = false
            }
        )
    }

    if (showStockDialog) {
        DetailStockEditBottomSheet(
            coffeeDetails = coffeeDetails,
            isCustom = isCustom,
            currentStock = currentStock,
            onDismiss = { showStockDialog = false },
            onSave = { t, r, n, b -> onUpdateStock(t, r, n, b) }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- HERO IMAGE ---
        Box(modifier = Modifier.fillMaxWidth().height(450.dp).graphicsLayer {
            translationY = -scrollState.firstVisibleItemScrollOffset * 0.5f
            alpha = 1f - (scrollState.firstVisibleItemScrollOffset / 1000f).coerceIn(0f, 1f)
        }) {
            AsyncImage(model = coffee.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                startY = 600f
            )))
            
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 60.dp, end = 100.dp)) {
                Text(text = coffee.marca.uppercase(), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = coffee.nombre, color = Color.White, style = MaterialTheme.typography.headlineLarge, lineHeight = 38.sp)
            }

            if (!isCustom) {
                Surface(
                    modifier = Modifier.padding(end = 24.dp, bottom = 60.dp).align(Alignment.BottomEnd),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "NOTA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val ratingStr = String.format(Locale.getDefault(), "%.1f", coffeeDetails.averageRating)
                        Text(text = ratingStr, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // --- CONTENT ---
        LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize()) {
            item { Spacer(modifier = Modifier.height(400.dp)) }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp), 
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        
                        if (!isCustom && coffee.descripcion.isNotBlank()) {
                            Text(text = "HISTORIA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = coffee.descripcion, 
                                style = MaterialTheme.typography.bodyLarge, 
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.animateContentSize()
                            )
                            Spacer(Modifier.height(24.dp))
                        }

                        Text(text = "DETALLES TÉCNICOS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
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
                            Text(text = "PERFIL SENSORIAL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
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
                                Text(text = "ADQUIRIR", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(16.6.dp))
                                BuyPremiumCard(coffee.productUrl) { openCustomTab(context, it) }
                            }

                            Spacer(Modifier.height(40.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "OPINIONES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { showAddReviewDialog = true }, 
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = if (userReview != null) "EDITAR" else "AÑADIR")
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
            modifier = Modifier.statusBarsPadding().fillMaxWidth().padding(top = 16.dp, start = 16.dp, end = 16.dp), 
            horizontalArrangement = Arrangement.SpaceBetween, 
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassyIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, iconColor = MaterialTheme.colorScheme.onSurface, onClick = onBackClick)
            Row {
                GlassyIconButton(icon = Icons.Default.Inventory, iconColor = MaterialTheme.colorScheme.onSurface, onClick = { showStockDialog = true })
                Spacer(Modifier.width(12.dp))
                GlassyIconButton(
                    icon = if (coffeeDetails.isFavorite) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                    iconColor = if (coffeeDetails.isFavorite) ElectricRed else MaterialTheme.colorScheme.onSurface,
                    onClick = { onFavoriteToggle(!coffeeDetails.isFavorite) }
                )
            }
        }
    }
}

@Composable
fun GlassyIconButton(icon: ImageVector, iconColor: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick, 
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), 
        shape = RoundedCornerShape(16.dp), 
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
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
                Text(text = info.authorName ?: "Usuario", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = formatRelativeTime(info.review.timestamp).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            val ratingStr = String.format(Locale.getDefault(), "%.1f", info.review.rating)
            Text(text = ratingStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(12.dp))
        Text(text = info.review.comment, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface)
        if (!info.review.imageUrl.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            AsyncImage(
                model = info.review.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.FillWidth, // IMAGEN COMPLETA
                modifier = Modifier.fillMaxWidth().wrapContentHeight().clip(RoundedCornerShape(24.dp))
            )
        }
    }
}

@Composable
fun PremiumCharacteristicBar(label: String, value: Float) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            val ratingStr = String.format(Locale.getDefault(), "%.1f", value)
            Text(text = "$ratingStr/10", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { value / 10f }, 
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), 
            color = MaterialTheme.colorScheme.primary, 
            trackColor = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun BuyPremiumCard(url: String, onClick: (String) -> Unit) {
    PremiumCard(modifier = Modifier.clickable { onClick(url) }) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Storefront, null, tint = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(16.dp))
            val domain = url.removePrefix("https://").removePrefix("www.").substringBefore("/")
            Text(text = domain.uppercase(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewBottomSheet(
    existingReview: ReviewEntity?, 
    onDismissRequest: () -> Unit, 
    onSaveReview: (Float, String, Uri?) -> Unit,
    commentsViewModel: CommentsViewModel = hiltViewModel()
) {
    var rating by remember { mutableFloatStateOf(existingReview?.rating ?: 0f) }
    var comment by remember { mutableStateOf(existingReview?.comment ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var showPickerSheet by remember { mutableStateOf(false) }

    val mentionSuggestions by commentsViewModel.mentionSuggestions.collectAsState()
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        // Necesitaríamos guardar el bitmap localmente para obtener un Uri si queremos el mismo comportamiento
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) selectedImageUri = uri
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest, 
        containerColor = MaterialTheme.colorScheme.surfaceContainer, 
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp), 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "TU RESEÑA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            
            SemicircleRatingBar(rating = rating, onRatingChanged = { rating = it })
            
            Spacer(Modifier.height(24.dp))
            
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = comment, 
                    onValueChange = { 
                        comment = it 
                        commentsViewModel.onTextChanged(it)
                    }, 
                    placeholder = { Text(text = "¿Qué te ha parecido?") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), 
                    shape = RoundedCornerShape(16.dp), 
                    enabled = !isSaving,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp).clickable { showPickerSheet = true },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            if (selectedImageUri != null) {
                                AsyncImage(model = selectedImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else if (!existingReview?.imageUrl.isNullOrBlank()) {
                                AsyncImage(model = existingReview?.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Default.PhotoCamera, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(mentionSuggestions) { user ->
                                SuggestionChip(user) {
                                    val parts = comment.split(" ").toMutableList()
                                    parts[parts.size - 1] = "@${user.username} "
                                    comment = parts.joinToString(" ")
                                    commentsViewModel.onTextChanged(comment)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(28.dp),
                    enabled = !isSaving,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = "CANCELAR", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                Button(
                    onClick = { 
                        isSaving = true
                        onSaveReview(rating, comment, selectedImageUri) 
                    }, 
                    enabled = rating > 0 && comment.isNotBlank() && !isSaving,
                    modifier = Modifier.weight(1f).height(54.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(28.dp)
                ) { 
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text(text = "PUBLICAR", fontWeight = FontWeight.Bold, letterSpacing = 1.sp) 
                    }
                }
            }
        }
    }

    if (showPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPickerSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(Modifier.padding(bottom = 40.dp, start = 24.dp, end = 24.dp)) {
                Text(text = "AÑADIR FOTO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp))
                ModalMenuOption("Hacer Foto", Icons.Default.PhotoCamera, MaterialTheme.colorScheme.primary) {
                    cameraLauncher.launch(null)
                    showPickerSheet = false
                }
                ModalMenuOption("Elegir de Galería", Icons.Default.Collections, MaterialTheme.colorScheme.primary) {
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    showPickerSheet = false
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(user: UserEntity, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = user.avatarUrl, contentDescription = null, modifier = Modifier.size(20.dp).clip(CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(text = user.username, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailStockEditBottomSheet(coffeeDetails: CoffeeWithDetails, isCustom: Boolean, currentStock: PantryItemEntity?, onDismiss: () -> Unit, onSave: (Int, Int, String?, String?) -> Unit) {
    var total by remember { mutableFloatStateOf(currentStock?.totalGrams?.toFloat() ?: 600f) }
    var rem by remember { mutableFloatStateOf(currentStock?.gramsRemaining?.toFloat() ?: total) }
    var name by remember { mutableStateOf(coffeeDetails.coffee.nombre) }
    var brand by remember { mutableStateOf(coffeeDetails.coffee.marca) }
    val isInPantry = currentStock != null

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Añadir a mi despensa", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            if (isCustom) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(text = "Nombre") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text(text = "Marca") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary))
                Spacer(Modifier.height(24.dp))
            }
            StockSliderSection(label = "TOTAL BOLSA", value = total, maxValue = 1000f) { total = it; if (rem > it) rem = it }
            Spacer(Modifier.height(24.dp))
            StockSliderSection(label = "RESTANTE", value = rem, maxValue = total) { rem = it }
            Spacer(Modifier.height(40.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = "CANCELAR", fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = { onSave(total.roundToInt(), rem.roundToInt(), if(isCustom) name else null, if(isCustom) brand else null) }, 
                    modifier = Modifier.weight(1f).height(54.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(28.dp)
                ) { 
                    Text(text = if (isInPantry) "ACTUALIZAR" else "AÑADIR", fontWeight = FontWeight.Bold) 
                }
            }
        }
    }
}
