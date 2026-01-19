package com.example.cafesito.ui.diary

import android.util.Log
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.data.DiaryEntryEntity
import com.example.cafesito.data.PantryItemWithDetails
import com.example.cafesito.ui.theme.CoffeeBrown
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onCoffeeClick: (String) -> Unit,
    onAddWaterClick: () -> Unit,
    onAddCoffeeClick: () -> Unit,
    onAddStockClick: () -> Unit,
    onEditStockClick: (String, Boolean) -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val entries by viewModel.diaryEntries.collectAsState(initial = emptyList())
    val pantryItems by viewModel.pantryItems.collectAsState(initial = emptyList())
    val analytics by viewModel.analytics.collectAsState(initial = null)
    val selectedPeriod by viewModel.selectedPeriod.collectAsState(initial = DiaryPeriod.HOY)
    
    var showStockSheet by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<PantryItemWithDetails?>(null) }

    if (showStockSheet && itemToEdit != null) {
        StockEditBottomSheet(
            item = itemToEdit!!,
            onDismiss = { showStockSheet = false },
            onSave = { total, remaining ->
                viewModel.updateStock(itemToEdit!!.coffee.id, total, remaining)
                showStockSheet = false
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFFF8F8F8),
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mi Diario", 
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.Black
                        )
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.2.dp, CoffeeBrown, RoundedCornerShape(8.dp))
                                .clickable { /* showPeriodMenu logic */ }
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
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8F8F8),
                    scrolledContainerColor = Color(0xFFF8F8F8)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp) 
        ) {
            item {
                analytics?.let { CaffeineAnalyticsCard(it) }
                Spacer(Modifier.height(32.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Actividad", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold, 
                        color = Color.Black
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = onAddWaterClick,
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFE3F2FD), contentColor = Color(0xFF2196F3)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Default.WaterDrop, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Agua", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = onAddCoffeeClick,
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = CoffeeBrown.copy(alpha = 0.1f), contentColor = CoffeeBrown),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Default.Coffee, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Café", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (entries.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No hay registros hoy", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                items(entries, key = { "${it.id}_${it.timestamp}" }) { entry ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SwipeableDiaryItem(
                            entry = entry,
                            onDelete = { viewModel.deleteEntry(entry.id) }
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(32.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PantrySection(
                        items = pantryItems,
                        onAddClick = { onAddStockClick() },
                        onItemClick = onCoffeeClick,
                        onRemoveItem = { id -> viewModel.removeFromPantry(id) },
                        onUpdateGrams = { id, grams -> viewModel.addToPantry(id, grams) },
                        onEditClick = { id, isCustom ->
                            if (isCustom) {
                                onEditStockClick(id, true)
                            } else {
                                itemToEdit = pantryItems.find { it.coffee.id == id }
                                showStockSheet = true
                            }
                        }
                    )
                }
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockEditBottomSheet(
    item: PantryItemWithDetails,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit
) {
    var totalGrams by remember { mutableFloatStateOf(item.pantryItem.totalGrams.toFloat()) }
    var remainingGrams by remember { mutableFloatStateOf(item.pantryItem.gramsRemaining.toFloat()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Gestionar Stock", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(32.dp))

            StockSliderSection("Total de la bolsa", totalGrams, 1000f) { 
                totalGrams = it
                if (remainingGrams > it) remainingGrams = it
            }
            
            Spacer(Modifier.height(24.dp))

            StockSliderSection("Gramos restantes", remainingGrams, totalGrams) { 
                remainingGrams = it 
            }

            Spacer(Modifier.height(40.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Text("CANCELAR", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onSave(totalGrams.roundToInt(), remainingGrams.roundToInt()) },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("GUARDAR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StockSliderSection(label: String, value: Float, maxValue: Float = 1000f, onValueChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Text(
            text = "${value.roundToInt()} g", 
            style = MaterialTheme.typography.headlineSmall, 
            fontWeight = FontWeight.Black, 
            color = CoffeeBrown
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..maxValue.coerceAtLeast(1f),
            colors = SliderDefaults.colors(thumbColor = CoffeeBrown, activeTrackColor = CoffeeBrown)
        )
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
                Modifier.fillMaxSize().background(Color.Red.copy(alpha = 0.8f)),
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
fun DiaryEntryItem(entry: DiaryEntryEntity) {
    val dateStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.timestamp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        border = BorderStroke(0.2.dp, Color(0xFFEEEEEE))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(
                    if (entry.type == "WATER") Color(0xFFE3F2FD) else CoffeeBrown.copy(alpha = 0.1f), 
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (entry.type == "WATER") Icons.Default.WaterDrop 
                                 else Icons.Default.Coffee, 
                    contentDescription = null, 
                    tint = if (entry.type == "WATER") Color(0xFF2196F3) else CoffeeBrown, 
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.coffeeName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "$dateStr • ${if (entry.type == "WATER") "${entry.amountMl}ml" else "${entry.coffeeGrams}g - ${entry.preparationType}"}", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = Color.Gray
                )
            }
        }
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
            elevation = CardDefaults.cardElevation(0.dp),
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
                    
                    val compColor = if (analytics.comparisonPercentage > 0) Color(0xFFE57373) else Color(0xFF81C784)
                    Surface(color = compColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Icon(if (analytics.comparisonPercentage > 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown, null, tint = compColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${if (analytics.comparisonPercentage > 0) "+" else ""}${analytics.comparisonPercentage}%", color = compColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val scrollState = rememberScrollState()
                    val density = LocalDensity.current
                    val screenWidthPx = with(density) { maxWidth.toPx() }
                    val itemWidthPx = with(density) { (32 + 16).dp.toPx() } 

                    LaunchedEffect(analytics.chartData, analytics.period) {
                        val calendar = Calendar.getInstance()
                        val currentIndex = when (analytics.period) {
                            DiaryPeriod.HOY -> calendar.get(Calendar.HOUR_OF_DAY)
                            DiaryPeriod.SEMANA -> {
                                val day = calendar.get(Calendar.DAY_OF_WEEK)
                                if (day == Calendar.SUNDAY) 6 else day - 2
                            }
                            DiaryPeriod.MES -> calendar.get(Calendar.DAY_OF_MONTH) - 1
                        }
                        
                        val targetScroll = (currentIndex * itemWidthPx) - (screenWidthPx / 2) + (itemWidthPx / 2)
                        scrollState.animateScrollTo(targetScroll.roundToInt().coerceIn(0, scrollState.maxValue))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().height(100.dp).horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        analytics.chartData.forEach { (label, amount) ->
                            val barHeight = (amount.toFloat() / 500f).coerceIn(0.05f, 1f)
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .fillMaxHeight(barHeight)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(if (amount > 400) Color.Red.copy(alpha = 0.6f) else CoffeeBrown.copy(alpha = 0.8f))
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(label, fontSize = 9.sp, color = Color.Gray)
                            }
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
                        value = "${analytics.averageCaffeine}",
                        icon = Icons.Default.BarChart,
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
fun MetricBox(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xFFFBFBFB),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.Black)
            }
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun PantrySection(
    items: List<PantryItemWithDetails>,
    onAddClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onUpdateGrams: (String, Int) -> Unit,
    onEditClick: (String, Boolean) -> Unit
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
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFEEEEEE))) {
                Text("Registra tu café para controlar el stock.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items, key = { it.coffee.id }) { item ->
                    PantryCard(
                        item = item, 
                        onClick = { onItemClick(item.coffee.id) }, 
                        onRemove = { onRemoveItem(item.coffee.id) },
                        onUpdateGrams = { grams -> onUpdateGrams(item.coffee.id, grams) },
                        onEditClick = onEditClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryCard(
    item: PantryItemWithDetails, 
    onClick: () -> Unit, 
    onRemove: () -> Unit, 
    onUpdateGrams: (Int) -> Unit,
    onEditClick: (String, Boolean) -> Unit
) {
    val progress = if (item.pantryItem.totalGrams > 0) {
        item.pantryItem.gramsRemaining.toFloat() / item.pantryItem.totalGrams
    } else 0f
    
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showDeleteSheet by remember { mutableStateOf(false) }

    // MODAL DE OPCIONES (Editar / Eliminar)
    if (showOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet = false },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                ListItem(
                    headlineContent = { Text("Editar información") },
                    leadingContent = { Icon(Icons.Default.Edit, null) },
                    modifier = Modifier.clickable { 
                        showOptionsSheet = false
                        onEditClick(item.coffee.id, item.isCustom) 
                    }
                )
                ListItem(
                    headlineContent = { Text("Eliminar de la despensa", color = Color.Red) },
                    leadingContent = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                    modifier = Modifier.clickable { 
                        showOptionsSheet = false
                        showDeleteSheet = true 
                    }
                )
            }
        }
    }

    // MODAL DE CONFIRMACIÓN DE BORRADO
    if (showDeleteSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDeleteSheet = false },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("¿Eliminar de la despensa?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Text("Se borrará este café de tu stock. Podrás añadirlo de nuevo más tarde.", textAlign = TextAlign.Center, color = Color.Gray)
                Spacer(Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { showDeleteSheet = false },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Text("CANCELAR", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onRemove(); showDeleteSheet = false },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("ELIMINAR", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.pantryItem.gramsRemaining}g / ${item.pantryItem.totalGrams}g",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Box {
                        IconButton(onClick = { showOptionsSheet = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
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
