package com.cafesito.app.ui.timeline

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CoffeeMaker
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafesito.app.data.PantryItemWithDetails
import com.cafesito.app.ui.components.*
import com.cafesito.app.ui.theme.*
import com.cafesito.shared.domain.brew.BREW_METHOD_AGUA
import com.cafesito.shared.domain.brew.BREW_METHOD_OTROS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onUserClick: (Int) -> Unit,
    onCoffeeClick: (String) -> Unit,
    onBrewLabClick: () -> Unit,
    onAddToPantryClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onEditCoffeeClick: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val unreadCount by viewModel.unreadCount.collectAsState()
    val badgeScale = remember { Animatable(1f) }
    val hasUnread = unreadCount > 0
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        viewModel.refreshData()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }

    LaunchedEffect(hasUnread) {
        if (hasUnread) {
            badgeScale.snapTo(1.9f)
            badgeScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing)
            )
        }
    }

    var showPantryOptionsId by remember { mutableStateOf<String?>(null) }
    var itemToEdit by remember { mutableStateOf<PantryItemWithDetails?>(null) }
    var showStockSheet by remember { mutableStateOf(false) }
    var itemToDeleteId by remember { mutableStateOf<String?>(null) }
    var showFinishedConfirmId by remember { mutableStateOf<String?>(null) }

    val state = uiState as? HomeUiState.Success
    val pantryItems = state?.pantryItems ?: emptyList()

    if (showStockSheet && itemToEdit != null) {
        StockEditBottomSheet(
            item = itemToEdit!!,
            onDismiss = { showStockSheet = false },
            onSave = { total, remaining ->
                viewModel.updateStock(itemToEdit!!.pantryItem.id, total, remaining)
                showStockSheet = false
                itemToEdit = null
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
                Column(Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp, bottom = 40.dp)) {
                    Text(
                        text = "Opciones",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Organiza",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column {
                            PantryOptionRow(
                                icon = Icons.Default.Edit,
                                label = "Editar stock",
                                onClick = {
                                    itemToEdit = selectedItem
                                    showStockSheet = true
                                    showPantryOptionsId = null
                                }
                            )
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            PantryOptionRow(
                                icon = Icons.Default.Done,
                                label = "Café terminado",
                                onClick = {
                                    showFinishedConfirmId = showPantryOptionsId
                                    showPantryOptionsId = null
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "General",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column {
                            if (selectedItem.isCustom) {
                                PantryOptionRow(
                                    icon = Icons.Default.LocalCafe,
                                    label = "Editar café",
                                    onClick = {
                                        onEditCoffeeClick(selectedItem.coffee.id)
                                        showPantryOptionsId = null
                                    }
                                )
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            }
                            PantryOptionRow(
                                icon = Icons.Default.Delete,
                                label = "Eliminar de la despensa",
                                onClick = {
                                    itemToDeleteId = showPantryOptionsId
                                    showPantryOptionsId = null
                                }
                            )
                        }
                    }
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
                viewModel.removeFromPantry(id) { }
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
                viewModel.markCoffeeAsFinished(id) { }
                showFinishedConfirmId = null
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "CAFESITO",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                },
                actions = {
                    IconButton(onClick = onNotificationsClick) {
                        BadgedBox(
                            badge = {
                                TimelineNotificationBadge(
                                    visible = hasUnread,
                                    scale = badgeScale.value
                                )
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notificaciones", modifier = Modifier.size(24.dp))
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                    scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                )
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> HomeLoadingContent()
                is HomeUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorStateMessage(message = state.message, onRetry = { viewModel.refreshData() })
                }
                is HomeUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        item {
                            HomeBrewMethodsCarousel(
                                methodNames = state.orderedBrewMethodNames,
                                onMethodClick = onBrewLabClick
                            )
                        }
                        item {
                            Spacer(Modifier.height(16.dp))
                            HomePantrySection(
                                pantryItems = state.pantryItems,
                                onCoffeeClick = onCoffeeClick,
                                onAddToPantryClick = onAddToPantryClick,
                                onOptionsClick = { showPantryOptionsId = it }
                            )
                        }
                        item {
                            Spacer(Modifier.height(16.dp))
                            RecommendationCarousel(
                                recommendations = state.recommendations,
                                onCoffeeClick = onCoffeeClick
                            )
                        }
                        if (state.suggestedUsers.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(16.dp))
                                UserSuggestionCarousel(
                                    users = state.suggestedUsers,
                                    followingIds = state.myFollowingIds,
                                    onUserClick = { userId ->
                                        if (userId == state.activeUser.id) onUserClick(0)
                                        else onUserClick(userId)
                                    },
                                    onFollowClick = { viewModel.toggleFollowSuggestion(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeBrewMethodsCarousel(
    methodNames: List<String>,
    onMethodClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val iconResMap = remember {
        mapOf(
            "Aeropress" to "maq_aeropress",
            "Chemex" to "maq_chemex",
            "Espresso" to "maq_espresso",
            "Goteo" to "maq_goteo",
            "Hario V60" to "maq_hario_v60",
            "Italiana" to "maq_italiana",
            "Manual" to "maq_manual",
            "Prensa francesa" to "maq_prensa_francesa",
            "Sifón" to "maq_sifon",
            "Turco" to "maq_turco",
            BREW_METHOD_OTROS to "relampago",
            BREW_METHOD_AGUA to "agua"
        )
    }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(methodNames) { name ->
                val iconResName = iconResMap[name] ?: "maq_manual"
                val resId = remember(iconResName) {
                    context.resources.getIdentifier(iconResName, "drawable", context.packageName)
                }
                Surface(
                    modifier = Modifier
                        .width(100.dp)
                        .height(100.dp)
                        .clip(Shapes.shapeCardMedium)
                        .clickable(onClick = onMethodClick),
                    shape = Shapes.shapeCardMedium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (resId != 0) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = name,
                                modifier = Modifier.size(36.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(
                                Icons.Default.CoffeeMaker,
                                contentDescription = name,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = name.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomePantrySection(
    pantryItems: List<PantryItemWithDetails>,
    onCoffeeClick: (String) -> Unit,
    onAddToPantryClick: () -> Unit,
    onOptionsClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            text = "Tu despensa",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(pantryItems, key = { it.pantryItem.id }) { item ->
                PantryPremiumMiniCard(
                    item = item,
                    onClick = { onCoffeeClick(item.coffee.id) },
                    onOptionsClick = onOptionsClick
                )
            }
            item(key = "home-add-pantry-card") {
                PantryAddActionCard(onClick = onAddToPantryClick)
            }
        }
    }
}

@Composable
private fun TimelineNotificationBadge(
    visible: Boolean,
    scale: Float
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(100)),
        exit = fadeOut(tween(100))
    ) {
        Badge(
            containerColor = ElectricRed,
            modifier = Modifier
                .size(10.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        ) {}
    }
}
