package com.example.cafesito.ui.diary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
    onAddWaterClick: () -> Unit,
    onAddCoffeeClick: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val entries by viewModel.diaryEntries.collectAsState()
    val pantryItems by viewModel.pantryItems.collectAsState()
    val availableCoffees by viewModel.availableCoffees.collectAsState()
    val analytics by viewModel.analytics.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    
    var showAddPantryDialog by remember { mutableStateOf(false) }
    var showPeriodMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFFF8F8F8),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Mi Diario", 
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.Black
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8F8F8),
                    scrolledContainerColor = Color(0xFFF8F8F8)
                ),
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.2.dp, CoffeeBrown, RoundedCornerShape(8.dp))
                            .clickable { showPeriodMenu = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedPeriod.name.lowercase().replaceFirstChar { it.uppercase() },
                                color = CoffeeBrown,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = CoffeeBrown)
                        }
                        DropdownMenu(
                            expanded = showPeriodMenu,
                            onDismissRequest = { showPeriodMenu = false }
                        ) {
                            DiaryPeriod.entries.forEach { period ->
                                DropdownMenuItem(
                                    text = { Text(period.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        viewModel.setPeriod(period)
                                        showPeriodMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ESTADÍSTICAS
            item {
                analytics?.let { CaffeineAnalyticsCard(it) }
            }

            // ACCIONES DE REGISTRO (DOS BOTONES VISUALES)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RegistrationQuickCard(
                        title = "+ Agua",
                        subtitle = "Hidratación",
                        icon = Icons.Default.WaterDrop,
                        backgroundColor = Color(0xFFE3F2FD),
                        contentColor = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f),
                        onClick = onAddWaterClick
                    )
                    RegistrationQuickCard(
                        title = "+ Café",
                        subtitle = "Energía",
                        icon = Icons.Default.Coffee,
                        backgroundColor = CoffeeBrown.copy(alpha = 0.1f),
                        contentColor = CoffeeBrown,
                        modifier = Modifier.weight(1f),
                        onClick = onAddCoffeeClick
                    )
                }
            }

            // ACTIVIDAD
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Actividad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            if (entries.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No hay registros", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                items(entries, key = { it.id }) { entry ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SwipeableDiaryItem(
                            entry = entry,
                            onDelete = { viewModel.deleteEntry(entry.id) }
                        )
                    }
                }
            }

            // DESPENSA
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PantrySection(
                        items = pantryItems,
                        onAddClick = { showAddPantryDialog = true },
                        onItemClick = onCoffeeClick,
                        onRemoveItem = { viewModel.removeFromPantry(it) }
                    )
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
    }
}

@Composable
fun RegistrationQuickCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(85.dp),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.ExtraBold, color = contentColor, fontSize = 16.sp)
                Text(subtitle, color = contentColor.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableDiaryItem(
    entry: DiaryEntryEntity,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(Color.Red.copy(alpha = 0.8f)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.padding(end = 16.dp))
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        DiaryEntryItem(entry)
    }
}

@Composable
fun CaffeineAnalyticsCard(analytics: DiaryAnalytics) {
    val totalLabel = when(analytics.period) {
        DiaryPeriod.HOY -> "Hoy"
        DiaryPeriod.SEMANA -> "Semana"
        DiaryPeriod.MES -> "Mes"
    }

    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Consumo $totalLabel", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text(
                            text = "${analytics.totalCaffeine} mg",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                    
                    val isIncrease = analytics.comparisonPercentage > 0
                    val compColor = if (isIncrease) Color(0xFFE57373) else Color(0xFF81C784)
                    Surface(
                        color = compColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isIncrease) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = compColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${if (isIncrease) "+" else ""}${analytics.comparisonPercentage}%",
                                color = compColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    analytics.chartData.forEach { (label, amount) ->
                        val barHeight = (amount.toFloat() / 500f).coerceIn(0.05f, 1f)
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(12.dp)
                                    .fillMaxHeight(barHeight)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(if (amount > 400) Color.Red.copy(alpha = 0.6f) else CoffeeBrown.copy(alpha = 0.8f))
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(label, fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        label = "Media $totalLabel",
                        value = "${analytics.averageCaffeine} mg",
                        icon = Icons.Default.Equalizer,
                        color = Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "Tazas",
                        value = "${analytics.cupsCount}",
                        icon = Icons.Default.Coffee,
                        color = CoffeeBrown,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "Agua",
                        value = "${analytics.waterCount}",
                        icon = Icons.Default.WaterDrop,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun MetricBox(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xFFFBFBFB),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, fontSize = 9.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
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
            Text("Mi Despensa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
            TextButton(onClick = onAddClick) {
                Text("+ Añadir stock", color = CoffeeBrown, fontWeight = FontWeight.Bold)
            }
        }
        
        if (items.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Text("Registra tu café para controlar el stock.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
                AsyncImage(model = item.coffee.imageUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(item.coffee.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("${item.pantryItem.gramsRemaining}g", fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = color, trackColor = Color(0xFFF0F0F0))
        }
    }
}

@Composable
fun DiaryEntryItem(entry: com.example.cafesito.data.DiaryEntryEntity) {
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.timestamp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).background(if (entry.type == "WATER") Color(0xFFE3F2FD) else CoffeeBrown.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(if (entry.type == "WATER") Icons.Default.WaterDrop else Icons.Default.Coffee, contentDescription = null, tint = if (entry.type == "WATER") Color(0xFF2196F3) else CoffeeBrown, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.coffeeName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                Text("$time • ${if (entry.caffeineAmount > 0) "${entry.caffeineAmount}mg" else "Hidratación"}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun AddPantryDialog(availableCoffees: List<CoffeeWithDetails>, onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var grams by remember { mutableStateOf("250") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir a mi Despensa", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LazyColumn(Modifier.height(250.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))) {
                    items(availableCoffees) { item ->
                        Row(Modifier.fillMaxWidth().clickable { selectedId = item.coffee.id }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedId == item.coffee.id, onClick = { selectedId = item.coffee.id })
                            Spacer(Modifier.width(8.dp))
                            Text(item.coffee.nombre, fontSize = 14.sp)
                        }
                    }
                }
                OutlinedTextField(value = grams, onValueChange = { grams = it }, label = { Text("Cantidad (gramos)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = { Button(onClick = { selectedId?.let { onConfirm(it, grams.toIntOrNull() ?: 250) } }, enabled = selectedId != null && grams.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown)) { Text("Añadir") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
