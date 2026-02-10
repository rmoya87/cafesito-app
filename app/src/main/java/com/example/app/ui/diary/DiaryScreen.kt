package com.cafesito.app.ui.diary

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafesito.app.data.DiaryEntryEntity
import com.cafesito.app.data.PantryItemWithDetails
import com.cafesito.app.ui.components.*
import com.cafesito.app.ui.theme.*
import com.cafesito.app.ui.utils.formatRelativeTime
import kotlinx.coroutines.delay
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
    onEditCoffeeClick: (String) -> Unit,
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
    var isRefreshing by remember { mutableStateOf(false) }

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
                        title = "Editar café",
                        icon = Icons.Default.LocalCafe,
                        color = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            val id = showPantryOptionsId!!
                            showPantryOptionsId = null
                            if (selectedItem.isCustom) onEditCoffeeClick(id) else onEditStockClick(id, false)
                        }
                    )
                    ModalMenuOption(
                        title = "Eliminar de la despensa",
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
