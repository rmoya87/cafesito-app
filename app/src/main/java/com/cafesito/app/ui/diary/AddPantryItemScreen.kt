package com.cafesito.app.ui.diary

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.cafesito.app.ui.theme.DisabledGray
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cafesito.app.data.CoffeeWithDetails
import com.cafesito.app.ui.theme.*
import com.cafesito.app.ui.components.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.io.File
import java.util.Locale
import java.util.UUID
import com.cafesito.app.ui.components.toCoffeeBrandFormat
import com.cafesito.app.ui.components.toCoffeeNameFormat

private val SPECIALTY_OPTIONS = listOf("Arabica", "Mezcla")
private val ROAST_OPTIONS = listOf("Ligero", "Medio", "Medio-Oscuro")
private val FORMAT_OPTIONS = listOf("Grano", "Molido", "Capsula")
private val PROCESS_OPTIONS = listOf("Natural", "Lavado", "Honey", "Semi-lavado", "Otro")
private val GRIND_OPTIONS = listOf("Molido fino", "Molido medio", "Molido grueso", "Grano entero")
private val VARIETY_OPTIONS = listOf("Geisha", "Caturra", "Arábica 100%", "Robusta", "Bourbon", "Typica", "Maragogype", "Pacamara", "Otro")
private const val SENSORY_MIN = 0
private const val SENSORY_MAX = 5
private val SENSORY_LABELS = mapOf(
    "aroma" to "Aroma", "sabor" to "Sabor", "cuerpo" to "Cuerpo",
    "acidez" to "Acidez", "dulzura" to "Dulzura"
)

private fun parseMultiValue(s: String): List<String> =
    s.split(",").map { it.trim() }.filter { it.isNotEmpty() }
