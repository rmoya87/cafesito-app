package com.cafesito.app.ui.diary

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cafesito.app.data.DiaryEntryEntity
import com.cafesito.app.data.PantryItemWithDetails
import com.cafesito.app.ui.components.*
import com.cafesito.app.ui.theme.*
import com.cafesito.app.ui.diary.formatMonthOnly
import com.cafesito.app.ui.diary.formatMonthYear
import com.cafesito.app.ui.diary.formatWeekRange
import com.cafesito.app.ui.diary.getMondayOfWeek
import com.cafesito.app.ui.utils.formatRelativeTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    navigateTo: String = "",
    refreshSignal: Long? = null,
    forcePantry: Boolean = false,
    showPendingPantryPlaceholder: Boolean = false,
    onConsumeForcePantry: () -> Unit = {},
    onConsumePendingPantryPlaceholder: () -> Unit = {},
    onRefreshSignalConsumed: () -> Unit = {},
    onCoffeeClick: (String) -> Unit,
    onAddWaterClick: () -> Unit,
    onAddCoffeeClick: () -> Unit,
    onAddStockClick: () -> Unit,
    onEditStockClick: (String, Boolean) -> Unit,
    onEditCoffeeClick: (String) -> Unit,
    onCafesProbadosClick: () -> Unit = {},
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val entries by viewModel.diaryEntries.collectAsState(initial = emptyList())
    val allDiaryEntries by viewModel.allDiaryEntries.collectAsState(initial = emptyList())
    val pantryItems by viewModel.pantryItems.collectAsState(initial = emptyList())
    val analytics by viewModel.analytics.collectAsState(initial = viewModel.analytics.value)
    val habitStats by viewModel.habitStats.collectAsState()
    val consumptionStats by viewModel.consumptionStats.collectAsState()
    val baristaStats by viewModel.baristaStats.collectAsState()
    val availableCoffees by viewModel.availableCoffees.collectAsState(initial = emptyList())
    val selectedPeriod by viewModel.selectedPeriod.collectAsState(initial = DiaryPeriod.HOY)
    val selectedDiaryDateMs by viewModel.selectedDiaryDateMs.collectAsState(initial = 0L)
    val isLoading by viewModel.isLoading.collectAsState(initial = true)
    
    var showStockSheet by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<PantryItemWithDetails?>(null) }
    var showPeriodMenu by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<DiaryEntryEntity?>(null) }
    
    val todayStartMs = remember {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        c.timeInMillis
    }
    val isSelectedToday = selectedDiaryDateMs in todayStartMs until (todayStartMs + 86400000L)
    val dateLabel = remember(selectedDiaryDateMs, selectedPeriod) {
        when (selectedPeriod) {
            DiaryPeriod.SEMANA -> formatWeekRange(if (selectedDiaryDateMs != 0L) selectedDiaryDateMs else getMondayOfWeek(System.currentTimeMillis()))
            DiaryPeriod.MES -> if (selectedDiaryDateMs != 0L) formatMonthOnly(selectedDiaryDateMs) else formatMonthOnly(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis)
            else -> if (selectedDiaryDateMs == 0L) "Hoy" else if (isSelectedToday) "Hoy" else SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(selectedDiaryDateMs))
        }
    }
    val canGoNext = when (selectedPeriod) {
        DiaryPeriod.SEMANA -> selectedDiaryDateMs < getMondayOfWeek(System.currentTimeMillis())
        DiaryPeriod.MES -> {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDiaryDateMs }
            val now = Calendar.getInstance()
            cal.get(Calendar.YEAR) < now.get(Calendar.YEAR) || (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) && cal.get(Calendar.MONTH) < now.get(Calendar.MONTH))
        }
        else -> !isSelectedToday
    }
    
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshDiaryEntries()
    }
    var showPantryPlaceholder by remember { mutableStateOf(false) }
    val isDarkMode = isSystemInDarkTheme()

    val coffeeImageMap = remember(availableCoffees) {
        availableCoffees.associate { it.coffee.id to it.coffee.imageUrl }
    }
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(forcePantry) {
        if (forcePantry) onConsumeForcePantry()
    }

    LaunchedEffect(showPendingPantryPlaceholder) {
        if (showPendingPantryPlaceholder) {
            showPantryPlaceholder = true
            delay(1800)
            showPantryPlaceholder = false
            onConsumePendingPantryPlaceholder()
        }
    }

    LaunchedEffect(refreshSignal) {
        if (refreshSignal != null) {
            viewModel.refreshData(showLoader = false)
            onRefreshSignalConsumed()
        }
    }

    // Estado para la modal de opciones de despensa
    var showPantryOptionsId by remember { mutableStateOf<String?>(null) }
    var showFinishedConfirmId by remember { mutableStateOf<String?>(null) }
    var itemToDeleteId by remember { mutableStateOf<String?>(null) }

    if (showStockSheet && itemToEdit != null) {
        StockEditBottomSheet(
            item = itemToEdit!!,
            onDismiss = { showStockSheet = false },
            onSave = { total, remaining ->
                viewModel.updateStock(itemToEdit!!.pantryItem.id, total, remaining)
                showStockSheet = false
            }
        )
    }

    if (showPantryOptionsId != null) {
        val selectedItem = pantryItems.find { it.pantryItem.id == showPantryOptionsId }
        if (selectedItem != null) {
            ModalBottomSheet(
                onDismissRequest = { showPantryOptionsId = null },
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                shape = Shapes.sheetLarge,
                scrimColor = ScrimDefault
            ) {
                Column(Modifier.padding(bottom = 40.dp, start = 24.dp, end = 24.dp)) {
                    Text(
                        text = "Opciones",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    ModalMenuOption(
                        title = "Editar stock",
                        icon = Icons.Default.Edit,
                        color = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            itemToEdit = selectedItem
                            showStockSheet = true
                            showPantryOptionsId = null
                        }
                    )
                    ModalMenuOption(
                        title = "Café terminado",
                        icon = Icons.Default.Done,
                        color = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            showFinishedConfirmId = showPantryOptionsId
                            showPantryOptionsId = null
                        }
                    )
                    if (selectedItem.isCustom) {
                        ModalMenuOption(
                            title = "Editar café",
                            icon = Icons.Default.LocalCafe,
                            color = MaterialTheme.colorScheme.onSurface,
                            onClick = {
                                val coffeeId = selectedItem.coffee.id
                                showPantryOptionsId = null
                                onEditCoffeeClick(coffeeId)
                            }
                        )
                    }
                    ModalMenuOption(
                        title = "Eliminar de la despensa",
                        icon = Icons.Default.Delete,
                        color = MaterialTheme.colorScheme.onSurface,
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
        DeleteConfirmationDialog(
            onDismissRequest = { itemToDeleteId = null },
            title = "Eliminar de la despensa",
            text = "¿Estás seguro de eliminar este café? Se borrará tu stock actual.",
            onConfirm = {
                val id = itemToDeleteId!!
                viewModel.removeFromPantry(id) {
                    viewModel.refreshData()
                }
                itemToDeleteId = null
            }
        )
    }

    if (showFinishedConfirmId != null) {
        DeleteConfirmationDialog(
            onDismissRequest = { showFinishedConfirmId = null },
            title = "Café terminado",
            text = "¿Marcar este café como terminado? Se quitará de tu despensa y se guardará en Historial.",
            onConfirm = {
                val id = showFinishedConfirmId!!
                viewModel.markCoffeeAsFinished(id) {
                    viewModel.refreshData()
                }
                showFinishedConfirmId = null
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GlassyTopBar(
                title = "MI DIARIO",
                navigationContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val chipBackground = if (isDarkMode) MaterialTheme.colorScheme.surface else PureWhite
                        val chipBorderColor = MaterialTheme.colorScheme.outline
                        val chipContentColor = if (isDarkMode) MaterialTheme.colorScheme.onSurface else PureBlack
                        Row(
                            modifier = Modifier
                                .wrapContentWidth()
                                .height(40.dp)
                                .clip(Shapes.pill)
                                .background(chipBackground)
                                .border(1.dp, chipBorderColor, Shapes.pill)
                                .padding(horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    when (selectedPeriod) {
                                        DiaryPeriod.SEMANA -> viewModel.prevWeek()
                                        DiaryPeriod.MES -> viewModel.prevMonth()
                                        else -> viewModel.prevDay()
                                    }
                                },
                                modifier = Modifier.size(32.dp).minimumInteractiveComponentSize(),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = chipContentColor
                                )
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = if (selectedPeriod == DiaryPeriod.MES) "Mes anterior" else "Anterior", modifier = Modifier.size(20.dp))
                            }
                            Row(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .fillMaxHeight()
                                    .minimumInteractiveComponentSize()
                                    .clickable(onClick = { showPeriodMenu = true })
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    dateLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = chipContentColor
                                )
                            }
                            if (canGoNext) {
                                IconButton(
                                    onClick = {
                                        when (selectedPeriod) {
                                            DiaryPeriod.SEMANA -> viewModel.nextWeek()
                                            DiaryPeriod.MES -> viewModel.nextMonth()
                                            else -> viewModel.nextDay()
                                        }
                                    },
                                    modifier = Modifier.size(32.dp).minimumInteractiveComponentSize(),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = chipContentColor
                                    )
                                ) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = if (selectedPeriod == DiaryPeriod.MES) "Mes siguiente" else "Siguiente", modifier = Modifier.size(20.dp))
                                }
                            } else {
                                Spacer(Modifier.size(32.dp))
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.refreshData()
                coroutineScope.launch {
                    delay(400)
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                CaffeinePremiumCard(analytics)
                Spacer(Modifier.height(24.dp))
            }
            item {
                Text(
                    text = "Hábito",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                DiaryHabitCard(habitStats)
            }
            item {
                Text(
                    text = "Consumo",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                DiaryConsumptionCard(consumptionStats)
            }
            item {
                Text(
                    text = "Barista",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                DiaryBaristaCard(baristaStats, onCafesProbadosClick = onCafesProbadosClick)
            }

            item {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "ACTIVIDAD",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (isLoading) {
                items(5) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        DiaryItemShimmer()
                    }
                }
            } else if (entries.isEmpty()) {
                item { EmptyStateMessage("Sin café o agua registrada") }
            } else {
                itemsIndexed(entries, key = { _, it -> "${it.id}_${it.timestamp}" }) { index, entry ->
                    if (selectedPeriod != DiaryPeriod.HOY) {
                        val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(entry.timestamp))
                        val previousDayKey = entries.getOrNull(index - 1)?.let {
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp))
                        }
                        if (dayKey != previousDayKey) {
                            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(todayStartMs))
                            val isCurrentDay = dayKey == todayStr
                            val dayHeaderColor = when {
                                isCurrentDay && isDarkMode -> Color.White
                                isCurrentDay && !isDarkMode -> Color.Black
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Text(
                                text = SimpleDateFormat("d MMMM", Locale.forLanguageTag("es-ES")).format(Date(entry.timestamp)),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = dayHeaderColor,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                    val isNew = entry.timestamp >= System.currentTimeMillis() - 10_000
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(220)) +
                            slideInVertically(initialOffsetY = { it / 5 }, animationSpec = tween(220)) +
                            scaleIn(initialScale = 0.92f, animationSpec = tween(220)),
                        label = "DiaryEntryAppear"
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            SwipeableDiaryItem(
                                entry = entry,
                                coffeeImageUrl = if (entry.type == "CUP") coffeeImageMap[entry.coffeeId] else null,
                                highlightNew = isNew,
                                selectedPeriod = selectedPeriod,
                                enableDeleteSwipe = true,
                                onDelete = { viewModel.deleteEntry(entry.id) },
                                onClick = { selectedEntry = entry }
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(140.dp)) }
        }
        }


        selectedEntry?.let { entry ->
            DiaryEntryEditBottomSheet(
                entry = entry,
                onDismiss = { selectedEntry = null },
                onSave = { updatedEntry ->
                    viewModel.updateEntry(updatedEntry)
                    selectedEntry = null
                }
            )
        }

        if (showPeriodMenu) {
            PeriodBottomSheet(
                selectedPeriod = selectedPeriod,
                selectedDateMs = selectedDiaryDateMs,
                canGoNextMonth = canGoNext,
                onDismiss = { showPeriodMenu = false },
                onPeriodSelected = { viewModel.setPeriod(it) },
                onPrevMonth = { viewModel.prevMonth() },
                onNextMonth = { viewModel.nextMonth() }
            )
        }

        if (showCalendar) {
            DiaryDatePickerSheet(
                onDismiss = { showCalendar = false },
                onGoToToday = {
                    viewModel.setSelectedDiaryDateMs(getMondayOfWeek(System.currentTimeMillis()))
                },
                onPickDate = { year, month, dayOfMonth ->
                    val c = Calendar.getInstance()
                    c.set(Calendar.YEAR, year)
                    c.set(Calendar.MONTH, month)
                    c.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    c.set(Calendar.HOUR_OF_DAY, 0)
                    c.set(Calendar.MINUTE, 0)
                    c.set(Calendar.SECOND, 0)
                    c.set(Calendar.MILLISECOND, 0)
                    viewModel.setSelectedDiaryDateMs(getMondayOfWeek(c.timeInMillis))
                },
                selectedDateMs = selectedDiaryDateMs,
                entries = allDiaryEntries
            )
        }

    }
}
