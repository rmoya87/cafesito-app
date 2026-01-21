package com.example.cafesito.ui.diary

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cafesito.data.DiaryEntryEntity
import com.example.cafesito.data.PantryItemWithDetails
import com.example.cafesito.ui.components.*
import com.example.cafesito.ui.theme.*
import com.example.cafesito.ui.utils.formatRelativeTime
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

    Scaffold(
        containerColor = SoftOffWhite,
        topBar = {
            GlassyTopBar(
                title = "MI DIARIO",
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
                        IconButton(onClick = { showQuickActions = true }) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Añadir", tint = EspressoDeep, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        PeriodSelectorPremium(selectedPeriod) { showPeriodMenu = true }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item {
                analytics?.let { CaffeinePremiumCard(it) }
                Spacer(Modifier.height(24.dp))
            }

            item {
                // PESTAÑAS PREMIUM
                PremiumTabRow(
                    selectedTabIndex = selectedTab,
                    tabs = listOf("ACTIVIDAD", "DESPENSA"),
                    onTabSelected = { selectedTab = it }
                )
                Spacer(Modifier.height(16.dp))
            }

            if (selectedTab == 0) {
                if (entries.isEmpty()) {
                    item { EmptyStateMessage("No hay registros en este periodo") }
                } else {
                    items(entries, key = { "${it.id}_${it.timestamp}" }) { entry ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { 20 })
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                SwipeableDiaryItem(entry = entry, onDelete = { viewModel.deleteEntry(entry.id) })
                            }
                        }
                    }
                }
            } else {
                item {
                    PantryPremiumGrid(
                        items = pantryItems,
                        onItemClick = onCoffeeClick,
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
            item { Spacer(Modifier.height(100.dp)) }
        }

        if (showPeriodMenu) {
            PeriodBottomSheet(
                selectedPeriod = selectedPeriod,
                onDismiss = { showPeriodMenu = false },
                onPeriodSelected = { viewModel.setPeriod(it) }
            )
        }

        if (showQuickActions) {
            AddEntryBottomSheet(
                onDismiss = { showQuickActions = false },
                onAddWater = { onAddWaterClick(); showQuickActions = false },
                onAddCoffee = { onAddCoffeeClick(); showQuickActions = false },
                onAddPantry = { onAddStockClick(); showQuickActions = false }
            )
        }
    }
}

