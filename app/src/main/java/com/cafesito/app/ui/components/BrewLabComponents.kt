package com.cafesito.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.ExperimentalMaterial3Api
import coil.compose.AsyncImage
import com.cafesito.app.R
import com.cafesito.app.data.Coffee
import com.cafesito.app.data.CoffeeWithDetails
import com.cafesito.app.data.CommentWithAuthor
import com.cafesito.app.data.DiaryEntryEntity
import com.cafesito.app.data.PantryItemWithDetails
import com.cafesito.app.data.PostWithDetails
import com.cafesito.app.data.UserEntity
import com.cafesito.app.data.UserReviewInfo
import com.cafesito.app.ui.brewlab.BrewLabViewModel
import com.cafesito.app.ui.brewlab.BaristaTip
import com.cafesito.app.ui.brewlab.BrewMethod
import com.cafesito.app.ui.brewlab.BrewPhaseInfo
import com.cafesito.shared.domain.brew.BREW_COFFEE_ABS_MAX_G
import com.cafesito.shared.domain.brew.BREW_COFFEE_ABS_MIN_G
import com.cafesito.shared.domain.brew.BREW_METHOD_OTROS
import com.cafesito.shared.domain.brew.BREW_SLIDER_MAX_COFFEE_G
import com.cafesito.shared.domain.brew.BREW_SLIDER_MIN_COFFEE_G
import com.cafesito.shared.domain.brew.BREW_SLIDER_MAX_TIME_S
import com.cafesito.shared.domain.brew.BREW_SLIDER_MAX_WATER_ML
import com.cafesito.shared.domain.brew.BREW_SLIDER_MIN_WATER_ML
import com.cafesito.shared.domain.brew.BREW_WATER_ABS_MAX_ML
import com.cafesito.shared.domain.brew.BREW_WATER_ABS_MIN_ML
import com.cafesito.shared.domain.brew.BrewMethodProfile
import com.cafesito.shared.domain.brew.BrewTimeProfile
import com.cafesito.app.ui.diary.DiaryAnalytics
import com.cafesito.app.ui.diary.DiaryPeriod
import com.cafesito.app.ui.profile.ProfileUiState
import com.cafesito.app.ui.profile.ProfileViewModel
import com.cafesito.app.ui.search.BarcodeActionIcon
import com.cafesito.app.ui.theme.*
import com.cafesito.app.ui.utils.containsSearchQuery
import com.cafesito.app.ui.timeline.CommentsViewModel
import com.cafesito.app.ui.timeline.TimelineNotification
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

// --- BREWLAB COMPONENTS ---

@Composable
fun ChooseMethodStep(methods: List<BrewMethod>, onSelect: (BrewMethod) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.space6, end = Spacing.space6, top = Spacing.space6, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.space4)
    ) {
        val chunkedMethods = methods.chunked(2)
        items(chunkedMethods) { rowMethods ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                rowMethods.forEach { method ->
                    MethodCard(method, Modifier.weight(1f)) { onSelect(method) }
                }
                if (rowMethods.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun MethodCard(method: BrewMethod, modifier: Modifier = Modifier, selected: Boolean = false, onClick: () -> Unit) {
    val methodLabel = localizedBrewMethodName(method.name)
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val resId = remember(method.iconResName) {
        context.resources.getIdentifier(method.iconResName, "drawable", context.packageName)
    }
    val cardColor = if (selected) LocalCaramelAccent.current else if (isDark) PureBlack else PureWhite
    val contentColor = if (selected) if (isDark) PureBlack else PureWhite else if (isDark) PureWhite else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = Shapes.pill,
        color = cardColor,
        border = if (selected) BorderStroke(0.dp, Color.Transparent) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.space4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (resId != 0) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = methodLabel,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(Icons.Default.CoffeeMaker, contentDescription = stringResource(id = R.string.diary_brew_method_cd), tint = contentColor, modifier = Modifier.size(Spacing.space8))
            }
            Spacer(Modifier.height(Spacing.space3))
            Text(
                text = methodLabel.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Método en elaboración: rectángulo con imagen a la izquierda y texto a la derecha (dos líneas si son dos palabras); mismo ancho para todos. */
@Composable
fun BrewMethodRowCard(method: BrewMethod, modifier: Modifier = Modifier, selected: Boolean = false, onClick: () -> Unit) {
    val methodLabel = localizedBrewMethodName(method.name)
    val context = LocalContext.current
    val resId = remember(method.iconResName) {
        context.resources.getIdentifier(method.iconResName, "drawable", context.packageName)
    }
    val isDark = isSystemInDarkTheme()
    val cardColor = if (selected) LocalCaramelAccent.current else if (isDark) MaterialTheme.colorScheme.background else AdviceCardBgLight
    val contentColor = if (selected) if (isDark) PureBlack else PureWhite else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier
            .width(130.dp)
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = Shapes.card,
        color = cardColor,
        border = BorderStroke(0.dp, Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.space3, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.space3)
        ) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                if (resId != 0) {
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = methodLabel,
                        modifier = Modifier.size(28.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(Icons.Default.CoffeeMaker, contentDescription = methodLabel, tint = contentColor, modifier = Modifier.size(24.dp))
                }
            }
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                methodLabel.split(Regex("\\s+")).forEach { word ->
                    Text(
                        text = word,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private data class BrewLabPrepOption(val key: String, val labelResId: Int, val drawableName: String?)
private data class BrewLabSizeOption(val key: String, val labelResId: Int, val rangeResId: Int, val defaultMl: Int, val drawableName: String)

private val BREWLAB_TIPO_OPTIONS = listOf(
    BrewLabPrepOption("Espresso", R.string.prep_espresso, "espresso"),
    BrewLabPrepOption("Americano", R.string.prep_americano, "americano"),
    BrewLabPrepOption("Capuchino", R.string.prep_capuchino, "capuchino"),
    BrewLabPrepOption("Latte", R.string.prep_latte, "latte"),
    BrewLabPrepOption("Macchiato", R.string.prep_macchiato, "macchiato"),
    BrewLabPrepOption("Moca", R.string.prep_moca, "moca"),
    BrewLabPrepOption("Vienés", R.string.prep_vienes, "vienes"),
    BrewLabPrepOption("Irlandés", R.string.prep_irlandes, "irlandes"),
    BrewLabPrepOption("Frappuccino", R.string.prep_frappuccino, "frappuccino"),
    BrewLabPrepOption("Caramelo macchiato", R.string.prep_caramel_macchiato, "caramel_macchiato"),
    BrewLabPrepOption("Corretto", R.string.prep_corretto, "corretto"),
    BrewLabPrepOption("Freddo", R.string.prep_freddo, "freddo"),
    BrewLabPrepOption("Latte macchiato", R.string.prep_latte_macchiato, "latte_macchiato"),
    BrewLabPrepOption("Leche con chocolate", R.string.prep_leche_con_chocolate, "leche_con_chocolate"),
    BrewLabPrepOption("Marroquí", R.string.prep_marroqui, "marroqui"),
    BrewLabPrepOption("Romano", R.string.prep_romano, "romano"),
    BrewLabPrepOption("Descafeinado", R.string.prep_descafeinado, "descafeinado")
)
private val BREWLAB_SIZE_OPTIONS = listOf(
    BrewLabSizeOption("Espresso", R.string.size_espresso, R.string.size_range_espresso, 30, "taza_espresso"),
    BrewLabSizeOption("Pequeño", R.string.size_small, R.string.size_range_small, 180, "taza_pequeno"),
    BrewLabSizeOption("Mediano", R.string.size_medium, R.string.size_range_medium, 275, "taza_mediano"),
    BrewLabSizeOption("Grande", R.string.size_large, R.string.size_range_large, 375, "taza_grande"),
    BrewLabSizeOption("Tazón XL", R.string.size_xl, R.string.size_range_xl, 475, "taza_xl")
)

@Composable
fun BrewLabMainStepContent(
    brewMethods: List<BrewMethod>,
    selectedMethod: BrewMethod?,
    onSelectMethod: (BrewMethod) -> Unit,
    selectedCoffee: Coffee?,
    selectedPantryItem: PantryItemWithDetails?,
    onSelectCoffeeClick: () -> Unit,
    pantryItems: List<PantryItemWithDetails>,
    allCoffees: List<CoffeeWithDetails>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddToPantryClick: () -> Unit,
    onCreateCoffeeClick: () -> Unit,
    onCoffeeSelected: (Coffee, Boolean) -> Unit,
    drinkType: String,
    onDrinkTypeChange: (String) -> Unit,
    selectedSizeLabel: String?,
    onSizeSelected: (label: String, defaultMl: Float) -> Unit,
    isEspressoMethod: Boolean,
    isRatioEditable: Boolean,
    isWaterEditable: Boolean,
    brewTimeSeconds: Int,
    timeProfile: BrewTimeProfile,
    methodProfile: BrewMethodProfile,
    ratio: Float,
    coffeeGrams: Float,
    valuation: String,
    baristaTips: List<BaristaTip>,
    brewTimerEnabled: Boolean,
    onBrewTimerEnabledChange: (Boolean) -> Unit,
    viewModel: BrewLabViewModel
) {
    val scrollState = rememberScrollState()
    val methodsListState = rememberLazyListState()
    LaunchedEffect(Unit) {
        methodsListState.scrollToItem(0)
    }
    LaunchedEffect(brewMethods.size) {
        if (brewMethods.isNotEmpty()) methodsListState.scrollToItem(0)
    }
    val water by viewModel.waterAmount.collectAsState(initial = 250f)
    val isDarkBrew = isSystemInDarkTheme()
    val cardColor = if (isDarkBrew) PureBlack else PureWhite
    val cardDividerColor = if (isDarkBrew) SliderTrackInactiveDark else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 120.dp)
    ) {
        Spacer(Modifier.height(Spacing.space4))
        PremiumCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space6),
            shape = Shapes.shapeCardMedium,
            containerColor = cardColor
        ) {
            Column(modifier = Modifier.padding(horizontal = Spacing.space4, vertical = Spacing.space4)) {
                val density = LocalDensity.current
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().layout { measurable, constraints ->
                        val space4Px = with(density) { Spacing.space4.roundToPx() }
                        val extendedMax = constraints.maxWidth + 2 * space4Px
                        val placeable = measurable.measure(constraints.copy(maxWidth = extendedMax))
                        layout(constraints.maxWidth, placeable.height) {
                            placeable.placeRelative(-space4Px, 0)
                        }
                    }
                ) {
                    LazyRow(
                        state = methodsListState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(start = Spacing.space4, end = 0.dp, top = 2.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(brewMethods, key = { it.name }) { method ->
                            val selected = method.name == selectedMethod?.name
                            BrewMethodRowCard(
                                method = method,
                                modifier = Modifier,
                                selected = selected
                            ) { onSelectMethod(method) }
                        }
                    }
                }
                if (selectedMethod?.isAgua != true) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Spacing.space3),
                        color = cardDividerColor
                    )
                    val rowDisplayItem = selectedPantryItem ?: remember(selectedCoffee, pantryItems) {
                        selectedCoffee?.let { c -> pantryItems.find { it.coffee.id == c.id } }
                    }
                    Surface(
                        onClick = onSelectCoffeeClick,
                        shape = RoundedCornerShape(0.dp),
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = Spacing.space4),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                if (selectedCoffee != null) {
                                    val displayCoffee = rowDisplayItem?.coffee ?: selectedCoffee
                                    if (displayCoffee.imageUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = displayCoffee.imageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(Shapes.cardSmall)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(
                                            text = displayCoffee.nombre.take(40),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isDarkBrew) PureWhite else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (rowDisplayItem != null) {
                                            val rem = rowDisplayItem.pantryItem.gramsRemaining.coerceIn(0, rowDisplayItem.pantryItem.totalGrams)
                                            val tot = rowDisplayItem.pantryItem.totalGrams
                                            Text(
                                                text = "$rem/$tot g",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = stringResource(id = R.string.diary_add_select_coffee),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDarkBrew) PureWhite else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = stringResource(id = R.string.common_open), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (selectedMethod != null && !selectedMethod.isOtros) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Spacing.space3),
                        color = cardDividerColor
                    )
                    ConfigStep(
                        methodName = selectedMethod.name,
                        isEspressoMethod = isEspressoMethod,
                        isRatioEditable = isRatioEditable,
                        isWaterEditable = isWaterEditable,
                        isAguaMethod = selectedMethod.isAgua == true,
                        brewTimeSeconds = brewTimeSeconds,
                        timeProfile = timeProfile,
                        methodProfile = methodProfile,
                        water = water,
                        ratio = ratio,
                        coffeeGrams = coffeeGrams,
                        valuation = valuation,
                        baristaTips = baristaTips,
                        brewTimerEnabled = brewTimerEnabled,
                        onBrewTimerEnabledChange = onBrewTimerEnabledChange,
                        viewModel = viewModel,
                        showSectionTitle = false,
                        wrapInCard = false
                    )
                }
            }
        }
    }
}

