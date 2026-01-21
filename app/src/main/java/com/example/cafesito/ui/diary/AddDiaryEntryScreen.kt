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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.cafesito.ui.theme.CoffeeBrown
import kotlin.math.roundToInt

// --- TABLA DE CAFEÍNA ---
object CaffeineCalculator {
    private val baseCaffeineMap = mapOf(
        "Espresso" to 63,
        "Marroqui" to 95,
        "Aguamiles" to 95,
        "Americano" to 95,
        "Capuchino" to 95,
        "Latte" to 95,
        "Macchiato" to 95,
        "Moca" to 105,
        "Vienés" to 80,
        "Irlandés" to 96,
        "Corretto" to 63,
        "Frappuccino" to 80
    )

    fun calculate(type: String, grams: Int?, isFromPantry: Boolean): Int {
        val base = baseCaffeineMap[type] ?: 80
        if (!isFromPantry || grams == null) return base
        
        // Si es de despensa, escalamos ligeramente por el peso (base 15g)
        val ratio = grams.toFloat() / 15f
        return (base * ratio).roundToInt()
    }
}

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
        topBar = {
            if (!isCustomizing) {
                TopAppBar(
                    title = { 
                        Text(
                            text = if (initialType == "WATER") "Añadir Agua" else "Seleccionar Café",
                            fontWeight = FontWeight.Normal,
                            color = Color.Black
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.Black)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF9F5F2))
                )
            }
        },
        containerColor = if (isCustomizing) Color.White else Color(0xFFF9F5F2)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().let { if (!isCustomizing) it.padding(padding) else it }) {
            if (initialType == "WATER") {
                WaterRegistrationStep(
                    onRegister = { ml ->
                        viewModel.addWaterConsumption(ml)
                        onBackClick()
                    }
                )
            } else {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                        }
                    },
                    label = "StepTransition"
                ) { currentStep ->
                    if (currentStep == 1) {
                        CoffeeSelectionStep(
                            pantryItems = pantryItems,
                            allCoffees = allCoffees,
                            searchQuery = searchQuery,
                            isSearchActive = isSearchActive,
                            onSearchToggle = { isSearchActive = !isSearchActive },
                            onSearchQueryChange = { searchQuery = it },
                            onQuickAddClick = {
                                selectedCoffee = null // Café genérico
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
                        CoffeeCustomizationStep(
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
fun CoffeeSelectionStep(
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
    }.take(15)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text("Mi despensa", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Normal)
            Spacer(Modifier.height(16.dp))
            if (pantryItems.isEmpty()) {
                Text("Tu despensa está vacía", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(pantryItems) { item ->
                        PantrySuggestionCard(item) { 
                            onCoffeeSelected(item.coffee, true) 
                        }
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
                Text("Explorar catálogo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Normal)
                IconButton(onClick = onSearchToggle) {
                    Icon(if (isSearchActive) Icons.Default.Close else Icons.Default.Search, contentDescription = null, tint = Color.Black)
                }
            }
            
            if (isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    placeholder = { Text("Busca un café...") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CoffeeBrown,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    )
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        items(filteredCatalog) { coffee ->
            CoffeeRowItem(coffee) { onCoffeeSelected(coffee.coffee, false) }
        }

        item {
            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = onQuickAddClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CoffeeBrown)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = CoffeeBrown)
                Spacer(Modifier.width(8.dp))
                Text("Registro rápido (sin café específico)", fontWeight = FontWeight.Bold, color = CoffeeBrown)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun CoffeeCustomizationStep(
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
            Triple("Marroqui", "marroqui", "Con especias"),
            Triple("Aguamiles", "aguamiel", "Endulzado con aguamiel"),
            Triple("Americano", "americano", "Espresso con agua caliente"),
            Triple("Capuchino", "capuchino", "Espresso, leche y espuma"),
            Triple("Latte", "latte", "Mucha leche"),
            Triple("Macchiato", "macchiato", "Espresso con gota de leche"),
            Triple("Moca", "moca", "Espresso con chocolate y leche"),
            Triple("Vienés", "vienes", "Espresso con nata montada"),
            Triple("Irlandés", "irlandes", "Con whisky y nata"),
            Triple("Corretto", "corretto", "Con un toque de licor"),
            Triple("Frappuccino", "frappuccino", "Batido con hielo y nata")
        )
    }

    val prepConfig = mapOf(
        "Espresso" to Pair(80, 40),
        "Marroqui" to Pair(70, 60),
        "Aguamiles" to Pair(60, 150),
        "Americano" to Pair(80, 150),
        "Capuchino" to Pair(80, 180),
        "Latte" to Pair(80, 250),
        "Macchiato" to Pair(70, 50),
        "Moca" to Pair(90, 250),
        "Vienés" to Pair(60, 100),
        "Irlandés" to Pair(60, 150),
        "Corretto" to Pair(70, 50),
        "Frappuccino" to Pair(60, 300)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
            if (coffee != null) {
                AsyncImage(
                    model = coffee.imageUrl.ifBlank { null },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = rememberVectorPainter(Icons.Default.Coffee)
                )
            } else {
                Box(Modifier.fillMaxSize().background(CoffeeBrown.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Coffee, null, Modifier.size(80.dp), tint = CoffeeBrown)
                }
            }
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 0f
                    ))
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, end = 24.dp, bottom = 60.dp)
            ) {
                Text(text = coffee?.marca ?: "Registro rápido", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                Text(
                    text = coffee?.nombre ?: "Selecciona tu preparación", 
                    color = Color.White, 
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.Bold, 
                    maxLines = 2, 
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { Spacer(Modifier.height(320.dp)) }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .padding(top = 32.dp)
                ) {
                    if (isFromPantry) {
                        Text("Cantidad de café", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("${grams.roundToInt()} g", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = CoffeeBrown, modifier = Modifier.padding(horizontal = 24.dp))
                        Slider(
                            value = grams,
                            onValueChange = { grams = it },
                            valueRange = 5f..50f,
                            steps = 44,
                            colors = SliderDefaults.colors(thumbColor = CoffeeBrown, activeTrackColor = CoffeeBrown),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                        )
                        Spacer(Modifier.height(32.dp))
                    }

                    Text("Tipo de preparación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(Modifier.height(16.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.height(800.dp).padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        userScrollEnabled = false
                    ) {
                        items(prepTypes) { (type, resName, _) ->
                            val isSelected = selectedPrepType == type
                            val imageRes = remember(resName) {
                                context.resources.getIdentifier(resName, "drawable", context.packageName)
                            }
                            
                            Surface(
                                onClick = { selectedPrepType = type },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) CoffeeBrown.copy(alpha = 0.08f) else Color(0xFFFBFBFB),
                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) CoffeeBrown else Color.LightGray.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (imageRes != 0) {
                                        Image(
                                            painter = painterResource(id = imageRes),
                                            contentDescription = type,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    } else {
                                        Icon(Icons.Default.Coffee, null, tint = CoffeeBrown, modifier = Modifier.size(48.dp))
                                    }
                                    
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        type, 
                                        color = Color.Black, 
                                        fontSize = 12.sp, 
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    val calculatedCaffeine = CaffeineCalculator.calculate(
                        selectedPrepType, 
                        if (isFromPantry) grams.roundToInt() else null, 
                        isFromPantry
                    )
                    
                    val config = prepConfig[selectedPrepType] ?: Pair(80, 150)
                    
                    Button(
                        onClick = { 
                            val finalName = if (coffee != null) "${coffee.nombre} ($selectedPrepType)" else selectedPrepType
                            onRegister(finalName, calculatedCaffeine, config.second, grams.roundToInt(), selectedPrepType) 
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("AÑADIR AL DIARIO", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 14.sp)
                            Text("Estimado: $calculatedCaffeine mg cafeína", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
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
            Surface(color = Color.Black.copy(alpha = 0.3f), shape = CircleShape, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun WaterRegistrationStep(onRegister: (Int) -> Unit) {
    var ml by remember { mutableFloatStateOf(250f) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.WaterDrop,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = Color(0xFF2196F3)
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = "${ml.roundToInt()} ml",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = Color(0xFF2196F3)
        )
        Spacer(Modifier.height(48.dp))
        Slider(
            value = ml,
            onValueChange = { ml = it },
            valueRange = 50f..1000f,
            steps = 18,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF2196F3),
                activeTrackColor = Color(0xFF2196F3)
            )
        )
        Spacer(Modifier.height(64.dp))
        Button(
            onClick = { onRegister(ml.roundToInt()) },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("REGISTRAR AGUA", fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
    }
}

@Composable
fun PantrySuggestionCard(item: PantryItemWithDetails, onClick: () -> Unit) {
    val progress = if (item.pantryItem.totalGrams > 0) {
        item.pantryItem.gramsRemaining.toFloat() / item.pantryItem.totalGrams
    } else 0f
    
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                AsyncImage(
                    model = item.coffee.imageUrl.ifBlank { null },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = rememberVectorPainter(Icons.Default.Coffee)
                )
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 200f
                        ))
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = item.coffee.marca,
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = item.coffee.nombre,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "${item.pantryItem.gramsRemaining}g / ${item.pantryItem.totalGrams}g",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = CoffeeBrown,
                    trackColor = Color(0xFFF0F0F0)
                )
            }
        }
    }
}

@Composable
fun CoffeeRowItem(coffee: CoffeeWithDetails, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(0.5.dp, Color(0xFFEEEEEE))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = coffee.coffee.imageUrl.ifBlank { null }, 
                contentDescription = null, 
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(Color.White), 
                contentScale = ContentScale.Crop,
                error = rememberVectorPainter(Icons.Default.Coffee)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = coffee.coffee.nombre, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 15.sp, 
                    maxLines = 2, 
                    overflow = TextOverflow.Ellipsis
                )
                Text(coffee.coffee.marca, color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}