private fun formatMultiValue(list: List<String>): String = list.joinToString(", ")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddPantryItemScreen(
    onBackClick: (String?) -> Unit,
    onlyActivity: Boolean = false,
    diaryEntryFlow: Boolean = false,
    brewLabFlow: Boolean = false,
    onCoffeeCreatedForDiary: ((String) -> Unit)? = null,
    onCoffeeCreatedForBrewLab: ((String) -> Unit)? = null,
    coffeeId: String? = null,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var specialty by remember { mutableStateOf("") }
    var roast by remember { mutableStateOf("") }
    var variety by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var hasCaffeine by remember { mutableStateOf(true) }
    var format by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("250") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var existingImageUrl by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var proceso by remember { mutableStateOf("") }
    var moliendaRecomendada by remember { mutableStateOf("") }
    var codigoBarras by remember { mutableStateOf("") }
    var productUrl by remember { mutableStateOf("") }
    var brandDropdownExpanded by remember { mutableStateOf(false) }
    var pickerOpen by remember { mutableStateOf<String?>(null) }
    var tempMultiSelection by remember { mutableStateOf<List<String>>(emptyList()) }
    var aroma by remember { mutableStateOf(0) }
    var sabor by remember { mutableStateOf(0) }
    var cuerpo by remember { mutableStateOf(0) }
    var acidez by remember { mutableStateOf(0) }
    var dulzura by remember { mutableStateOf(0) }

    val coffees by viewModel.availableCoffees.collectAsState()
    val pantryItems by viewModel.pantryItems.collectAsState()

    LaunchedEffect(coffeeId, coffees, pantryItems) {
        if (coffeeId != null) {
            val foundInCoffees = coffees.find { it.coffee.id == coffeeId }
            val foundInPantry = pantryItems.find { it.coffee.id == coffeeId }
            
            val details = foundInCoffees ?: foundInPantry?.let { CoffeeWithDetails(it.coffee, null, emptyList()) }
            
            details?.let { d ->
                val c = d.coffee
                name = c.nombre.toCoffeeNameFormat()
                brand = c.marca.trim().let { raw -> if (raw.isBlank()) "" else raw.replaceFirstChar { it.uppercase() } + raw.drop(1).lowercase() }
                specialty = c.especialidad ?: ""
                roast = c.tueste
                variety = c.variedadTipo ?: ""
                country = c.paisOrigen ?: ""
                hasCaffeine = c.cafeina == "Sí"
                format = c.formato
                existingImageUrl = c.imageUrl
                descripcion = c.descripcion ?: ""
                proceso = c.proceso ?: ""
                moliendaRecomendada = c.moliendaRecomendada ?: ""
                codigoBarras = c.codigoBarras ?: ""
                productUrl = c.productUrl ?: ""
                aroma = c.aroma.toInt().coerceIn(SENSORY_MIN, SENSORY_MAX).takeIf { c.aroma > 0f } ?: 0
                sabor = c.sabor.toInt().coerceIn(SENSORY_MIN, SENSORY_MAX).takeIf { c.sabor > 0f } ?: 0
                cuerpo = c.cuerpo.toInt().coerceIn(SENSORY_MIN, SENSORY_MAX).takeIf { c.cuerpo > 0f } ?: 0
                acidez = c.acidez.toInt().coerceIn(SENSORY_MIN, SENSORY_MAX).takeIf { c.acidez > 0f } ?: 0
                dulzura = c.dulzura.toInt().coerceIn(SENSORY_MIN, SENSORY_MAX).takeIf { c.dulzura > 0f } ?: 0
                foundInPantry?.let {
                    grams = it.pantryItem.totalGrams.toString()
                }
            }
        }
    }

    var countryExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(pickerOpen) {
        when (pickerOpen) {
            "country" -> tempMultiSelection = parseMultiValue(country)
            "variety" -> tempMultiSelection = parseMultiValue(variety)
            "process" -> tempMultiSelection = parseMultiValue(proceso)
            else -> { }
        }
    }
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val fieldBackground = if (isDark) PureBlack else PureWhite
    val noBorderColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = Color.Transparent,
        focusedBorderColor = Color.Transparent,
        unfocusedContainerColor = fieldBackground,
        focusedContainerColor = fieldBackground
    )
    val countries = remember { 
        Locale.getISOCountries().map { Locale.Builder().setRegion(it).build().getDisplayCountry(Locale.forLanguageTag("es")) }.sorted() 
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showImagePickerSheet by remember { mutableStateOf(false) }

    // Solo cámara; galería vía Photo Picker (sin permisos READ_MEDIA_*)
    val permissions = remember { listOf(Manifest.permission.CAMERA) }
    val permissionState = rememberMultiplePermissionsState(permissions)

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) imageUri = pendingCameraUri
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) imageUri = uri
    }

    val customFlow = diaryEntryFlow || brewLabFlow
    val gramsNum = grams.toIntOrNull() ?: 0
    val quantityValid = diaryEntryFlow || (grams.isNotBlank() && gramsNum in 1..5000)
    val barcodeValid = codigoBarras.isBlank() || codigoBarras.replace(" ", "").matches(Regex("^[0-9]{6,}$"))
    val urlValid = productUrl.isBlank() || productUrl.trim().matches(Regex("^https?://.+", RegexOption.IGNORE_CASE))
    val isFormValid = name.isNotBlank() && brand.isNotBlank() && (imageUri != null || existingImageUrl.isNotBlank()) && quantityValid && barcodeValid && urlValid

    val brandSuggestions = remember(coffees, brand) {
        val byKey = mutableMapOf<String, String>()
        coffees.forEach { c ->
            val raw = c.coffee.marca.trim()
            if (raw.isEmpty()) return@forEach
            val key = raw.lowercase()
            if (key in byKey) return@forEach
            byKey[key] = raw.replaceFirstChar { it.uppercase() } + raw.drop(1).lowercase()
        }
        val list = byKey.values.sortedWith(java.text.Collator.getInstance(java.util.Locale("es")))
        val q = brand.trim().lowercase()
        if (q.isEmpty()) list else list.filter { it.lowercase().contains(q) }.take(8)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (coffeeId != null) "Editar café" else if (customFlow || brewLabFlow) "Crea tu café" else "Nuevo café", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
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
                                    totalGrams = grams.toIntOrNull()?.coerceIn(1, 5000) ?: 250,
                                    imageUri = imageUri,
                                    onSuccess = { onBackClick(onSuccessNavigation) },
                                    descripcion = descripcion.ifBlank { null },
                                    proceso = proceso.ifBlank { null },
                                    codigoBarras = codigoBarras.ifBlank { null },
                                    moliendaRecomendada = moliendaRecomendada.ifBlank { null },
                                    productUrl = productUrl.ifBlank { null },
                                    aroma = aroma.toFloat(), sabor = sabor.toFloat(), cuerpo = cuerpo.toFloat(), acidez = acidez.toFloat(), dulzura = dulzura.toFloat()
                                )
                            } else if (customFlow) {
                                viewModel.saveCustomCoffeeForDiary(
                                    name = name,
                                    brand = brand,
                                    specialty = specialty,
                                    roast = roast,
                                    variety = variety,
                                    country = country,
                                    hasCaffeine = hasCaffeine,
                                    format = format,
                                    imageUri = imageUri,
                                    totalGrams = grams.toIntOrNull()?.coerceIn(1, 5000) ?: 250,
                                    onSuccess = { createdCoffeeId ->
                                        if (diaryEntryFlow) onCoffeeCreatedForDiary?.invoke(createdCoffeeId)
                                        if (brewLabFlow) onCoffeeCreatedForBrewLab?.invoke(createdCoffeeId)
                                        onBackClick(
                                            when {
                                                diaryEntryFlow -> "pantry_loading"
                                                brewLabFlow -> "brewlab"
                                                else -> null
                                            }
                                        )
                                    },
                                    descripcion = descripcion.ifBlank { null },
                                    proceso = proceso.ifBlank { null },
                                    codigoBarras = codigoBarras.ifBlank { null },
                                    moliendaRecomendada = moliendaRecomendada.ifBlank { null },
                                    productUrl = productUrl.ifBlank { null },
                                    aroma = aroma.toFloat(), sabor = sabor.toFloat(), cuerpo = cuerpo.toFloat(), acidez = acidez.toFloat(), dulzura = dulzura.toFloat()
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
                                    totalGrams = grams.toIntOrNull()?.coerceIn(1, 5000) ?: 250,
                                    imageUri = imageUri,
                                    onSuccess = { onBackClick(onSuccessNavigation) },
                                    descripcion = descripcion.ifBlank { null },
                                    proceso = proceso.ifBlank { null },
                                    codigoBarras = codigoBarras.ifBlank { null },
                                    moliendaRecomendada = moliendaRecomendada.ifBlank { null },
                                    productUrl = productUrl.ifBlank { null },
                                    aroma = aroma.toFloat(), sabor = sabor.toFloat(), cuerpo = cuerpo.toFloat(), acidez = acidez.toFloat(), dulzura = dulzura.toFloat()
                                )
                            }
                        },
                        enabled = isFormValid,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = DisabledGray
                        ),
                        border = null
                    ) {
                        Text(
                            text = "Guardar",
                            fontWeight = FontWeight.SemiBold
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
            // Hero: foto a la izquierda (cuadrado), nombre y tostador a la derecha (sin título en card)
            item {
                FormSectionCard(title = null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(fieldBackground)
                                .clickable { showImagePickerSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUri != null || existingImageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = imageUri ?: existingImageUrl,
                                    contentDescription = "Imagen del café",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) PureWhite else PureBlack)
                                        .clickable {
                                            imageUri = null
                                            existingImageUrl = ""
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Quitar foto",
                                        tint = if (isDark) PureBlack else PureWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = "Añadir foto", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("Añadir foto", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it.toCoffeeNameFormat() },
                                placeholder = { Text("Nombre del café *") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = Shapes.cardSmall,
                                singleLine = true,
                                colors = noBorderColors
                            )
                            Spacer(Modifier.height(12.dp))
                            ExposedDropdownMenuBox(
                                expanded = brandDropdownExpanded,
                                onExpandedChange = { brandDropdownExpanded = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = brand,
                                    onValueChange = { v -> brand = v; brandDropdownExpanded = v.isNotBlank() },
                                    placeholder = { Text("Tostador") },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, brandDropdownExpanded)
                                        .fillMaxWidth(),
                                    shape = Shapes.cardSmall,
                                    singleLine = true,
                                    colors = noBorderColors
                                )
                                ExposedDropdownMenu(
                                    expanded = brandDropdownExpanded,
                                    onDismissRequest = { brandDropdownExpanded = false }
                                ) {
                                    Text("Tostadores sugeridos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                                    brandSuggestions.forEach { suggestion ->
                                        DropdownMenuItem(
                                            text = { Text(suggestion) },
                                            onClick = {
                                                brand = suggestion
                                                brandDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Origen y perfil — título fuera, menos espacio con la card de abajo
            item {
                Text("Origen y perfil", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
            }
            item {
                FormSectionCard(title = null) {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        PickerTriggerRow(value = specialty, placeholder = "Seleccionar especialidad", onClick = { pickerOpen = "specialty" })
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        Spacer(Modifier.height(12.dp))
                        PickerTriggerRow(value = roast, placeholder = "Seleccionar tueste", onClick = { pickerOpen = "roast" })
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        Spacer(Modifier.height(12.dp))
                        PickerTriggerRow(value = formatMultiValue(parseMultiValue(country)), placeholder = "Seleccionar país(es)", onClick = { pickerOpen = "country" })
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        Spacer(Modifier.height(12.dp))
                        PickerTriggerRow(value = formatMultiValue(parseMultiValue(variety)), placeholder = "Seleccionar variedad(es)", onClick = { pickerOpen = "variety" })
                    }
                }
            }

            // Presentación — título fuera
            item {
                Text("Presentación", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
            }
            item {
                FormSectionCard(title = null) {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("¿Tiene cafeína?", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
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
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        Spacer(Modifier.height(12.dp))
                        PickerTriggerRow(value = format, placeholder = "Seleccionar formato", onClick = { pickerOpen = "format" })
                        if (!diaryEntryFlow) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cantidad (g)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                BasicTextField(
                                    value = grams,
                                    onValueChange = { input -> if (input.isEmpty() || input.all { c -> c.isDigit() }) grams = input },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.End
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.widthIn(min = 80.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Detalles opcionales — título y badge fuera
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Detalles opcionales", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text("Opcional", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            item {
                FormSectionCard(title = null) {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        OutlinedTextField(
                            value = descripcion,
                            onValueChange = { descripcion = it },
                            placeholder = { Text("Descripción") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = Shapes.cardSmall,
                            minLines = 3,
                            colors = noBorderColors
                        )
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        Spacer(Modifier.height(12.dp))
                        PickerTriggerRow(value = formatMultiValue(parseMultiValue(proceso)), placeholder = "Seleccionar proceso(s)", onClick = { pickerOpen = "process" })
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        Spacer(Modifier.height(12.dp))
                        PickerTriggerRow(value = moliendaRecomendada, placeholder = "Seleccionar molienda", onClick = { pickerOpen = "grind" })
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = codigoBarras,
                            onValueChange = { codigoBarras = it },
                            placeholder = { Text("Código de barras") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = Shapes.cardSmall,
                            singleLine = true,
                            isError = codigoBarras.isNotBlank() && !barcodeValid,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = noBorderColors
                        )
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = productUrl,
                            onValueChange = { productUrl = it },
                            placeholder = { Text("Enlace al producto") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = Shapes.cardSmall,
                            singleLine = true,
                            isError = productUrl.isNotBlank() && !urlValid,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            colors = noBorderColors
                        )
                    }
                }
            }

            // Perfil sensorial — título y badge fuera
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Perfil sensorial", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text("Opcional", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            item {
                FormSectionCard(title = null) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Valoración del 1 al 5", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        listOf(
                            "aroma" to aroma, "sabor" to sabor, "cuerpo" to cuerpo,
                            "acidez" to acidez, "dulzura" to dulzura
                        ).forEach { (key, value) ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(SENSORY_LABELS[key] ?: key, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("$value", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Slider(
                                    value = value.toFloat(),
                                    onValueChange = { when (key) {
                                        "aroma" -> aroma = it.toInt().coerceIn(SENSORY_MIN, SENSORY_MAX)
                                        "sabor" -> sabor = it.toInt().coerceIn(SENSORY_MIN, SENSORY_MAX)
                                        "cuerpo" -> cuerpo = it.toInt().coerceIn(SENSORY_MIN, SENSORY_MAX)
                                        "acidez" -> acidez = it.toInt().coerceIn(SENSORY_MIN, SENSORY_MAX)
                                        "dulzura" -> dulzura = it.toInt().coerceIn(SENSORY_MIN, SENSORY_MAX)
                                        else -> { }
                                    } },
                                    valueRange = SENSORY_MIN.toFloat()..SENSORY_MAX.toFloat(),
                                    steps = SENSORY_MAX - SENSORY_MIN,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = if (isDark) CaramelSoft else CaramelAccent,
                                        inactiveTrackColor = if (isDark) SliderTrackInactiveDark else SliderTrackInactiveLight
                                    )
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
        
        if (showImagePickerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showImagePickerSheet = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                scrimColor = ScrimDefault
            ) {
                Column(Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp, bottom = 40.dp)) {
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
                            if (permissionState.allPermissionsGranted) {
                                val uri = createTempImageUri(context)
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                                showImagePickerSheet = false
                            } else {
                                permissionState.launchMultiplePermissionRequest()
                            }
                        }
                    )
                    ModalMenuOption(
                        title = "Elegir de Galería",
                        icon = Icons.Default.Collections,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = {
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            showImagePickerSheet = false
                        }
                    )
                }
            }
        }

        // Modal de opciones (especialidad, tueste, país, variedad, formato, proceso, molienda)
        val pickerId = pickerOpen
        if (pickerId != null) {
            val (title, options, isMulti) = when (pickerId) {
                "specialty" -> Triple("Especialidad", SPECIALTY_OPTIONS, false)
                "roast" -> Triple("Tueste", ROAST_OPTIONS, false)
                "country" -> Triple("País(es)", countries, true)
                "variety" -> Triple("Variedad(es)", VARIETY_OPTIONS, true)
                "format" -> Triple("Formato", FORMAT_OPTIONS, false)
                "process" -> Triple("Proceso", PROCESS_OPTIONS, true)
                "grind" -> Triple("Molienda recomendada", GRIND_OPTIONS, false)
                else -> Triple("", emptyList(), false)
            }
            val optionBg = if (isDark) PureBlack else PureWhite
            ModalBottomSheet(
                onDismissRequest = { pickerOpen = null },
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                scrimColor = ScrimDefault
            ) {
                Column(Modifier.padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 20.dp)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        if (isMulti) {
                            TextButton(
                                onClick = {
                                    when (pickerId) {
                                        "country" -> country = formatMultiValue(tempMultiSelection)
                                        "variety" -> variety = formatMultiValue(tempMultiSelection)
                                        "process" -> proceso = formatMultiValue(tempMultiSelection)
                                        else -> { }
                                    }
                                    pickerOpen = null
                                },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) { Text("Aplicar") }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    if (isMulti) {
                        options.forEach { opt ->
                            val isSelected = opt in tempMultiSelection
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        tempMultiSelection = if (opt in tempMultiSelection) tempMultiSelection - opt else tempMultiSelection + opt
                                    },
                                shape = Shapes.cardSmall,
                                color = optionBg,
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            tempMultiSelection = if (opt in tempMultiSelection) tempMultiSelection - opt else tempMultiSelection + opt
                                        }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(opt, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    } else {
                        options.forEach { opt ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        when (pickerId) {
                                            "specialty" -> specialty = opt
                                            "roast" -> roast = opt
                                            "format" -> format = opt
                                            "grind" -> moliendaRecomendada = opt
                                            else -> { }
                                        }
                                        pickerOpen = null
                                    },
                                shape = Shapes.cardSmall,
                                color = optionBg,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        opt,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun PickerTriggerRow(
    value: String,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.cardSmall,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (value.isNotBlank()) value else placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = if (value.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Abrir",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FormSectionCard(title: String? = null, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = Shapes.pill,
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
        shape = Shapes.card,
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
            Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = tint, textAlign = TextAlign.Center)
        }
    }
}

private fun createTempImageUri(context: Context): Uri {
    val file = File(context.cacheDir, "custom_${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
