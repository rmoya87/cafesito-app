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
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }

    val isCustomizing = initialType != "WATER" && step == 2

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!isCustomizing) {
                GlassyTopBar(
                    title = if (initialType == "WATER") "AÑADIR AGUA" else if (quickStart) "SELECCIÓN RÁPIDA" else "SELECCIONAR CAFÉ",
                    onBackClick = if (initialType == "WATER") null else onBackClick
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
                                step = 2
                            },
                            onCoffeeSelected = { coffee, fromPantry -> 
                                selectedCoffee = coffee
                                isFromPantry = fromPantry
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
                                                step = 2
                                            } else {
                                                searchQuery = value
                                            }
                                        }
                                    }
                            }
                        )
                    } else {
                         CoffeeCustomizationStepPremium(
                            coffee = selectedCoffee,
                            isFromPantry = isFromPantry,
                            onBackStep = { if (quickStart) onBackClick() else step = 1 },
                            quickStart = quickStart,
                            isSaving = isSaving,
                            onRegister = { name, caffeine, ml, grams, prepType ->
                                scope.launch {
                                    isSaving = true
                                    viewModel.addCoffeeConsumption(
                                        coffeeId = selectedCoffee?.id, 
                                        coffeeName = name, 
                                        coffeeBrand = selectedCoffee?.marca ?: "Café rápido", 
                                        caffeineAmount = caffeine, 
                                        amountMl = ml, 
                                        coffeeGrams = grams, 
                                        preparationType = prepType
                                    )
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
fun CoffeeCustomizationStepPremium(
    coffee: Coffee?,
    isFromPantry: Boolean,
    onBackStep: () -> Unit,
    isSaving: Boolean,
    quickStart: Boolean = false,
    onRegister: (String, Int, Int, Int, String) -> Unit
) {
    var grams by remember { mutableFloatStateOf(15f) }
    var selectedPrepType by remember { mutableStateOf("Espresso") }
    val context = LocalContext.current
    
    val prepTypes = remember {
        listOf(
            Triple("Espresso", "espresso", "Corto e intenso"),
            Triple("Americano", "americano", "Suave y largo"),
            Triple("Capuchino", "capuchino", "Equilibrado"),
            Triple("Latte", "latte", "Cremoso"),
            Triple("Macchiato", "macchiato", "Manchado"),
            Triple("Moca", "moca", "Con chocolate"),
            Triple("Vienés", "vienes", "Con nata"),
            Triple("Irlandés", "irlandes", "Con carácter"),
            Triple("Frappuccino", "frappuccino", "Refrescante")
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp) // Espacio para el botón fijo
        ) {
            if (!quickStart) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                        if (coffee != null) {
                            AsyncImage(model = coffee.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Coffee, null, Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)))))

                        Column(Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                            Text(coffee?.marca?.uppercase() ?: "CAFESITO", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge, fontSize = 10.sp)
                            Text(coffee?.nombre ?: "Selecciona tu estilo", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }

            item {
                Column(Modifier.padding(24.dp)) {
                    if (isFromPantry) {
                        Text("DOSIS DE CAFÉ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("${grams.roundToInt()} g", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                            Slider(
                                value = grams,
                                onValueChange = { grams = it },
                                valueRange = 5f..50f,
                                enabled = !isSaving,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary, 
                                    activeTrackColor = MaterialTheme.colorScheme.primary, 
                                    inactiveTrackColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                        Spacer(Modifier.height(32.dp))
                    }

                    Text("ESTILO DE PREPARACIÓN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                }
            }

            items(prepTypes.chunked(2)) { rowItems ->
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { (type, resName, _) ->
                        val isSelected = selectedPrepType == type
                        val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
                        
                        Surface(
                            onClick = { if (!isSaving) selectedPrepType = type },
                            shape = RoundedCornerShape(24.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.weight(1f).height(120.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (resId != 0) {
                                    Image(
                                        painter = painterResource(id = resId),
                                        contentDescription = type,
                                        modifier = Modifier.size(50.dp).padding(bottom = 8.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Text(
                                    text = type.uppercase(), 
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, 
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
                        startY = 0f
                    )
                )
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            val calculatedCaffeine = CaffeineCalculator.calculate(selectedPrepType, if (isFromPantry) grams.roundToInt() else null, isFromPantry)
            Button(
                onClick = { 
                    val finalName = if (coffee != null) "${coffee.nombre} ($selectedPrepType)" else selectedPrepType
                    onRegister(finalName, calculatedCaffeine, 200, grams.roundToInt(), selectedPrepType) 
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(30.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        @Suppress("DEPRECATION")
                        Text("AÑADIR REGISTRO", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onPrimary)
                        Text("ESTIMACIÓN: $calculatedCaffeine MG CAFEÍNA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    }
                }
            }
        }

        IconButton(
            onClick = { if (!isSaving) onBackStep() },
            modifier = Modifier.statusBarsPadding().padding(8.dp)
        ) {
            Surface(color = Color.Black.copy(alpha = 0.3f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBackIos, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
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
