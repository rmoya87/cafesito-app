package com.example.cafesito.ui.diary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cafesito.ui.theme.CoffeeBrown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStockScreen(
    onBackClick: () -> Unit,
    onAddCustomClick: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val coffees by viewModel.availableCoffees.collectAsState()
    
    // Sugerencias: 10 cafés (favoritos primero)
    val suggestions = remember(coffees) { 
        (coffees.filter { it.isFavorite } + coffees.filter { !it.isFavorite }).take(10)
    }
    
    var selectedCoffeeId by remember { mutableStateOf<String?>(null) }
    var grams by remember { mutableStateOf("250") }

    if (selectedCoffeeId != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedCoffeeId = null },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Añadir Stock", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Introduce el peso total de la bolsa", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = grams,
                    onValueChange = { if (it.all { c -> c.isDigit() }) grams = it },
                    label = { Text("Gramos (g)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                
                Spacer(Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { selectedCoffeeId = null },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Text("CANCELAR", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            val g = grams.toIntOrNull() ?: 250
                            viewModel.addToPantry(selectedCoffeeId!!, g)
                            selectedCoffeeId = null
                            onBackClick()
                        }, 
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("AÑADIR", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Sugerencias", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") 
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    }
                    TextButton(onClick = onAddCustomClick) {
                        Text("+ otro café", color = CoffeeBrown, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF9F5F2))
            )
        },
        containerColor = Color(0xFFF9F5F2)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            items(suggestions) { coffeeDetails ->
                CoffeeRowItem(coffeeDetails) { selectedCoffeeId = coffeeDetails.coffee.id }
            }
        }
    }
}
