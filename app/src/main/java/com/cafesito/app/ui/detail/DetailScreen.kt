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
import androidx.compose.material.icons.outlined.Star
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
import android.os.Bundle
import androidx.core.os.bundleOf
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
    commentsViewModel: CommentsViewModel = hiltViewModel(),
    onTrackEvent: (String, Bundle) -> Unit = { _, _ -> }
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
                is DetailUiState.Loading -> DetailLoadingContent()
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
                        onTrackEvent = onTrackEvent,
                        onFavoriteToggle = { viewModel.toggleFavorite(it) },
                        onUpdateStock = { t, r, n, b -> viewModel.updateStock(t, r, n, b) },
                        onReviewSubmit = { r, c, i -> viewModel.submitReview(r, c, i) },
                        onReviewDelete = { viewModel.deleteReview() },
                        onSensorySubmit = { a, sa, cu, ac, du -> viewModel.submitSensoryProfile(a, sa, cu, ac, du) },
                        sensoryAverages = state.sensoryAverages,
                        sensoryEditorsCount = state.sensoryEditorsCount,
                        allUsers = allUsers,
                        userLists = state.userLists,
                        listIdsContainingCoffee = state.listIdsContainingCoffee,
                        isListActive = state.isListActive,
                        isFavorite = state.isFavorite,
                        currentUserId = state.activeUser?.id ?: 0,
                        onCreateList = { name, privacy, membersCanEdit -> viewModel.createList(name, privacy, membersCanEdit) },
                        onApplyAddToList = { add, remove, fav -> viewModel.applyAddToListModal(add, remove, fav) },
                        onRefreshAddToListModal = { viewModel.refreshForAddToListModal() }
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
    onTrackEvent: (String, Bundle) -> Unit = { _, _ -> },
    onFavoriteToggle: (Boolean) -> Unit,
    onUpdateStock: (Int, Int, String?, String?) -> Unit,
    onReviewSubmit: (Float, String, Uri?) -> Unit,
    onReviewDelete: () -> Unit,
    onSensorySubmit: (Float, Float, Float, Float, Float) -> Unit,
    sensoryAverages: Map<String, Float>,
    sensoryEditorsCount: Int,
    allUsers: List<UserEntity>,
    userLists: List<com.cafesito.app.data.UserListRow> = emptyList(),
    listIdsContainingCoffee: Set<String> = emptySet(),
    isListActive: Boolean = false,
    isFavorite: Boolean = false,
    currentUserId: Int = 0,
    onCreateList: (name: String, privacy: String, membersCanEdit: Boolean) -> Unit = { _, _, _ -> },
    onApplyAddToList: (add: Set<String>, remove: Set<String>, favoriteShouldBe: Boolean) -> Unit = { _, _, _ -> },
    onRefreshAddToListModal: () -> Unit = {}
) {
    val scrollState = rememberLazyListState()
    val coffee = coffeeDetails.coffee
    val context = LocalContext.current
    val usersByUsername = remember(allUsers) { allUsers.associateBy { it.username.lowercase() } }
    var showAddReviewDialog by remember { mutableStateOf(false) }
    var showStockDialog by remember { mutableStateOf(false) }
    var showSensoryEditor by remember { mutableStateOf(false) }
    var showAddToListModal by remember { mutableStateOf(false) }
    LaunchedEffect(showAddToListModal) {
        if (showAddToListModal) onRefreshAddToListModal()
    }
    var showCreateListSheet by remember { mutableStateOf(false) }
    val userListsForAddModal = remember(userLists, currentUserId) {
        userLists.filter { list ->
            list.userId == currentUserId.toLong() || list.membersCanEdit == true
        }
    }

    if (showAddReviewDialog) {
        ReviewBottomSheet(
            existingReview = userReview,
            onDismissRequest = { onTrackEvent("modal_close", bundleOf("modal_id" to "review")); showAddReviewDialog = false },
            onSaveReview = { r, c, i ->
                onReviewSubmit(r, c, i)
                onTrackEvent("modal_close", bundleOf("modal_id" to "review"))
                showAddReviewDialog = false
            },
            onDeleteRequest = if (userReview != null) {
                { onReviewDelete(); onTrackEvent("modal_close", bundleOf("modal_id" to "review")); showAddReviewDialog = false }
            } else null
        )
    }

    if (showStockDialog) {
        DetailStockEditBottomSheet(
            coffeeDetails = coffeeDetails,
            isCustom = isCustom,
            currentStock = currentStock,
            onDismiss = { onTrackEvent("modal_close", bundleOf("modal_id" to "stock_edit")); showStockDialog = false },
            onSave = { t: Int, r: Int, n: String?, b: String? ->
                onTrackEvent("modal_close", bundleOf("modal_id" to "stock_edit"))
                onUpdateStock(t, r, n, b)
            }
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
            onDismiss = { onTrackEvent("modal_close", bundleOf("modal_id" to "sensory_profile")); showSensoryEditor = false },
            onConfirm = { updatedValues ->
                val map = updatedValues.toMap()
                onSensorySubmit(
                    map["Aroma"] ?: 0f,
                    map["Sabor"] ?: 0f,
                    map["Cuerpo"] ?: 0f,
                    map["Acidez"] ?: 0f,
                    map["Dulzura"] ?: 0f
                )
                onTrackEvent("modal_close", bundleOf("modal_id" to "sensory_profile"))
                showSensoryEditor = false
            }
        )
    }

    if (showCreateListSheet) {
        com.cafesito.app.ui.components.CreateListBottomSheet(
            onDismiss = { onTrackEvent("modal_close", bundleOf("modal_id" to "create_list")); showCreateListSheet = false },
            onCreate = { name, privacy, membersCanEdit ->
                onCreateList(name, privacy, membersCanEdit)
                onTrackEvent("modal_close", bundleOf("modal_id" to "create_list"))
                showCreateListSheet = false
                showAddToListModal = true
            }
        )
    }

    if (showAddToListModal) {
        com.cafesito.app.ui.components.AddToListBottomSheet(
            onDismiss = { onTrackEvent("modal_close", bundleOf("modal_id" to "add_to_list")); showAddToListModal = false },
            currentUserId = currentUserId,
            userLists = userListsForAddModal,
            listIdsContainingCoffee = listIdsContainingCoffee,
            isFavorite = isFavorite,
            onCreateListRequest = {
                onTrackEvent("modal_close", bundleOf("modal_id" to "add_to_list"))
                onTrackEvent("modal_open", bundleOf("modal_id" to "create_list"))
                showAddToListModal = false
                showCreateListSheet = true
            },
            onApply = { toAdd, toRemove, favoriteShouldBe ->
                onApplyAddToList(toAdd, toRemove, favoriteShouldBe)
                onTrackEvent("modal_close", bundleOf("modal_id" to "add_to_list"))
                showAddToListModal = false
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
                colors = listOf(Color.Transparent, PureBlack.copy(alpha = 0.85f)),
                startY = 600f
            )))
            
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 60.dp, end = 100.dp)) {
                Text(text = coffee.marca.uppercase(), color = PureWhite.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(text = coffee.nombre, color = PureWhite, style = MaterialTheme.typography.headlineLarge)
            }

            if (!isCustom && reviews.isNotEmpty()) {
                Surface(
                    modifier = Modifier.padding(end = 24.dp, bottom = 60.dp).align(Alignment.BottomEnd),
                    color = PureWhite,
                    shape = Shapes.card
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "NOTA", style = MaterialTheme.typography.labelSmall, color = PureBlack)
                        val ratingStr = String.format(Locale.getDefault(), "%.1f", coffeeDetails.averageRating)
                        Text(text = ratingStr, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = PureBlack)
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
                    shape = Shapes.sheetLarge, 
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

                        data class DetailTechnicalItem(
                            val label: String,
                            val value: String,
                            val icon: ImageVector? = null,
                            val iconPainter: Painter? = null
                        )

                        val detailsItems = listOfNotNull(
                            coffee.paisOrigen?.takeIf { it.isNotBlank() }?.let {
                                DetailTechnicalItem(
                                    label = "PAÍS",
                                    value = it,
                                    iconPainter = painterResource(id = R.drawable.pais)
                                )
                            },
                            coffee.especialidad?.takeIf { it.isNotBlank() }?.let {
                                DetailTechnicalItem(
                                    label = "ESPECIALIDAD",
                                    value = it,
                                    iconPainter = painterResource(id = R.drawable.especialidad)
                                )
                            },
                            coffee.variedadTipo?.takeIf { it.isNotBlank() }?.let {
                                DetailTechnicalItem(
                                    label = "VARIEDAD",
                                    value = it,
                                    iconPainter = painterResource(id = R.drawable.variedad)
                                )
                            },
                            coffee.tueste.takeIf { it.isNotBlank() }?.let {
                                DetailTechnicalItem(
                                    label = "TUESTE",
                                    value = it,
                                    iconPainter = painterResource(id = R.drawable.tueste)
                                )
                            },
                            coffee.proceso.takeIf { it.isNotBlank() }?.let {
                                DetailTechnicalItem(
                                    label = "PROCESO",
                                    value = it,
                                    iconPainter = painterResource(id = R.drawable.proceso)
                                )
                            },
                            coffee.moliendaRecomendada.takeIf { it.isNotBlank() }?.let {
                                DetailTechnicalItem(label = "MOLIENDA", value = it, icon = Icons.Default.Grain)
                            },
                            DetailTechnicalItem(
                                label = "FORMATO",
                                value = coffee.formato.takeIf { it.isNotBlank() } ?: "No especificado",
                                iconPainter = painterResource(id = R.drawable.formato)
                            ),
                            DetailTechnicalItem(
                                label = "CAFEÍNA",
                                value = coffee.cafeina.takeIf { it.isNotBlank() } ?: "No especificada",
                                iconPainter = painterResource(id = R.drawable.grano_cafe)
                            )
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
                                detailsItems.forEach { item ->
                                    when {
                                        item.iconPainter != null -> DetailPremiumBlock(
                                            label = item.label,
                                            value = item.value,
                                            icon = item.iconPainter,
                                            modifier = Modifier.fillMaxWidth(0.48f)
                                        )
                                        item.icon != null -> DetailPremiumBlock(
                                            label = item.label,
                                            value = item.value,
                                            icon = item.icon,
                                            modifier = Modifier.fillMaxWidth(0.48f)
                                        )
                                    }
                                }
                            }
                        }

                        if (!isCustom) {
                            Spacer(Modifier.height(40.dp))
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "PERFIL SENSORIAL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = { onTrackEvent("modal_open", bundleOf("modal_id" to "sensory_profile")); showSensoryEditor = true }) { Text("Editar") }
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
                                if (userReview == null) {
                                    Button(
                                        onClick = { onTrackEvent("modal_open", bundleOf("modal_id" to "review")); showAddReviewDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = Shapes.shapeCardMedium
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Añadir reseña", Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(text = "AÑADIR")
                                    }
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
                                                        .clip(Shapes.pill)
                                                        .background(color),
                                                    contentAlignment = Alignment.CenterEnd
                                                ) {
                                                    if (isDragging) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Borrar",
                                                            tint = if (isSystemInDarkTheme()) PureBlack else PureWhite,
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
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                DetailReviewPremiumItem(
                                                    info = info,
                                                    isOwnReview = true,
                                                    resolveMentionUser = { username -> usersByUsername[username.trim().lowercase()] },
                                                    onEditClick = { onTrackEvent("modal_open", bundleOf("modal_id" to "review")); showAddReviewDialog = true }
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
            GlassyIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, iconColor = PureBlack, contentDescription = "Volver", onClick = onBackClick)
            Row {
                GlassyIconButton(
                    icon = Icons.Default.Share,
                    iconColor = PureBlack,
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
                    iconColor = PureBlack,
                    contentDescription = "Añadir a despensa",
                    onClick = { onTrackEvent("modal_open", bundleOf("modal_id" to "stock_edit")); showStockDialog = true }
                )
                Spacer(Modifier.width(12.dp))
                GlassyIconButton(
                    iconPainter = if (isListActive) rememberListAltCheckSvgPainter() else rememberListAltAddSvgPainter(),
                    iconColor = if (isListActive) ElectricGreen else PureBlack,
                    premiumAnimated = true,
                    contentDescription = if (isListActive) "Quitar de listas" else "Añadir a listas",
                    onClick = { onTrackEvent("modal_open", bundleOf("modal_id" to "add_to_list")); showAddToListModal = true }
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
        shape = Shapes.card,
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
    resolveMentionUser: (String) -> UserEntity? = { null },
    onEditClick: (() -> Unit)? = null
) {
    val starTint = if (isSystemInDarkTheme()) PureBlack else PureWhite
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.card,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                ModernAvatar(imageUrl = info.authorAvatarUrl, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
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
                    Spacer(Modifier.height(4.dp))
                    Text(text = formatRelativeTime(info.review.timestamp).uppercase(), style = MaterialTheme.typography.labelSmall, color = LocalDateMetaColor.current)
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = Shapes.pillFull,
                        color = OrangeYellow
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Star, contentDescription = "Nota", modifier = Modifier.size(14.dp), tint = starTint)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${info.review.rating.toInt()}/5",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = starTint
                            )
                        }
                    }
                }
                if (isOwnReview && onEditClick != null) {
                    val editButtonBg = LocalCaramelAccent.current
                    val editButtonTextColor = if (isSystemInDarkTheme()) PureBlack else PureWhite
                    Button(
                        onClick = onEditClick,
                        modifier = Modifier.height(40.dp),
                        shape = Shapes.shapeCardMedium,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = editButtonBg, contentColor = editButtonTextColor),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                    ) {
                        Text(text = "Editar", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (info.review.comment.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                MentionText(
                    text = info.review.comment,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface),
                    onMentionClick = {},
                    resolveMentionUser = resolveMentionUser,
                    modifier = Modifier.padding(start = 52.dp)
                )
            }
            if (!info.review.imageUrl.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    model = info.review.imageUrl,
                    contentDescription = "Imagen de la reseña",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().wrapContentHeight().clip(Shapes.cardSmall)
                )
            }
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
                shape = Shapes.cardSmall,
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
    onDeleteRequest: (() -> Unit)? = null,
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
        shape = Shapes.sheetLarge,
        sheetState = sheetState,
        scrimColor = ScrimDefault
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

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.card,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
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
                            .padding(4.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            val cameraBg = if (isSystemInDarkTheme()) CameraBgDark else CameraBgLight
                            Surface(
                                modifier = Modifier.size(36.dp).clickable { showPickerSheet = true },
                                shape = CircleShape,
                                color = cameraBg
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    if (selectedImageUri != null) {
                                        AsyncImage(model = selectedImageUri, contentDescription = "Foto de la reseña", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } else if (!existingReview?.imageUrl.isNullOrBlank()) {
                                        AsyncImage(model = existingReview.imageUrl, contentDescription = "Foto de la reseña", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } else {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = "Añadir foto a la reseña", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
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
            }

            Spacer(Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val deleteTextColor = if (isSystemInDarkTheme()) PureBlack else PureWhite
                if (existingReview != null && onDeleteRequest != null) {
                    Button(
                        onClick = {
                            onDeleteRequest()
                            onDismissRequest()
                        },
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = Shapes.shapeXl,
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricRed, contentColor = deleteTextColor)
                    ) {
                        Text(text = "ELIMINAR", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                val publishEnabled = rating > 0 && commentValue.text.isNotBlank() && !isSaving
                val publishBg = if (publishEnabled) CaramelAccent else if (isSystemInDarkTheme()) ButtonInactiveDark else ButtonInactiveLight
                Button(
                    onClick = {
                        isSaving = true
                        onSaveReview(rating, commentValue.text, selectedImageUri)
                    },
                    enabled = publishEnabled,
                    modifier = Modifier.weight(1f).height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = publishBg, contentColor = if (publishEnabled) PureWhite else MaterialTheme.colorScheme.onSurfaceVariant),
                    shape = Shapes.shapeXl
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
            scrimColor = ScrimDefault
        ) {
            Column(Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp, bottom = 40.dp)) {
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
        shape = Shapes.sheetLarge,
        scrimColor = ScrimDefault
    ) {
        Column(Modifier.fillMaxWidth().padding(top = 8.dp, start = 24.dp, end = 24.dp, bottom = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Añadir a mi despensa", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            if (isCustom) {
                OutlinedTextField(value = name, onValueChange = { name = it.toCoffeeNameFormat() }, label = { Text(text = "Nombre") }, modifier = Modifier.fillMaxWidth(), shape = Shapes.card, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = brand, onValueChange = { brand = it.toCoffeeBrandFormat() }, label = { Text(text = "Marca") }, modifier = Modifier.fillMaxWidth(), shape = Shapes.card, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary))
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
            val saveTextColor = if (isSystemInDarkTheme()) PureBlack else PureWhite
            val cancelBorderAndText = MaterialTheme.colorScheme.onSurface

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = Shapes.shapeXl,
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
                    shape = Shapes.shapeXl
                ) {
                    Text(text = if (isInPantry) "ACTUALIZAR" else "AÑADIR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
