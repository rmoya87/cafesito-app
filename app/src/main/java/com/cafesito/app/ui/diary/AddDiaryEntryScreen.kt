package com.cafesito.app.ui.diary

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.cafesito.app.ui.theme.WaterBlue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cafesito.app.R
import com.cafesito.app.data.Coffee
import com.cafesito.app.data.CoffeeWithDetails
import com.cafesito.app.data.PantryItemWithDetails
import com.cafesito.app.ui.components.*
import com.cafesito.app.ui.search.BarcodeActionIcon
import com.cafesito.app.ui.theme.*
import com.cafesito.app.ui.utils.*
import com.cafesito.shared.domain.brew.BrewCaffeineInput
import com.cafesito.shared.domain.brew.BrewEngine
import com.cafesito.shared.domain.brew.BrewSource
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDiaryEntryScreen(
    initialType: String,
    quickStart: Boolean = false,
    createdCoffeeId: String? = null,
    onCreatedCoffeeConsumed: () -> Unit = {},
    onBackClick: () -> Unit,
    onAddNotFoundClick: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val pantryItems by viewModel.pantryItems.collectAsState(initial = emptyList())
    val allCoffees by viewModel.availableCoffees.collectAsState(initial = emptyList())
    
    var selectedCoffee by remember { mutableStateOf<Coffee?>(null) }
    var selectedPantryItemId by remember { mutableStateOf<String?>(null) }
    var step by remember(quickStart, initialType) { mutableIntStateOf(if (quickStart && initialType != "WATER") 2 else 1) }
    var isFromPantry by remember { mutableStateOf(false) }
    var selectedDoseGrams by remember { mutableFloatStateOf(15f) }
    var selectedPrepType by remember { mutableStateOf("Espresso") }
    var selectedSize by remember { mutableStateOf(CoffeeSizeOption.default()) }
    var waterMl by remember { mutableFloatStateOf(250f) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(createdCoffeeId, allCoffees) {
        val newCoffeeId = createdCoffeeId ?: return@LaunchedEffect
        val createdCoffee = allCoffees.firstOrNull { it.coffee.id == newCoffeeId }?.coffee ?: return@LaunchedEffect
        selectedCoffee = createdCoffee
        isFromPantry = false
        selectedPantryItemId = null
        selectedDoseGrams = 15f
        selectedPrepType = "Espresso"
        selectedSize = CoffeeSizeOption.default()
        step = 2
        onCreatedCoffeeConsumed()
    }

    val registerCoffee: () -> Unit = {
        scope.launch {
            isSaving = true
            val caffeine = calculateAverageCaffeine(
                type = selectedPrepType,
                grams = selectedDoseGrams.roundToInt(),
                isDecaf = selectedCoffee?.cafeina?.equals("No", ignoreCase = true) == true
            )
            viewModel.addCoffeeConsumption(
                coffeeId = selectedCoffee?.id,
                coffeeName = selectedCoffee?.nombre ?: selectedPrepType,
                coffeeBrand = selectedCoffee?.marca ?: "Café rápido",
                caffeineAmount = caffeine,
                amountMl = selectedSize.defaultMl,
                coffeeGrams = selectedDoseGrams.roundToInt(),
                preparationType = selectedPrepType,
                sizeLabel = selectedSize.label,
                pantryItemId = selectedPantryItemId
            )
            onBackClick()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            val title = if (initialType == "WATER") stringResource(id = R.string.diary_add_water) else when (step) {
                1 -> if (quickStart) stringResource(id = R.string.diary_add_selection) else stringResource(id = R.string.diary_add_select)
                2 -> stringResource(id = R.string.diary_add_dose)
                3 -> stringResource(id = R.string.diary_add_type)
                else -> stringResource(id = R.string.diary_add_size)
            }
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (initialType == "WATER") {
                                onBackClick()
                            } else {
                                when {
                                    step == 1 -> onBackClick()
                                    quickStart && step == 2 -> onBackClick()
                                    else -> step -= 1
                                }
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.join_list_back))
                    }
                },
                actions = {
                    if (initialType == "WATER") {
                        TextButton(onClick = {
                            scope.launch {
                                isSaving = true
                                viewModel.addWaterConsumption(waterMl.roundToInt())
                                onBackClick()
                            }
                        }) {
                            Text(stringResource(id = R.string.diary_add_register).uppercase(), fontWeight = FontWeight.Bold)
                        }
                    } else if (step == 1) {
                        IconButton(
                            onClick = {
                                selectedCoffee = null
                                selectedPantryItemId = null
                                isFromPantry = false
                                selectedDoseGrams = 15f
                                selectedPrepType = "Espresso"
                                selectedSize = CoffeeSizeOption.default()
                                step = 2
                            }
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = stringResource(id = R.string.diary_add_quick_entry))
                        }
                    } else if (step in 2..3) {
                        IconButton(onClick = { step += 1 }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(id = R.string.diary_next))
                        }
                    } else if (step == 4) {
                        TextButton(onClick = registerCoffee, enabled = !isSaving) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text(stringResource(id = R.string.diary_add_register).uppercase(), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                    scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (initialType == "WATER") {
                WaterRegistrationStepPremium(
                    ml = waterMl,
                    onMlChange = { waterMl = it },
                    isSaving = isSaving
                )
            } else {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        fadeIn() + slideInHorizontally { it } togetherWith fadeOut() + slideOutHorizontally { -it }
                    },
                    label = "StepTransition"
                ) { currentStep ->
                    if (currentStep == 1) {
                        CoffeeSelectionStepPremium(
                            pantryItems = pantryItems,
                            allCoffees = allCoffees,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            onAddNotFoundClick = onAddNotFoundClick,
                            onCoffeeSelected = { coffee, fromPantry, pantryItemId -> 
                                selectedCoffee = coffee
                                isFromPantry = fromPantry
                                selectedPantryItemId = pantryItemId
                                selectedDoseGrams = 15f
                                selectedPrepType = "Espresso"
                                selectedSize = CoffeeSizeOption.default()
                                step = 2 
                            },
                            onBarcodeClick = {
                                val options = GmsBarcodeScannerOptions.Builder()
                                    .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8)
                                    .build()
                                GmsBarcodeScanning.getClient(context, options)
                                    .startScan()
                                    .addOnSuccessListener { barcode ->
                                        barcode.rawValue?.let { value ->
                                            val found = allCoffees.find { it.coffee.codigoBarras == value }
                                            if (found != null) {
                                                selectedCoffee = found.coffee
                                                isFromPantry = false
                                                selectedPantryItemId = null
                                                selectedDoseGrams = 15f
                                                selectedPrepType = "Espresso"
                                                selectedSize = CoffeeSizeOption.default()
                                                step = 2
                                            } else {
                                                searchQuery = value
                                            }
                                        }
                                    }
                            }
                        )
                    } else when (currentStep) {
                        2 -> CoffeeDoseStepPremium(
                            coffee = selectedCoffee,
                            doseGrams = selectedDoseGrams,
                            onDoseChange = { selectedDoseGrams = it }
                        )

                        3 -> CoffeeTypeStepPremium(
                            selectedType = selectedPrepType,
                            onTypeSelected = { selectedPrepType = it }
                        )

                        else -> CoffeeSizeStepPremium(
                            selected = selectedSize,
                            isSaving = isSaving,
                            onSelected = { selectedSize = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WaterRegistrationStepPremium(
    ml: Float,
    onMlChange: (Float) -> Unit,
    isSaving: Boolean
) {
    val waterBlue = WaterBlue
    var mlInput by remember(ml) { mutableStateOf(ml.roundToInt().toString()) }
    val sliderInactiveTrackColor = if (isSystemInDarkTheme()) SliderTrackInactiveDark else SliderTrackInactiveLight

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(0.5f))
        
        PremiumCard(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.WaterDrop,
                    contentDescription = stringResource(id = R.string.diary_add_water_cd),
                    modifier = Modifier.size(80.dp),
                    tint = waterBlue
                )
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = mlInput,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all(Char::isDigit)) {
                            mlInput = input
                            input.toFloatOrNull()?.let(onMlChange)
                        }
                    },
                    suffix = { Text(stringResource(id = R.string.diary_ml)) },
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        }
        
        Spacer(Modifier.height(48.dp))
        
        Slider(
            value = ml,
            onValueChange = onMlChange,
            valueRange = 50f..1000f,
            steps = 0,
            enabled = !isSaving,
            colors = SliderDefaults.colors(
                thumbColor = waterBlue,
                activeTrackColor = waterBlue,
                inactiveTrackColor = sliderInactiveTrackColor,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(Modifier.weight(1f))
        
        Spacer(Modifier.height(56.dp))
    }
}

@Composable
fun CoffeeSelectionStepPremium(
    pantryItems: List<PantryItemWithDetails>,
    allCoffees: List<CoffeeWithDetails>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddNotFoundClick: () -> Unit,
    onCoffeeSelected: (Coffee, Boolean, String?) -> Unit,
    onBarcodeClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val filteredCatalog = allCoffees.filter { 
        !it.coffee.isCustom && (
            it.coffee.nombre.containsSearchQuery(searchQuery) || 
            it.coffee.marca.containsSearchQuery(searchQuery)
        )
    }.take(10)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(id = R.string.diary_pantry_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                
            }
            Spacer(Modifier.height(16.dp))
            if (pantryItems.isEmpty()) {
                PremiumCard {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(id = R.string.empty_diary_no_pantry), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(pantryItems, key = { it.pantryItem.id }) { item ->
                        PantryPremiumMiniCard(item) { onCoffeeSelected(item.coffee, true, item.pantryItem.id) }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(id = R.string.add_stock_search_placeholder)) },
                shape = Shapes.pillFull,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = if (isDark) PureBlack else PureWhite,
                    focusedContainerColor = if (isDark) PureBlack else PureWhite
                ),
                singleLine = true,
                leadingIcon = { 
                    Box(modifier = Modifier.padding(start = 4.dp)) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(id = R.string.search_icon_search_cd), tint = MaterialTheme.colorScheme.onSurfaceVariant) 
                    }
                },
                trailingIcon = {
                    IconButton(
                        onClick = onBarcodeClick,
                        modifier = Modifier.padding(end = 12.dp),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
                    ) {
                        BarcodeActionIcon(
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(id = R.string.diary_suggestions_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = onAddNotFoundClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(stringResource(id = R.string.diary_create_coffee), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        items(filteredCatalog, key = { it.coffee.id }) { coffee ->
            CoffeePremiumRowItem(coffee) { onCoffeeSelected(coffee.coffee, false, null) }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun CoffeeDoseStepPremium(
    coffee: Coffee?,
    doseGrams: Float,
    onDoseChange: (Float) -> Unit
) {
    val coffeeColor = LocalCaramelAccent.current
    var doseInput by remember(doseGrams) { mutableStateOf(String.format(Locale.getDefault(), "%.1f", doseGrams)) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(0.5f))

        PremiumCard(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.LocalCafe,
                    contentDescription = stringResource(id = R.string.diary_add_coffee_cd),
                    modifier = Modifier.size(80.dp),
                    tint = coffeeColor
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = coffee?.nombre ?: stringResource(id = R.string.diary_add_quick_entry),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = doseInput,
                    onValueChange = { input ->
                        val normalized = input.replace(',', '.')
                        if (input.isEmpty() || normalized.all { it.isDigit() || it == '.' }) {
                            doseInput = input
                            normalized.toFloatOrNull()?.let(onDoseChange)
                        }
                    },
                    suffix = { Text(stringResource(id = R.string.diary_grams_suffix)) },
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        }

        Spacer(Modifier.height(48.dp))

        Slider(
            value = doseGrams,
            onValueChange = onDoseChange,
            valueRange = 3f..30f,
            steps = 269,
            colors = SliderDefaults.colors(
                thumbColor = coffeeColor,
                activeTrackColor = coffeeColor,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun CoffeeTypeStepPremium(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val prepTypes = remember {
        listOf(
            PrepOptionUi("Espresso", "espresso", R.string.prep_espresso),
            PrepOptionUi("Americano", "americano", R.string.prep_americano),
            PrepOptionUi("Capuchino", "capuchino", R.string.prep_capuchino),
            PrepOptionUi("Latte", "latte", R.string.prep_latte),
            PrepOptionUi("Macchiato", "macchiato", R.string.prep_macchiato),
            PrepOptionUi("Moca", "moca", R.string.prep_moca),
            PrepOptionUi("Vienés", "vienes", R.string.prep_vienes),
            PrepOptionUi("Irlandés", "irlandes", R.string.prep_irlandes),
            PrepOptionUi("Frappuccino", "frappuccino", R.string.prep_frappuccino),
            PrepOptionUi("Caramelo macchiato", "caramel_macchiato", R.string.prep_caramel_macchiato),
            PrepOptionUi("Corretto", "corretto", R.string.prep_corretto),
            PrepOptionUi("Freddo", "freddo", R.string.prep_freddo),
            PrepOptionUi("Latte macchiato", "latte_macchiato", R.string.prep_latte_macchiato),
            PrepOptionUi("Leche con chocolate", "leche_con_chocolate", R.string.prep_leche_con_chocolate),
            PrepOptionUi("Marroquí", "marroqui", R.string.prep_marroqui),
            PrepOptionUi("Romano", "romano", R.string.prep_romano),
            PrepOptionUi("Descafeinado", "descafeinado", R.string.prep_descafeinado)
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(prepTypes, key = { it.drawableName }) { option ->
                    val isSelected = selectedType == option.value
            val resId = context.resources.getIdentifier(option.drawableName, "drawable", context.packageName)
            Surface(
                onClick = { onTypeSelected(option.value) },
                shape = Shapes.shapeCardMedium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) LocalCaramelAccent.current else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                modifier = Modifier.height(120.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (resId != 0) {
                        Image(
                            painter = painterResource(resId),
                        contentDescription = stringResource(id = option.labelResId),
                            modifier = Modifier.size(46.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(Icons.Default.CoffeeMaker, contentDescription = stringResource(id = R.string.diary_brew_method_cd), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(id = option.labelResId), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun CoffeeSizeStepPremium(
    selected: CoffeeSizeOption,
    isSaving: Boolean,
    onSelected: (CoffeeSizeOption) -> Unit
) {
    val context = LocalContext.current
    val options = remember { CoffeeSizeOption.defaults() }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.forEach { option ->
            val isSelected = option.label == selected.label
            Surface(
                onClick = { onSelected(option) },
                shape = Shapes.cardLarge,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) LocalCaramelAccent.current else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val resId = context.resources.getIdentifier(option.drawableName, "drawable", context.packageName)
                    if (resId != 0) {
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = stringResource(id = option.labelResId),
                            modifier = Modifier.size(34.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(Icons.Default.LocalCafe, contentDescription = stringResource(id = R.string.diary_add_coffee_cd), tint = MaterialTheme.colorScheme.primary)
                    }
                    Column {
                        Text(stringResource(id = option.labelResId), fontWeight = FontWeight.Bold)
                        Text(stringResource(id = option.rangeResId), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

data class CoffeeSizeOption(
    val label: String,
    val labelResId: Int,
    val rangeLabel: String,
    val rangeResId: Int,
    val defaultMl: Int,
    val multiplier: Float,
    val drawableName: String
) {
    companion object {
        fun defaults(): List<CoffeeSizeOption> = listOf(
            CoffeeSizeOption("Espresso", R.string.size_espresso, "25–30 ml", R.string.size_range_espresso, 30, 0.35f, "taza_espresso"),
            CoffeeSizeOption("Pequeño", R.string.size_small, "150–200 ml", R.string.size_range_small, 180, 0.8f, "taza_pequeno"),
            CoffeeSizeOption("Mediano", R.string.size_medium, "250–300 ml", R.string.size_range_medium, 275, 1.0f, "taza_mediano"),
            CoffeeSizeOption("Grande", R.string.size_large, "350–400 ml", R.string.size_range_large, 375, 1.25f, "taza_grande"),
            CoffeeSizeOption("Tazón XL", R.string.size_xl, "450–500 ml", R.string.size_range_xl, 475, 1.5f, "taza_xl")
        )

        fun default(): CoffeeSizeOption = defaults()[2]
    }
}

data class PrepOptionUi(val value: String, val drawableName: String, val labelResId: Int)

private fun calculateAverageCaffeine(type: String, grams: Int, isDecaf: Boolean): Int {
    return BrewEngine.estimateCaffeineMg(
        BrewCaffeineInput(
            source = BrewSource.DIARY,
            methodOrPreparation = type,
            coffeeGrams = grams.toDouble(),
            hasCaffeine = !isDecaf
        )
    )
}

@Composable
fun PantryPremiumMiniCard(item: PantryItemWithDetails, onClick: () -> Unit) {
    PremiumCard(
        modifier = Modifier.width(160.dp).clickable { onClick() },
        shape = Shapes.pill
    ) {
        Column {
            AsyncImage(
                model = item.coffee.imageUrl,
                contentDescription = item.coffee.nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(100.dp)
            )
            Column(Modifier.padding(12.dp)) {
                Text(item.coffee.nombre, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    stringResource(id = R.string.diary_pantry_remaining, item.pantryItem.gramsRemaining),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 8.sp
                )
            }
        }
    }
}

@Composable
fun CoffeePremiumRowItem(coffee: CoffeeWithDetails, onClick: () -> Unit) {
    PremiumCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = Shapes.shapeCardMedium) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = coffee.coffee.imageUrl,
                contentDescription = coffee.coffee.nombre,
                modifier = Modifier.size(50.dp).clip(Shapes.cardSmall),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(coffee.coffee.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(coffee.coffee.marca.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontSize = 9.sp)
            }
        }
    }
}
