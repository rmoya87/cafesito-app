package com.example.cafesito.ui.diary

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.ui.theme.CoffeeBrown
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNormalStockScreen(
    coffeeId: String,
    onBackClick: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val coffees by viewModel.availableCoffees.collectAsState()
    val coffeeDetails = remember(coffees) { coffees.find { it.coffee.id == coffeeId } }
    
    val pantryItems by viewModel.pantryItems.collectAsState()
    val currentPantry = remember(pantryItems) { pantryItems.find { it.coffee.id == coffeeId } }

    var gramsRemaining by remember { mutableStateOf("") }
    var isDescExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(currentPantry) {
        currentPantry?.let { gramsRemaining = it.pantryItem.gramsRemaining.toString() }
    }

    if (coffeeDetails == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = CoffeeBrown) }
        return
    }

    val coffee = coffeeDetails.coffee
    val scrollState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // CABECERA INMERSIVA ESTILO EXPLORAR
        Box(modifier = Modifier.fillMaxWidth().height(400.dp).graphicsLayer {
            translationY = -scrollState.firstVisibleItemScrollOffset * 0.4f
            alpha = 1f - (scrollState.firstVisibleItemScrollOffset / 800f).coerceIn(0f, 1f)
        }) {
            AsyncImage(model = coffee.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)), startY = 600f)))
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 48.dp, end = 120.dp)) {
                Text(text = coffee.marca, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                Text(text = coffee.nombre, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
            }
            Surface(modifier = Modifier.padding(end = 24.dp, bottom = 48.dp).align(Alignment.BottomEnd), color = Color.White.copy(alpha = 0.95f), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nota Media", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(text = String.format(Locale.getDefault(), "%.1f", coffeeDetails.averageRating), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize()) {
            item { Spacer(Modifier.height(360.dp)) }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), 
                    color = Color.White,
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        // SECCIÓN DE EDICIÓN DE STOCK (PREMIUM)
                        Text("Mi Stock", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = gramsRemaining,
                            onValueChange = { if (it.all { c -> c.isDigit() }) gramsRemaining = it },
                            label = { Text("Gramos restantes en la bolsa (g)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = { Icon(Icons.Default.Scale, null, tint = CoffeeBrown) },
                            singleLine = true
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.addToPantry(coffeeId, gramsRemaining.toIntOrNull() ?: 0)
                                onBackClick()
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown)
                        ) {
                            Text("ACTUALIZAR STOCK", fontWeight = FontWeight.ExtraBold)
                        }

                        Spacer(Modifier.height(32.dp))
                        HorizontalDivider(thickness = 0.5.dp)
                        Spacer(Modifier.height(32.dp))

                        // DESCRIPCIÓN
                        Text("Descripción", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = coffee.descripcion, 
                            style = MaterialTheme.typography.bodyMedium, 
                            color = Color.Gray,
                            maxLines = if (isDescExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.animateContentSize()
                        )
                        Text(
                            text = if (isDescExpanded) "Leer menos" else "Leer más",
                            color = CoffeeBrown,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp).clickable { isDescExpanded = !isDescExpanded }
                        )

                        Spacer(Modifier.height(32.dp))

                        // CARACTERÍSTICAS
                        Text("Características", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        listOf("Aroma" to coffee.aroma, "Sabor" to coffee.sabor, "Cuerpo" to coffee.cuerpo, "Acidez" to coffee.acidez).forEach { (label, value) ->
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(label, style = MaterialTheme.typography.bodyMedium)
                                    Text("${value.toInt()}/10", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                LinearProgressIndicator(
                                    progress = { value / 10f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                    color = CoffeeBrown,
                                    trackColor = Color(0xFFF5F5F5)
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                        
                        Spacer(Modifier.height(100.dp))
                    }
                }
            }
        }

        // Botón atrás
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.statusBarsPadding().padding(16.dp).size(44.dp).background(Color.White.copy(alpha = 0.9f), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
        }
    }
}
