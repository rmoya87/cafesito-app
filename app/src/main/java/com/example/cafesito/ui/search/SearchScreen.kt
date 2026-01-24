@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)

package com.example.cafesito.ui.search

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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.ui.components.PremiumCard
import com.example.cafesito.ui.components.ShimmerItem
import com.example.cafesito.ui.components.TagChip
import com.example.cafesito.ui.theme.BorderLight
import com.example.cafesito.ui.theme.CaramelAccent
import com.example.cafesito.ui.theme.CreamLight
import com.example.cafesito.ui.theme.ElectricRed
import com.example.cafesito.ui.theme.EspressoDeep
import com.example.cafesito.ui.theme.SoftOffWhite
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onCancel: () -> Unit,
    interactionSource: MutableInteractionSource,
    filterCounts: Map<String, Int>,
    onFilterClick: (String) -> Unit
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

    Surface(color = SoftOffWhite) {
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
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    interactionSource = interactionSource,
                    cursorBrush = SolidColor(CaramelAccent),
                    decorationBox = { innerTextField ->
                        OutlinedTextFieldDefaults.DecorationBox(
                            value = query,
                            innerTextField = innerTextField,
                            enabled = true,
                            singleLine = true,
                            visualTransformation = VisualTransformation.None,
                            interactionSource = interactionSource,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search Icon",
                                    tint = Color.Gray
                                )
                            },
                            placeholder = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Busca ", color = Color.Gray)
                                    AnimatedContent(
                                        targetState = targetWord,
                                        transitionSpec = {
                                            (slideInVertically { h -> h } + fadeIn())
                                                .togetherWith(slideOutVertically { h -> -h } + fadeOut())
                                        },
                                        label = "PlaceholderAnimation"
                                    ) { word ->
                                        Text(word, color = Color.Gray)
                                    }
                                    Text("...", color = Color.Gray)
                                }
                            },
                            contentPadding = PaddingValues(top = 0.dp, bottom = 0.dp, start = 0.dp, end = 12.dp),
                            container = {
                                OutlinedTextFieldDefaults.Container(
                                    enabled = true,
                                    isError = false,
                                    interactionSource = interactionSource,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = Color.White,
                                        focusedContainerColor = Color.White,
                                        unfocusedBorderColor = BorderLight,
                                        focusedBorderColor = CaramelAccent
                                    ),
                                    shape = RoundedCornerShape(32.dp),
                                )
                            }
                        )
                    }
                )

                AnimatedVisibility(
                    visible = query.isNotBlank() || isFocused,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    TextButton(
                        onClick = onCancel,
                        contentPadding = PaddingValues(start = 12.dp, end = 0.dp)
                    ) {
                        Text("Cancelar", color = EspressoDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            FilterChipsRow(filterCounts = filterCounts, onFilterClick = onFilterClick)
        }
    }
}

