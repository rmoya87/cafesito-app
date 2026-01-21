package com.example.cafesito.ui.diary

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.cafesito.data.Coffee
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.data.PantryItemWithDetails
import com.example.cafesito.ui.components.*
import com.example.cafesito.ui.theme.*
import com.example.cafesito.ui.utils.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDiaryEntryScreen(
    initialType: String,
    onBackClick: () -> Unit,
    onAddNotFoundClick: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val pantryItems by viewModel.pantryItems.collectAsState(initial = emptyList())
    val allCoffees by viewModel.availableCoffees.collectAsState(initial = emptyList())
    
    var selectedCoffee by remember { mutableStateOf<Coffee?>(null) }
    var step by remember { mutableIntStateOf(1) } 
    var isFromPantry by remember { mutableStateOf(false) }

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val isCustomizing = initialType != "WATER" && step == 2

    Scaffold(
        containerColor = SoftOffWhite,
        topBar = {
            if (!isCustomizing) {
                GlassyTopBar(
                    title = if (initialType == "WATER") "AÑADIR AGUA" else "SELECCIONAR CAFÉ",
                    onBackClick = onBackClick
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (initialType == "WATER") {
                WaterRegistrationStepPremium(
                    onRegister = { ml ->
                        viewModel.addWaterConsumption(ml)
                        onBackClick()
                    }
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
                            isSearchActive = isSearchActive,
                            onSearchToggle = { isSearchActive = !isSearchActive },
                            onSearchQueryChange = { searchQuery = it },
                            onQuickAddClick = {
                                selectedCoffee = null
                                isFromPantry = false
                                step = 2
                            },
                            onCoffeeSelected = { coffee, fromPantry -> 
                                selectedCoffee = coffee
                                isFromPantry = fromPantry
                                step = 2 
                            }
                        )
                    } else {
                        CoffeeCustomizationStepPremium(
                            coffee = selectedCoffee,
                            isFromPantry = isFromPantry,
                            onBackStep = { step = 1 },
                            onRegister = { name, caffeine, ml, grams, prepType ->
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
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WaterRegistrationStepPremium(onRegister: (Int) -> Unit) {
    var ml by remember { mutableFloatStateOf(250f) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PremiumCard(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.WaterDrop,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color(0xFF2196F3)
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "${ml.roundToInt()} ml",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = EspressoDeep
                )
                Text("CANTIDAD DE AGUA", style = MaterialTheme.typography.labelLarge, color = CaramelAccent, fontSize = 10.sp)
            }
        }
        
        Spacer(Modifier.height(48.dp))
        
        Slider(
            value = ml,
            onValueChange = { ml = it },
            valueRange = 50f..1000f,
            colors = SliderDefaults.colors(
                thumbColor = EspressoDeep,
                activeTrackColor = EspressoDeep,
                inactiveTrackColor = BorderLight
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(Modifier.height(64.dp))
        
        Button(
            onClick = { onRegister(ml.roundToInt()) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EspressoDeep),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("REGISTRAR HIDRATACIÓN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun CoffeeSelectionStepPremium(
    pantryItems: List<PantryItemWithDetails>,
    allCoffees: List<CoffeeWithDetails>,
    searchQuery: String,
    isSearchActive: Boolean,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onQuickAddClick: () -> Unit,
    onCoffeeSelected: (Coffee, Boolean) -> Unit
) {
    val filteredCatalog = allCoffees.filter { 
        it.coffee.nombre.contains(searchQuery, ignoreCase = true) || 
        it.coffee.marca.contains(searchQuery, ignoreCase = true)
    }.take(10)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp)
    ) {
        item {
            Text("TU DESPENSA", style = MaterialTheme.typography.labelLarge, color = CaramelAccent)
            Spacer(Modifier.height(16.dp))
            if (pantryItems.isEmpty()) {
                PremiumCard {
                    Text("Tu despensa está vacía", color = Color.Gray, modifier = Modifier.padding(24.dp))
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(pantryItems) { item ->
                        PantryPremiumMiniCard(item) { onCoffeeSelected(item.coffee, true) }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CATÁLOGO", style = MaterialTheme.typography.labelLarge, color = CaramelAccent)
                IconButton(onClick = onSearchToggle) {
                    Icon(if (isSearchActive) Icons.Default.Close else Icons.Default.Search, null, tint = EspressoDeep)
                }
            }
            
            if (isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    placeholder = { Text("Busca un grano...") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CaramelAccent)
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        items(filteredCatalog) { coffee ->
            CoffeePremiumRowItem(coffee) { onCoffeeSelected(coffee.coffee, false) }
            Spacer(Modifier.height(12.dp))
        }

        item {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onQuickAddClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EspressoDeep),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(Icons.Default.Bolt, null)
                Spacer(Modifier.width(12.dp))
                Text("REGISTRO RÁPIDO", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CoffeeCustomizationStepPremium(
    coffee: Coffee?,
    isFromPantry: Boolean,
    onBackStep: () -> Unit,
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

    Box(modifier = Modifier.fillMaxSize().background(SoftOffWhite)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    if (coffee != null) {
                        AsyncImage(model = coffee.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Box(Modifier.fillMaxSize().background(CreamLight), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Coffee, null, Modifier.size(60.dp), tint = CaramelAccent)
                        }
                    }
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, EspressoDeep.copy(alpha = 0.9f)))))
                    
                    Column(Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                        Text(coffee?.marca?.uppercase() ?: "CAFESITO", color = CaramelAccent, style = MaterialTheme.typography.labelLarge, fontSize = 10.sp)
                        Text(coffee?.nombre ?: "Selecciona tu estilo", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }

            item {
                Column(Modifier.padding(24.dp)) {
                    if (isFromPantry) {
                        Text("DOSIS DE CAFÉ", style = MaterialTheme.typography.labelLarge, color = CaramelAccent)
                        Spacer(Modifier.height(16.dp))
                        PremiumCard {
                            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${grams.roundToInt()} g", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                                Slider(
                                    value = grams,
                                    onValueChange = { grams = it },
                                    valueRange = 5f..50f,
                                    colors = SliderDefaults.colors(thumbColor = EspressoDeep, activeTrackColor = EspressoDeep)
                                )
                            }
                        }
                        Spacer(Modifier.height(32.dp))
                    }

                    Text("ESTILO DE PREPARACIÓN", style = MaterialTheme.typography.labelLarge, color = CaramelAccent)
                    Spacer(Modifier.height(16.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.height(600.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        userScrollEnabled = false
                    ) {
                        items(prepTypes) { (type, resName, _) ->
                            val isSelected = selectedPrepType == type
                            Surface(
                                onClick = { selectedPrepType = type },
                                shape = RoundedCornerShape(24.dp),
                                color = if (isSelected) EspressoDeep else Color.White,
                                border = if (isSelected) null else BorderStroke(1.dp, BorderLight),
                                modifier = Modifier.height(100.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = type.uppercase(), 
                                        color = if (isSelected) Color.White else EspressoDeep, 
                                        style = MaterialTheme.typography.labelLarge,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    val calculatedCaffeine = CaffeineCalculator.calculate(selectedPrepType, if (isFromPantry) grams.roundToInt() else null, isFromPantry)
                    
                    Button(
                        onClick = { 
                            val finalName = if (coffee != null) "${coffee.nombre} ($selectedPrepType)" else selectedPrepType
                            onRegister(finalName, calculatedCaffeine, 200, grams.roundToInt(), selectedPrepType) 
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CaramelAccent),
                        shape = RoundedCornerShape(30.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("AÑADIR REGISTRO", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text("ESTIMACIÓN: $calculatedCaffeine MG CAFEÍNA", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }

        IconButton(
            onClick = onBackStep,
            modifier = Modifier.statusBarsPadding().padding(8.dp)
        ) {
            Surface(color = Color.White.copy(alpha = 0.2f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
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
                Text(item.coffee.nombre, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.pantryItem.gramsRemaining}G REST.", style = MaterialTheme.typography.labelLarge, color = CaramelAccent, fontSize = 8.sp)
            }
        }
    }
}

@Composable
fun CoffeePremiumRowItem(coffee: CoffeeWithDetails, onClick: () -> Unit) {
    PremiumCard(modifier = Modifier.clickable { onClick() }, shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = coffee.coffee.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(coffee.coffee.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(coffee.coffee.marca.uppercase(), style = MaterialTheme.typography.labelSmall, color = CaramelAccent, fontSize = 9.sp)
            }
        }
    }
}
