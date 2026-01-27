package com.example.cafesito.ui.diary

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.ui.theme.CoffeeBrown
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPantryItemScreen(
    onBackClick: (String?) -> Unit,
    onlyActivity: Boolean = false,
    coffeeId: String? = null,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var specialty by remember { mutableStateOf("Arabica") }
    var roast by remember { mutableStateOf("Medio") }
    var variety by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("España") }
    var hasCaffeine by remember { mutableStateOf(true) }
    var format by remember { mutableStateOf("Grano") }
    var grams by remember { mutableStateOf("250") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var existingImageUrl by remember { mutableStateOf("") }

    val coffees by viewModel.availableCoffees.collectAsState()
    val pantryItems by viewModel.pantryItems.collectAsState()

    LaunchedEffect(coffeeId, coffees, pantryItems) {
        if (coffeeId != null) {
            val foundInCoffees = coffees.find { it.coffee.id == coffeeId }
            val foundInPantry = pantryItems.find { it.coffee.id == coffeeId }
            
            val details = foundInCoffees ?: foundInPantry?.let { CoffeeWithDetails(it.coffee, null, emptyList()) }
            
            details?.let { d ->
                val c = d.coffee
                name = c.nombre
                brand = c.marca
                specialty = c.especialidad
                roast = c.tueste
                variety = c.variedadTipo ?: ""
                country = c.paisOrigen ?: "España"
                hasCaffeine = c.cafeina == "Sí"
                format = c.formato
                existingImageUrl = c.imageUrl
                
                foundInPantry?.let {
                    grams = it.pantryItem.totalGrams.toString()
                }
            }
        }
    }

    var countryExpanded by remember { mutableStateOf(false) }
    val countries = remember { 
        Locale.getISOCountries().map { Locale.Builder().setRegion(it).build().getDisplayCountry(Locale.forLanguageTag("es")) }.sorted() 
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    val isFormValid = name.isNotBlank() && brand.isNotBlank() && (imageUri != null || existingImageUrl.isNotBlank()) && grams.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (coffeeId != null) "Editar Café" else "Nuevo Café", fontWeight = FontWeight.Bold, color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = { onBackClick(null) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F8F8))
            )
        },
        containerColor = Color(0xFFF8F8F8)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // IDENTIDAD
            item {
                FormSectionCard {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFFF5F5F5))
                                .clickable { launcher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUri != null) {
                                AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else if (existingImageUrl.isNotBlank()) {
                                AsyncImage(model = existingImageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = CoffeeBrown, modifier = Modifier.size(44.dp))
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nombre del café") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = { Text("Marca") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // PERFIL Y ORIGEN
            item {
                FormSectionCard(title = "Perfil y Origen") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Especialidad", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            VisualOptionTile(
                                label = "Arabica",
                                icon = Icons.Default.Eco,
                                isSelected = specialty == "Arabica",
                                modifier = Modifier.weight(1f),
                                onClick = { specialty = "Arabica" }
                            )
                            VisualOptionTile(
                                label = "Mezcla",
                                icon = Icons.Default.Grain,
                                isSelected = specialty == "Mezcla",
                                modifier = Modifier.weight(1f),
                                onClick = { specialty = "Mezcla" }
                            )
                        }

                        Text("Tueste", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                Triple("Ligero", Icons.Default.LocalFireDepartment, 0.6f),
                                Triple("Medio", Icons.Default.Whatshot, 0.8f),
                                Triple("Medio-Oscuro", Icons.Default.Fireplace, 1f)
                            ).forEach { (label, icon, _) ->
                                VisualOptionTile(
                                    label = label,
                                    icon = icon,
                                    isSelected = roast == label,
                                    modifier = Modifier.weight(1f),
                                    onClick = { roast = label }
                                )
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = countryExpanded,
                            onExpandedChange = { countryExpanded = !countryExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = country,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("País") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = countryExpanded,
                                onDismissRequest = { countryExpanded = false }
                            ) {
                                countries.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption) },
                                        onClick = {
                                            country = selectionOption
                                            countryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // FORMATO Y STOCK
            item {
                FormSectionCard(title = "Formato y Despensa") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("¿Tiene cafeína?", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Switch(
                                checked = hasCaffeine, 
                                onCheckedChange = { hasCaffeine = it }, 
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CoffeeBrown,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.LightGray,
                                    uncheckedBorderColor = Color.Transparent
                                )
                            )
                        }

                        Text("Presentación", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Grano", "Molido", "Capsula").forEach { option ->
                                VisualOptionTile(
                                    label = option,
                                    icon = if(option == "Capsula") Icons.Default.Inventory2 else Icons.Default.BlurOn,
                                    isSelected = format == option,
                                    modifier = Modifier.weight(1f),
                                    onClick = { format = option }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = grams,
                            onValueChange = { if (it.all { c -> c.isDigit() }) grams = it },
                            label = { Text("Peso total de la bolsa (g)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }
                }
            }

            // BOTÓN GUARDAR / ACTUALIZAR
            item {
                Button(
                    onClick = { 
                        val onSuccessNavigation = if (onlyActivity) "pantry" else null
                        if (coffeeId != null) {
                            viewModel.updateCustomCoffee(
                                id = coffeeId,
                                name = name,
                                brand = brand,
                                specialty = specialty,
                                roast = roast,
                                variety = variety,
                                country = country,
                                hasCaffeine = hasCaffeine,
                                format = format,
                                totalGrams = grams.toIntOrNull() ?: 250,
                                imageUri = imageUri,
                                onSuccess = { onBackClick(onSuccessNavigation) }
                            )
                        } else {
                            viewModel.saveCustomCoffee(
                                name = name,
                                brand = brand,
                                specialty = specialty,
                                roast = roast,
                                variety = variety,
                                country = country,
                                hasCaffeine = hasCaffeine,
                                format = format,
                                totalGrams = grams.toIntOrNull() ?: 250,
                                imageUri = imageUri,
                                onSuccess = { onBackClick(onSuccessNavigation) }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoffeeBrown,
                        disabledContainerColor = Color.LightGray,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (coffeeId != null) "GUARDAR CAMBIOS" else if (onlyActivity) "AÑADIR A MI DESPENSA" else "DAR DE ALTA CAFÉ",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
fun FormSectionCard(title: String? = null, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(Modifier.padding(20.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = Color.Black, modifier = Modifier.padding(bottom = 16.dp))
            }
            content()
        }
    }
}

@Composable
fun VisualOptionTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(75.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) CoffeeBrown.copy(alpha = 0.1f) else Color(0xFFFBFBFB),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) CoffeeBrown else Color(0xFFEEEEEE))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = if (isSelected) CoffeeBrown else Color.Gray, modifier = Modifier.size(24.dp))
            Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) CoffeeBrown else Color.Gray, textAlign = TextAlign.Center)
        }
    }
}
