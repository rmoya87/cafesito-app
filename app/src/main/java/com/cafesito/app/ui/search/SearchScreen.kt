@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalLayoutApi::class)

package com.cafesito.app.ui.search

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cafesito.app.data.CoffeeWithDetails
import com.cafesito.app.camera.NativeBarcodeScannerActivity
import com.cafesito.app.ui.components.PremiumCard
import com.cafesito.app.ui.components.ShimmerItem
import com.cafesito.app.ui.components.TagChip
import com.cafesito.app.ui.theme.*
import com.cafesito.app.ui.components.toCoffeeBrandFormat
import com.cafesito.app.ui.components.toCoffeeNameFormat
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onCancel: () -> Unit,
    interactionSource: MutableInteractionSource,
    filterCounts: Map<String, Int>,
    availableFilters: List<String>,
    onFilterClick: (String) -> Unit,
    onBarcodeClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    isLoading: Boolean
) {
    val animatedWords = remember { listOf("marca", "café") }
    var targetWord by remember { mutableStateOf(animatedWords[0]) }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            targetWord = animatedWords[(animatedWords.indexOf(targetWord) + 1) % animatedWords.size]
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .onGloballyPositioned { coords ->
                val height = coords.size.height.toFloat()
                if (scrollBehavior.state.heightOffsetLimit != -height) {
                    scrollBehavior.state.heightOffsetLimit = -height
                }
            }
            .offset { IntOffset(0, scrollBehavior.state.heightOffset.roundToInt()) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    ShimmerItem(Modifier.weight(1f).height(48.dp))
                } else {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        interactionSource = interactionSource,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        decorationBox = { innerTextField ->
                            OutlinedTextFieldDefaults.DecorationBox(
                                value = query,
                                innerTextField = innerTextField,
                                enabled = true,
                                singleLine = true,
                                visualTransformation = VisualTransformation.None,
                                interactionSource = interactionSource,
                                leadingIcon = { 
                                    Box(modifier = Modifier.padding(start = 4.dp)) {
                                        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) 
                                    }
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = onBarcodeClick,
                                        modifier = Modifier.padding(end = 12.dp)
                                    ) {
                                        BarcodeActionIcon(
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                placeholder = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Busca ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        AnimatedContent(
                                            targetState = targetWord,
                                            transitionSpec = {
                                                (slideInVertically { h -> h } + fadeIn())
                                                    .togetherWith(slideOutVertically { h -> -h } + fadeOut())
                                            },
                                            label = "PlaceholderAnimation"
                                        ) { word -> Text(word, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                    }
                                },
                                contentPadding = PaddingValues(top = 0.dp, bottom = 0.dp, start = 0.dp, end = 0.dp),
                                container = {
                                    OutlinedTextFieldDefaults.Container(
                                        enabled = true,
                                        isError = false,
                                        interactionSource = interactionSource,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedBorderColor = MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(999.dp),
                                    )
                                }
                            )
                        }
                    )
                }

                AnimatedVisibility(
                    visible = (query.isNotBlank() || isFocused) && !isLoading,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    TextButton(
                        onClick = onCancel,
                        contentPadding = PaddingValues(start = 12.dp, end = 0.dp)
                    ) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            FilterChipsRow(
                availableFilters = availableFilters,
                filterCounts = filterCounts, 
                onFilterClick = onFilterClick,
                isLoading = isLoading
            )
        }
    }
}

@Composable
fun BarcodeActionIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier.size(22.dp)
    ) {
        val bars = listOf(1.5f, 3f, 1.5f, 2f, 1.2f, 2.8f, 1.4f, 2.2f, 1f, 2.8f)
        val spacing = 1.6.dp.toPx()
        var x = 0f
        bars.forEach { wDp ->
            val width = wDp.dp.toPx()
            drawRoundRect(
                color = tint,
                topLeft = androidx.compose.ui.geometry.Offset(x, size.height * 0.12f),
                size = Size(width, size.height * 0.76f),
                cornerRadius = CornerRadius(width / 2f, width / 2f),
                style = Fill
            )
            x += width + spacing
        }
    }
}

