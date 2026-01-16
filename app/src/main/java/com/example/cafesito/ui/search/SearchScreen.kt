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
import com.example.cafesito.ui.theme.CoffeeBrown
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
    val syncError by viewModel.syncError.collectAsState()
    
    val selectedOrigin: String? by viewModel.selectedOrigin.collectAsState(initial = null)
    val selectedRoast: String? by viewModel.selectedRoast.collectAsState(initial = null)
    val selectedSpecialty: String? by viewModel.selectedSpecialty.collectAsState(initial = null)
    val selectedVariety: String? by viewModel.selectedVariety.collectAsState(initial = null)
    val selectedFormat: String? by viewModel.selectedFormat.collectAsState(initial = null)
    val minRating: Float by viewModel.minRating.collectAsState(initial = 0f)

    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    LaunchedEffect(syncError) {
        syncError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8F8F8),
        topBar = {
            if (!isSearchActive) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Explorar",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.Black
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFF8F8F8),
                        scrolledContainerColor = Color(0xFFF8F8F8)
                    ),
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.Black)
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            
            when (val state = uiState) {
                is SearchUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = CoffeeBrown) }
                is SearchUiState.Error -> ErrorMessage(state.message, modifier = Modifier.align(Alignment.Center))
                is SearchUiState.Success -> {
                    if (state.coffees.isEmpty() && !isRefreshing) {
                        EmptySearchResults(modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            itemsIndexed(state.coffees, key = { _, item -> item.coffee.id }) { index, coffeeDetails ->
                                LaunchedEffect(index) {
                                    viewModel.onItemDisplayed(index)
                                }
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    CoffeeListItem(
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

            if (isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = CoffeeBrown
                )
            }

            AnimatedVisibility(
                visible = isSearchActive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { isSearchActive = false }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                                }
                                TextField(
                                    value = searchQuery,
                                    onValueChange = viewModel::onSearchQueryChanged,
                                    placeholder = { Text("¿Qué café buscas hoy?") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(focusRequester),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = { 
                                            isSearchActive = false
                                            viewModel.onSearchQueryChanged("")
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = "Cerrar buscador")
                                        }
                                    }
                                )
                            }
                            HorizontalDivider(thickness = 0.5.dp)

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp)
                            ) {
                                Text("Filtrar por", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.height(16.dp))

                                FilterSection("País de Origen", options.origins, selectedOrigin, viewModel::setOrigin)
                                FilterSection("Tueste", options.roasts, selectedRoast, viewModel::setRoast)
                                FilterSection("Variedad", options.varieties, selectedVariety, viewModel::setVariety)
                                FilterSection("Especialidad", options.specialties, selectedSpecialty, viewModel::setSpecialty)
                                FilterSection("Formato", options.formats, selectedFormat, viewModel::setFormat)
                                
                                Spacer(Modifier.height(8.dp))
                                Text("Puntuación", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                Slider(
                                    value = minRating,
                                    onValueChange = { viewModel.setMinRating(it.roundToInt().toFloat()) },
                                    valueRange = 0f..5f,
                                    steps = 4,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    colors = SliderDefaults.colors(thumbColor = CoffeeBrown, activeTrackColor = CoffeeBrown)
                                )
                                Text(
                                    text = if (minRating > 0) "★ ${minRating.toInt()} o más" else "Cualquier puntuación",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .shadow(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .navigationBarsPadding(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.clearFilters() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Limpiar filtro")
                                }
                                
                                Button(
                                    onClick = { isSearchActive = false },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown)
                                ) {
                                    Text("Ver resultado", color = Color.White)
                                }
                            }
                        }
                    }
                }
                
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    title: String,
    options: List<String>,
    selected: String?,
    onSelected: (String?) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            if (options.size > 6) {
                Text(
                    text = if (isExpanded) "Ver menos" else "Ver más",
                    style = MaterialTheme.typography.labelMedium,
                    color = CoffeeBrown,
                    modifier = Modifier.clickable { isExpanded = !isExpanded }
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val displayOptions = if (isExpanded) options else options.take(6)
            
            displayOptions.forEach { option ->
                val isSelected = option == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelected(if (isSelected) null else option) },
                    label = { Text(option) },
                    leadingIcon = null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CoffeeBrown,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoffeeListItem(
    coffeeDetails: CoffeeWithDetails,
    onCoffeeClick: (String) -> Unit,
    onFavoriteClick: () -> Unit
) {
    val coffee = coffeeDetails.coffee
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onCoffeeClick(coffee.id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                AsyncImage(
                    model = coffee.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 450f
                        ))
                )
                Surface(
                    modifier = Modifier.padding(16.dp).align(Alignment.TopEnd).size(40.dp),
                    color = Color.White.copy(alpha = 0.9f),
                    shape = CircleShape
                ) {
                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            imageVector = if (coffeeDetails.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (coffeeDetails.isFavorite) Color.Red else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = coffee.marca, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                        Text(text = coffee.nombre, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Surface(color = Color.White.copy(alpha = 0.95f), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Opiniones", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(text = String.format(Locale.getDefault(), "%.1f", coffeeDetails.averageRating), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(20.dp)) {
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("País" to (coffee.paisOrigen ?: "N/A"), "Esp." to coffee.especialidad, "Var." to (coffee.variedadTipo ?: "N/A")).forEach { (label, value) ->
                        value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { subItem ->
                            TagChip(label = label, value = subItem)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagChip(label: String, value: String) {
    Surface(color = Color.White, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFEEEEEE))) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(text = "$label: ", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(text = value, style = MaterialTheme.typography.labelMedium, color = Color.DarkGray, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptySearchResults(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No hay cafés disponibles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Revisa tu conexión o intenta más tarde", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorMessage(message: String, modifier: Modifier = Modifier) {
    Text(text = message, color = MaterialTheme.colorScheme.error, modifier = modifier.padding(32.dp), textAlign = TextAlign.Center)
}
