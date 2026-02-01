package com.cafesito.app.ui.diary

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
import com.cafesito.app.ui.components.*
import com.cafesito.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStockScreen(
    onBackClick: () -> Unit,
    onAddCustomClick: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val coffees by viewModel.availableCoffees.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredSuggestions = remember(coffees, searchQuery) {
        coffees.filter { 
            !it.coffee.isCustom && (
                it.coffee.nombre.contains(searchQuery, ignoreCase = true) || 
                it.coffee.marca.contains(searchQuery, ignoreCase = true)
            )
        }.sortedByDescending { it.isFavorite }
    }
    
    var selectedCoffeeId by remember { mutableStateOf<String?>(null) }
    var grams by remember { mutableStateOf("250") }
    var isSaving by remember { mutableStateOf(false) }

    if (selectedCoffeeId != null) {
        ModalBottomSheet(
            onDismissRequest = { if (!isSaving) selectedCoffeeId = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("AÑADIR STOCK", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = grams,
                    onValueChange = { if (it.all { c -> c.isDigit() }) grams = it },
                    label = { Text("Peso total (g)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !isSaving,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )
                
                Spacer(Modifier.height(32.dp))
                
                Button(
                    onClick = {
                        val g = grams.toIntOrNull() ?: 250
                        isSaving = true
                        viewModel.addToPantry(selectedCoffeeId!!, g) {
                            // Este callback se ejecuta SOLO tras el éxito en Supabase
                            isSaving = false
                            selectedCoffeeId = null
                            onSuccess()
                        }
                    }, 
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isSaving && grams.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("GUARDAR EN DESPENSA", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GlassyTopBar(
                title = "SUGERENCIAS",
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = onAddCustomClick) {
                        Icon(Icons.Default.Bolt, contentDescription = "Añadir rápido", tint = MaterialTheme.colorScheme.onSurface)
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
                    placeholder = { Text("Busca un café o marca") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
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
