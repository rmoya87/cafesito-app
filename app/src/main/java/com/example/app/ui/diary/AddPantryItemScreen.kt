package com.cafesito.app.ui.diary

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.cafesito.app.data.CoffeeWithDetails
import com.cafesito.app.ui.theme.*
import com.cafesito.app.ui.components.*
import java.io.File
import java.util.Locale
import java.util.UUID

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

    val context = androidx.compose.ui.platform.LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showImagePickerSheet by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) imageUri = pendingCameraUri
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) imageUri = uri
    }

    val isFormValid = name.isNotBlank() && brand.isNotBlank() && (imageUri != null || existingImageUrl.isNotBlank()) && grams.isNotEmpty()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (coffeeId != null) "EDITAR CAFÉ" else "NUEVO CAFÉ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = { onBackClick(null) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    TextButton(
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
                        enabled = isFormValid
                    ) {
                        Text(
                            text = "AÑADIR",
                            fontWeight = FontWeight.Bold,
                            color = if (isFormValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
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
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { showImagePickerSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUri != null) {
                                AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                            } else if (existingImageUrl.isNotBlank()) {
                                AsyncImage(model = existingImageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                            } else {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nombre del café") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = { Text("Marca") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            // PERFIL Y ORIGEN
            item {
                FormSectionCard(title = "Perfil y Origen") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Especialidad", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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

                        Text("Tueste", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                                country,
                                {},
                                readOnly = true,
                                label = { Text("País") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(focusedBorderColor = MaterialTheme.colorScheme.primary)
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
                            Text("¿Tiene cafeína?", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = hasCaffeine, 
                                onCheckedChange = { hasCaffeine = it }, 
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                                    uncheckedBorderColor = Color.Transparent
                                )
                            )
                        }

                        Text("Presentación", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Grano", "Molido", "Capsula").forEach { option ->
                                VisualOptionTile(
                                    label = option,
                                    icon = when(option) {
                                        "Grano" -> Icons.Default.SportsRugby
                                        "Molido" -> Icons.Default.Grain
                                        else -> Icons.Default.AutoMode
                                    },
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
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
        }
    }

    if (showImagePickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImagePickerSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(Modifier.padding(bottom = 40.dp, start = 24.dp, end = 24.dp)) {
                Text(
                    text = "AÑADIR FOTO",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp),
                    fontWeight = FontWeight.Bold
                )
                ModalMenuOption(
                    title = "Hacer Foto",
                    icon = Icons.Default.PhotoCamera,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = {
                        val uri = createTempImageUri(context)
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                        showImagePickerSheet = false
                    }
                )
                ModalMenuOption(
                    title = "Elegir de Galería",
                    icon = Icons.Default.Collections,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = {
                        galleryLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        showImagePickerSheet = false
                    }
                )
            }
        }
    }
}

@Composable
fun FormSectionCard(title: String? = null, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(20.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 16.dp))
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
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
            Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = tint, textAlign = TextAlign.Center)
        }
    }
}

private fun createTempImageUri(context: Context): Uri {
    val file = File(context.cacheDir, "custom_${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