@Composable
fun ChooseCoffeeStep(
    pantryItems: List<PantryItemWithDetails>,
    allCoffees: List<CoffeeWithDetails>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddToPantryClick: () -> Unit,
    onCreateCoffeeClick: () -> Unit,
    onCoffeeSelected: (Coffee, Boolean, pantryItemId: String?) -> Unit
) {
    val filteredPantry = if (searchQuery.isBlank()) pantryItems else pantryItems.filter {
        it.coffee.nombre.containsSearchQuery(searchQuery) || it.coffee.marca.containsSearchQuery(searchQuery)
    }
    val filteredSuggestions = if (searchQuery.isBlank()) allCoffees.take(10) else allCoffees.filter {
        it.coffee.nombre.containsSearchQuery(searchQuery) || it.coffee.marca.containsSearchQuery(searchQuery)
    }.take(10)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 0.dp, bottom = Spacing.space4),
        verticalArrangement = Arrangement.spacedBy(Spacing.space4)
    ) {
        item {
            Text(
                stringResource(id = R.string.diary_pantry_title).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = Spacing.space6)
            )
            Spacer(Modifier.height(Spacing.space3))
            if (filteredPantry.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.space6, vertical = Spacing.space6),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (searchQuery.isBlank()) stringResource(id = R.string.brew_pantry_empty) else stringResource(id = R.string.brew_pantry_no_matches),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = Spacing.space6, end = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space3)
                ) {
                    items(filteredPantry, key = { it.pantryItem.id }) { item ->
                        PantryPremiumMiniCard(item = item, onClick = { onCoffeeSelected(item.coffee, true, item.pantryItem.id) })
                    }
                    item(key = "brewlab-add-pantry-card") {
                        PantryAddActionCard(onClick = onAddToPantryClick)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(20.dp))
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = Spacing.space6)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.diary_suggestions_title).uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onCreateCoffeeClick) {
                        Text(stringResource(id = R.string.diary_create_coffee), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(stringResource(id = R.string.brew_search_coffee_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.pillFull,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(id = R.string.brew_search_coffee_cd)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }

        if (filteredSuggestions.isEmpty()) {
            item {
                Text(
                    stringResource(id = R.string.brew_no_suggestions),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.space6)
                )
            }
        } else {
            items(filteredSuggestions, key = { it.coffee.id }) { item ->
                Box(modifier = Modifier.padding(horizontal = Spacing.space6)) {
                    SimpleCoffeeSelectionCard(item.coffee) { onCoffeeSelected(item.coffee, false, null) }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun SimpleCoffeeSelectionCard(coffee: Coffee, onClick: () -> Unit) {
    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = Shapes.shapeCardMedium
    ) {
        Row(modifier = Modifier.padding(Spacing.space4), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = coffee.imageUrl,
                contentDescription = coffee.nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(Shapes.cardSmall)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(Spacing.space4))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = coffee.nombre.toCoffeeNameFormat(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                @Suppress("DEPRECATION")
                Text(
                    text = coffee.marca.toCoffeeBrandFormat(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Filled.Add, contentDescription = stringResource(id = R.string.common_add), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigStep(
    methodName: String?,
    isEspressoMethod: Boolean,
    isRatioEditable: Boolean,
    isWaterEditable: Boolean,
    isAguaMethod: Boolean = false,
    brewTimeSeconds: Int,
    timeProfile: BrewTimeProfile,
    methodProfile: BrewMethodProfile,
    water: Float,
    ratio: Float,
    coffeeGrams: Float,
    valuation: String,
    baristaTips: List<BaristaTip>,
    brewTimerEnabled: Boolean,
    onBrewTimerEnabledChange: (Boolean) -> Unit,
    viewModel: BrewLabViewModel,
    showSectionTitle: Boolean = true,
    wrapInCard: Boolean = true
) {
    val waterBlue = WaterBlue
    var waterDraft by remember { mutableStateOf(water.roundToInt().toString()) }
    var coffeeDraft by remember { mutableStateOf(formatDecimalWithComma(coffeeGrams)) }
    LaunchedEffect(water) { waterDraft = water.roundToInt().toString() }
    LaunchedEffect(coffeeGrams) { coffeeDraft = formatDecimalWithComma(coffeeGrams) }

    val ratioProfileRes = remember(ratio, methodProfile) {
        val span = (methodProfile.ratioMax - methodProfile.ratioMin).toFloat().coerceAtLeast(0.1f)
        val normalized = (ratio - methodProfile.ratioMin.toFloat()) / span
        when {
            normalized <= 0.35f -> R.string.brew_ratio_concentrated
            normalized <= 0.7f -> R.string.brew_ratio_balanced
            else -> R.string.brew_ratio_light
        }
    }
    val ratioProfile = stringResource(id = ratioProfileRes).uppercase()
    val sliderInactiveTrackColor = if (isSystemInDarkTheme()) SliderTrackInactiveDark else SliderTrackInactiveLight
    val coffeeColor = LocalCaramelAccent.current
    val isOtrosMethod = methodName == BREW_METHOD_OTROS // "Rápido"
    var showBaristaModal by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        if (showSectionTitle) {
            Spacer(Modifier.height(Spacing.space4))
            val configTitle = if (isAguaMethod) {
                stringResource(id = R.string.brew_config_water)
            } else {
                stringResource(id = R.string.brew_config_method, methodName ?: stringResource(id = R.string.nav_brewlab))
            }
            Text(
                text = configTitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = Spacing.space6)
            )
            Spacer(Modifier.height(Spacing.space2))
        }
        val configContent: @Composable () -> Unit = {
                    if (isOtrosMethod) {
                        Text(
                            text = stringResource(id = R.string.brew_method_no_specific_params),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = Spacing.space4)
                        )
                    } else if (isAguaMethod) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            BrewMetricInputBlock(
                                label = stringResource(id = R.string.brew_water_ml),
                                value = waterDraft,
                                valueColor = waterBlue,
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f),
                                onValueChange = { raw ->
                                    val filtered = raw.filter { it.isDigit() }
                                    waterDraft = filtered
                                    filtered.toFloatOrNull()?.let { parsed ->
                                        viewModel.setWaterAmount(
                                            parsed.coerceIn(BREW_WATER_ABS_MIN_ML.toFloat(), BREW_WATER_ABS_MAX_ML.toFloat())
                                        )
                                    }
                                }
                            )
                            Slider(
                                modifier = Modifier.weight(2f),
                                value = water.coerceIn(BREW_SLIDER_MIN_WATER_ML.toFloat(), BREW_SLIDER_MAX_WATER_ML.toFloat()),
                                onValueChange = { viewModel.setWaterAmount(it) },
                                valueRange = BREW_SLIDER_MIN_WATER_ML.toFloat()..BREW_SLIDER_MAX_WATER_ML.toFloat(),
                                steps = 0,
                                colors = SliderDefaults.colors(
                                    thumbColor = waterBlue,
                                    activeTrackColor = waterBlue,
                                    inactiveTrackColor = sliderInactiveTrackColor,
                                    activeTickColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent
                                )
                            )
                        }
                    } else {
                    if (isWaterEditable) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            BrewMetricInputBlock(
                                label = stringResource(id = R.string.brew_water_ml),
                                value = waterDraft,
                                valueColor = waterBlue,
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f),
                                onValueChange = { raw ->
                                    val filtered = raw.filter { it.isDigit() }
                                    waterDraft = filtered
                                    filtered.toFloatOrNull()?.let { parsed ->
                                        viewModel.setWaterAmount(
                                            parsed.coerceIn(BREW_WATER_ABS_MIN_ML.toFloat(), BREW_WATER_ABS_MAX_ML.toFloat())
                                        )
                                    }
                                }
                            )
                            Slider(
                                modifier = Modifier.weight(2f),
                                value = water.coerceIn(BREW_SLIDER_MIN_WATER_ML.toFloat(), BREW_SLIDER_MAX_WATER_ML.toFloat()),
                                onValueChange = { viewModel.setWaterAmount(it) },
                                valueRange = BREW_SLIDER_MIN_WATER_ML.toFloat()..BREW_SLIDER_MAX_WATER_ML.toFloat(),
                                steps = 0,
                                colors = SliderDefaults.colors(
                                    thumbColor = waterBlue,
                                    activeTrackColor = waterBlue,
                                    inactiveTrackColor = sliderInactiveTrackColor,
                                    activeTickColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent
                                )
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                    }

                    run {
                        val coffeeMin = BREW_SLIDER_MIN_COFFEE_G
                        val coffeeSliderMax = BREW_SLIDER_MAX_COFFEE_G
                        val ratioLabelValue = if (methodProfile.ratioStep < 1.0) String.format(Locale.US, "%.1f", ratio) else ratio.roundToInt().toString()
                        val ratioTitle = stringResource(id = R.string.brew_ratio_label, ratioLabelValue, ratioProfile)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            BrewMetricInputBlock(
                                label = stringResource(id = R.string.brew_coffee_g),
                                value = coffeeDraft,
                                valueColor = coffeeColor,
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f),
                                onValueChange = { raw ->
                                    val normalized = raw.replace(",", ".")
                                    if (normalized.count { it == '.' } > 1) return@BrewMetricInputBlock
                                    if (normalized.any { !it.isDigit() && it != '.' }) return@BrewMetricInputBlock
                                    coffeeDraft = raw
                                    normalized.toFloatOrNull()?.let { viewModel.setCoffeeGrams(it) }
                                }
                            )
                            Column(modifier = Modifier.weight(2f)) {
                                @Suppress("DEPRECATION")
                                Text(ratioTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Slider(
                                    value = coffeeGrams.coerceIn(coffeeMin, coffeeSliderMax),
                                    onValueChange = { viewModel.setCoffeeGrams(it) },
                                    valueRange = coffeeMin..coffeeSliderMax,
                                    steps = 0,
                                    colors = SliderDefaults.colors(
                                        thumbColor = coffeeColor,
                                        activeTrackColor = coffeeColor,
                                        inactiveTrackColor = sliderInactiveTrackColor,
                                        activeTickColor = Color.Transparent,
                                        inactiveTickColor = Color.Transparent
                                    )
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }

                    if (isEspressoMethod) {
                        val timeSliderColor = if (isSystemInDarkTheme()) PureWhite else PureBlack
                        val timeSliderMax = minOf(BREW_SLIDER_MAX_TIME_S, timeProfile.maxSeconds)
                        val timeSliderMin = minOf(timeProfile.minSeconds, timeSliderMax)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            BrewMetricInputBlock(
                                label = stringResource(id = R.string.brew_time_s),
                                value = brewTimeSeconds.toString(),
                                valueColor = timeSliderColor,
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f),
                                onValueChange = { raw ->
                                    raw.filter { it.isDigit() }.toIntOrNull()?.let { viewModel.setBrewTimeSeconds(it) }
                                }
                            )
                            Slider(
                                modifier = Modifier.weight(2f),
                                value = brewTimeSeconds.toFloat().coerceIn(timeSliderMin.toFloat(), timeSliderMax.toFloat()),
                                onValueChange = { viewModel.setBrewTimeSeconds(it.roundToInt()) },
                                valueRange = timeSliderMin.toFloat()..timeSliderMax.toFloat(),
                                steps = 0,
                                colors = SliderDefaults.colors(
                                    thumbColor = timeSliderColor,
                                    activeTrackColor = timeSliderColor,
                                    inactiveTrackColor = sliderInactiveTrackColor,
                                    activeTickColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent
                                )
                            )
                        }
                    }

                    if (baristaTips.isNotEmpty() && !isAguaMethod) {
                        Spacer(Modifier.height(Spacing.space6))
                        val baristaCardBg = if (isSystemInDarkTheme()) AdviceCardBgDark else AdviceCardBgLight
                        Surface(
                            onClick = { showBaristaModal = true },
                            color = baristaCardBg,
                            shape = Shapes.shapeCardMedium
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.brew_barista_tips),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(id = R.string.common_open), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    if (!isAguaMethod) {
                        val configDividerColor = if (isSystemInDarkTheme()) SliderTrackInactiveDark else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = configDividerColor
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.brew_timer_label),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = brewTimerEnabled,
                                onCheckedChange = onBrewTimerEnabledChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureWhite,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = if (isSystemInDarkTheme()) SwitchThumbOffDark else PureWhite,
                                    uncheckedTrackColor = if (isSystemInDarkTheme()) SwitchTrackOffDark else DisabledGray,
                                    uncheckedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
        }
        if (wrapInCard) {
            PremiumCard(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space6)) {
                Box {
                    Column(Modifier.padding(horizontal = Spacing.space5, vertical = Spacing.space4)) {
                        configContent()
                    }
                }
            }
        } else {
            configContent()
        }

        if (showBaristaModal && baristaTips.isNotEmpty()) {
            val baristaSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showBaristaModal = false },
                sheetState = baristaSheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                shape = Shapes.sheetTopPill
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = Spacing.space6, end = Spacing.space6, bottom = Spacing.space8)
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(id = R.string.brew_barista_tips),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(Spacing.space4))
                    baristaTips.forEach { tip ->
                        BaristaTipPill(tip = tip, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}
@Composable
fun PreparationStep(
    timerSeconds: Int,
    remainingSeconds: Int,
    timeline: List<BrewPhaseInfo>,
    currentPhaseIndex: Int,
    processAdvice: String,
    isTimerRunning: Boolean,
    hasTimerStarted: Boolean,
    selectedTaste: String?,
    recommendation: String?,
    viewModel: BrewLabViewModel
) {
    val configuration = LocalConfiguration.current
    val appLanguage = remember(configuration) {
        val localeFromConfig = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale
        }
        (localeFromConfig?.language ?: Locale.getDefault().language).lowercase(Locale.ROOT)
    }
    val haptic = LocalHapticFeedback.current
    val currentPhase = timeline.getOrNull(currentPhaseIndex) ?: BrewPhaseInfo(stringResource(id = R.string.brew_ready), stringResource(id = R.string.brew_process_completed), 0)
    val nextPhase = timeline.getOrNull(currentPhaseIndex + 1)
    val totalSeconds = timeline.sumOf { it.durationSeconds }.coerceAtLeast(1)
    val totalProgress = (timerSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
    val adviceLines = remember(processAdvice) {
        processAdvice
            .split(".")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
    val language = appLanguage
    val adviceCards = remember(currentPhase.instruction, adviceLines, language) {
        listOf(translatePhaseInstructionByLanguage(currentPhase.instruction, language)) +
            adviceLines.map { "${translateProcessAdviceLineByLanguage(it, language)}." }
    }
    val advicePages = remember(adviceCards) { adviceCards.chunked(3) }

    val timerScale by animateFloatAsState(
        targetValue = if (remainingSeconds <= 5 && isTimerRunning) 1.04f else 1f,
        animationSpec = if (remainingSeconds <= 5 && isTimerRunning) {
            infiniteRepeatable(
                animation = tween(500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(300)
        },
        label = "timerScale"
    )

    LaunchedEffect(currentPhaseIndex) {
        if (hasTimerStarted) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Column(modifier = Modifier.padding(horizontal = Spacing.space6).fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            val isDarkPrep = isSystemInDarkTheme()
            val timerCardBg = if (isDarkPrep) PureBlack else PureWhite
            val timerCardText = if (isDarkPrep) PureWhite else MaterialTheme.colorScheme.onSurface
            PremiumCard(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.shapePremium,
                containerColor = timerCardBg
            ) {
                Box {
                    WaterWaveAnimation(
                        progress = totalProgress,
                        color = LocalCaramelAccent.current.copy(alpha = 0.05f),
                        modifier = Modifier.matchParentSize()
                    )

                    Column(modifier = Modifier.padding(Spacing.space6)) {
                            Text(
                                text = stringResource(id = R.string.brew_step_label, currentPhase.label),
                                style = MaterialTheme.typography.titleMedium,
                                color = PureWhite
                            )

                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                val maxFontSize = 72.sp
                                val minFontSize = 48.sp
                                val fontSize = when {
                                    maxWidth < 280.dp -> minFontSize
                                    maxWidth < 340.dp -> 56.sp
                                    else -> maxFontSize
                                }

                                val timerColor = if (remainingSeconds <= 5 && isTimerRunning) ElectricRed else if (isSystemInDarkTheme()) PureWhite else PureBlack
                                Text(
                                    text = String.format("%02d:%02d", remainingSeconds / 60, remainingSeconds % 60),
                                    style = MaterialTheme.typography.displayLarge.copy(fontSize = fontSize),
                                    fontWeight = FontWeight.Black,
                                    color = timerColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer(scaleX = timerScale, scaleY = timerScale)
                                )
                            }

                            val nextLabel = nextPhase?.label ?: stringResource(id = R.string.brew_finish)
                            Text(
                                text = stringResource(id = R.string.brew_next_label, nextLabel),
                                style = MaterialTheme.typography.bodyMedium,
                                color = timerCardText
                            )

                            Spacer(Modifier.height(Spacing.space6))

                            BrewTimeline(phases = timeline, elapsedTotalSeconds = timerSeconds)

                            Spacer(Modifier.height(Spacing.space3))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                @Suppress("DEPRECATION")
                                Text(
                                    text = String.format(stringResource(id = R.string.brew_total_timer_pattern), totalSeconds / 60, totalSeconds % 60),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = timerCardText
                                )
                                @Suppress("DEPRECATION")
                                Text(
                                    text = String.format("%02d:%02d", timerSeconds / 60, timerSeconds % 60),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = timerCardText
                                )
                            }
                        }
                }
            }
            }

            val timerEnded = hasTimerStarted && totalSeconds > 0 && timerSeconds >= totalSeconds
            if (timerEnded) {
                Spacer(Modifier.height(Spacing.space3))
                PreparationTasteCard(selectedTaste, recommendation, viewModel)
            } else if (adviceCards.isNotEmpty()) {
                val isDark = isSystemInDarkTheme()
                val adviceCardBg = if (isDark) AdviceCardBgDark else AdviceCardBgLight
                val adviceCardText = if (isDark) AdviceCardTextDark else AdviceCardTextLight
                val adviceBorder = if (isDark) PureWhite.copy(alpha = 0.10f) else PureBlack.copy(alpha = 0.08f)
                Spacer(Modifier.height(Spacing.space3))
                LazyRow(
                    state = rememberLazyListState(),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = Spacing.space6, end = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space3)
                ) {
                    items(advicePages) { page ->
                        Column(
                            modifier = Modifier.fillParentMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(Spacing.space2)
                        ) {
                            page.forEach { line ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = adviceCardBg,
                                    shape = Shapes.shapeCardMedium,
                                    border = BorderStroke(1.dp, adviceBorder)
                                ) {
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = adviceCardText,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = Spacing.space4, vertical = Spacing.space3),
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!timerEnded) {
                Spacer(Modifier.height(Spacing.space4))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.space6),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space4)
                ) {
                    if (hasTimerStarted) {
                        OutlinedButton(
                            onClick = { viewModel.resetTimer() },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = Shapes.shapeXl,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(stringResource(id = R.string.brew_reset).uppercase(), fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = { viewModel.toggleTimer() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTimerRunning) ElectricRed else MaterialTheme.colorScheme.primary,
                            contentColor = if (isTimerRunning) PureWhite else if (isSystemInDarkTheme()) PureBlack else MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = Shapes.shapeXl
                    ) {
                        Icon(
                            if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isTimerRunning) stringResource(id = R.string.brew_timer_pause) else stringResource(id = R.string.brew_timer_start),
                            tint = if (isTimerRunning) PureWhite else if (isSystemInDarkTheme()) PureBlack else MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(Spacing.space2))
                        Text(
                            if (isTimerRunning) stringResource(id = R.string.brew_timer_pause).uppercase() else stringResource(id = R.string.brew_timer_start).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = if (isTimerRunning) PureWhite else if (isSystemInDarkTheme()) PureBlack else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhaseDurationLabel(phase: BrewPhaseInfo) {
    @Suppress("DEPRECATION")
    Text(
        text = "${phase.durationSeconds}s",
        style = MaterialTheme.typography.labelSmall,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
fun BrewTimeline(phases: List<BrewPhaseInfo>, elapsedTotalSeconds: Int) {
    val totalSeconds = phases.sumOf { it.durationSeconds }.coerceAtLeast(1)
    val timeBarColor = if (isSystemInDarkTheme()) PureWhite else PureBlack
    val softGray = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Column(Modifier
        .fillMaxWidth()
        .padding(vertical = Spacing.space2)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            phases.forEach { phase ->
                val weight = (phase.durationSeconds.toFloat() / totalSeconds).coerceAtLeast(0.05f)
                Box(
                    modifier = Modifier.weight(weight),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    PhaseDurationLabel(phase)
                }
            }
        }

        Spacer(Modifier.height(Spacing.space2))

        Row(Modifier
            .fillMaxWidth()
            .height(Spacing.space3), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            var elapsedBeforeThisPhase = 0
            phases.forEach { phase ->
                val weight = (phase.durationSeconds.toFloat() / totalSeconds).coerceAtLeast(0.05f)
                val phaseProgress = if (elapsedTotalSeconds <= elapsedBeforeThisPhase) {
                    0f
                } else if (elapsedTotalSeconds >= elapsedBeforeThisPhase + phase.durationSeconds) {
                    1f
                } else {
                    (elapsedTotalSeconds - elapsedBeforeThisPhase).toFloat() / phase.durationSeconds
                }

Box(
                        Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(softGray)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(phaseProgress)
                                    .fillMaxHeight()
                                    .background(timeBarColor)
                            )
                        }
                elapsedBeforeThisPhase += phase.durationSeconds
            }
        }
    }
}

@Composable
private fun PreparationTasteCard(
    selectedTaste: String?,
    recommendation: String?,
    viewModel: BrewLabViewModel
) {
    val tastes = listOf(
        "Amargo" to (Icons.Default.LocalFireDepartment to R.string.brew_taste_bitter),
        "Ácido" to (Icons.Default.Science to R.string.brew_taste_acidic),
        "Equilibrado" to (Icons.Default.Verified to R.string.brew_taste_balanced),
        "Salado" to (Icons.Default.Waves to R.string.brew_taste_salty),
        "Acuoso" to (Icons.Default.WaterDrop to R.string.brew_taste_watery),
        "Aspero" to (Icons.Default.Grain to R.string.brew_taste_rough),
        "Dulce" to (Icons.Default.Favorite to R.string.brew_taste_sweet)
    )
    val isDark = isSystemInDarkTheme()
    PremiumCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (isDark) PureBlack else PureWhite
    ) {
        Column(Modifier.padding(Spacing.space6)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.space3)
            ) {
                tastes.chunked(2).forEach { rowTastes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.space3)
                    ) {
                        rowTastes.forEach { (label, iconAndRes) ->
                            val icon = iconAndRes.first
                            val localizedTaste = stringResource(id = iconAndRes.second)
                            TasteChip(
                                label = localizedTaste.uppercase(),
                                icon = icon,
                                isSelected = selectedTaste == label,
                                modifier = Modifier.weight(1f)
                            ) {
                                viewModel.onTasteFeedback(label)
                            }
                        }
                        if (rowTastes.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = recommendation != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(Spacing.space8))
                    Surface(
                        color = if (isDark) PureBlack else MaterialTheme.colorScheme.surfaceVariant,
                        shape = Shapes.pill,
                        border = BorderStroke(0.dp, Color.Transparent)
                    ) {
                        Column(Modifier.padding(Spacing.space6)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(id = R.string.brew_recommendation), tint = LocalCaramelAccent.current, modifier = Modifier.size(Spacing.space6))
                                Spacer(Modifier.width(Spacing.space3))
                                @Suppress("DEPRECATION")
                                Text(
                                    stringResource(id = R.string.brew_recommendation),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    color = LocalCaramelAccent.current
                                )
                            }
                            Spacer(Modifier.height(Spacing.space4))
                            Text(
                                text = recommendation ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isDark) PureWhite else MaterialTheme.colorScheme.onSurface,
                                lineHeight = 24.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Pantalla Consumo: primero card Configura tu café (tipo + tamaño), luego título Resultado y card de sabor. */
@Composable
fun ResultStep(
    selectedTaste: String?,
    recommendation: String?,
    onSave: () -> Unit,
    viewModel: BrewLabViewModel
) {
    val drinkType by viewModel.drinkType.collectAsState()
    val selectedSizeLabel by viewModel.selectedSizeLabel.collectAsState()
    val isDark = isSystemInDarkTheme()
    val cardColor = if (isDark) PureBlack else PureWhite
    val cardDividerColor = if (isDark) SliderTrackInactiveDark else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val context = LocalContext.current
    val contextSize = LocalContext.current
    val unselectedChipBg = if (isDark) MaterialTheme.colorScheme.background else AdviceCardBgLight
    val unselectedChipContent = MaterialTheme.colorScheme.onSurface
    val selectedTextColorTipo = if (isDark) PureBlack else PureWhite
    val tipoChipWidth = 140.dp
    val tipoChipHeight = 56.dp
    val unselectedChipBgSize = if (isDark) MaterialTheme.colorScheme.background else AdviceCardBgLight
    val selectedTextColorSize = if (isDark) PureBlack else PureWhite

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = Spacing.space6, top = 0.dp, end = Spacing.space6, bottom = Spacing.space6)
    ) {
        Spacer(Modifier.height(Spacing.space4))
        Text(
            text = stringResource(id = R.string.brew_config_your_coffee),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 0.dp)
        )
        Spacer(Modifier.height(4.dp))
        PremiumCard(
            modifier = Modifier.fillMaxWidth(),
            shape = Shapes.shapeCardMedium,
            containerColor = cardColor
        ) {
            Column(modifier = Modifier.padding(horizontal = Spacing.space4, vertical = Spacing.space4)) {
                val densityTipo = LocalDensity.current
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().layout { measurable, constraints ->
                        val space4Px = with(densityTipo) { Spacing.space4.roundToPx() }
                        val extendedMax = constraints.maxWidth + 2 * space4Px
                        val placeable = measurable.measure(constraints.copy(maxWidth = extendedMax))
                        layout(constraints.maxWidth, placeable.height) {
                            placeable.placeRelative(-space4Px, 0)
                        }
                    }
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(start = Spacing.space4, end = 0.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(BREWLAB_TIPO_OPTIONS, key = { it.key }) { option ->
                            val localizedLabel = stringResource(id = option.labelResId)
                            val isSelected = drinkType.equals(option.key, ignoreCase = true)
                            Surface(
                                onClick = { viewModel.setDrinkType(option.key) },
                                shape = Shapes.card,
                                color = if (isSelected) LocalCaramelAccent.current else unselectedChipBg,
                                border = BorderStroke(0.dp, Color.Transparent),
                                modifier = Modifier.width(tipoChipWidth).height(tipoChipHeight)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = Spacing.space3, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.space2)
                                ) {
                                    val resId = option.drawableName?.let { context.resources.getIdentifier(it, "drawable", context.packageName) } ?: 0
                                    val iconTint = if (isSelected) selectedTextColorTipo else unselectedChipContent
                                    if (resId != 0) {
                                        Image(
                                            painter = painterResource(id = resId),
                                            contentDescription = localizedLabel,
                                            modifier = Modifier.size(24.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Icon(Icons.Default.CoffeeMaker, contentDescription = localizedLabel, tint = iconTint, modifier = Modifier.size(24.dp))
                                    }
                                    Text(
                                        text = localizedLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) selectedTextColorTipo else unselectedChipContent,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = Spacing.space3),
                    color = cardDividerColor
                )
                val densitySize = LocalDensity.current
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().layout { measurable, constraints ->
                        val space4Px = with(densitySize) { Spacing.space4.roundToPx() }
                        val extendedMax = constraints.maxWidth + 2 * space4Px
                        val placeable = measurable.measure(constraints.copy(maxWidth = extendedMax))
                        layout(constraints.maxWidth, placeable.height) {
                            placeable.placeRelative(-space4Px, 0)
                        }
                    }
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(start = Spacing.space4, end = 0.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(BREWLAB_SIZE_OPTIONS, key = { it.key }) { option ->
                            val localizedLabel = stringResource(id = option.labelResId)
                            val localizedRange = stringResource(id = option.rangeResId)
                            val isSelected = selectedSizeLabel == option.key
                            val chipBg = if (isSelected) LocalCaramelAccent.current else unselectedChipBgSize
                            val chipContent = if (isSelected) selectedTextColorSize else unselectedChipContent
                            Surface(
                                onClick = { viewModel.setSelectedSize(option.key, option.defaultMl.toFloat()) },
                                shape = Shapes.cardSmall,
                                color = chipBg,
                                border = BorderStroke(0.dp, Color.Transparent)
                            ) {
                                Row(
                                    Modifier.padding(horizontal = Spacing.space3, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val sizeResId = contextSize.resources.getIdentifier(option.drawableName, "drawable", contextSize.packageName)
                                    if (sizeResId != 0) {
                                        Image(
                                            painter = painterResource(id = sizeResId),
                                            contentDescription = localizedLabel,
                                            modifier = Modifier.size(Spacing.space6),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Icon(Icons.Default.LocalCafe, contentDescription = localizedLabel, modifier = Modifier.size(20.dp), tint = chipContent)
                                    }
                                    Column {
                                        Text(localizedLabel, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = chipContent)
                                        Text(localizedRange, style = MaterialTheme.typography.bodySmall, color = chipContent.copy(alpha = 0.9f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.space6))
        Text(
            text = stringResource(id = R.string.brew_result),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 0.dp)
        )
        Spacer(Modifier.height(4.dp))
        PreparationTasteCard(selectedTaste, recommendation, viewModel)
    }
}

@Composable
fun TasteChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    val contentColorWhenSelected = if (isDark) PureBlack else PureWhite
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = Shapes.card,
        color = if (isSelected) primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, primary),
        shadowElevation = if (isSelected) 3.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.space4, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) contentColorWhenSelected else primary
            )
            Spacer(Modifier.width(Spacing.space3))
            @Suppress("DEPRECATION")
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) contentColorWhenSelected else primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PantrySelectionCard(item: PantryItemWithDetails, onClick: () -> Unit) {
    val progress = if (item.pantryItem.totalGrams > 0) item.pantryItem.gramsRemaining.toFloat() / item.pantryItem.totalGrams else 0f

    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = Shapes.shapeXl
    ) {
        Row(modifier = Modifier.padding(Spacing.space4), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.coffee.imageUrl,
                contentDescription = item.coffee.nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(70.dp)
                    .clip(Shapes.card)
            )
            Spacer(Modifier.width(Spacing.space4))
            Column(Modifier.weight(1f)) {
                Text(text = item.coffee.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                @Suppress("DEPRECATION")
                Text(text = item.coffee.marca.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(Spacing.space2))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = stringResource(id = R.string.common_open), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun BottomActionContainer(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = Spacing.space6, end = Spacing.space6, top = Spacing.space4, bottom = 80.dp),
        content = content
    )
}

@Composable
fun SectionHeader(title: String) {
    @Suppress("DEPRECATION")
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = Spacing.space3)
    )
}

@Composable
fun DataBlock(label: String, value: String, valueColor: Color) {
    Column {
        @Suppress("DEPRECATION")
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = valueColor)
    }
}

@Composable
fun BrewMetricInputBlock(
    label: String,
    value: String,
    valueColor: Color,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    Column(modifier = modifier) {
        @Suppress("DEPRECATION")
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .clip(Shapes.cardSmall)
                .background(Color.Transparent)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = valueColor
                ),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.widthIn(min = 92.dp, max = 170.dp)
            )
        }
    }
}

private fun formatDecimalWithComma(value: Float): String {
    return String.format(Locale.US, "%.1f", value).replace('.', ',')
}

@Composable
fun TasteOption(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = Shapes.card,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
    ) {
        @Suppress("DEPRECATION")
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 14.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun WaterWaveAnimation(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = WaterBlue.copy(alpha = 0.15f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val waveHeight = 10.dp.toPx()
        val baseFillHeight = height * (1f - progress.coerceIn(0f, 1f))

        val path = Path()
        path.moveTo(0f, baseFillHeight)

        for (x in 0..width.toInt()) {
            val progressX = x.toFloat() / width
            val y = baseFillHeight + waveHeight * sin((progressX * 2.0 * PI + waveOffset.toDouble())).toFloat()
            path.lineTo(x.toFloat(), y)
        }

        path.lineTo(width, height)
        path.lineTo(0f, height)
        path.close()

        clipRect {
            drawPath(path, color = color)
        }
    }
}

@Composable
private fun baristaTipIcon(iconKey: String): ImageVector {
    return when (iconKey) {
        "grind" -> Icons.Default.Grain
        "thermostat" -> Icons.Default.Thermostat
        "water" -> Icons.Default.Grain
        "clock" -> Icons.Default.AccessTime
        "coffee" -> Icons.Default.Coffee
        else -> Icons.Default.AutoAwesome
    }
}

@Composable
fun BaristaTipPill(tip: BaristaTip, modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val appLanguage = remember(configuration) {
        val localeFromConfig = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale
        }
        (localeFromConfig?.language ?: Locale.getDefault().language).lowercase(Locale.ROOT)
    }
    val isSpanish = appLanguage.startsWith("es")
    val localizedTipLabel = remember(tip.label, appLanguage, isSpanish) {
        if (isSpanish) tip.label else translateBaristaTipLabelByLanguage(tip.label, appLanguage)
    }
    val localizedTipValue = remember(tip.value, appLanguage, isSpanish) {
        if (isSpanish) tip.value else translateBaristaTipValueByLanguage(tip.value, appLanguage)
    }
    val icon = baristaTipIcon(tip.iconKey)
    val useDrawable = tip.iconKey == "water" || tip.iconKey == "bolt"
    val drawableId = when (tip.iconKey) {
        "water" -> com.cafesito.app.R.drawable.agua
        "bolt" -> com.cafesito.app.R.drawable.relampago
        else -> null
    }
    PremiumCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .heightIn(min = 52.dp)
                .padding(horizontal = Spacing.space3, vertical = Spacing.space2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier
                .size(26.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                if (useDrawable && drawableId != null) {
                    Image(
                        painter = painterResource(id = drawableId),
                        contentDescription = localizedTipLabel,
                        modifier = Modifier.size(14.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(icon, contentDescription = localizedTipLabel, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                @Suppress("DEPRECATION")
                Text(text = localizedTipLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                @Suppress("DEPRECATION")
                Text(
                    text = localizedTipValue,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

private fun translateBaristaTipLabelByLanguage(label: String, language: String): String {
    val key = normalizeText(label)
    return when {
        language.startsWith("fr") -> when (key) {
            "perfil actual" -> "PROFIL ACTUEL"
            "volumen" -> "VOLUME"
            "tiempo actual" -> "TEMPS ACTUEL"
            "dosis" -> "DOSE"
            "base" -> "BASE"
            "proceso" -> "PROCESSUS"
            "ajuste" -> "AJUSTEMENT"
            "detalle" -> "DETAIL"
            else -> label
        }
        language.startsWith("pt") -> when (key) {
            "perfil actual" -> "PERFIL ATUAL"
            "volumen" -> "VOLUME"
            "tiempo actual" -> "TEMPO ATUAL"
            "dosis" -> "DOSE"
            "base" -> "BASE"
            "proceso" -> "PROCESSO"
            "ajuste" -> "AJUSTE"
            "detalle" -> "DETALHE"
            else -> label
        }
        language.startsWith("de") -> when (key) {
            "perfil actual" -> "AKTUELLES PROFIL"
            "volumen" -> "VOLUMEN"
            "tiempo actual" -> "AKTUELLE ZEIT"
            "dosis" -> "DOSIS"
            "base" -> "BASIS"
            "proceso" -> "PROZESS"
            "ajuste" -> "ANPASSUNG"
            "detalle" -> "DETAIL"
            else -> label
        }
        else -> when (key) {
            "perfil actual" -> "CURRENT PROFILE"
            "volumen" -> "VOLUME"
            "tiempo actual" -> "CURRENT TIME"
            "dosis" -> "DOSE"
            "base" -> "BASE"
            "proceso" -> "PROCESS"
            "ajuste" -> "ADJUSTMENT"
            "detalle" -> "DETAIL"
            else -> label
        }
    }
}

private fun translateBaristaTipValueByLanguage(value: String, language: String): String {
    var text = value
    val replacements = when {
        language.startsWith("fr") -> listOf(
            "Equilibrado" to "Équilibré",
            "Mas intenso" to "Plus intense",
            "Más intenso" to "Plus intense",
            "Tramo medio" to "Segment moyen",
            "buen balance entre cuerpo y claridad" to "bon équilibre entre corps et clarté",
            "manten ritmo y" to "maintiens le rythme et une",
            "mantén ritmo y" to "maintiens le rythme et une",
            "constantes" to "constants",
            "vierte suave para mantener dulzor" to "verse doucement pour préserver la douceur",
            "INFUSION" to "INFUSION",
            "PRESION" to "PRESSION",
            "REMOVIDO" to "AGITATION",
            "Suave y constante" to "Douce et constante",
            "1-2 agitaciones suaves" to "1-2 agitations douces",
            "Fina-media" to "Fine-moyenne",
            "PAPEL" to "PAPIER",
            "METAL" to "MÉTAL",
            "Mas limpieza en taza" to "Plus de netteté en tasse",
            "Más limpieza en taza" to "Plus de netteté en tasse",
            "Mas cuerpo y textura" to "Plus de corps et de texture",
            "Más cuerpo y textura" to "Plus de corps et de texture",
            "Más concentrado" to "Plus concentré",
            "Mas concentrado" to "Plus concentré",
            "si amarga" to "si c'est amer",
            "abre punto de molienda" to "ouvre un peu la mouture",
            "Tramo corto del método" to "Segment court de la méthode",
            "Tramo corto del metodo" to "Segment court de la méthode",
            "prioriza control y uniformidad" to "privilégie contrôle et uniformité",
            "En ventana ideal" to "Dans la fenêtre idéale",
            "busca flujo continuo y crema uniforme" to "cherche un flux continu et une crema uniforme",
            "Dentro de rango clasico para espresso" to "Dans la plage classique pour espresso",
            "MOLIENDA" to "MOUTURE",
            "TEMPERATURA" to "TEMPÉRATURE",
            "RATIO" to "RATIO",
            "TIEMPO" to "TEMPS",
            "PREINFUSION" to "PRÉ-INFUSION",
            "Suave para evitar canalizacion" to "Douce pour éviter la canalisation",
            "AJUSTE RAPIDO" to "AJUSTEMENT RAPIDE",
            "Si corre rapido, mas fino" to "Si ça coule vite, mouture plus fine",
            "AJUSTE LENTO" to "AJUSTEMENT LENT",
            "Si se ahoga, mas grueso" to "Si ça s'étouffe, mouture plus grossière",
            "DISTRIBUCION" to "DISTRIBUTION",
            "Nivela antes del tamp" to "Nivelle avant le tassage"
        )
        language.startsWith("pt") -> listOf(
            "Equilibrado" to "Equilibrado",
            "Mas intenso" to "Mais intenso",
            "Más intenso" to "Mais intenso",
            "Tramo medio" to "Faixa média",
            "buen balance entre cuerpo y claridad" to "bom equilíbrio entre corpo e clareza",
            "manten ritmo y" to "mantenha ritmo e",
            "mantén ritmo y" to "mantenha ritmo e",
            "constantes" to "constantes",
            "vierte suave para mantener dulzor" to "despeje suavemente para manter a doçura",
            "INFUSION" to "INFUSÃO",
            "PRESION" to "PRESSÃO",
            "REMOVIDO" to "AGITAÇÃO",
            "Suave y constante" to "Suave e constante",
            "1-2 agitaciones suaves" to "1-2 agitações suaves",
            "Fina-media" to "Fina-média",
            "PAPEL" to "PAPEL",
            "METAL" to "METAL",
            "Mas limpieza en taza" to "Mais limpeza na xícara",
            "Más limpieza en taza" to "Mais limpeza na xícara",
            "Mas cuerpo y textura" to "Mais corpo e textura",
            "Más cuerpo y textura" to "Mais corpo e textura",
            "Más concentrado" to "Mais concentrado",
            "Mas concentrado" to "Mais concentrado",
            "si amarga" to "se amargar",
            "abre punto de molienda" to "abra um pouco a moagem",
            "Tramo corto del método" to "Trecho curto do método",
            "Tramo corto del metodo" to "Trecho curto do método",
            "prioriza control y uniformidad" to "priorize controle e uniformidade",
            "En ventana ideal" to "Na faixa ideal",
            "busca flujo continuo y crema uniforme" to "busque fluxo contínuo e crema uniforme",
            "Dentro de rango clasico para espresso" to "Dentro da faixa clássica para espresso",
            "MOLIENDA" to "MOAGEM",
            "TEMPERATURA" to "TEMPERATURA",
            "RATIO" to "RÁCIO",
            "TIEMPO" to "TEMPO",
            "PREINFUSION" to "PRÉ-INFUSÃO",
            "Suave para evitar canalizacion" to "Suave para evitar canalização",
            "AJUSTE RAPIDO" to "AJUSTE RÁPIDO",
            "Si corre rapido, mas fino" to "Se correr rápido, moa mais fino",
            "AJUSTE LENTO" to "AJUSTE LENTO",
            "Si se ahoga, mas grueso" to "Se afogar, moa mais grosso",
            "DISTRIBUCION" to "DISTRIBUIÇÃO",
            "Nivela antes del tamp" to "Nivele antes do tamper"
        )
        language.startsWith("de") -> listOf(
            "Equilibrado" to "Ausgewogen",
            "Mas intenso" to "Intensiver",
            "Más intenso" to "Intensiver",
            "Tramo medio" to "Mittlerer Abschnitt",
            "buen balance entre cuerpo y claridad" to "gute Balance zwischen Körper und Klarheit",
            "manten ritmo y" to "halte den Rhythmus und eine",
            "mantén ritmo y" to "halte den Rhythmus und eine",
            "constantes" to "konstante",
            "vierte suave para mantener dulzor" to "gieße sanft, um die Süße zu erhalten",
            "INFUSION" to "INFUSION",
            "PRESION" to "DRUCK",
            "REMOVIDO" to "RÜHREN",
            "Suave y constante" to "Sanft und konstant",
            "1-2 agitaciones suaves" to "1-2 sanfte Bewegungen",
            "Fina-media" to "Fein-mittel",
            "PAPEL" to "PAPIER",
            "METAL" to "METALL",
            "Mas limpieza en taza" to "Mehr Klarheit in der Tasse",
            "Más limpieza en taza" to "Mehr Klarheit in der Tasse",
            "Mas cuerpo y textura" to "Mehr Körper und Textur",
            "Más cuerpo y textura" to "Mehr Körper und Textur",
            "Más concentrado" to "Konzentrierter",
            "Mas concentrado" to "Konzentrierter",
            "si amarga" to "wenn bitter",
            "abre punto de molienda" to "Mahlgrad etwas gröber stellen",
            "Tramo corto del método" to "Kurzer Methodenabschnitt",
            "Tramo corto del metodo" to "Kurzer Methodenabschnitt",
            "prioriza control y uniformidad" to "Kontrolle und Gleichmäßigkeit priorisieren",
            "En ventana ideal" to "Im idealen Bereich",
            "busca flujo continuo y crema uniforme" to "gleichmäßigen Fluss und uniforme Crema anstreben",
            "Dentro de rango clasico para espresso" to "Im klassischen Espresso-Bereich",
            "MOLIENDA" to "MAHLGRAD",
            "TEMPERATURA" to "TEMPERATUR",
            "RATIO" to "VERHÄLTNIS",
            "TIEMPO" to "ZEIT",
            "PREINFUSION" to "PRÄINFUSION",
            "Suave para evitar canalizacion" to "Sanft, um Channeling zu vermeiden",
            "AJUSTE RAPIDO" to "SCHNELLE ANPASSUNG",
            "Si corre rapido, mas fino" to "Wenn es zu schnell läuft, feiner mahlen",
            "AJUSTE LENTO" to "LANGSAME ANPASSUNG",
            "Si se ahoga, mas grueso" to "Wenn es stockt, gröber mahlen",
            "DISTRIBUCION" to "VERTEILUNG",
            "Nivela antes del tamp" to "Vor dem Tampen nivellieren"
        )
        else -> listOf(
            "Equilibrado" to "Balanced",
            "Mas intenso" to "More intense",
            "Más intenso" to "More intense",
            "Tramo medio" to "Mid segment",
            "buen balance entre cuerpo y claridad" to "good balance between body and clarity",
            "manten ritmo y" to "keep pace and",
            "mantén ritmo y" to "keep pace and",
            "constantes" to "consistent",
            "vierte suave para mantener dulzor" to "pour gently to keep sweetness",
            "INFUSION" to "INFUSION",
            "PRESION" to "PRESSURE",
            "REMOVIDO" to "STIR",
            "Suave y constante" to "Gentle and steady",
            "1-2 agitaciones suaves" to "1-2 gentle stirs",
            "Fina-media" to "Medium-fine",
            "PAPEL" to "PAPER",
            "METAL" to "METAL",
            "Mas limpieza en taza" to "More cup clarity",
            "Más limpieza en taza" to "More cup clarity",
            "Mas cuerpo y textura" to "More body and texture",
            "Más cuerpo y textura" to "More body and texture",
            "Más concentrado" to "More concentrated",
            "Mas concentrado" to "More concentrated",
            "si amarga" to "if bitter",
            "abre punto de molienda" to "coarsen the grind a bit",
            "Tramo corto del método" to "Short method segment",
            "Tramo corto del metodo" to "Short method segment",
            "prioriza control y uniformidad" to "prioritize control and uniformity",
            "En ventana ideal" to "In ideal range",
            "busca flujo continuo y crema uniforme" to "aim for steady flow and even crema",
            "Dentro de rango clasico para espresso" to "Within classic espresso range",
            "MOLIENDA" to "GRIND",
            "TEMPERATURA" to "TEMPERATURE",
            "RATIO" to "RATIO",
            "TIEMPO" to "TIME",
            "PREINFUSION" to "PRE-INFUSION",
            "Suave para evitar canalizacion" to "Gentle to avoid channeling",
            "AJUSTE RAPIDO" to "QUICK ADJUSTMENT",
            "Si corre rapido, mas fino" to "If it runs fast, grind finer",
            "AJUSTE LENTO" to "SLOW ADJUSTMENT",
            "Si se ahoga, mas grueso" to "If it chokes, grind coarser",
            "DISTRIBUCION" to "DISTRIBUTION",
            "Nivela antes del tamp" to "Level before tamp"
        )
    }
    replacements.forEach { (from, to) -> text = text.replace(from, to, ignoreCase = true) }
    return text
}

private fun normalizeText(value: String): String {
    val normalized = java.text.Normalizer.normalize(value.trim(), java.text.Normalizer.Form.NFD)
    return normalized.replace("\\p{M}+".toRegex(), "").lowercase(Locale.ROOT)
}

private fun translatePhaseInstructionByLanguage(instruction: String, language: String): String {
    if (language.startsWith("es")) return instruction
    val key = normalizeText(instruction)
    return when {
        key.contains("vierte unos") && key.contains("humedecer") -> when {
            language.startsWith("fr") -> "Verse environ 50 ml d'eau pour humidifier tout le lit. Remue doucement 3 fois pour une extraction homogène."
            language.startsWith("pt") -> "Despeja cerca de 50 ml de água para umedecer todo o leito. Mexa suavemente 3 vezes para extração uniforme."
            language.startsWith("de") -> "Gieße etwa 50 ml Wasser, um das gesamte Bett zu benetzen. Rühre 3-mal sanft für eine gleichmäßige Extraktion."
            else -> "Pour about 50ml of water to wet the whole bed. Stir gently 3 times for even extraction."
        }
        key.contains("fuego medio-bajo") -> when {
            language.startsWith("fr") -> "Maintiens un feu moyen-doux. L'eau de la base crée la pression qui remonte par la cheminée."
            language.startsWith("pt") -> "Mantenha fogo médio-baixo. A água na base cria pressão e sobe pela chaminé."
            language.startsWith("de") -> "Halte mittlere bis niedrige Hitze. Das Wasser unten baut Druck auf und steigt durch den Trichter."
            else -> "Keep medium-low heat. Water in the lower chamber builds pressure up the funnel."
        }
        key.contains("empiece a salir") -> when {
            language.startsWith("fr") -> "Quand le café commence à sortir, baisse le feu ou retire du feu avant le bouillonnement final."
            language.startsWith("pt") -> "Quando o café começar a sair, reduza o fogo ou retire antes do borbulhar final."
            language.startsWith("de") -> "Wenn der Kaffee zu fließen beginnt, Hitze reduzieren oder vor dem letzten Sprudeln vom Herd nehmen."
            else -> "When coffee starts flowing, lower heat or remove from stove before the final sputter."
        }
        key.contains("presion constante") -> when {
            language.startsWith("fr") -> "Garde une pression constante et un débit régulier, comme un filet de miel."
            language.startsWith("pt") -> "Mantenha pressão constante e fluxo regular, como fio de mel."
            language.startsWith("de") -> "Halte gleichmäßigen Druck und einen stetigen Fluss wie Honigfaden."
            else -> "Keep steady pressure and a honey-like flow."
        }
        key.contains("humedece el cafe") -> when {
            language.startsWith("fr") -> "Pré-infuse le lit et laisse le CO2 s'échapper avant de continuer."
            language.startsWith("pt") -> "Faça a pré-infusão e deixe o CO2 sair antes de continuar."
            language.startsWith("de") -> "Bette das Kaffeebett an und lass CO2 entweichen, bevor du fortfährst."
            else -> "Bloom the bed and let trapped CO2 release before continuing."
        }
        key.contains("deja que el lecho termine de drenar") -> when {
            language.startsWith("fr") -> "Laisse le lit finir de s'égoutter pour compléter l'extraction."
            language.startsWith("pt") -> "Deixe o leito terminar de drenar para concluir a extração."
            language.startsWith("de") -> "Lass das Bett vollständig ablaufen, um die Extraktion abzuschließen."
            else -> "Let the bed finish draining to complete extraction."
        }
        else -> instruction
    }
}

private fun translateProcessAdviceLineByLanguage(line: String, language: String): String {
    if (language.startsWith("es")) return line
    val key = normalizeText(line)
    return when {
        key.contains("asegura saturacion completa del lecho") -> when {
            language.startsWith("fr") -> "Assure une saturation complète du lit avant de continuer."
            language.startsWith("pt") -> "Garanta saturação completa do leito antes de continuar."
            language.startsWith("de") -> "Sorge für eine vollständige Sättigung des Kaffeebetts, bevor du weitermachst."
            else -> "Ensure the coffee bed is fully saturated before moving on."
        }
        key.contains("mantiene altura corta de vertido") -> when {
            language.startsWith("fr") -> "Garde une faible hauteur de versement pour éviter la canalisation."
            language.startsWith("pt") -> "Mantenha baixa altura de despejo para evitar canalização."
            language.startsWith("de") -> "Halte die Gießhöhe niedrig, um Channeling zu vermeiden."
            else -> "Keep a low pour height to avoid channeling."
        }
        key.contains("controla el flujo") -> when {
            language.startsWith("fr") -> "Contrôle le débit : s'il accélère trop, affine la mouture."
            language.startsWith("pt") -> "Controle o fluxo: se acelerar demais, moa mais fino."
            language.startsWith("de") -> "Kontrolliere den Fluss: Wird er zu schnell, feiner mahlen."
            else -> "Control the flow: if it speeds up too much, grind finer."
        }
        key.contains("mantiene temperatura estable") -> when {
            language.startsWith("fr") -> "Maintiens une température stable et évite d'agiter excessivement."
            language.startsWith("pt") -> "Mantenha a temperatura estável e evite agitação excessiva."
            language.startsWith("de") -> "Halte die Temperatur stabil und vermeide übermäßige Bewegung."
            else -> "Keep temperature stable and avoid excessive agitation."
        }
        key.contains("busca consistencia de flujo") -> when {
            language.startsWith("fr") -> "Recherche un débit régulier et un lit uniforme."
            language.startsWith("pt") -> "Busque consistência de fluxo e leito uniforme."
            language.startsWith("de") -> "Achte auf gleichmäßigen Fluss und ein gleichmäßiges Kaffeebett."
            else -> "Keep flow consistency and an even coffee bed."
        }
        key.contains("cierra esta fase en") -> when {
            language.startsWith("fr") -> line.replace(Regex("(?i)cierra esta fase en\\s*"), "Termine cette phase en ").replace(Regex("(?i)\\s*s y prepara la transicion"), " s et prépare la transition.")
            language.startsWith("pt") -> line.replace(Regex("(?i)cierra esta fase en\\s*"), "Feche esta fase em ").replace(Regex("(?i)\\s*s y prepara la transicion"), " s e prepare a transição.")
            language.startsWith("de") -> line.replace(Regex("(?i)cierra esta fase en\\s*"), "Schließe diese Phase in ").replace(Regex("(?i)\\s*s y prepara la transicion"), " s ab und bereite den Übergang vor.")
            else -> line.replace(Regex("(?i)cierra esta fase en\\s*"), "Close this phase in ").replace(Regex("(?i)\\s*s y prepara la transicion"), "s and prepare the transition.")
        }
        key.contains("queda poco de fase") -> when {
            language.startsWith("fr") -> "La phase se termine bientôt : privilégie la précision à la vitesse."
            language.startsWith("pt") -> "A fase está no fim: priorize precisão sobre velocidade."
            language.startsWith("de") -> "Die Phase endet bald: Präzision vor Geschwindigkeit."
            else -> "This phase is almost done: prioritize precision over speed."
        }
        key.contains("mantiene el patron actual") -> when {
            language.startsWith("fr") -> "Garde le schéma actuel pour maintenir la cohérence d'extraction."
            language.startsWith("pt") -> "Mantenha o padrão atual para preservar a consistência da extração."
            language.startsWith("de") -> "Behalte das aktuelle Muster bei, um die Extraktionskonsistenz zu halten."
            else -> "Keep the current pattern to maintain extraction consistency."
        }
        key.contains("tiempo corto para espresso") -> when {
            language.startsWith("fr") -> "Temps espresso court : tendance à plus d'acidité."
            language.startsWith("pt") -> "Tempo curto de espresso: tende a aumentar a acidez."
            language.startsWith("de") -> "Kurze Espressozeit: tendenziell mehr Säure."
            else -> "Short espresso time: tends to increase acidity."
        }
        key.contains("tiempo largo para espresso") -> when {
            language.startsWith("fr") -> "Temps espresso long : plus de corps et risque d'amertume."
            language.startsWith("pt") -> "Tempo longo de espresso: mais corpo e risco de amargor."
            language.startsWith("de") -> "Lange Espressozeit: mehr Körper und Bitterkeitsrisiko."
            else -> "Long espresso time: increases body and bitterness risk."
        }
        key.contains("tiempo de espresso en ventana recomendada") -> when {
            language.startsWith("fr") -> "Le temps espresso est dans la fenêtre recommandée."
            language.startsWith("pt") -> "O tempo de espresso está na faixa recomendada."
            language.startsWith("de") -> "Die Espressozeit liegt im empfohlenen Bereich."
            else -> "Espresso time is within the recommended window."
        }
        key.contains("en espresso, corta al rubio claro") -> when {
            language.startsWith("fr") -> "En espresso, arrête à blond clair pour éviter l'amertume finale."
            language.startsWith("pt") -> "No espresso, pare no loiro claro para evitar amargor final."
            language.startsWith("de") -> "Beim Espresso bei heller Blondierung stoppen, um späte Bitterkeit zu vermeiden."
            else -> "For espresso, stop at light blonding to avoid late bitterness."
        }
        key.contains("en italiana, retira al primer burbujeo fuerte") -> when {
            language.startsWith("fr") -> "En moka, retire au premier bouillonnement fort pour éviter les notes brûlées."
            language.startsWith("pt") -> "Na moka, retire no primeiro borbulhar forte para evitar notas queimadas."
            language.startsWith("de") -> "Bei Moka beim ersten starken Blubbern vom Herd nehmen, um verbrannte Noten zu vermeiden."
            else -> "For moka, remove at the first strong sputter to avoid burnt notes."
        }
        key.contains("en prensa, rompe costra suave") -> when {
            language.startsWith("fr") -> "En presse, casse la croûte doucement et décante immédiatement."
            language.startsWith("pt") -> "Na prensa, quebre a crosta suavemente e decante imediatamente."
            language.startsWith("de") -> "Bei French Press die Kruste sanft brechen und sofort dekantieren."
            else -> "For French press, break crust gently and decant immediately."
        }
        key.contains("en aeropress, presion constante") -> when {
            language.startsWith("fr") -> "En Aeropress, maintiens une pression fluide et constante."
            language.startsWith("pt") -> "Na Aeropress, mantenha pressão suave e constante."
            language.startsWith("de") -> "Bei Aeropress gleichmäßigen und sanften Druck halten."
            else -> "For Aeropress, keep pressure smooth and steady."
        }
        key.contains("cuida temperatura y distribucion") -> when {
            language.startsWith("fr") -> "Surveille température et distribution pour une tasse plus propre."
            language.startsWith("pt") -> "Cuide da temperatura e da distribuição para uma xícara mais limpa."
            language.startsWith("de") -> "Achte auf Temperatur und Verteilung für eine sauberere Tasse."
            else -> "Watch temperature and distribution for a cleaner cup."
        }
        key.contains("receta larga") -> when {
            language.startsWith("fr") -> "Recette longue : évite de trop diluer la tasse."
            language.startsWith("pt") -> "Receita longa: evite diluir demais a xícara."
            language.startsWith("de") -> "Langes Rezept: die Tasse nicht überverdünnen."
            else -> "Long recipe: avoid over-diluting the cup."
        }
        key.contains("receta corta") -> when {
            language.startsWith("fr") -> "Recette courte : évite la surextraction par contact excessif."
            language.startsWith("pt") -> "Receita curta: evite superextração por contato excessivo."
            language.startsWith("de") -> "Kurzes Rezept: Überextraktion durch zu lange Kontaktzeit vermeiden."
            else -> "Short recipe: avoid over-extraction from excessive contact time."
        }
        key.contains("volumen dentro de rango recomendado") -> when {
            language.startsWith("fr") -> "Le volume est dans la plage recommandée pour cette méthode."
            language.startsWith("pt") -> "O volume está na faixa recomendada para este método."
            language.startsWith("de") -> "Das Volumen liegt im empfohlenen Bereich für diese Methode."
            else -> "Volume is within the recommended range for this method."
        }
        key.contains("perfil concentrado") -> when {
            language.startsWith("fr") -> "Profil concentré : verse doucement et évite l'agitation excessive."
            language.startsWith("pt") -> "Perfil concentrado: despeje com suavidade e evite agitação excessiva."
            language.startsWith("de") -> "Konzentriertes Profil: sanft gießen und übermäßige Bewegung vermeiden."
            else -> "Concentrated profile: pour gently and avoid excessive agitation."
        }
        key.contains("perfil ligero") -> when {
            language.startsWith("fr") -> "Profil léger : pour plus de corps, augmente le contact ou affine un peu la mouture."
            language.startsWith("pt") -> "Perfil leve: para mais corpo, aumente o contato ou moa um pouco mais fino."
            language.startsWith("de") -> "Leichtes Profil: Für mehr Körper Kontaktzeit erhöhen oder etwas feiner mahlen."
            else -> "Lighter profile: for more body, increase contact or grind slightly finer."
        }
        key.contains("perfil equilibrado") -> when {
            language.startsWith("fr") -> "Profil équilibré : garde un rythme et un débit constants."
            language.startsWith("pt") -> "Perfil equilibrado: mantenha ritmo e fluxo consistentes."
            language.startsWith("de") -> "Ausgewogenes Profil: Rhythmus und Fluss konstant halten."
            else -> "Balanced profile: keep rhythm and flow consistent."
        }
        else -> line
    }
}

@Composable
private fun localizedBrewMethodName(name: String): String {
    val configuration = LocalConfiguration.current
    val appLanguage = remember(configuration) {
        val localeFromConfig = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale
        }
        (localeFromConfig?.language ?: Locale.getDefault().language).lowercase(Locale.ROOT)
    }
    val isSpanish = appLanguage.startsWith("es")
    if (isSpanish) return name
    return when (name.trim().lowercase(Locale.ROOT)) {
        "goteo" -> "Drip"
        "prensa francesa" -> "French press"
        "italiana" -> "Moka"
        "otros" -> "Other"
        "agua" -> "Water"
        else -> name
    }
}





