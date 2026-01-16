package com.example.cafesito.ui.diary

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.data.PantryItemWithDetails
import com.example.cafesito.ui.theme.CoffeeBrown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDiaryEntryScreen(
    initialType: String,
    onBackClick: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val pantryItems by viewModel.pantryItems.collectAsState()
    val allCoffees by viewModel.availableCoffees.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredCoffees = allCoffees.filter { 
        it.coffee.nombre.contains(searchQuery, ignoreCase = true) || 
        it.coffee.marca.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (initialType == "WATER") "Registrar Agua" else "Registrar Café",
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (initialType == "WATER") {
                item {
                    Text("Hidratación", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    WaterRegistrationCard(onAdd = { 
                        viewModel.addWaterConsumption()
                        onBackClick()
                    })
                }
            } else {
                // SECCIÓN DESPENSA (Solo si elegimos Café)
                if (pantryItems.isNotEmpty()) {
                    item {
                        Text("De tu despensa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(pantryItems) { item ->
                                PantrySuggestionCard(item) {
                                    viewModel.addCoffeeConsumption(item.coffee.id, item.coffee.nombre, 80)
                                    onBackClick()
                                }
                            }
                        }
                    }
                }

                // BUSCADOR VISUAL DE OTROS CAFÉS
                item {
                    Text("Explorar cafés", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Busca una marca o nombre...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CoffeeBrown)
                    )
                }

                items(filteredCoffees.chunked(2)) { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { coffee ->
                            CoffeeGridItem(coffee, Modifier.weight(1f)) {
                                viewModel.addCoffeeConsumption(coffee.coffee.id, coffee.coffee.nombre, 80)
                                onBackClick()
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }

                item {
                    Button(
                        onClick = { /* Lógica para añadir manual */ },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.EditNote, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Añadir café personalizado")
                    }
                }
            }
        }
    }
}

@Composable
fun WaterRegistrationCard(onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(160.dp).clickable(onClick = onAdd),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF2196F3), Color(0xFF64B5F6)))
        )) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("Registrar Vaso de Agua", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("250 ml", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun PantrySuggestionCard(item: PantryItemWithDetails, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(130.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = item.coffee.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(50.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(8.dp))
            Text(item.coffee.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("${item.pantryItem.gramsRemaining}g", fontSize = 10.sp, color = CoffeeBrown)
        }
    }
}

@Composable
fun CoffeeGridItem(coffee: CoffeeWithDetails, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = coffee.coffee.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(coffee.coffee.nombre, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(coffee.coffee.marca, fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}
