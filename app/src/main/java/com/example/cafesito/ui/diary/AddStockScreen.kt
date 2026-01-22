package com.example.cafesito.ui.diary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
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
import com.example.cafesito.ui.components.*
import com.example.cafesito.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStockScreen(
    onBackClick: () -> Unit,
    onAddCustomClick: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val coffees by viewModel.availableCoffees.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredSuggestions = remember(coffees, searchQuery) {
        // Filtrar solo cafés oficiales (no personalizados) y por búsqueda
        coffees.filter { 
            !it.coffee.isCustom && (
                it.coffee.nombre.contains(searchQuery, ignoreCase = true) || 
                it.coffee.marca.contains(searchQuery, ignoreCase = true)
            )
        }.sortedByDescending { it.isFavorite }
    }
    
    var selectedCoffeeId by remember { mutableStateOf<String?>(null) }
    var grams by remember { mutableStateOf("250") }

    if (selectedCoffeeId != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedCoffeeId = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("AÑADIR STOCK", style = MaterialTheme.typography.labelLarge, color = CaramelAccent)
                Spacer(Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = grams,
                    onValueChange = { if (it.all { c -> c.isDigit() }) grams = it },
                    label = { Text("Peso total (g)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CaramelAccent)
                )
                
                Spacer(Modifier.height(32.dp))
                
                Button(
                    onClick = {
                        val g = grams.toIntOrNull() ?: 250
                        viewModel.addToPantry(selectedCoffeeId!!, g)
                        selectedCoffeeId = null
                        onBackClick()
                    }, 
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EspressoDeep),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("GUARDAR EN DESPENSA", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Scaffold(
        containerColor = SoftOffWhite,
        topBar = {
            GlassyTopBar(
                title = "SUGERENCIAS",
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = onAddCustomClick) {
                        Icon(Icons.Default.Bolt, contentDescription = "Añadir rápido", tint = EspressoDeep)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Busca un café o marca") }, // Cambiado según solicitud
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CaramelAccent),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) }
                )
                Spacer(Modifier.height(8.dp))
            }

            items(filteredSuggestions) { coffeeDetails ->
                CoffeePremiumRowItem(coffeeDetails) { 
                    selectedCoffeeId = coffeeDetails.coffee.id 
                }
            }
        }
    }
}
