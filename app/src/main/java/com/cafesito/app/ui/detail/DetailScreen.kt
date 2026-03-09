package com.cafesito.app.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cafesito.app.R
import com.cafesito.app.data.CoffeeWithDetails
import com.cafesito.app.data.PantryItemEntity
import com.cafesito.app.data.ReviewEntity
import com.cafesito.app.data.UserEntity
import com.cafesito.app.data.UserReviewInfo
import com.cafesito.app.ui.components.*
import com.cafesito.app.ui.components.toCoffeeBrandFormat
import com.cafesito.app.ui.components.toCoffeeNameFormat
import com.cafesito.app.ui.theme.*
import com.cafesito.app.ui.timeline.CommentsViewModel
import com.cafesito.app.ui.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale
import kotlin.math.roundToInt

/** Slug para URL de detalle de café (compatible con WebApp toCoffeeSlug). */
private fun toCoffeeSlug(nombre: String, marca: String): String {
    fun slugify(value: String): String {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").replace(Regex("\\s+"), " ").trim().lowercase()
        return normalized.replace(Regex("[^a-z0-9\\s-]"), "").replace(Regex("\\s+"), "-").replace(Regex("-+"), "-").replace(Regex("^-|-$"), "")
    }
    val baseFromName = slugify(nombre)
    if (baseFromName.length > 10) return baseFromName.ifBlank { "cafe" }
    val withBrand = slugify("$nombre ${marca.ifBlank { "" }}")
    return withBrand.ifBlank { baseFromName.ifBlank { "cafe" } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBackClick: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
    commentsViewModel: CommentsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val allUsers by commentsViewModel.allUsers.collectAsState()
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
                is DetailUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorStateMessage(message = state.message, onRetry = { viewModel.loadInitialIfNeeded() })
                }
                is DetailUiState.Success -> {
                    DetailContent(
                        coffeeDetails = state.coffee,
                        userReview = state.userReview,
                        reviews = state.reviews,
                        isCustom = state.isCustom,
                        currentStock = state.currentPantryItem,
                        activeUser = state.activeUser,
                        onBackClick = onBackClick,
                        onFavoriteToggle = { viewModel.toggleFavorite(it) },
                        onUpdateStock = { t, r, n, b -> viewModel.updateStock(t, r, n, b) },
                        onReviewSubmit = { r, c, i -> viewModel.submitReview(r, c, i) },
                        onReviewDelete = { viewModel.deleteReview() },
                        onSensorySubmit = { a, sa, cu, ac, du -> viewModel.submitSensoryProfile(a, sa, cu, ac, du) },
                        sensoryAverages = state.sensoryAverages,
                        sensoryEditorsCount = state.sensoryEditorsCount,
                        allUsers = allUsers
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
    activeUser: UserEntity?,
    onBackClick: () -> Unit,
    onFavoriteToggle: (Boolean) -> Unit,
    onUpdateStock: (Int, Int, String?, String?) -> Unit,
    onReviewSubmit: (Float, String, Uri?) -> Unit,
    onReviewDelete: () -> Unit,
    onSensorySubmit: (Float, Float, Float, Float, Float) -> Unit,
    sensoryAverages: Map<String, Float>,
    sensoryEditorsCount: Int,
    allUsers: List<UserEntity>
) {
    val scrollState = rememberLazyListState()
    val coffee = coffeeDetails.coffee
    val context = LocalContext.current
    val usersByUsername = remember(allUsers) { allUsers.associateBy { it.username.lowercase() } }
    var showAddReviewDialog by remember { mutableStateOf(false) }
    var showStockDialog by remember { mutableStateOf(false) }
    var showSensoryEditor by remember { mutableStateOf(false) }

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

    if (showSensoryEditor) {
        SensoryProfileBottomSheet(
            initialValues = listOf(
                "Aroma" to (sensoryAverages["Aroma"] ?: coffee.aroma),
                "Sabor" to (sensoryAverages["Sabor"] ?: coffee.sabor),
                "Cuerpo" to (sensoryAverages["Cuerpo"] ?: coffee.cuerpo),
                "Acidez" to (sensoryAverages["Acidez"] ?: coffee.acidez),
                "Dulzura" to (sensoryAverages["Dulzura"] ?: coffee.dulzura)
            ),
            onDismiss = { showSensoryEditor = false },
            onConfirm = { updatedValues ->
                val map = updatedValues.toMap()
                onSensorySubmit(
                    map["Aroma"] ?: 0f,
                    map["Sabor"] ?: 0f,
                    map["Cuerpo"] ?: 0f,
                    map["Acidez"] ?: 0f,
                    map["Dulzura"] ?: 0f
                )
                showSensoryEditor = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- HERO IMAGE ---
        Box(modifier = Modifier.fillMaxWidth().height(450.dp).graphicsLayer {
            translationY = -scrollState.firstVisibleItemScrollOffset * 0.5f
            alpha = 1f - (scrollState.firstVisibleItemScrollOffset / 1000f).coerceIn(0f, 1f)
        }) {
            AsyncImage(model = coffee.imageUrl, contentDescription = "Foto del café ${coffee.nombre}", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                startY = 600f
            )))
            
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 60.dp, end = 100.dp)) {
                Text(text = coffee.marca.uppercase(), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = coffee.nombre, color = Color.White, style = MaterialTheme.typography.headlineLarge, lineHeight = 38.sp)
            }

            if (!isCustom && reviews.isNotEmpty()) {
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

                        val detailsItems = listOfNotNull(
                            coffee.paisOrigen?.takeIf { it.isNotBlank() }?.let { Triple("PAÍS", it, Icons.Default.Public) },
                            coffee.especialidad?.takeIf { it.isNotBlank() }?.let { Triple("ESPECIALIDAD", it, Icons.Default.Verified) },
                            coffee.variedadTipo?.takeIf { it.isNotBlank() }?.let { Triple("VARIEDAD", it, Icons.Default.Category) },
                            coffee.tueste.takeIf { it.isNotBlank() }?.let { Triple("TUESTE", it, Icons.Default.LocalFireDepartment) },
                            coffee.proceso.takeIf { it.isNotBlank() }?.let { Triple("PROCESO", it, Icons.Default.Settings) },
                            coffee.moliendaRecomendada.takeIf { it.isNotBlank() }?.let { Triple("MOLIENDA", it, Icons.Default.Grain) }
                        )
                        if (detailsItems.isNotEmpty()) {
                            Text(text = "DETALLES TÉCNICOS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(20.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                maxItemsInEachRow = 2
                            ) {
                                detailsItems.forEach { (label, value, icon) ->
                                    DetailPremiumBlock(label, value, icon, Modifier.fillMaxWidth(0.48f))
                                }
                            }
                        }

                        if (!isCustom) {
                            Spacer(Modifier.height(40.dp))
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "PERFIL SENSORIAL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = { showSensoryEditor = true }) { Text("Editar") }
                            }
                            if (sensoryEditorsCount > 0) {
                                Text(
                                    text = "Basado en los comentarios de $sensoryEditorsCount usuarios. $sensoryEditorsCount son las personas que lo han editado.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(12.dp))
                            } else {
                                Spacer(Modifier.height(8.dp))
                            }
                            listOf(
                                "Aroma" to (sensoryAverages["Aroma"] ?: coffee.aroma),
                                "Sabor" to (sensoryAverages["Sabor"] ?: coffee.sabor),
                                "Cuerpo" to (sensoryAverages["Cuerpo"] ?: coffee.cuerpo),
                                "Acidez" to (sensoryAverages["Acidez"] ?: coffee.acidez),
                                "Dulzura" to (sensoryAverages["Dulzura"] ?: coffee.dulzura)
                            ).forEach { (label, value) ->
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
                                    Icon(if (userReview != null) Icons.Default.Edit else Icons.Default.Add, contentDescription = if (userReview != null) "Editar reseña" else "Añadir reseña", Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = if (userReview != null) "EDITAR" else "AÑADIR")
                                }
                            }
                            
                            Spacer(Modifier.height(24.dp))
                            if (reviews.isEmpty()) {
                                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                    Text("No hay opiniones aún. ¡Sé el primero!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                reviews.sortedByDescending { it.review.timestamp }.forEach { info ->
                                    val isOwnReview = info.review.userId == activeUser?.id
                                    if (isOwnReview) {
                                        val dismissState = rememberSwipeToDismissBoxState(
                                            confirmValueChange = {
                                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                                    onReviewDelete()
                                                    true
                                                } else false
                                            }
                                        )

                                        SwipeToDismissBox(
                                            state = dismissState,
                                            enableDismissFromStartToEnd = false,
                                            backgroundContent = {
                                                val isDragging = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
                                                val color = if (isDragging) ElectricRed else Color.Transparent
                                                Box(
                                                    Modifier
                                                        .fillMaxSize()
                                                        .clip(RoundedCornerShape(24.dp))
                                                        .background(color),
                                                    contentAlignment = Alignment.CenterEnd
                                                ) {
                                                    if (isDragging) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Borrar",
                                                            tint = if (isSystemInDarkTheme()) Color.Black else Color.White,
                                                            modifier = Modifier
                                                                .padding(end = 24.dp)
                                                                .graphicsLayer {
                                                                    val p = dismissState.progress
                                                                    alpha = if (p > 0.05f) p.coerceIn(0f, 1f) else 0f
                                                                    scaleX = if (p > 0.05f) p.coerceIn(0.5f, 1f) else 0.5f
                                                                    scaleY = if (p > 0.05f) p.coerceIn(0.5f, 1f) else 0.5f
                                                                }
                                                        )
                                                    }
                                                }
                                            }
                                        ) {
                                            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                                                DetailReviewPremiumItem(
                                                    info = info,
                                                    isOwnReview = true,
                                                    resolveMentionUser = { username -> usersByUsername[username.trim().lowercase()] }
                                                )
                                            }
                                        }
                                    } else {
                                        DetailReviewPremiumItem(
                                            info = info,
                                            isOwnReview = false,
                                            resolveMentionUser = { username -> usersByUsername[username.trim().lowercase()] }
                                        )
                                    }
                                    Spacer(Modifier.height(24.dp))
                                }
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
            GlassyIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, iconColor = Color.Black, contentDescription = "Volver", onClick = onBackClick)
            Row {
                GlassyIconButton(
                    icon = Icons.Default.Share,
                    iconColor = Color.Black,
                    contentDescription = "Compartir café",
                    onClick = {
                        val slug = toCoffeeSlug(coffee.nombre, coffee.marca)
                        val link = "https://cafesitoapp.com/coffee/$slug/"
                        val text = "${coffee.marca} ${coffee.nombre} – Cafesito: $link"
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Compartir café"))
                    }
                )
                Spacer(Modifier.width(12.dp))
                GlassyIconButton(
                    iconPainter = painterResource(id = R.drawable.shelves_24),
                    iconColor = Color.Black,
                    contentDescription = "Añadir a despensa",
                    onClick = { showStockDialog = true }
                )
                Spacer(Modifier.width(12.dp))
                GlassyIconButton(
                    icon = if (coffeeDetails.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    iconColor = if (coffeeDetails.isFavorite) ElectricRed else Color.Black,
                    premiumAnimated = true,
                    contentDescription = if (coffeeDetails.isFavorite) "Quitar de favoritos" else "Guardar en favoritos",
                    onClick = { onFavoriteToggle(!coffeeDetails.isFavorite) }
                )
            }
        }
    }
}

