package com.example.cafesito.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.data.DiaryEntryEntity
import com.example.cafesito.data.PantryItemWithDetails
import com.example.cafesito.ui.theme.CoffeeBrown
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onCoffeeClick: (String) -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val entries by viewModel.diaryEntries.collectAsState()
    val pantryItems by viewModel.pantryItems.collectAsState()
    val availableCoffees by viewModel.availableCoffees.collectAsState()
    val analytics by viewModel.analytics.collectAsState()
    
    var showAddPantryDialog by remember { mutableStateOf(false) }
    var showAddEntryDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mi Diario", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F8F8)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // DASHBOARD ANALÍTICO
            item {
                analytics?.let { CaffeineAnalyticsCard(it) }
            }

            // MI DESPENSA (CON PROGRESO)
            item {
                PantrySection(
                    items = pantryItems,
                    onAddClick = { showAddPantryDialog = true },
                    onItemClick = onCoffeeClick,
                    onRemoveItem = { viewModel.removeFromPantry(it) }
                )
            }

            // CONSUMO DE HOY
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Actividad de hoy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { showAddEntryDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Registrar")
                    }
                }
            }

            if (entries.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No has registrado nada hoy", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                items(entries) { entry ->
                    DiaryEntryItem(entry, onDelete = { viewModel.deleteEntry(entry.id) })
                }
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }

        if (showAddPantryDialog) {
            AddPantryDialog(
                availableCoffees = availableCoffees,
                onDismiss = { showAddPantryDialog = false },
                onConfirm = { coffeeId, grams ->
                    viewModel.addToPantry(coffeeId, grams)
                    showAddPantryDialog = false
                }
            )
        }

        if (showAddEntryDialog) {
            AddEntryDialog(
                pantryItems = pantryItems,
                onDismiss = { showAddEntryDialog = false },
                onAddCoffee = { coffeeId, name, caffeine ->
                    viewModel.addCoffeeConsumption(coffeeId, name, caffeine)
                    showAddEntryDialog = false
                },
                onAddWater = {
                    viewModel.addWaterConsumption()
                    showAddEntryDialog = false
                }
            )
        }
    }
}

@Composable
fun CaffeineAnalyticsCard(analytics: DiaryAnalytics) {
    val limit = 400
    val progress = (analytics.totalCaffeineToday.toFloat() / limit).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Resumen Semanal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Consumo de cafeína (mg)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(48.dp),
                        color = if (analytics.totalCaffeineToday > limit) Color.Red else CoffeeBrown,
                        strokeWidth = 4.dp
                    )
                    Text("${(progress * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            // Gráfico de barras semanal simplificado
            Row(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                analytics.weeklyCaffeine.forEach { (day, amount) ->
                    val barHeight = (amount.toFloat() / 600f).coerceIn(0.05f, 1f)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .fillMaxHeight(barHeight)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (amount > limit) Color.Red else CoffeeBrown.copy(alpha = 0.6f))
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(day, fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(alpha = 0.1f)
            Spacer(Modifier.height(16.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AnalyticStat("Hoy", "${analytics.totalCaffeineToday} mg", if (analytics.totalCaffeineToday > limit) Color.Red else CoffeeBrown)
                AnalyticStat("Agua", "${analytics.waterCount} vasos", Color(0xFF2196F3))
                AnalyticStat("Estado", if (analytics.totalCaffeineToday > limit) "Excedido" else "Óptimo", if (analytics.totalCaffeineToday > limit) Color.Red else Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
fun AnalyticStat(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun PantrySection(
    items: List<PantryItemWithDetails>,
    onAddClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onRemoveItem: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mi Despensa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = onAddClick) {
                Text("+ Añadir stock", color = CoffeeBrown, fontWeight = FontWeight.Bold)
            }
        }
        
        if (items.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Text(
                    "Registra el café que tienes en casa para que Cafesito te avise cuando se esté agotando.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items) { item ->
                    PantryCard(item, onClick = { onItemClick(item.coffee.id) }, onRemove = { onRemoveItem(item.coffee.id) })
                }
            }
        }
    }
}

@Composable
fun PantryCard(item: PantryItemWithDetails, onClick: () -> Unit, onRemove: () -> Unit) {
    val progress = item.pantryItem.gramsRemaining.toFloat() / item.pantryItem.totalGrams
    val color = when {
        progress < 0.15f -> Color.Red
        progress < 0.4f -> Color(0xFFFFA000)
        else -> CoffeeBrown
    }
    
    Card(
        modifier = Modifier.width(150.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = item.coffee.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(item.coffee.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("${item.pantryItem.gramsRemaining}g", fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = color,
                trackColor = Color(0xFFF0F0F0)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (progress < 0.2f) "¡Reponer pronto!" else "En stock",
                fontSize = 9.sp,
                color = if (progress < 0.2f) Color.Red else Color.Gray,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun DiaryEntryItem(entry: DiaryEntryEntity, onDelete: () -> Unit) {
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.timestamp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(
                    if (entry.type == "WATER") Color(0xFFE3F2FD) else CoffeeBrown.copy(alpha = 0.1f),
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (entry.type == "WATER") Icons.Default.WaterDrop else Icons.Default.Coffee,
                    contentDescription = null,
                    tint = if (entry.type == "WATER") Color(0xFF2196F3) else CoffeeBrown,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.coffeeName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("$time • ${if (entry.caffeineAmount > 0) "${entry.caffeineAmount}mg cafeína" else "Hidratación"}", 
                    style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun AddPantryDialog(
    availableCoffees: List<CoffeeWithDetails>,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var grams by remember { mutableStateOf("250") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir a mi Despensa", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Selecciona el café que tienes en casa:", style = MaterialTheme.typography.labelMedium)
                LazyColumn(Modifier.height(250.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))) {
                    items(availableCoffees) { item ->
                        Row(
                            Modifier.fillMaxWidth().clickable { selectedId = item.coffee.id }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedId == item.coffee.id, onClick = { selectedId = item.coffee.id })
                            Spacer(Modifier.width(8.dp))
                            Text(item.coffee.nombre, fontSize = 14.sp)
                        }
                    }
                }
                OutlinedTextField(
                    value = grams,
                    onValueChange = { grams = it },
                    label = { Text("Cantidad de la bolsa (gramos)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedId?.let { onConfirm(it, grams.toIntOrNull() ?: 250) } },
                enabled = selectedId != null && grams.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Añadir stock") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun AddEntryDialog(
    pantryItems: List<PantryItemWithDetails>,
    onDismiss: () -> Unit,
    onAddCoffee: (String?, String, Int) -> Unit,
    onAddWater: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Qué has tomado ahora?", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (pantryItems.isNotEmpty()) {
                    Text("Usa tu despensa:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    pantryItems.forEach { item ->
                        Surface(
                            onClick = { onAddCoffee(item.coffee.id, item.coffee.nombre, 80) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF5F5F5)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Coffee, null, tint = CoffeeBrown, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(item.coffee.nombre, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), alpha = 0.1f)
                }
                
                Surface(
                    onClick = onAddWater,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE3F2FD)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WaterDrop, null, tint = Color(0xFF2196F3), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Vaso de Agua (250ml)", color = Color(0xFF1976D2), fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun HorizontalDivider(modifier: Modifier = Modifier, alpha: Float = 0.2f) {
    androidx.compose.material3.HorizontalDivider(modifier = modifier, thickness = 1.dp, color = Color.Black.copy(alpha = alpha))
}
