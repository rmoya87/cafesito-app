package com.example.cafesito.ui.diary

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
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
    var showQuickActions by remember { mutableStateOf(false) }
    var showPeriodMenu by remember { mutableStateOf(false) }
    
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Actividad, 1: Despensa

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

    if (showQuickActions) {
        ModalBottomSheet(
            onDismissRequest = { showQuickActions = false },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp)) {
                Text(
                    text = "Añadir registro", 
                    modifier = Modifier.fillMaxWidth().padding(24.dp), 
                    style = MaterialTheme.typography.titleLarge, 
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )
                ListItem(
                    headlineContent = { Text("Registro de agua", fontWeight = FontWeight.Medium) },
                    leadingContent = { 
                        Box(Modifier.size(40.dp).background(Color(0xFFE3F2FD), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.WaterDrop, null, tint = Color(0xFF2196F3)) 
                        }
                    },
                    modifier = Modifier.clickable { 
                        showQuickActions = false
                        onAddWaterClick() 
                    }
                )
                ListItem(
                    headlineContent = { Text("Registro de café", fontWeight = FontWeight.Medium) },
                    leadingContent = { 
                        Box(Modifier.size(40.dp).background(CoffeeBrown.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Coffee, null, tint = CoffeeBrown) 
                        }
                    },
                    modifier = Modifier.clickable { 
                        showQuickActions = false
                        onAddCoffeeClick() 
                    }
                )
                ListItem(
                    headlineContent = { Text("Café a mi despensa", fontWeight = FontWeight.Medium) },
                    leadingContent = { 
                        Box(Modifier.size(40.dp).background(Color(0xFFF5F5F5), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Inventory, null, tint = Color.Black) 
                        }
                    },
                    modifier = Modifier.clickable { 
                        showQuickActions = false
                        onAddStockClick() 
                    }
                )
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8F8F8),
        topBar = {
            TopAppBar(
                title = { 
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Mi Diario", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
                            IconButton(
                                onClick = { showQuickActions = true },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = CoffeeBrown, contentColor = Color.White),
                                modifier = Modifier.size(36.dp)
                            ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp)) }
                            Spacer(Modifier.width(12.dp))
                            PeriodSelector(selectedPeriod) { showPeriodMenu = true }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F8F8))
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item {
                analytics?.let { CaffeineAnalyticsCard(it) }
                Spacer(Modifier.height(24.dp))
            }

            item {
                // PESTAÑAS (TABS)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(Color(0xFFEEEEEE), RoundedCornerShape(12.dp)).padding(4.dp)
                ) {
                    TabItem("Actividad", selectedTab == 0, Modifier.weight(1f)) { selectedTab = 0 }
                    TabItem("Despensa", selectedTab == 1, Modifier.weight(1f)) { selectedTab = 1 }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (selectedTab == 0) {
                if (entries.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Text("No hay registros todavía", color = Color.Gray)
                        }
                    }
                } else {
                    items(entries, key = { "${it.id}_${it.timestamp}" }) { entry ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) { // Espacio reducido entre actividades
                            SwipeableDiaryItem(entry = entry, onDelete = { viewModel.deleteEntry(entry.id) })
                        }
                    }
                }
            } else {
                item {
                    // DESPENSA EN DOS COLUMNAS
                    Box(modifier = Modifier.padding(horizontal = 16.dp).heightIn(max = 2000.dp)) {
                        PantrySection(
                            items = pantryItems,
                            onAddClick = { onAddStockClick() },
                            onItemClick = onCoffeeClick,
                            onRemoveItem = { id -> viewModel.removeFromPantry(id) },
                            onUpdateGrams = { id, grams -> viewModel.addToPantry(id, grams) },
                            onEditClick = { id, isCustom ->
                                if (isCustom) onEditStockClick(id, true)
                                else {
                                    itemToEdit = pantryItems.find { it.coffee.id == id }
                                    showStockSheet = true
                                }
                            }
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun TabItem(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bgColor by animateColorAsState(if (isSelected) Color.White else Color.Transparent)
    val textColor by animateColorAsState(if (isSelected) CoffeeBrown else Color.Gray)
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = textColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
fun CaffeineAnalyticsCard(analytics: DiaryAnalytics) {
    var showInfo by remember { mutableStateOf(false) }
    if (showInfo) InfoBottomSheet { showInfo = false }

    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFEEEEEE))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Cafeína estimada", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            IconButton(onClick = { showInfo = true }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Outlined.Info, null, tint = CoffeeBrown, modifier = Modifier.size(14.dp))
                            }
                        }
                        Text("${analytics.totalCaffeine} mg", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                        ComparisonLabel(analytics.comparisonPercentage)
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Agua", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text("${analytics.totalWaterMl} ml", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                        ComparisonLabel(-2, Color(0xFF2196F3)) 
                    }
                }

                Spacer(Modifier.height(24.dp))
                ChartSection(analytics)
                Spacer(Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricBox("Cafeína media", "${analytics.averageCaffeine} mg", Icons.Default.BarChart, Color.Gray, Modifier.weight(1f))
                    MetricBox("Tazas hoy", "${analytics.cupsCount}", Icons.Default.Coffee, CoffeeBrown, Modifier.weight(1f))
                    MetricBox("Progreso", "85%", Icons.Default.CheckCircle, Color(0xFF81C784), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ComparisonLabel(percentage: Int, baseColor: Color? = null) {
    val isPositive = percentage > 0
    val color = baseColor ?: if (isPositive) Color(0xFFE57373) else Color(0xFF81C784)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, null, tint = color, modifier = Modifier.size(12.dp))
        Text("${if (isPositive) "+" else ""}$percentage%", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ChartSection(analytics: DiaryAnalytics) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val itemWidthPx = with(density) { (44 + 12).dp.toPx() }

    LaunchedEffect(analytics.chartData) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(100.dp).horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        analytics.chartData.forEach { entry ->
            val caffeineHeight = (entry.caffeine.toFloat() / 400f).coerceIn(0.05f, 1f)
            val waterHeight = (entry.water.toFloat() / 2000f).coerceIn(0.05f, 1f)
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
                Row(modifier = Modifier.height(70.dp), verticalAlignment = Alignment.Bottom) {
                    Box(Modifier.width(8.dp).fillMaxHeight(caffeineHeight).clip(CircleShape).background(CoffeeBrown))
                    Spacer(Modifier.width(4.dp))
                    Box(Modifier.width(8.dp).fillMaxHeight(waterHeight).clip(CircleShape).background(Color(0xFF2196F3).copy(alpha = 0.6f)))
                }
                Text(entry.label, fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun PeriodSelector(period: DiaryPeriod, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.2.dp, CoffeeBrown),
        color = Color.Transparent
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(period.name.lowercase().capitalize(), color = CoffeeBrown, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            Icon(Icons.Default.KeyboardArrowDown, null, tint = CoffeeBrown, modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBottomSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp)) {
            Text("Recomendaciones OMS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            InfoRow("Máximo Diario", "400 mg", "Aprox. 4 espressos. Límite seguro para adultos sanos.")
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            InfoRow("Embarazo", "200 mg", "Se recomienda reducir el consumo a la mitad.")
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            InfoRow("Hidratación", "2.5 L", "El agua es vital. El café deshidrata ligeramente.")
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, desc: String) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Bold)
            Text(value, color = CoffeeBrown, fontWeight = FontWeight.Black)
        }
        Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
fun MetricBox(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Color(0xFFFBFBFB), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFF0F0F0))) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(label, fontSize = 10.sp, color = Color.Gray)
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
    if (items.isEmpty()) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Text("No tienes café en stock.", Modifier.padding(24.dp), color = Color.Gray, textAlign = TextAlign.Center)
        }
    } else {
        // USO DE GRID PARA DOS COLUMNAS
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.heightIn(max = 1000.dp), // Permitimos que la grid crezca dentro del scroll de la pantalla
            userScrollEnabled = false // El scroll lo maneja la LazyColumn principal
        ) {
            items(items, key = { it.coffee.id }) { item ->
                PantryCard(
                    item = item, 
                    onClick = { onItemClick(item.coffee.id) }, 
                    onRemove = { onRemoveItem(item.coffee.id) }, 
                    onUpdateGrams = { onUpdateGrams(item.coffee.id, it) }, 
                    onEditClick = onEditClick
                )
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
    val progress = if (item.pantryItem.totalGrams > 0) item.pantryItem.gramsRemaining.toFloat() / item.pantryItem.totalGrams else 0f
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp), // Ligeramente menos redondeado para la grid
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) { // Altura reducida para la grid
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
                            startY = 100f
                        ))
                )
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                    Text(text = item.coffee.marca, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                    Text(text = item.coffee.nombre, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                
                IconButton(
                    onClick = { onEditClick(item.coffee.id, item.isCustom) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.2f), CircleShape).size(28.dp)
                ) {
                    Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "${item.pantryItem.gramsRemaining}g / ${item.pantryItem.totalGrams}g", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), color = CoffeeBrown, trackColor = Color(0xFFF0F0F0))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableDiaryItem(entry: DiaryEntryEntity, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false })
    
    SwipeToDismissBox(
        state = dismissState, 
        enableDismissFromStartToEnd = false, 
        backgroundContent = { 
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)) 
                    .background(Color.Red.copy(alpha = 0.8f)), 
                contentAlignment = Alignment.CenterEnd
            ) { 
                Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.padding(end = 16.dp)) 
            } 
        }
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
        shape = RoundedCornerShape(16.dp), 
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { // Padding reducido
            Box(modifier = Modifier.size(36.dp).background(if (entry.type == "WATER") Color(0xFFE3F2FD) else CoffeeBrown.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(if (entry.type == "WATER") Icons.Default.WaterDrop else Icons.Default.Coffee, null, tint = if (entry.type == "WATER") Color(0xFF2196F3) else CoffeeBrown, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.coffeeName, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, color = Color.Black)
                Text("$dateStr • ${if (entry.type == "WATER") "${entry.amountMl}ml" else "${entry.caffeineAmount}mg - ${entry.preparationType}"}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockEditBottomSheet(item: PantryItemWithDetails, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    var total by remember { mutableFloatStateOf(item.pantryItem.totalGrams.toFloat()) }
    var rem by remember { mutableFloatStateOf(item.pantryItem.gramsRemaining.toFloat()) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp)) {
            Text("Editar Stock", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(32.dp))
            StockSliderSection("Bolsa total", total, 1000f) { total = it; if (rem > it) rem = it }
            Spacer(Modifier.height(24.dp))
            StockSliderSection("Restante", rem, total) { rem = it }
            Spacer(Modifier.height(40.dp))
            Button(onClick = { onSave(total.roundToInt(), rem.roundToInt()) }, Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown), shape = RoundedCornerShape(16.dp)) { Text("GUARDAR") }
        }
    }
}

@Composable
fun StockSliderSection(label: String, value: Float, maxValue: Float, onValueChange: (Float) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text("${value.roundToInt()} g", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = CoffeeBrown)
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..maxValue, colors = SliderDefaults.colors(thumbColor = CoffeeBrown, activeTrackColor = CoffeeBrown))
    }
}
