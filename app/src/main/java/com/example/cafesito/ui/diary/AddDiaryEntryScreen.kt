package com.example.cafesito.ui.diary

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.data.PantryItemWithDetails
import com.example.cafesito.ui.theme.CoffeeBrown
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDiaryEntryScreen(
    initialType: String,
    onBackClick: () -> Unit,
    onAddPantryItemClick: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val pantryItems by viewModel.pantryItems.collectAsState(initial = emptyList())
    val allCoffees by viewModel.availableCoffees.collectAsState(initial = emptyList())
    
    var selectedCoffee by remember { mutableStateOf<CoffeeWithDetails?>(null) }
    var step by remember { mutableIntStateOf(1) } 
    var waterAmountMl by remember { mutableFloatStateOf(250f) }

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (initialType == "WATER") "Hidratación" 
                               else if (step == 1) "Seleccionar Café" 
                               else "Personalizar Taza",
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { if (step == 2) step = 1 else onBackClick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (initialType == "WATER") {
                WaterRegistrationView(
                    amount = waterAmountMl,
                    onAmountChange = { waterAmountMl = it },
                    onRegister = {
                        viewModel.addWaterConsumption(waterAmountMl.toInt())
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
                            onAddPantryClick = onAddPantryItemClick,
                            onCoffeeSelected = { 
                                selectedCoffee = it
                                step = 2 
                            }
                        )
                    } else {
                        selectedCoffee?.let { coffee ->
                            CoffeeCustomizationStep(
                                coffee = coffee,
                                onRegister = { name, caffeine, _ ->
                                    viewModel.addCoffeeConsumption(coffee.coffee.id, name, caffeine)
                                    onBackClick()
                                }
                            )
                        }
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
    onAddPantryClick: () -> Unit,
    onCoffeeSelected: (CoffeeWithDetails) -> Unit
) {
    val filteredSugerencias = allCoffees.filter { 
        it.coffee.nombre.contains(searchQuery, ignoreCase = true) || 
        it.coffee.marca.contains(searchQuery, ignoreCase = true)
    }.take(10)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text("Tu Despensa", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Surface(
                        onClick = onAddPantryClick,
                        modifier = Modifier.size(160.dp, 180.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF8F8F8),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = CoffeeBrown, modifier = Modifier.size(40.dp))
                        }
                    }
                }
                items(pantryItems) { item ->
                    PantrySuggestionCard(item) { 
                        onCoffeeSelected(CoffeeWithDetails(item.coffee, null, emptyList())) 
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
                Text("Sugerencias", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onSearchToggle) {
                    Icon(if (isSearchActive) Icons.Default.Close else Icons.Default.Search, contentDescription = null)
                }
            }
            
            if (isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    placeholder = { Text("Busca cualquier café...") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CoffeeBrown)
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        items(filteredSugerencias) { coffee ->
            CoffeeRowItem(coffee) { onCoffeeSelected(coffee) }
            HorizontalDivider(modifier = Modifier.padding(start = 64.dp), thickness = 0.5.dp, color = Color(0xFFEEEEEE))
        }
    }
}

@Composable
fun CoffeeCustomizationStep(
    coffee: CoffeeWithDetails,
    onRegister: (String, Int, Int) -> Unit
) {
    var selectedType by remember { mutableStateOf("Espresso") }
    val coffeeTypes = listOf(
        Triple("Espresso", 80, 40),
        Triple("Café con leche", 80, 200),
        Triple("Mocca", 90, 250),
        Triple("Americano", 80, 150),
        Triple("Capuchino", 80, 180)
    )

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = coffee.coffee.imageUrl,
            contentDescription = null,
            modifier = Modifier.size(120.dp).clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
        Text(text = coffee.coffee.nombre, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        Text(text = coffee.coffee.marca, color = Color.Gray)

        Spacer(Modifier.height(32.dp))
        Text("¿Cómo lo has preparado?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        
        Spacer(Modifier.height(16.dp))
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(coffeeTypes) { (type, caffeine, ml) ->
                val isSelected = selectedType == type
                Surface(
                    onClick = { selectedType = type },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) CoffeeBrown else Color(0xFFF5F5F5),
                    modifier = Modifier.width(130.dp)
                ) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (type == "Espresso") Icons.Default.Coffee else Icons.Default.LocalCafe,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else Color.Black
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(type, color = if (isSelected) Color.White else Color.Black, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        val currentData = coffeeTypes.find { it.first == selectedType }!!
        Button(
            onClick = { onRegister("${coffee.coffee.nombre} ($selectedType)", currentData.second, currentData.third) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("REGISTRAR TAZA", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun WaterRegistrationView(
    amount: Float,
    onAmountChange: (Float) -> Unit,
    onRegister: () -> Unit
) {
    val animatedFill = animateFloatAsState(targetValue = amount / 2000f, label = "waterFill")

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(10.dp))
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.width(40.dp).height(15.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF1976D2)))
            Box(Modifier.width(55.dp).height(30.dp).background(Color(0xFFF0F7FF)).border(2.dp, Color(0xFFE1E9F3)))
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(260.dp)
                    .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
                    .background(Color(0xFFF0F7FF))
                    .border(2.dp, Color(0xFFE1E9F3), RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(animatedFill.value)
                        .background(Brush.verticalGradient(listOf(Color(0xFF64B5F6), Color(0xFF2196F3))))
                )
                Box(
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp).width(8.dp).fillMaxHeight(0.8f).clip(CircleShape).background(Color.White.copy(alpha = 0.3f))
                )
            }
        }

        Spacer(Modifier.height(30.dp))

        Text(text = "${amount.roundToInt()} ml", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = Color(0xFF1976D2))
        
        Text(text = if (amount >= 1000) String.format("%.1f Litros", amount/1000f) else "Volumen de hidratación", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)

        Spacer(Modifier.weight(1f))

        Slider(
            value = amount,
            onValueChange = onAmountChange,
            valueRange = 0f..2000f,
            steps = 19,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF2196F3), activeTrackColor = Color(0xFF2196F3), inactiveTrackColor = Color(0xFFE3F2FD))
        )
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("0ml", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text("2L", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onRegister,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            Text("REGISTRAR", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
        }
    }
}

@Composable
fun PantrySuggestionCard(item: PantryItemWithDetails, onClick: () -> Unit) {
    val progress = item.pantryItem.gramsRemaining.toFloat() / item.pantryItem.totalGrams
    Card(
        modifier = Modifier.size(160.dp, 180.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            AsyncImage(
                model = item.coffee.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)).align(Alignment.CenterHorizontally),
                contentScale = ContentScale.Crop
            )
            Column {
                Text(text = item.coffee.nombre, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
                Text("${item.pantryItem.gramsRemaining}g restantes", fontSize = 11.sp, color = Color.Gray)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = if (progress < 0.2f) Color.Red else CoffeeBrown,
                trackColor = Color(0xFFF0F0F0)
            )
        }
    }
}

@Composable
fun CoffeeRowItem(coffee: CoffeeWithDetails, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = coffee.coffee.imageUrl, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(coffee.coffee.nombre, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(coffee.coffee.marca, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun HorizontalDivider(modifier: Modifier = Modifier, thickness: androidx.compose.ui.unit.Dp = 1.dp, color: Color = Color.Black) {
    androidx.compose.material3.HorizontalDivider(modifier = modifier, thickness = thickness, color = color)
}
