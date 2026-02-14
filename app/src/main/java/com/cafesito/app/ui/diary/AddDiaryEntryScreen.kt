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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cafesito.app.data.Coffee
import com.cafesito.app.data.CoffeeWithDetails
import com.cafesito.app.data.PantryItemWithDetails
import com.cafesito.app.ui.components.*
import com.cafesito.app.ui.search.BarcodeActionIcon
import com.cafesito.app.ui.theme.*
import com.cafesito.app.ui.utils.*
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDiaryEntryScreen(
    initialType: String,
    quickStart: Boolean = false,
    onBackClick: () -> Unit,
    onAddNotFoundClick: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val pantryItems by viewModel.pantryItems.collectAsState(initial = emptyList())
    val allCoffees by viewModel.availableCoffees.collectAsState(initial = emptyList())
    
    var selectedCoffee by remember { mutableStateOf<Coffee?>(null) }
    var step by remember(quickStart, initialType) { mutableIntStateOf(if (quickStart && initialType != "WATER") 2 else 1) }
    var isFromPantry by remember { mutableStateOf(false) }
    var selectedDoseGrams by remember { mutableFloatStateOf(15f) }
    var selectedPrepType by remember { mutableStateOf("Espresso") }
    var selectedSize by remember { mutableStateOf(CoffeeSizeOption.default()) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (initialType != "WATER") {
                GlassyTopBar(
                    title = when (step) {
                        1 -> if (quickStart) "SELECCIÓN RÁPIDA" else "SELECCIONAR CAFÉ"
                        2 -> "DOSIS DE CAFÉ"
                        3 -> "TIPO DE CAFÉ"
                        else -> "SELECCIONA TAMAÑO"
                    },
                    onBackClick = {
                        when {
                            step == 1 -> onBackClick()
                            quickStart && step == 2 -> onBackClick()
                            else -> step -= 1
                        }
                    },
                    actions = {
                        if (step in 2..3) {
                            IconButton(onClick = { step += 1 }) {
                                Icon(Icons.Default.ArrowForwardIos, contentDescription = "Siguiente")
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (initialType == "WATER") {
                WaterRegistrationStepPremium(
                    onRegister = { ml ->
                        scope.launch {
                            isSaving = true
                            viewModel.addWaterConsumption(ml)
                            onBackClick()
                        }
                    },
                    onCancel = onBackClick,
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
                            onQuickAddClick = {
                                selectedCoffee = null
                                isFromPantry = false
                                selectedDoseGrams = 15f
                                selectedPrepType = "Espresso"
                                selectedSize = CoffeeSizeOption.default()
                                step = 2
                            },
                            onCoffeeSelected = { coffee, fromPantry -> 
                                selectedCoffee = coffee
                                isFromPantry = fromPantry
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
                            onSelected = { selectedSize = it },
                            onRegister = {
                                scope.launch {
                                    if (!quickStart) {
                                        isSaving = true
                                        val caffeine = calculateAverageCaffeine(
                                            type = selectedPrepType,
                                            grams = selectedDoseGrams.roundToInt(),
                                            sizeFactor = selectedSize.multiplier,
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
                                            sizeLabel = selectedSize.label
                                        )
                                    }
                                    onBackClick()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WaterRegistrationStepPremium(onRegister: (Int) -> Unit, onCancel: () -> Unit, isSaving: Boolean) {
    var ml by remember { mutableFloatStateOf(250f) }
    val waterBlue = Color(0xFF2196F3)

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
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = waterBlue
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "${ml.roundToInt()} ml",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text("CANTIDAD DE AGUA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
            }
        }
        
        Spacer(Modifier.height(48.dp))
        
        Slider(
            value = ml,
            onValueChange = { ml = it },
            valueRange = 50f..1000f,
            enabled = !isSaving,
            colors = SliderDefaults.colors(
                thumbColor = waterBlue,
                activeTrackColor = waterBlue,
                inactiveTrackColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(Modifier.weight(1f))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                enabled = !isSaving,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, waterBlue),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = waterBlue)
            ) {
                Text("CANCELAR", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            Button(
                onClick = { onRegister(ml.roundToInt()) },
                enabled = !isSaving,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = waterBlue),
                shape = RoundedCornerShape(28.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("REGISTRAR", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CoffeeSelectionStepPremium(
    pantryItems: List<PantryItemWithDetails>,
    allCoffees: List<CoffeeWithDetails>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddNotFoundClick: () -> Unit,
    onQuickAddClick: () -> Unit,
    onCoffeeSelected: (Coffee, Boolean) -> Unit,
    onBarcodeClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val filteredCatalog = allCoffees.filter { 
        !it.coffee.isCustom && (
            it.coffee.nombre.contains(searchQuery, ignoreCase = true) || 
            it.coffee.marca.contains(searchQuery, ignoreCase = true)
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
                Text("TU DESPENSA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                
                TextButton(
                    onClick = onAddNotFoundClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Bolt, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("Añadir mi café", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(16.dp))
            if (pantryItems.isEmpty()) {
                PremiumCard {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Tu despensa está vacía", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(pantryItems) { item ->
                        PantryPremiumMiniCard(item) { onCoffeeSelected(item.coffee, true) }
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
                placeholder = { Text("Busca un café o marca") },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = if (isDark) Color.Black else Color.White,
                    focusedContainerColor = if (isDark) Color.Black else Color.White
                ),
                singleLine = true,
                leadingIcon = { 
                    Box(modifier = Modifier.padding(start = 4.dp)) {
                        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) 
                    }
                },
                trailingIcon = {
                    IconButton(
                        onClick = onBarcodeClick,
                        modifier = Modifier.padding(end = 12.dp)
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
                Text("SUGERENCIAS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = onQuickAddClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Bolt, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("Registro rápido", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        items(filteredCatalog) { coffee ->
            CoffeePremiumRowItem(coffee) { onCoffeeSelected(coffee.coffee, false) }
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
    val coffeeColor = CaramelAccent
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
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = coffeeColor
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = coffee?.nombre ?: "Registro rápido",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${doseGrams.roundToInt()} g",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text("DOSIS DE CAFÉ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
            }
        }

        Spacer(Modifier.height(48.dp))

        Slider(
            value = doseGrams,
            onValueChange = onDoseChange,
            valueRange = 5f..40f,
            colors = SliderDefaults.colors(
                thumbColor = coffeeColor,
                activeTrackColor = coffeeColor,
                inactiveTrackColor = MaterialTheme.colorScheme.outline
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
            PrepOptionUi("Espresso", "espresso"),
            PrepOptionUi("Americano", "americano"),
            PrepOptionUi("Capuchino", "capuchino"),
            PrepOptionUi("Latte", "latte"),
            PrepOptionUi("Macchiato", "macchiato"),
            PrepOptionUi("Moca", "moca"),
            PrepOptionUi("Vienés", "vienes"),
            PrepOptionUi("Irlandés", "irlandes"),
            PrepOptionUi("Frappuccino", "frappuccino")
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(prepTypes) { option ->
            val isSelected = selectedType == option.label
            val resId = context.resources.getIdentifier(option.drawableName, "drawable", context.packageName)
            Surface(
                onClick = { onTypeSelected(option.label) },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) CaramelAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
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
                            contentDescription = option.label,
                            modifier = Modifier.size(46.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(Icons.Default.CoffeeMaker, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(option.label, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun CoffeeSizeStepPremium(
    selected: CoffeeSizeOption,
    isSaving: Boolean,
    onSelected: (CoffeeSizeOption) -> Unit,
    onRegister: () -> Unit
) {
    val options = remember { CoffeeSizeOption.defaults() }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.forEach { option ->
            val isSelected = option.label == selected.label
            Surface(
                onClick = { onSelected(option) },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) CaramelAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(option.label, fontWeight = FontWeight.Bold)
                        Text(option.rangeLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.LocalCafe, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onRegister,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            } else {
                Text("REGISTRAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

data class CoffeeSizeOption(
    val label: String,
    val rangeLabel: String,
    val defaultMl: Int,
    val multiplier: Float
) {
    companion object {
        fun defaults(): List<CoffeeSizeOption> = listOf(
            CoffeeSizeOption("Espresso", "25–30 ml", 30, 0.35f),
            CoffeeSizeOption("Pequeño", "150–200 ml", 180, 0.8f),
            CoffeeSizeOption("Mediano", "250–300 ml", 275, 1.0f),
            CoffeeSizeOption("Grande", "350–400 ml", 375, 1.25f),
            CoffeeSizeOption("Tazón XL", "450–500 ml", 475, 1.5f)
        )

        fun default(): CoffeeSizeOption = defaults()[2]
    }
}

data class PrepOptionUi(val label: String, val drawableName: String)

private fun calculateAverageCaffeine(type: String, grams: Int, sizeFactor: Float, isDecaf: Boolean): Int {
    val base = CaffeineCalculator.calculate(type = type, grams = grams, isFromPantry = true, isDecaf = isDecaf)
    return (base * sizeFactor).roundToInt().coerceAtLeast(if (isDecaf) 1 else 10)
}

@Composable
fun PantryPremiumMiniCard(item: PantryItemWithDetails, onClick: () -> Unit) {
    PremiumCard(
        modifier = Modifier.width(160.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp)
    ) {
        Column {
            AsyncImage(
                model = item.coffee.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(100.dp)
            )
            Column(Modifier.padding(12.dp)) {
                Text(item.coffee.nombre, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                Text("${item.pantryItem.gramsRemaining}G REST.", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontSize = 8.sp)
            }
        }
    }
}

@Composable
fun CoffeePremiumRowItem(coffee: CoffeeWithDetails, onClick: () -> Unit) {
    PremiumCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(20.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = coffee.coffee.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)),
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
