package com.cafesito.app.ui.diary

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cafesito.app.data.DiaryEntryEntity
import com.cafesito.app.data.PantryItemWithDetails
import com.cafesito.app.ui.components.*
import com.cafesito.app.ui.theme.*
import com.cafesito.app.ui.utils.formatRelativeTime
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DiaryScreen(
    navigateTo: String = "",
    onCoffeeClick: (String) -> Unit,
    onAddWaterClick: () -> Unit,
    onAddCoffeeClick: () -> Unit,
    onAddStockClick: () -> Unit,
    onEditStockClick: (String, Boolean) -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val entries by viewModel.diaryEntries.collectAsState(initial = emptyList())
    val pantryItems by viewModel.pantryItems.collectAsState(initial = emptyList())
    val analytics by viewModel.analytics.collectAsState(initial = null)
    val selectedPeriod by viewModel.selectedPeriod.collectAsState(initial = DiaryPeriod.HOY)
    val isLoading by viewModel.isLoading.collectAsState(initial = true)
    
    var showStockSheet by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<PantryItemWithDetails?>(null) }
    var showQuickActions by remember { mutableStateOf(false) }
    var showPeriodMenu by remember { mutableStateOf(false) }
    
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    LaunchedEffect(navigateTo) {
        if (navigateTo == "pantry") {
            pagerState.scrollToPage(1)
        }
    }

    // Estado para la modal de opciones de despensa
    var showPantryOptionsId by remember { mutableStateOf<String?>(null) }
    var itemToDeleteId by remember { mutableStateOf<String?>(null) }

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

    if (showPantryOptionsId != null) {
        val selectedItem = pantryItems.find { it.coffee.id == showPantryOptionsId }
        if (selectedItem != null) {
            ModalBottomSheet(
                onDismissRequest = { showPantryOptionsId = null },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(Modifier.padding(bottom = 40.dp, start = 24.dp, end = 24.dp)) {
                    Text(
                        text = selectedItem.coffee.nombre.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    ModalMenuOption(
                        title = "Editar Stock",
                        icon = Icons.Default.Edit,
                        color = MaterialTheme.colorScheme.onSurface,
                        onClick = { 
                            val id = showPantryOptionsId!!
                            showPantryOptionsId = null
                            if (selectedItem.isCustom) onEditStockClick(id, true)
                            else {
                                itemToEdit = selectedItem
                                showStockSheet = true
                            }
                        }
                    )
                    ModalMenuOption(
                        title = "Eliminar de la Despensa",
                        icon = Icons.Default.Delete,
                        color = ErrorRed,
                        onClick = { 
                            itemToDeleteId = showPantryOptionsId
                            showPantryOptionsId = null 
                        }
                    )
                }
            }
        }
    }

    if (itemToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { itemToDeleteId = null },
            title = { Text("Eliminar de la Despensa", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas eliminar este café? Se borrará tu stock actual.") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = itemToDeleteId!!
                        viewModel.removeFromPantry(id) {
                            viewModel.refreshData()
                        }
                        itemToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("ELIMINAR") }
            },
            dismissButton = {
                TextButton(onClick = { itemToDeleteId = null }) { Text("CANCELAR", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GlassyTopBar(
                title = "MI DIARIO",
                scrollBehavior = scrollBehavior,
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        IconButton(
                            onClick = { showQuickActions = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.AddCircle, 
                                contentDescription = "Añadir", 
                                tint = MaterialTheme.colorScheme.onSurface, 
                                modifier = Modifier.size(28.dp)
                            )
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
                val currentAnalytics = analytics
                if (isLoading) {
                    CaffeinePremiumCardShimmer()
                } else if (currentAnalytics != null) {
                    CaffeinePremiumCard(currentAnalytics)
                }
                Spacer(Modifier.height(16.dp))
            }

            stickyHeader {
                PremiumTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    tabs = listOf("ACTIVIDAD", "DESPENSA"),
                    onTabSelected = { 
                        coroutineScope.launch { pagerState.animateScrollToPage(it) }
                    }
                )
            }
            
            item {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillParentMaxHeight(),
                    verticalAlignment = Alignment.Top
                ) { page ->
                    when(page) {
                        0 -> {
                            if (isLoading) {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    item { Spacer(Modifier.height(16.dp)) }
                                    items(5) { 
                                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                            DiaryItemShimmer()
                                        }
                                    }
                                }
                            } else if (entries.isEmpty()) {
                                EmptyStateMessage("No hay registros en este periodo")
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    item { Spacer(Modifier.height(16.dp)) }
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
                                    item { Spacer(Modifier.height(140.dp)) }
                                }
                            }
                        }
                        1 -> {
                             if (isLoading) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(4) { PantryItemShimmer() }
                                }
                            } else if (pantryItems.isEmpty()) {
                                EmptyStateMessage("No hay café en tu despensa")
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 140.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(pantryItems, key = { it.coffee.id }) { item ->
                                        PantryPremiumCard(
                                            item = item,
                                            onClick = onCoffeeClick,
                                            onOptionsClick = { coffeeId -> showPantryOptionsId = coffeeId }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
                        Text("CAFEÍNA ESTIMADA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                        IconButton(onClick = { showInfo = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(
                        text = "${analytics.totalCaffeine} mg",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    ComparisonPill(analytics.comparisonPercentage)
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("HIDRATACIÓN", style = MaterialTheme.typography.labelLarge, color = Color(0xFF2196F3), fontSize = 10.sp)
                    Text(
                        text = "${analytics.totalWaterMl} ml",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(label.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
    }
}

@Composable
fun PantryPremiumCard(
    item: PantryItemWithDetails,
    onClick: (String) -> Unit,
    onOptionsClick: ((String) -> Unit)? = null
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
                contentScale = Color.Transparent.let { ContentScale.Crop },
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
            )
            
            if (onOptionsClick != null) {
                IconButton(
                    onClick = { onOptionsClick(item.coffee.id) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp).background(Color.White.copy(alpha = 0.8f), CircleShape)
                ) { Icon(Icons.Default.MoreVert, null, tint = Color.Black, modifier = Modifier.size(16.dp)) }
            }
            
            Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(item.coffee.marca.uppercase(), color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(item.coffee.nombre, color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${item.pantryItem.gramsRemaining}g", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun PeriodSelectorPremium(period: DiaryPeriod, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.height(40.dp) // Explicit height to match IconButton
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                period.name, 
                style = MaterialTheme.typography.labelLarge, 
                color = MaterialTheme.colorScheme.onSurface, 
                fontSize = 11.sp
            )
            Icon(
                Icons.Default.ExpandMore, 
                null, 
                tint = MaterialTheme.colorScheme.onSurface, 
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryBottomSheet(onDismiss: () -> Unit, onAddWater: () -> Unit, onAddCoffee: () -> Unit, onAddPantry: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)) {
        Column(Modifier.padding(bottom = 40.dp, start = 24.dp, end = 24.dp)) {
            Text("NUEVO REGISTRO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp))
            ModalMenuOption(title = "Registro de Agua", icon = Icons.Default.WaterDrop, color = Color(0xFF2196F3), onClick = onAddWater)
            ModalMenuOption(title = "Registro de Café", icon = Icons.Default.Coffee, color = MaterialTheme.colorScheme.onSurface, onClick = onAddCoffee)
            ModalMenuOption(title = "Añadir a Despensa", icon = Icons.Default.Inventory, color = MaterialTheme.colorScheme.primary, onClick = onAddPantry)
        }
    }
}

@Composable
fun EntryOption(title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Box(Modifier.width(10.dp).fillMaxHeight(caffeineHeight).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Spacer(Modifier.width(4.dp))
                    Box(Modifier.width(10.dp).fillMaxHeight(waterHeight).clip(CircleShape).background(Color(0xFF2196F3).copy(alpha = 0.4f)))
                }
                Spacer(Modifier.height(8.dp))
                Text(entry.label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodBottomSheet(selectedPeriod: DiaryPeriod, onDismiss: () -> Unit, onPeriodSelected: (DiaryPeriod) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(bottom = 40.dp, start = 24.dp, end = 24.dp)) {
            Text("SELECCIONAR PERIODO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            DiaryPeriod.values().forEach { period ->
                val isSelected = period == selectedPeriod
                Surface(
                    onClick = { onPeriodSelected(period); onDismiss() },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        period.name, 
                        modifier = Modifier.padding(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
