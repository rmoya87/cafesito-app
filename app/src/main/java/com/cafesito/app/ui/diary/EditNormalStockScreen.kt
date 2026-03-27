package com.cafesito.app.ui.diary

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cafesito.app.R
import com.cafesito.app.data.CoffeeWithDetails
import com.cafesito.app.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNormalStockScreen(
    coffeeId: String,
    onBackClick: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val isSpanish = remember { Locale.getDefault().language.startsWith("es") }
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
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        return
    }

    val coffee = coffeeDetails.coffee
    val scrollState = rememberLazyListState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // CABECERA INMERSIVA ESTILO EXPLORAR
            Box(modifier = Modifier.fillMaxWidth().height(400.dp).graphicsLayer {
                translationY = -scrollState.firstVisibleItemScrollOffset * 0.4f
                alpha = 1f - (scrollState.firstVisibleItemScrollOffset / 800f).coerceIn(0f, 1f)
            }) {
                AsyncImage(model = coffee.imageUrl, contentDescription = coffee.nombre, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, PureBlack.copy(alpha = 0.7f)), startY = 600f)))
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 48.dp, end = 120.dp)) {
                    Text(text = coffee.marca.uppercase(), color = PureWhite.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                    Text(text = coffee.nombre, color = PureWhite, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
                }
                Surface(modifier = Modifier.padding(end = 24.dp, bottom = 48.dp).align(Alignment.BottomEnd), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), shape = Shapes.card) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (isSpanish) "Nota Media" else "Average rating", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = String.format(Locale.getDefault(), "%.1f", coffeeDetails.averageRating), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize()) {
                item { Spacer(Modifier.height(360.dp)) }
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(), 
                        shape = Shapes.sheetLarge, 
                        color = MaterialTheme.colorScheme.background,
                        tonalElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            // SECCIÓN DE EDICIÓN DE STOCK (PREMIUM)
                            Text(if (isSpanish) "Mi Stock" else "My stock", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(
                                value = gramsRemaining,
                                onValueChange = { if (it.all { c -> c.isDigit() } || it.isEmpty()) gramsRemaining = it },
                                label = { Text(if (isSpanish) "Gramos restantes en la bolsa (g)" else "Remaining grams in bag (g)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = Shapes.card,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                trailingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.portafiltro),
                                        contentDescription = if (isSpanish) "Gramos" else "Grams",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    viewModel.addToPantry(coffeeId, gramsRemaining.toIntOrNull() ?: 0)
                                    onBackClick()
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = Shapes.card,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(if (isSpanish) "ACTUALIZAR STOCK" else "UPDATE STOCK", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary)
                            }

                            Spacer(Modifier.height(32.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.height(32.dp))

                            // DESCRIPCIÓN
                            Text(if (isSpanish) "Descripción" else "Description", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = coffee.descripcion, 
                                style = MaterialTheme.typography.bodyMedium, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (isDescExpanded) Int.MAX_VALUE else 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.animateContentSize()
                            )
                            Text(
                                text = if (isSpanish) { if (isDescExpanded) "Leer menos" else "Leer más" } else { if (isDescExpanded) "Read less" else "Read more" },
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp).clickable { isDescExpanded = !isDescExpanded }
                            )

                            Spacer(Modifier.height(32.dp))

                            // CARACTERÍSTICAS
                            Text(if (isSpanish) "Características" else "Characteristics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(16.dp))
                            listOf(
                                (if (isSpanish) "Aroma" else "Aroma") to coffee.aroma,
                                (if (isSpanish) "Sabor" else "Flavor") to coffee.sabor,
                                (if (isSpanish) "Cuerpo" else "Body") to coffee.cuerpo,
                                (if (isSpanish) "Acidez" else "Acidity") to coffee.acidez
                            ).forEach { (label, value) ->
                                Column {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                        Text("${value.toInt()}/10", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    LinearProgressIndicator(
                                        progress = { value / 10f },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.outline
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
                modifier = Modifier.statusBarsPadding().padding(16.dp).size(44.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = if (isSpanish) "Volver" else "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