@Composable
private fun FilterChipsRow(
    availableFilters: List<String>,
    filterCounts: Map<String, Int>,
    onFilterClick: (String) -> Unit,
    isLoading: Boolean
) {
    val optionBackground = if (isSystemInDarkTheme()) PureBlack else PureWhite
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        if (isLoading) {
            items(4) { ShimmerItem(Modifier.size(width = 80.dp, height = 32.dp).clip(RoundedCornerShape(12.dp))) }
        } else {
            items(availableFilters) { filter ->
                val count = filterCounts[filter] ?: 0
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = optionBackground,
                    border = BorderStroke(1.dp, if (count > 0) CaramelAccent else MaterialTheme.colorScheme.outline),
                    modifier = Modifier.clickable { onFilterClick(filter) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = filter, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                        if (count > 0) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = count.toString(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    style = TextStyle(
                                        textAlign = TextAlign.Center,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    )
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
fun SearchScreen(
    onCoffeeClick: (String) -> Unit,
    onProfileClick: (Int) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val filterOptions by viewModel.adaptiveFilterOptions.collectAsState()
    
    val selectedOrigins by viewModel.selectedOrigins.collectAsState()
    val selectedRoasts by viewModel.selectedRoasts.collectAsState()
    val selectedSpecialties by viewModel.selectedSpecialties.collectAsState()
    val selectedFormats by viewModel.selectedFormats.collectAsState()
    val minRating by viewModel.minRating.collectAsState()

    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val actualScrollBehavior = scrollBehavior ?: TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    var showFilterSheet by remember { mutableStateOf(false) }
    var activeFilterType by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isLoading = uiState is SearchUiState.Loading && !isRefreshing

    val currentFilterCounts = remember(selectedOrigins, selectedRoasts, selectedSpecialties, selectedFormats, minRating) {
        mapOf(
            "País" to selectedOrigins.size,
            "Tueste" to selectedRoasts.size,
            "Especialidad" to selectedSpecialties.size,
            "Formato" to selectedFormats.size,
            "Nota" to if (minRating > 0) 1 else 0
        )
    }

    val nativeScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val value = result.data?.getStringExtra(NativeBarcodeScannerActivity.EXTRA_BARCODE_VALUE)
        if (!value.isNullOrBlank()) {
            viewModel.onBarcodeScanned(value, onCoffeeClick)
        }
    }

    val availableFilters = remember(filterOptions, currentFilterCounts) {
        listOfNotNull(
            if (filterOptions.origins.isNotEmpty() || (currentFilterCounts["País"] ?: 0) > 0) "País" else null,
            if (filterOptions.specialties.isNotEmpty() || (currentFilterCounts["Especialidad"] ?: 0) > 0) "Especialidad" else null,
            if (filterOptions.roasts.isNotEmpty() || (currentFilterCounts["Tueste"] ?: 0) > 0) "Tueste" else null,
            if (filterOptions.formats.isNotEmpty() || (currentFilterCounts["Formato"] ?: 0) > 0) "Formato" else null,
            "Nota"
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.nestedScroll(actualScrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SearchTopBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged,
                onCancel = {
                    focusManager.clearFocus()
                    if (searchQuery.isNotBlank()) { viewModel.addSearchToHistory(searchQuery) }
                    viewModel.onSearchQueryChanged("")
                },
                interactionSource = interactionSource,
                filterCounts = currentFilterCounts,
                availableFilters = availableFilters,
                onFilterClick = { type ->
                    activeFilterType = type
                    showFilterSheet = true
                },
                onBarcodeClick = {
                    context.findActivity()?.let { activity ->
                        nativeScannerLauncher.launch(Intent(activity, NativeBarcodeScannerActivity::class.java))
                    }
                },
                scrollBehavior = actualScrollBehavior,
                isLoading = isLoading
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .layout { measurable, constraints ->
                    val offset = actualScrollBehavior.state.heightOffset.roundToInt()
                    val extraHeight = actualScrollBehavior.state.heightOffsetLimit.roundToInt().coerceAtMost(0).absoluteValue
                    
                    val newMaxHeight = if (constraints.hasBoundedHeight) {
                        // Evitar desbordamiento de Int y asegurar que maxHeight >= minHeight
                        val sum = constraints.maxHeight.toLong() + extraHeight
                        if (sum > Int.MAX_VALUE) Int.MAX_VALUE else sum.toInt()
                    } else {
                        constraints.maxHeight
                    }

                    val placeable = measurable.measure(
                        constraints.copy(maxHeight = newMaxHeight.coerceAtLeast(constraints.minHeight))
                    )
                    layout(placeable.width, constraints.maxHeight) {
                        placeable.placeRelative(0, offset)
                    }
                }
        ) {
            AnimatedVisibility(visible = (isFocused || searchQuery.isNotBlank()) && recentSearches.isNotEmpty() && !isLoading) {
                RecentSearches(
                    recentSearches = recentSearches.take(10),
                    onRecentSearchClick = { term -> 
                        viewModel.onSearchQueryChanged(term)
                        focusManager.clearFocus()
                    },
                    onClearRecent = { viewModel.clearRecentSearches() }
                )
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshData() },
                modifier = Modifier.fillMaxSize()
            ) {
                when (val state = uiState) {
                    is SearchUiState.Loading -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            items(3) { ShimmerItem(Modifier.fillMaxWidth().height(350.dp).padding(16.dp)) }
                        }
                    }
                    is SearchUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(state.message, color = MaterialTheme.colorScheme.onSurface) }
                    is SearchUiState.Success -> {
                        if (state.coffees.isEmpty() && !isRefreshing) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptySearchResults(Modifier.padding(top = 64.dp))
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 100.dp)
                            ) {
                                itemsIndexed(state.coffees, key = { _, item -> item.coffee.id }) { index, coffeeDetails ->
                                    LaunchedEffect(index) { viewModel.onItemDisplayed(index) }
                                    Box(modifier = Modifier.padding(vertical = 4.dp)) {
                                        CoffeePremiumListItem(
                                            coffeeDetails = coffeeDetails,
                                            onCoffeeClick = onCoffeeClick,
                                            onFavoriteClick = { viewModel.toggleFavorite(coffeeDetails.coffee.id, coffeeDetails.isFavorite) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showFilterSheet) {
            val configuration = LocalConfiguration.current
            val screenHeight = configuration.screenHeightDp.dp
            
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                scrimColor = Color.Black.copy(alpha = 0.5f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = screenHeight * 0.85f)
                ) {
                    val currentActiveType = activeFilterType
                    if (currentActiveType == "Nota") {
                        RatingFilterContent(
                            currentRating = minRating,
                            onRatingChange = { viewModel.setMinRating(it) }
                        )
                    } else {
                        FilterSelectionContent(
                            options = when(currentActiveType) {
                                "País" -> filterOptions.origins
                                "Tueste" -> filterOptions.roasts
                                "Especialidad" -> filterOptions.specialties
                                "Formato" -> filterOptions.formats
                                else -> emptyList()
                            },
                            selectedValues = when(currentActiveType) {
                                "País" -> selectedOrigins
                                "Tueste" -> selectedRoasts
                                "Especialidad" -> selectedSpecialties
                                "Formato" -> selectedFormats
                                else -> emptySet()
                            },
                            onOptionToggle = { option ->
                                when(currentActiveType) {
                                    "País" -> viewModel.toggleOrigin(option)
                                    "Tueste" -> viewModel.toggleRoast(option)
                                    "Especialidad" -> viewModel.toggleSpecialty(option)
                                    "Formato" -> viewModel.toggleFormat(option)
                                }
                            },
                            onClearAll = { viewModel.clearFilters() }
                        )
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun RatingFilterContent(
    currentRating: Float,
    onRatingChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val displayRating = (5f - currentRating).toInt()
            Text(
                text = if (displayRating == 0) "Cualquier nota" else "Nota: $displayRating+",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
            if (displayRating > 0) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Slider(
            value = currentRating,
            onValueChange = onRatingChange,
            valueRange = 0f..5f,
            steps = 4,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.outline,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline,
                activeTickColor = MaterialTheme.colorScheme.outline,
                inactiveTickColor = MaterialTheme.colorScheme.outline
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("5 Estrellas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Todas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FilterSelectionContent(
    options: List<String>,
    selectedValues: Set<String>,
    onOptionToggle: (String) -> Unit,
    onClearAll: () -> Unit
) {
    val optionBackground = if (isSystemInDarkTheme()) PureBlack else PureWhite
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onClearAll) {
                Text("Limpiar filtros", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
        ) {
            items(options) { option ->
                val isSelected = selectedValues.contains(option)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onOptionToggle(option) },
                    shape = RoundedCornerShape(12.dp),
                    color = optionBackground,
                    border = BorderStroke(1.dp, if (isSelected) CaramelAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onOptionToggle(option) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = CaramelAccent,
                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                checkmarkColor = PureWhite
                            ),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
            item { 
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun RecentSearches(
    recentSearches: List<String>,
    onRecentSearchClick: (String) -> Unit,
    onClearRecent: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text("Búsquedas recientes", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClearRecent, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Clear, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(recentSearches) { term ->
                Surface(
                    modifier = Modifier.clickable { onRecentSearchClick(term) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(text = term, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun CoffeePremiumListItem(
    coffeeDetails: CoffeeWithDetails,
    onCoffeeClick: (String) -> Unit,
    onFavoriteClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCoffeeClick(coffeeDetails.coffee.id) },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.height(86.dp)) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = coffeeDetails.coffee.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                
                Spacer(Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f).padding(end = 36.dp)) {
                    Text(
                        text = coffeeDetails.coffee.nombre.toCoffeeNameFormat(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = coffeeDetails.coffee.marca.toCoffeeBrandFormat(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
            ) {
                Icon(
                    imageVector = if (coffeeDetails.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (coffeeDetails.isFavorite) ElectricRed else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptySearchResults(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Coffee, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))
        Text("No encontramos ese aroma...", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text("Prueba con otros términos o filtros.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