@Composable
fun GlassyIconButton(
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    iconColor: Color,
    premiumAnimated: Boolean = false,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    val iconScale = remember { Animatable(1f) }
    val iconRotation = remember { Animatable(0f) }
    val iconGlow = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val onPremiumClick = {
        if (premiumAnimated) {
            scope.launch {
                iconScale.animateTo(
                    targetValue = 1f,
                    animationSpec = keyframes {
                        durationMillis = 540
                        1.34f at 100
                        0.88f at 240
                        1.16f at 360
                        1f at 540
                    }
                )
            }
            scope.launch {
                iconRotation.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 480
                        -19f at 120
                        14f at 220
                        -8f at 330
                        0f at 480
                    }
                )
            }
            scope.launch {
                iconGlow.snapTo(0f)
                iconGlow.animateTo(1f, animationSpec = tween(190, easing = FastOutSlowInEasing))
                iconGlow.animateTo(0f, animationSpec = tween(360, easing = FastOutSlowInEasing))
            }
        }
        onClick()
    }

    Surface(
        onClick = onPremiumClick,
        color = PureWhite,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                iconPainter != null -> Icon(
                    painter = iconPainter,
                    contentDescription = contentDescription,
                    tint = iconColor,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            scaleX = iconScale.value
                            scaleY = iconScale.value
                            rotationZ = iconRotation.value
                            shadowElevation = 24f * iconGlow.value
                            alpha = 0.84f + (0.16f * iconGlow.value)
                        }
                )
                icon != null -> Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = iconColor,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            scaleX = iconScale.value
                            scaleY = iconScale.value
                            rotationZ = iconRotation.value
                            shadowElevation = 24f * iconGlow.value
                            alpha = 0.84f + (0.16f * iconGlow.value)
                        }
                )
            }
        }
    }
}