@Composable
private fun FilterChipsRow(
    filterCounts: Map<String, Int>,
    onFilterClick: (String) -> Unit
) {
    val filters = listOf("País", "Especialidad", "Variedad", "Tueste", "Formato", "Molienda", "Valoración")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        items(filters) { filter ->
            val count = filterCounts[filter] ?: 0
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (count > 0) CaramelAccent.copy(alpha = 0.1f) else Color.White,
                border = BorderStroke(1.dp, if (count > 0) CaramelAccent else BorderLight),
                modifier = Modifier.clickable { onFilterClick(filter) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = filter, style = MaterialTheme.typography.labelMedium, color = EspressoDeep, fontSize = 12.sp)
                    if (count > 0) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = CaramelAccent,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = count.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
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
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val filterOptions by viewModel.filterOptions.collectAsState()
    
    val selectedOrigin by viewModel.selectedOrigin.collectAsState()
    val selectedRoast by viewModel.selectedRoast.collectAsState()
    val selectedSpecialty by viewModel.selectedSpecialty.collectAsState()
    val selectedVariety by viewModel.selectedVariety.collectAsState()
    val selectedFormat by viewModel.selectedFormat.collectAsState()
    val selectedGrind by viewModel.selectedGrind.collectAsState()
    val minRating by viewModel.minRating.collectAsState()

    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var activeFilterType by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()

    val currentFilterCounts = remember(selectedOrigin, selectedRoast, selectedSpecialty, selectedVariety, selectedFormat, selectedGrind, minRating) {
        mapOf(
            "País" to if (selectedOrigin != null) 1 else 0,
            "Tueste" to if (selectedRoast != null) 1 else 0,
            "Especialidad" to if (selectedSpecialty != null) 1 else 0,
            "Variedad" to if (selectedVariety != null) 1 else 0,
            "Formato" to if (selectedFormat != null) 1 else 0,
            "Molienda" to if (selectedGrind != null) 1 else 0,
            "Valoración" to if (minRating > 0) 1 else 0
        )
    }

    Scaffold(
        containerColor = SoftOffWhite,
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
                onFilterClick = { type ->
                    activeFilterType = type
                    showFilterSheet = true
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            AnimatedVisibility(visible = (isFocused || searchQuery.isNotBlank()) && recentSearches.isNotEmpty()) {
                RecentSearches(
                    recentSearches = recentSearches.take(10),
                    onRecentSearchClick = { term -> 
                        viewModel.onSearchQueryChanged(term)
                        focusManager.clearFocus()
                    },
                    onClearRecent = { viewModel.clearRecentSearches() }
                )
            }

            when (val state = uiState) {
                is SearchUiState.Loading -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(3) { ShimmerItem(Modifier.fillMaxWidth().height(350.dp).padding(16.dp)) }
                    }
                }
                is SearchUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(state.message) }
                is SearchUiState.Success -> {
                    if (state.coffees.isEmpty() && !isRefreshing) {
                        EmptySearchResults(Modifier.align(Alignment.CenterHorizontally).padding(top = 64.dp))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 32.dp)
                        ) {
                            itemsIndexed(state.coffees, key = { _, item -> item.coffee.id }) { index, coffeeDetails ->
                                LaunchedEffect(index) { viewModel.onItemDisplayed(index) }
                                Box(modifier = Modifier.padding(vertical = 10.dp)) {
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

        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                FilterSelectionContent(
                    type = activeFilterType,
                    options = when(activeFilterType) {
                        "País" -> filterOptions.origins
                        "Tueste" -> filterOptions.roasts
                        "Especialidad" -> filterOptions.specialties
                        "Variedad" -> filterOptions.varieties
                        "Formato" -> filterOptions.formats
                        "Molienda" -> filterOptions.grinds
                        "Valoración" -> listOf("1", "2", "3", "4", "5")
                        else -> emptyList()
                    },
                    selectedValue = when(activeFilterType) {
                        "País" -> selectedOrigin
                        "Tueste" -> selectedRoast
                        "Especialidad" -> selectedSpecialty
                        "Variedad" -> selectedVariety
                        "Formato" -> selectedFormat
                        "Molienda" -> selectedGrind
                        "Valoración" -> if (minRating > 0) minRating.toInt().toString() else null
                        else -> null
                    },
                    onOptionSelected = { option ->
                        when(activeFilterType) {
                            "País" -> viewModel.setOrigin(if (selectedOrigin == option) null else option)
                            "Tueste" -> viewModel.setRoast(if (selectedRoast == option) null else option)
                            "Especialidad" -> viewModel.setSpecialty(if (selectedSpecialty == option) null else option)
                            "Variedad" -> viewModel.setVariety(if (selectedVariety == option) null else option)
                            "Formato" -> viewModel.setFormat(if (selectedFormat == option) null else option)
                            "Molienda" -> viewModel.setGrind(if (selectedGrind == option) null else option)
                            "Valoración" -> viewModel.setMinRating(if (minRating.toInt().toString() == option) 0f else option.toFloat())
                        }
                    }
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FilterSelectionContent(
    type: String,
    options: List<String>,
    selectedValue: String?,
    onOptionSelected: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        options.forEach { option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOptionSelected(option) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selectedValue == option,
                    onCheckedChange = { onOptionSelected(option) },
                    colors = CheckboxDefaults.colors(checkedColor = CaramelAccent),
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = if (type == "Valoración") "$option Estrellas" else option,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EspressoDeep,
                    modifier = Modifier.padding(start = 4.dp)
                )
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
            Icon(Icons.Default.History, null, tint = Color.Gray)
            Spacer(Modifier.width(8.dp))
            Text("Búsquedas recientes", style = MaterialTheme.typography.titleSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClearRecent, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Clear, null, tint = Color.Gray)
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
                    color = CreamLight,
                    border = BorderStroke(1.dp, BorderLight)
                ) {
                    Text(text = term, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 12.sp, color = EspressoDeep)
                }
            }
        }
        HorizontalDivider(color = BorderLight, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun CoffeePremiumListItem(
    coffeeDetails: CoffeeWithDetails,
    onCoffeeClick: (String) -> Unit,
    onFavoriteClick: () -> Unit
) {
    val coffee = coffeeDetails.coffee
    PremiumCard(modifier = Modifier.clickable { onCoffeeClick(coffee.id) }) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                AsyncImage(model = coffee.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, EspressoDeep.copy(alpha = 0.9f)), startY = 300f)))
                Surface(modifier = Modifier.padding(16.dp).align(Alignment.TopEnd).size(36.dp), color = Color.White.copy(alpha = 0.9f), shape = CircleShape) {
                    IconButton(onClick = onFavoriteClick) {
                        Icon(imageVector = if (coffeeDetails.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (coffeeDetails.isFavorite) ElectricRed else Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
                Row(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = coffee.marca.uppercase(), color = CreamLight, style = MaterialTheme.typography.labelLarge, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = coffee.nombre, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(16.dp))
                    Surface(color = Color.White.copy(alpha = 0.9f), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("NOTA", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 8.sp)
                            Text(text = String.format(Locale.getDefault(), "%.1f", coffeeDetails.averageRating), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = EspressoDeep)
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                coffee.paisOrigen?.let { TagChip("PAÍS", it) }
                TagChip("ESTILO", coffee.especialidad)
                coffee.tueste?.let { TagChip("TUESTE", it) }
            }
        }
    }
}

@Composable
private fun EmptySearchResults(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Coffee, null, modifier = Modifier.size(64.dp), tint = BorderLight)
        Spacer(Modifier.height(16.dp))
        Text("No encontramos ese aroma...", style = MaterialTheme.typography.titleMedium, color = EspressoDeep)
        Text("Prueba con otros términos o filtros.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
    }
}