@Composable
fun CaffeinePremiumCard(analytics: DiaryAnalytics) {
    var showInfo by remember { mutableStateOf(false) }
    if (showInfo) InfoBottomSheet { showInfo = false }

    PremiumCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("CAFEÍNA ESTIMADA", style = MaterialTheme.typography.labelLarge, color = CaramelAccent, fontSize = 10.sp)
                        IconButton(onClick = { showInfo = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.Info, null, tint = CaramelAccent, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(
                        text = "${analytics.totalCaffeine} mg",
                        style = MaterialTheme.typography.headlineLarge,
                        color = EspressoDeep,
                        fontWeight = FontWeight.Bold
                    )
                    ComparisonPill(analytics.comparisonPercentage)
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("HIDRATACIÓN", style = MaterialTheme.typography.labelLarge, color = Color(0xFF2196F3), fontSize = 10.sp)
                    Text(
                        text = "${analytics.totalWaterMl} ml",
                        style = MaterialTheme.typography.headlineSmall,
                        color = EspressoDeep,
                        fontWeight = FontWeight.Bold
                    )
                    ComparisonPill(-2, Color(0xFF2196F3)) 
                }
            }

            Spacer(Modifier.height(24.dp))
            ChartPremiumSection(analytics)
            Spacer(Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricBoxPremium("Media", "${analytics.averageCaffeine} mg", Icons.Default.AutoGraph, Modifier.weight(1f))
                MetricBoxPremium("Tazas", "${analytics.cupsCount}", Icons.Default.Coffee, Modifier.weight(1f))
                MetricBoxPremium("Progreso", "85%", Icons.Default.WaterDrop, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ComparisonPill(percentage: Int, baseColor: Color? = null) {
    val isPositive = percentage > 0
    val color = baseColor ?: if (isPositive) ErrorRed else SuccessGreen
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Icon(
                if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, 
                null, tint = color, modifier = Modifier.size(10.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("${if (isPositive) "+" else ""}$percentage%", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MetricBoxPremium(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CreamLight)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = EspressoDeep, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = EspressoDeep)
        Text(label.uppercase(), style = MaterialTheme.typography.labelLarge, color = Color.Gray, fontSize = 8.sp)
    }
}

@Composable
fun PantryPremiumGrid(
    items: List<PantryItemWithDetails>,
    onItemClick: (String) -> Unit,
    onEditClick: (String, Boolean) -> Unit
) {
    if (items.isEmpty()) {
        EmptyStateMessage("No hay café en tu despensa")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.heightIn(max = 2000.dp),
            userScrollEnabled = false
        ) {
            items(items, key = { it.coffee.id }) { item ->
                PantryPremiumCard(item, onItemClick, onEditClick)
            }
        }
    }
}

@Composable
fun PantryPremiumCard(
    item: PantryItemWithDetails,
    onClick: (String) -> Unit,
    onEditClick: (String, Boolean) -> Unit
) {
    val progress = if (item.pantryItem.totalGrams > 0) item.pantryItem.gramsRemaining.toFloat() / item.pantryItem.totalGrams else 0f
    
    PremiumCard(
        modifier = Modifier.clickable { onClick(item.coffee.id) },
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
            AsyncImage(
                model = item.coffee.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, EspressoDeep.copy(alpha = 0.8f))))
            )
            IconButton(
                onClick = { onEditClick(item.coffee.id, item.isCustom) },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) { Icon(Icons.Default.Tune, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
            
            Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(item.coffee.marca.uppercase(), color = CaramelAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(item.coffee.nombre, color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${item.pantryItem.gramsRemaining}g", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Text("${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall, color = CaramelAccent)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = CaramelAccent,
                trackColor = BorderLight
            )
        }
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

@Composable
fun PeriodSelectorPremium(period: DiaryPeriod, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = CreamLight,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(period.name, style = MaterialTheme.typography.labelLarge, color = EspressoDeep, fontSize = 11.sp)
            Icon(Icons.Default.ExpandMore, null, tint = EspressoDeep, modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryBottomSheet(onDismiss: () -> Unit, onAddWater: () -> Unit, onAddCoffee: () -> Unit, onAddPantry: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)) {
        Column(Modifier.padding(bottom = 40.dp, start = 24.dp, end = 24.dp)) {
            Text("NUEVO REGISTRO", style = MaterialTheme.typography.labelLarge, color = CaramelAccent, modifier = Modifier.padding(bottom = 16.dp))
            EntryOption("Registro de Agua", Icons.Default.WaterDrop, Color(0xFF2196F3), onAddWater)
            EntryOption("Registro de Café", Icons.Default.Coffee, EspressoDeep, onAddCoffee)
            EntryOption("Añadir a Despensa", Icons.Default.Inventory, CaramelAccent, onAddPantry)
        }
    }
}

@Composable
fun EntryOption(title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = SoftOffWhite,
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}

@Composable
fun ChartPremiumSection(analytics: DiaryAnalytics) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val itemWidthPx = with(density) { (40 + 12).dp.toPx() }

    LaunchedEffect(analytics.chartData) { scrollState.animateScrollTo(scrollState.maxValue) }

    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp).horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        analytics.chartData.forEach { entry ->
            val caffeineHeight = (entry.caffeine.toFloat() / 400f).coerceIn(0.05f, 1f)
            val waterHeight = (entry.water.toFloat() / 2000f).coerceIn(0.05f, 1f)
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
                Row(modifier = Modifier.height(80.dp), verticalAlignment = Alignment.Bottom) {
                    Box(Modifier.width(10.dp).fillMaxHeight(caffeineHeight).clip(CircleShape).background(EspressoDeep))
                    Spacer(Modifier.width(4.dp))
                    Box(Modifier.width(10.dp).fillMaxHeight(waterHeight).clip(CircleShape).background(Color(0xFF2196F3).copy(alpha = 0.4f)))
                }
                Spacer(Modifier.height(8.dp))
                Text(entry.label, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodBottomSheet(selectedPeriod: DiaryPeriod, onDismiss: () -> Unit, onPeriodSelected: (DiaryPeriod) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(Modifier.padding(bottom = 40.dp, start = 24.dp, end = 24.dp)) {
            Text("SELECCIONAR PERIODO", style = MaterialTheme.typography.labelLarge, color = CaramelAccent)
            Spacer(Modifier.height(16.dp))
            DiaryPeriod.values().forEach { period ->
                val isSelected = period == selectedPeriod
                Surface(
                    onClick = { onPeriodSelected(period); onDismiss() },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) EspressoDeep else SoftOffWhite,
                    border = if (isSelected) null else BorderStroke(1.dp, BorderLight)
                ) {
                    Text(
                        period.name, 
                        modifier = Modifier.padding(16.dp),
                        color = if (isSelected) Color.White else EspressoDeep,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
