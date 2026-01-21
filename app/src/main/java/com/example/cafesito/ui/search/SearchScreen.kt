package com.example.cafesito.ui.search

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.ui.components.*
import com.example.cafesito.ui.theme.*
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onCoffeeClick: (String) -> Unit,
    onProfileClick: (Int) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val options by viewModel.filterOptions.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    val selectedOrigin: String? by viewModel.selectedOrigin.collectAsState(initial = null)
    val selectedRoast: String? by viewModel.selectedRoast.collectAsState(initial = null)
    val minRating: Float by viewModel.minRating.collectAsState(initial = 0f)

    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    Scaffold(
        containerColor = SoftOffWhite,
        topBar = {
            if (!isSearchActive) {
                GlassyTopBar(
                    title = "EXPLORAR",
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar", tint = EspressoDeep)
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            
            when (val state = uiState) {
                is SearchUiState.Loading -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(3) { ShimmerItem(Modifier.fillMaxWidth().height(350.dp).padding(16.dp)) }
                    }
                }
                is SearchUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(state.message) }
                is SearchUiState.Success -> {
                    if (state.coffees.isEmpty() && !isRefreshing) {
                        EmptySearchResults(Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            itemsIndexed(state.coffees, key = { _, item -> item.coffee.id }) { index, coffeeDetails ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn() + slideInVertically(initialOffsetY = { 50 })
                                ) {
                                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                        CoffeePremiumListItem(
                                            coffeeDetails = coffeeDetails,
                                            onCoffeeClick = onCoffeeClick,
                                            onFavoriteClick = { 
                                                viewModel.toggleFavorite(coffeeDetails.coffee.id, coffeeDetails.isFavorite) 
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isSearchActive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SearchFilterPanel(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    onClose = { isSearchActive = false },
                    options = options,
                    selectedOrigin = selectedOrigin,
                    onOriginSelected = viewModel::setOrigin,
                    selectedRoast = selectedRoast,
                    onRoastSelected = viewModel::setRoast,
                    minRating = minRating,
                    onRatingChange = viewModel::setMinRating,
                    onClear = viewModel::clearFilters,
                    focusRequester = focusRequester
                )
            }
        }
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
                AsyncImage(
                    model = coffee.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(Brush.verticalGradient(
                            colors = listOf(Color.Transparent, EspressoDeep.copy(alpha = 0.9f)),
                            startY = 300f
                        ))
                )
                
                Surface(
                    modifier = Modifier.padding(16.dp).align(Alignment.TopEnd).size(36.dp),
                    color = Color.White.copy(alpha = 0.9f),
                    shape = CircleShape
                ) {
                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            imageVector = if (coffeeDetails.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (coffeeDetails.isFavorite) ErrorRed else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                    Text(text = coffee.marca.uppercase(), color = CaramelAccent, style = MaterialTheme.typography.labelLarge, fontSize = 10.sp)
                    Text(text = coffee.nombre, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2)
                }

                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = CaramelAccent, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(text = String.format(Locale.getDefault(), "%.1f", coffeeDetails.averageRating), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                coffee.paisOrigen?.let { TagChip(it) }
                TagChip(coffee.especialidad)
                coffee.tueste?.let { TagChip(it) }
            }
        }
    }
}

@Composable
private fun TagChip(text: String) {
    Surface(
        color = CreamLight,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = EspressoDeep,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SearchFilterPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    options: FilterOptions,
    selectedOrigin: String?,
    onOriginSelected: (String?) -> Unit,
    selectedRoast: String?,
    onRoastSelected: (String?) -> Unit,
    minRating: Float,
    onRatingChange: (Float) -> Unit,
    onClear: () -> Unit,
    focusRequester: FocusRequester
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.statusBarsPadding().fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Busca granos, marcas...") },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
            HorizontalDivider(color = BorderLight)

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)
            ) {
                Text("FILTRAR", style = MaterialTheme.typography.labelLarge, color = CaramelAccent)
                Spacer(Modifier.height(24.dp))

                PremiumFilterSection("ORIGEN", options.origins, selectedOrigin, onOriginSelected)
                PremiumFilterSection("TUESTE", options.roasts, selectedRoast, onRoastSelected)

                Text("PUNTUACIÓN MÍNIMA", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Slider(
                    value = minRating,
                    onValueChange = { onRatingChange(it.roundToInt().toFloat()) },
                    valueRange = 0f..5f,
                    steps = 4,
                    colors = SliderDefaults.colors(thumbColor = CaramelAccent, activeTrackColor = CaramelAccent)
                )
            }

            Surface(modifier = Modifier.fillMaxWidth().shadow(12.dp), color = Color.White) {
                Row(modifier = Modifier.padding(16.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        border = BorderStroke(1.dp, ErrorRed),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                    ) { Text("LIMPIAR", color = ErrorRed) }
                    
                    Button(
                        onClick = onClose,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EspressoDeep)
                    ) { Text("APLICAR", color = Color.White) }
                }
            }
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PremiumFilterSection(title: String, items: List<String>, selected: String?, onSelected: (String?) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                val isSelected = item == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelected(if (isSelected) null else item) },
                    label = { Text(item, fontSize = 11.sp) },
                    enabled = true,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CaramelAccent,
                        selectedLabelColor = Color.White,
                        containerColor = CreamLight,
                        labelColor = EspressoDeep
                    ),
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, borderColor = if(isSelected) CaramelAccent else BorderLight, borderWidth = 1.dp)
                )
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