@Composable
fun DetailReviewPremiumItem(
    info: UserReviewInfo,
    isOwnReview: Boolean,
    resolveMentionUser: (String) -> UserEntity? = { null }
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ModernAvatar(imageUrl = info.authorAvatarUrl, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = info.authorName ?: "Usuario", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    if (isOwnReview) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ) {
                            Text("TÚ", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(text = formatRelativeTime(info.review.timestamp).uppercase(), style = MaterialTheme.typography.labelSmall, color = LocalDateMetaColor.current)
            }
            Spacer(Modifier.weight(1f))
            val ratingStr = String.format(Locale.getDefault(), "%.1f", info.review.rating)
            Text(text = ratingStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(12.dp))
        MentionText(
            text = info.review.comment,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface),
            onMentionClick = {},
            resolveMentionUser = resolveMentionUser
        )
        if (!info.review.imageUrl.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            AsyncImage(
                model = info.review.imageUrl,
                contentDescription = "Imagen de la reseña",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().wrapContentHeight().clip(RoundedCornerShape(24.dp))
            )
        }
    }
}

@Composable
fun PremiumCharacteristicBar(label: String, value: Float) {
    val caramelColor = LocalCaramelAccent.current
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            val ratingStr = String.format(Locale.getDefault(), "%.1f", value)
            Text(text = "$ratingStr/10", style = MaterialTheme.typography.labelSmall, color = caramelColor)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { value / 10f },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = caramelColor,
            trackColor = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun BuyPremiumCard(url: String, onClick: (String) -> Unit) {
    PremiumCard(modifier = Modifier.clickable { onClick(url) }) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Storefront, contentDescription = "Tienda", tint = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(16.dp))
            val domain = url.removePrefix("https://").removePrefix("www.").substringBefore("/")
            Text(text = domain.uppercase(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Ir a la tienda", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SensoryProfileBottomSheet(
    initialValues: List<Pair<String, Float>>,
    onDismiss: () -> Unit,
    onConfirm: (List<Pair<String, Float>>) -> Unit
) {
    val values = remember(initialValues) { initialValues.toMutableStateList() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val caramelColor = LocalCaramelAccent.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Text("Perfil sensorial", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Tu opinión se unirá a la media de todas las valoraciones",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            values.forEachIndexed { index, (label, score) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f", score.coerceIn(0f, 10f)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = score.coerceIn(0f, 10f),
                        onValueChange = { updated -> values[index] = label to updated },
                        valueRange = 0f..10f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.outline,
                            activeTrackColor = caramelColor,
                            inactiveTrackColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Text("10", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
            }
            Button(
                onClick = { onConfirm(values.toList()) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = caramelColor)
            ) {
                Text("Listo")
            }
            Spacer(Modifier.height(16.dp))
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
    var commentValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = existingReview?.comment ?: "",
                selection = TextRange((existingReview?.comment ?: "").length)
            )
        )
    }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var showPickerSheet by remember { mutableStateOf(false) }

    val mentionSuggestions by commentsViewModel.mentionSuggestions.collectAsState()
    val allUsers by commentsViewModel.allUsers.collectAsState()
    val context = LocalContext.current
    
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) selectedImageUri = uri
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest, 
        containerColor = MaterialTheme.colorScheme.surfaceContainer, 
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.5f)
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
                MentionComposerField(
                    value = commentValue,
                    onValueChange = { 
                        commentValue = it
                        commentsViewModel.onTextChanged(it.text)
                    }, 
                    placeholder = "¿Qué te ha parecido?",
                    validUsers = allUsers,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    minHeight = 120.dp
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
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            if (selectedImageUri != null) {
                                AsyncImage(model = selectedImageUri, contentDescription = "Foto de la reseña", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else if (!existingReview?.imageUrl.isNullOrBlank()) {
                                AsyncImage(model = existingReview?.imageUrl, contentDescription = "Foto de la reseña", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Default.PhotoCamera, contentDescription = "Añadir foto a la reseña", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(mentionSuggestions) { user ->
                                SuggestionChip(user) {
                                    val updated = commentValue.text
                                        .trimEnd()
                                        .split(" ")
                                        .toMutableList()
                                        .let { tokens ->
                                            if (tokens.isEmpty() || tokens.firstOrNull().isNullOrBlank()) {
                                                "@${user.username} "
                                            } else {
                                                val lastIndex = tokens.lastIndex
                                                tokens[lastIndex] = if (tokens[lastIndex].startsWith("@")) "@${user.username}" else "${tokens[lastIndex]} @${user.username}"
                                                tokens.joinToString(" ") + " "
                                            }
                                        }
                                    commentValue = TextFieldValue(updated, selection = TextRange(updated.length))
                                    commentsViewModel.onTextChanged(updated)
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
                        onSaveReview(rating, commentValue.text, selectedImageUri) 
                    }, 
                    enabled = rating > 0 && commentValue.text.isNotBlank() && !isSaving,
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
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            scrimColor = Color.Black.copy(alpha = 0.5f)
        ) {
            Column(Modifier.padding(bottom = 40.dp, start = 24.dp, end = 24.dp)) {
                Text(text = "AÑADIR FOTO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp))
                ModalMenuOption("Elegir de Galería", Icons.Default.Collections, MaterialTheme.colorScheme.primary) {
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    showPickerSheet = false
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailStockEditBottomSheet(coffeeDetails: CoffeeWithDetails, isCustom: Boolean, currentStock: PantryItemEntity?, onDismiss: () -> Unit, onSave: (Int, Int, String?, String?) -> Unit) {
    var total by remember { mutableFloatStateOf(currentStock?.totalGrams?.toFloat() ?: 600f) }
    var rem by remember { mutableFloatStateOf(currentStock?.gramsRemaining?.toFloat() ?: total) }
    var name by remember { mutableStateOf(coffeeDetails.coffee.nombre.toCoffeeNameFormat()) }
    var brand by remember { mutableStateOf(coffeeDetails.coffee.marca.toCoffeeBrandFormat()) }
    val isInPantry = currentStock != null
    val caramelColor = LocalCaramelAccent.current

    ModalBottomSheet(
        onDismissRequest = onDismiss, 
        containerColor = MaterialTheme.colorScheme.surfaceContainer, 
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Añadir a mi despensa", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            if (isCustom) {
                OutlinedTextField(value = name, onValueChange = { name = it.toCoffeeNameFormat() }, label = { Text(text = "Nombre") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = brand, onValueChange = { brand = it.toCoffeeBrandFormat() }, label = { Text(text = "Marca") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary))
                Spacer(Modifier.height(24.dp))
            }
            StockSliderSection(
                label = "Cantidad de cafe total (g)", 
                value = total, 
                maxValue = 1000f,
                onValueChange = { 
                    total = it
                    if (rem > it) rem = it 
                }
            )
            Spacer(Modifier.height(24.dp))
            StockSliderSection(
                label = "Cantidad de cafe restante (g)", 
                value = rem, 
                maxValue = total,
                onValueChange = { rem = it }
            )
            Spacer(Modifier.height(40.dp))

            val saveBackground = caramelColor
            val saveTextColor = if (isSystemInDarkTheme()) Color.Black else Color.White
            val cancelBorderAndText = MaterialTheme.colorScheme.onSurface

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, cancelBorderAndText),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = cancelBorderAndText)
                ) {
                    Text(text = "CANCELAR", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onSave(total.roundToInt(), rem.roundToInt(), if(isCustom) name else null, if(isCustom) brand else null) },
                    modifier = Modifier.weight(1f).height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = saveBackground,
                        contentColor = saveTextColor
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(text = if (isInPantry) "ACTUALIZAR" else "AÑADIR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
