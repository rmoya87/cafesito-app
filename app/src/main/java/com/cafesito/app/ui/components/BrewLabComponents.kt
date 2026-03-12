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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
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
import com.cafesito.shared.domain.brew.BREW_METHOD_OTROS
import com.cafesito.shared.domain.brew.BrewMethodProfile
import com.cafesito.shared.domain.brew.BrewTimeProfile
import com.cafesito.app.ui.diary.DiaryAnalytics
import com.cafesito.app.ui.diary.DiaryPeriod
import com.cafesito.app.ui.profile.ProfileUiState
import com.cafesito.app.ui.profile.ProfileViewModel
import com.cafesito.app.ui.search.BarcodeActionIcon
import com.cafesito.app.ui.theme.*
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
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val chunkedMethods = methods.chunked(2)
        items(chunkedMethods) { rowMethods ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
    val context = LocalContext.current
    val resId = remember(method.iconResName) {
        context.resources.getIdentifier(method.iconResName, "drawable", context.packageName)
    }
    val cardColor = if (selected) LocalCaramelAccent.current else Color.Black
    val contentColor = if (selected) Color.White else Color.White

    Surface(
        modifier = modifier
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = cardColor,
        border = if (selected) BorderStroke(0.dp, Color.Transparent) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (resId != 0) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = method.name,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(Icons.Default.CoffeeMaker, contentDescription = "Método de elaboración", tint = contentColor, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = method.name.uppercase(),
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

private data class BrewLabPrepOption(val label: String, val drawableName: String?)
private data class BrewLabSizeOption(val label: String, val rangeLabel: String, val defaultMl: Int, val drawableName: String)

private val BREWLAB_TIPO_OPTIONS = listOf(
    BrewLabPrepOption("Espresso", "espresso"),
    BrewLabPrepOption("Americano", "americano"),
    BrewLabPrepOption("Capuchino", "capuchino"),
    BrewLabPrepOption("Latte", "latte"),
    BrewLabPrepOption("Macchiato", "macchiato"),
    BrewLabPrepOption("Moca", "moca"),
    BrewLabPrepOption("Goteo", null),
    BrewLabPrepOption("Otro", null)
)
private val BREWLAB_SIZE_OPTIONS = listOf(
    BrewLabSizeOption("Espresso", "25–30 ml", 30, "taza_espresso"),
    BrewLabSizeOption("Pequeño", "150–200 ml", 180, "taza_pequeno"),
    BrewLabSizeOption("Mediano", "250–300 ml", 275, "taza_mediano"),
    BrewLabSizeOption("Grande", "350–400 ml", 375, "taza_grande"),
    BrewLabSizeOption("Tazón XL", "450–500 ml", 475, "taza_xl")
)

@Composable
fun BrewLabMainStepContent(
    brewMethods: List<BrewMethod>,
    selectedMethod: BrewMethod?,
    onSelectMethod: (BrewMethod) -> Unit,
    selectedCoffee: Coffee?,
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 120.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        LazyRow(
            state = methodsListState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(brewMethods, key = { it.name }) { method ->
                val selected = method.name == selectedMethod?.name
                MethodCard(
                    method = method,
                    modifier = Modifier.width(140.dp),
                    selected = selected
                ) { onSelectMethod(method) }
            }
        }

        Spacer(Modifier.height(20.dp))
        PremiumCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clickable(onClick = onSelectCoffeeClick),
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.Black
        ) {
            Row(
                Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedCoffee?.nombre?.take(40) ?: "Selecciona café",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Abrir", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Tipo",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(8.dp))
        val context = LocalContext.current
        val unselectedChipBg = Color.Black
        val unselectedChipContent = Color.White
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(BREWLAB_TIPO_OPTIONS, key = { it.label }) { option ->
                    val isSelected = drinkType.equals(option.label, ignoreCase = true)
                    val selectedTextColor = Color.White
                    Surface(
                        onClick = { onDrinkTypeChange(option.label) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) LocalCaramelAccent.current else unselectedChipBg,
                        border = if (isSelected) BorderStroke(0.dp, Color.Transparent) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val resId = option.drawableName?.let { context.resources.getIdentifier(it, "drawable", context.packageName) } ?: 0
                            val iconTint = if (isSelected) selectedTextColor else unselectedChipContent
                            if (resId != 0) {
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = option.label,
                                    modifier = Modifier.size(28.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(Icons.Default.CoffeeMaker, contentDescription = option.label, tint = iconTint, modifier = Modifier.size(28.dp))
                            }
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) selectedTextColor else unselectedChipContent,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
        }

        if (selectedMethod?.isAgua != true) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Tamaño",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(8.dp))
            val contextSize = LocalContext.current
            val unselectedChipBgSize = Color.Black
            val unselectedChipContentSize = Color.White
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(BREWLAB_SIZE_OPTIONS, key = { it.label }) { option ->
                    val isSelected = selectedSizeLabel == option.label
                    val chipBg = if (isSelected) LocalCaramelAccent.current else unselectedChipBgSize
                    val chipContent = if (isSelected) Color.White else unselectedChipContentSize
                    Surface(
                        onClick = { onSizeSelected(option.label, option.defaultMl.toFloat()) },
                        shape = RoundedCornerShape(14.dp),
                        color = chipBg,
                        border = if (isSelected) BorderStroke(0.dp, Color.Transparent) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val sizeResId = contextSize.resources.getIdentifier(option.drawableName, "drawable", contextSize.packageName)
                            if (sizeResId != 0) {
                                Image(
                                    painter = painterResource(id = sizeResId),
                                    contentDescription = option.label,
                                    modifier = Modifier.size(24.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(Icons.Default.LocalCafe, contentDescription = option.label, modifier = Modifier.size(20.dp), tint = chipContent)
                            }
                            Column {
                                Text(option.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = chipContent)
                                Text(option.rangeLabel, style = MaterialTheme.typography.bodySmall, color = chipContent.copy(alpha = 0.9f))
                            }
                        }
                    }
                }
            }
        }

        if (selectedMethod != null && !selectedMethod.isOtros) {
            Spacer(Modifier.height(16.dp))
            ConfigStep(
                    methodName = selectedMethod?.name,
                    isEspressoMethod = isEspressoMethod,
                    isRatioEditable = isRatioEditable,
                    isWaterEditable = isWaterEditable,
                    isAguaMethod = selectedMethod?.isAgua == true,
                    brewTimeSeconds = brewTimeSeconds,
                    timeProfile = timeProfile,
                    methodProfile = methodProfile,
                    water = water,
                    ratio = ratio,
                    coffeeGrams = coffeeGrams,
                    valuation = valuation,
                    baristaTips = baristaTips,
                    viewModel = viewModel
                )
        }

        if (selectedMethod != null && !selectedMethod.isOtros && !selectedMethod.isAgua) {
            Spacer(Modifier.height(16.dp))
            PremiumCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.Black
            ) {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Temporizador",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = brewTimerEnabled, onCheckedChange = onBrewTimerEnabledChange)
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
    onCoffeeSelected: (Coffee, Boolean) -> Unit
) {
    val filteredPantry = if (searchQuery.isBlank()) pantryItems else pantryItems.filter {
        it.coffee.nombre.contains(searchQuery, true) || it.coffee.marca.contains(searchQuery, true)
    }
    val filteredSuggestions = if (searchQuery.isBlank()) allCoffees.take(10) else allCoffees.filter {
        it.coffee.nombre.contains(searchQuery, true) || it.coffee.marca.contains(searchQuery, true)
    }.take(10)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "TU DESPENSA",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(12.dp))
            if (filteredPantry.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (searchQuery.isBlank()) "Tu despensa está vacía." else "No hay coincidencias en tu despensa.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 24.dp, end = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredPantry, key = { it.pantryItem.id }) { item ->
                        PantryPremiumMiniCard(item = item, onClick = { onCoffeeSelected(item.coffee, true) })
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
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("SUGERENCIAS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onCreateCoffeeClick) {
                        Text("Crea tu café", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Buscar café...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(999.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar café") },
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
                    "No hay sugerencias disponibles.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            items(filteredSuggestions, key = { it.coffee.id }) { item ->
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    SimpleCoffeeSelectionCard(item.coffee) { onCoffeeSelected(item.coffee, false) }
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
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = coffee.imageUrl,
                contentDescription = coffee.nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(16.dp))
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
            Icon(Icons.Filled.Add, contentDescription = "Añadir", tint = MaterialTheme.colorScheme.primary)
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
    viewModel: BrewLabViewModel
) {
    val waterBlue = WaterBlue
    var waterDraft by remember { mutableStateOf(water.roundToInt().toString()) }
    var coffeeDraft by remember { mutableStateOf(formatDecimalWithComma(coffeeGrams)) }
    LaunchedEffect(water) { waterDraft = water.roundToInt().toString() }
    LaunchedEffect(coffeeGrams) { coffeeDraft = formatDecimalWithComma(coffeeGrams) }

    val ratioProfile = remember(ratio, methodProfile) {
        val span = (methodProfile.ratioMax - methodProfile.ratioMin).toFloat().coerceAtLeast(0.1f)
        val normalized = (ratio - methodProfile.ratioMin.toFloat()) / span
        when {
            normalized <= 0.35f -> "CONCENTRADO"
            normalized <= 0.7f -> "EQUILIBRADO"
            else -> "LIGERO"
        }
    }
    val sliderInactiveTrackColor = if (isSystemInDarkTheme()) Color(0xFF404040) else Color(0xFFE0E0E0)
    val coffeeColor = LocalCaramelAccent.current
    val isOtrosMethod = methodName == BREW_METHOD_OTROS // "Rápido"
    var showBaristaModal by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(16.dp))
        val configTitle = if (isAguaMethod) "Configura tu Agua" else "Configura tu ${methodName ?: "elaboración"}"
        Text(
            text = configTitle,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(8.dp))
        PremiumCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Box {
                Column(Modifier.padding(24.dp)) {
                    if (isOtrosMethod) {
                        Text(
                            text = "Este método no tiene parámetros específicos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else if (isAguaMethod) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            BrewMetricInputBlock(
                                label = "Agua (ml)",
                                value = waterDraft,
                                valueColor = waterBlue,
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f),
                                onValueChange = { raw ->
                                    val filtered = raw.filter { it.isDigit() }
                                    waterDraft = filtered
                                    filtered.toFloatOrNull()?.let { parsed ->
                                        viewModel.setWaterAmount(
                                            parsed.coerceIn(
                                                methodProfile.waterMinMl.toFloat(),
                                                methodProfile.waterMaxMl.toFloat()
                                            )
                                        )
                                    }
                                }
                            )
                            Slider(
                                modifier = Modifier.weight(2f),
                                value = water,
                                onValueChange = { viewModel.setWaterAmount(it) },
                                valueRange = methodProfile.waterMinMl.toFloat()..methodProfile.waterMaxMl.toFloat(),
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
                                label = "Agua (ml)",
                                value = waterDraft,
                                valueColor = waterBlue,
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f),
                                onValueChange = { raw ->
                                    val filtered = raw.filter { it.isDigit() }
                                    waterDraft = filtered
                                    filtered.toFloatOrNull()?.let { parsed ->
                                        viewModel.setWaterAmount(
                                            parsed.coerceIn(
                                                methodProfile.waterMinMl.toFloat(),
                                                methodProfile.waterMaxMl.toFloat()
                                            )
                                        )
                                    }
                                }
                            )
                            Slider(
                                modifier = Modifier.weight(2f),
                                value = water,
                                onValueChange = { viewModel.setWaterAmount(it) },
                                valueRange = methodProfile.waterMinMl.toFloat()..methodProfile.waterMaxMl.toFloat(),
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
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            BrewMetricInputBlock(
                                label = "Café (g)",
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
                            if (isRatioEditable) {
                                Column(modifier = Modifier.weight(2f)) {
                                    Slider(
                                        value = ratio,
                                        onValueChange = { viewModel.setRatio(it) },
                                        valueRange = methodProfile.ratioMin.toFloat()..methodProfile.ratioMax.toFloat(),
                                        steps = 0,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = sliderInactiveTrackColor,
                                            activeTickColor = Color.Transparent,
                                            inactiveTickColor = Color.Transparent
                                        )
                                    )
                                }
                            } else {
                                val coffeeMin = (methodProfile.waterMinMl / methodProfile.ratioMax).toFloat().coerceAtLeast(1f)
                                val coffeeMax = (methodProfile.waterMaxMl / methodProfile.ratioMin).toFloat().coerceIn(coffeeMin, 250f)
                                val ratioLabelValue = if (methodProfile.ratioStep < 1.0) String.format(Locale.US, "%.1f", ratio) else ratio.roundToInt().toString()
                                val ratioTitle = "RATIO 1:$ratioLabelValue - $ratioProfile"
                                Column(modifier = Modifier.weight(2f)) {
                                    @Suppress("DEPRECATION")
                                    Text(ratioTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Slider(
                                        value = coffeeGrams.coerceIn(coffeeMin, coffeeMax),
                                        onValueChange = { viewModel.setCoffeeGrams(it) },
                                        valueRange = coffeeMin..coffeeMax,
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
                        }
                        Spacer(Modifier.height(20.dp))
                    }

                    if (isEspressoMethod) {
                        val timeSliderColor = if (isSystemInDarkTheme()) Color.White else Color.Black
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            BrewMetricInputBlock(
                                label = "Tiempo (s)",
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
                                value = brewTimeSeconds.toFloat(),
                                onValueChange = { viewModel.setBrewTimeSeconds(it.roundToInt()) },
                                valueRange = timeProfile.minSeconds.toFloat()..timeProfile.maxSeconds.toFloat(),
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
                        Spacer(Modifier.height(24.dp))
                        Surface(
                            onClick = { showBaristaModal = true },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Consejos del barista",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Abrir", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                }
            }
        }

        if (showBaristaModal && baristaTips.isNotEmpty()) {
            val baristaSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showBaristaModal = false },
                sheetState = baristaSheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .padding(bottom = 32.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Consejos del barista",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(16.dp))
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
    val haptic = LocalHapticFeedback.current
    val currentPhase = timeline.getOrNull(currentPhaseIndex) ?: BrewPhaseInfo("Listo", "Proceso completado.", 0)
    val nextPhase = timeline.getOrNull(currentPhaseIndex + 1)
    val totalSeconds = timeline.sumOf { it.durationSeconds }.coerceAtLeast(1)
    val totalProgress = (timerSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
    val adviceLines = remember(processAdvice) {
        processAdvice
            .split(".")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
    val adviceCards = remember(currentPhase.instruction, adviceLines) {
        listOf(currentPhase.instruction) + adviceLines.map { "$it." }
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
            Column(modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            PremiumCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                containerColor = Color.Black
            ) {
                Box {
                    WaterWaveAnimation(
                        progress = totalProgress,
                        color = LocalCaramelAccent.current.copy(alpha = 0.05f),
                        modifier = Modifier.matchParentSize()
                    )

                    Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = currentPhase.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
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

                                val timerColor = if (remainingSeconds <= 5 && isTimerRunning) ElectricRed else if (isSystemInDarkTheme()) Color.White else Color.Black
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

                            val nextLabel = nextPhase?.label ?: "Finalizar"
                            Text(
                                text = "Siguiente: $nextLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )

                            Spacer(Modifier.height(24.dp))

                            BrewTimeline(phases = timeline, elapsedTotalSeconds = timerSeconds)

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                @Suppress("DEPRECATION")
                                Text(
                                    text = String.format("TOTAL %02d:%02d", totalSeconds / 60, totalSeconds % 60),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                val timeDisplayColor = Color.White
                                @Suppress("DEPRECATION")
                                Text(
                                    text = String.format("%02d:%02d", timerSeconds / 60, timerSeconds % 60),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = timeDisplayColor
                                )
                            }
                        }
                }
            }
            }

            val timerEnded = hasTimerStarted && totalSeconds > 0 && timerSeconds >= totalSeconds
            if (timerEnded) {
                Spacer(Modifier.height(12.dp))
                PreparationTasteCard(selectedTaste, recommendation, viewModel)
            } else if (adviceCards.isNotEmpty()) {
                val isDark = isSystemInDarkTheme()
                val adviceCardBg = if (isDark) Color(0xFF303030) else Color(0xFFEFEFEF)
                val adviceCardText = if (isDark) Color(0xFFF0F0F0) else Color(0xFF2F2F2F)
                val adviceBorder = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    state = rememberLazyListState(),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 24.dp, end = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(advicePages) { page ->
                        Column(
                            modifier = Modifier.fillParentMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            page.forEach { line ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = adviceCardBg,
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, adviceBorder)
                                ) {
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = adviceCardText,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!timerEnded) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (hasTimerStarted) {
                        OutlinedButton(
                            onClick = { viewModel.resetTimer() },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("REINICIAR", fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = { viewModel.toggleTimer() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTimerRunning) ElectricRed else MaterialTheme.colorScheme.primary,
                            contentColor = if (isTimerRunning) Color.White else if (isSystemInDarkTheme()) Color.Black else MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(
                            if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isTimerRunning) "Pausar" else "Iniciar",
                            tint = if (isTimerRunning) Color.White else if (isSystemInDarkTheme()) Color.Black else MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isTimerRunning) "PAUSAR" else "INICIAR",
                            fontWeight = FontWeight.Bold,
                            color = if (isTimerRunning) Color.White else if (isSystemInDarkTheme()) Color.Black else MaterialTheme.colorScheme.onPrimary
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
    val timeBarColor = if (isSystemInDarkTheme()) Color.White else Color.Black
    val softGray = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Column(Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
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

        Spacer(Modifier.height(8.dp))

        Row(Modifier
            .fillMaxWidth()
            .height(12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
        "Amargo" to Icons.Default.LocalFireDepartment,
        "Ácido" to Icons.Default.Science,
        "Equilibrado" to Icons.Default.Verified,
        "Salado" to Icons.Default.Waves,
        "Acuoso" to Icons.Default.WaterDrop,
        "Aspero" to Icons.Default.Grain,
        "Dulce" to Icons.Default.Favorite
    )
    PremiumCard(modifier = Modifier.fillMaxWidth(), containerColor = Color.Black) {
        Column(Modifier.padding(24.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                tastes.chunked(2).forEach { rowTastes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowTastes.forEach { (label, icon) ->
                            TasteChip(
                                label = label.uppercase(),
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
                    Spacer(Modifier.height(32.dp))
                    Surface(
                        color = Color.Black,
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(0.dp, Color.Transparent)
                    ) {
                        Column(Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Recomendación", tint = LocalCaramelAccent.current, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                @Suppress("DEPRECATION")
                                Text(
                                    "Recomendación",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    color = LocalCaramelAccent.current
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = recommendation ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
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

/** Pantalla de resultado: selección de sabor (Guardar en topBar). Mismo ancho que elaboración. */
@Composable
fun ResultStep(
    selectedTaste: String?,
    recommendation: String?,
    onSave: () -> Unit,
    viewModel: BrewLabViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
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
    val contentColorWhenSelected = if (isDark) Color.Black else Color.White
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, primary),
        shadowElevation = if (isSelected) 3.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) contentColorWhenSelected else primary
            )
            Spacer(Modifier.width(12.dp))
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
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.coffee.imageUrl,
                contentDescription = item.coffee.nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(text = item.coffee.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                @Suppress("DEPRECATION")
                Text(text = item.coffee.marca.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
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
            Icon(Icons.Default.ChevronRight, contentDescription = "Abrir", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun BottomActionContainer(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 80.dp),
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
        modifier = Modifier.padding(bottom = 12.dp)
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
                .clip(RoundedCornerShape(8.dp))
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
        shape = RoundedCornerShape(16.dp),
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier
                .size(26.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                if (useDrawable && drawableId != null) {
                    Image(
                        painter = painterResource(id = drawableId),
                        contentDescription = tip.label,
                        modifier = Modifier.size(14.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(icon, contentDescription = tip.label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                @Suppress("DEPRECATION")
                Text(text = tip.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                @Suppress("DEPRECATION")
                Text(
                    text = tip.value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 15.sp
                )
            }
        }
    }
}





