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
import androidx.compose.ui.res.stringResource
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
import android.os.Bundle
import androidx.core.os.bundleOf
import com.cafesito.app.R
import com.cafesito.app.ui.components.toCoffeeNameFormat

private val SPECIALTY_OPTIONS = listOf("Arabica", "Mezcla")
private val ROAST_OPTIONS = listOf("Ligero", "Medio", "Medio-Oscuro")
private val FORMAT_OPTIONS = listOf("Grano", "Molido", "Capsula")
private val PROCESS_OPTIONS = listOf("Natural", "Lavado", "Honey", "Semi-lavado", "Otro")
private val GRIND_OPTIONS = listOf("Molido fino", "Molido medio", "Molido grueso", "Grano entero")
private val VARIETY_OPTIONS = listOf("Geisha", "Caturra", "Arábica 100%", "Robusta", "Bourbon", "Typica", "Maragogype", "Pacamara", "Otro")
private const val SENSORY_MIN = 0
private const val SENSORY_MAX = 5
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
    viewModel: DiaryViewModel = hiltViewModel(),
    onTrackEvent: (String, Bundle) -> Unit = { _, _ -> }
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
    val screenTitle = if (coffeeId != null) {
        stringResource(id = R.string.add_pantry_edit_coffee)
    } else if (customFlow || brewLabFlow) {
        stringResource(id = R.string.diary_create_coffee)
    } else {
        stringResource(id = R.string.add_pantry_new_coffee)
    }

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
                title = { Text(screenTitle, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = { onBackClick(null) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.join_list_back), tint = MaterialTheme.colorScheme.onSurface)
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
                            text = stringResource(id = R.string.brew_save),
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
                                .clickable { onTrackEvent("modal_open", bundleOf("modal_id" to "add_pantry_image_picker")); showImagePickerSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUri != null || existingImageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = imageUri ?: existingImageUrl,
                                    contentDescription = stringResource(id = R.string.add_pantry_image_cd),
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
                                        contentDescription = stringResource(id = R.string.add_pantry_remove_photo),
                                        tint = if (isDark) PureBlack else PureWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = stringResource(id = R.string.add_pantry_add_photo), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text(stringResource(id = R.string.add_pantry_add_photo), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                placeholder = { Text(stringResource(id = R.string.add_pantry_name_placeholder)) },
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
                                    placeholder = { Text(stringResource(id = R.string.add_pantry_roaster_placeholder)) },
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
                                    Text(stringResource(id = R.string.add_pantry_suggested_roasters), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
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
                Text(stringResource(id = R.string.add_pantry_origin_profile), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
            }
            item {
                FormSectionCard(title = null) {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        PickerTriggerRow(value = specialty, placeholder = stringResource(id = R.string.add_pantry_select_specialty), onClick = { onTrackEvent("modal_open", bundleOf("modal_id" to "add_pantry_option_picker")); pickerOpen = "specialty" })
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        Spacer(Modifier.height(12.dp))
                        PickerTriggerRow(value = roast, placeholder = stringResource(id = R.string.add_pantry_select_roast), onClick = { onTrackEvent("modal_open", bundleOf("modal_id" to "add_pantry_option_picker")); pickerOpen = "roast" })
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        Spacer(Modifier.height(12.dp))
                        PickerTriggerRow(value = formatMultiValue(parseMultiValue(country)), placeholder = stringResource(id = R.string.add_pantry_select_countries), onClick = { onTrackEvent("modal_open", bundleOf("modal_id" to "add_pantry_option_picker")); pickerOpen = "country" })
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        Spacer(Modifier.height(12.dp))
                        PickerTriggerRow(value = formatMultiValue(parseMultiValue(variety)), placeholder = stringResource(id = R.string.add_pantry_select_varieties), onClick = { onTrackEvent("modal_open", bundleOf("modal_id" to "add_pantry_option_picker")); pickerOpen = "variety" })
                    }
                }
            }

            // Presentación — título fuera
            item {
                Text(stringResource(id = R.string.add_pantry_presentation), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
            }
            item {
                FormSectionCard(title = null) {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(id = R.string.add_pantry_has_caffeine), modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
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
                        PickerTriggerRow(value = format, placeholder = stringResource(id = R.string.add_pantry_select_format), onClick = { onTrackEvent("modal_open", bundleOf("modal_id" to "add_pantry_option_picker")); pickerOpen = "format" })
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
                                    text = stringResource(id = R.string.add_pantry_amount_g),
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
                    Text(stringResource(id = R.string.add_pantry_optional_details), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(stringResource(id = R.string.add_pantry_optional_badge), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
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
                            placeholder = { Text(stringResource(id = R.string.add_pantry_description)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = Shapes.cardSmall,
                            minLines = 3,
                            colors = noBorderColors
                        )
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        Spacer(Modifier.height(12.dp))
                        PickerTriggerRow(value = formatMultiValue(parseMultiValue(proceso)), placeholder = stringResource(id = R.string.add_pantry_select_processes), onClick = { onTrackEvent("modal_open", bundleOf("modal_id" to "add_pantry_option_picker")); pickerOpen = "process" })
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        Spacer(Modifier.height(12.dp))
                        PickerTriggerRow(value = moliendaRecomendada, placeholder = stringResource(id = R.string.add_pantry_select_grind), onClick = { onTrackEvent("modal_open", bundleOf("modal_id" to "add_pantry_option_picker")); pickerOpen = "grind" })
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = codigoBarras,
                            onValueChange = { codigoBarras = it },
                            placeholder = { Text(stringResource(id = R.string.add_pantry_barcode)) },
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
                            placeholder = { Text(stringResource(id = R.string.add_pantry_product_link)) },
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
                    Text(stringResource(id = R.string.add_pantry_sensory_profile), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(stringResource(id = R.string.add_pantry_optional_badge), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            item {
                FormSectionCard(title = null) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(id = R.string.add_pantry_rating_1_5), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        listOf(
                            "aroma" to aroma, "sabor" to sabor, "cuerpo" to cuerpo,
                            "acidez" to acidez, "dulzura" to dulzura
                        ).forEach { (key, value) ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    val sensoryLabel = when (key) {
                                        "aroma" -> stringResource(id = R.string.sensory_aroma)
                                        "sabor" -> stringResource(id = R.string.sensory_flavor)
                                        "cuerpo" -> stringResource(id = R.string.sensory_body)
                                        "acidez" -> stringResource(id = R.string.sensory_acidity)
                                        "dulzura" -> stringResource(id = R.string.sensory_sweetness)
                                        else -> key
                                    }
                                    Text(sensoryLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
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
                onDismissRequest = { onTrackEvent("modal_close", bundleOf("modal_id" to "add_pantry_image_picker")); showImagePickerSheet = false },
                containerColor = CafesitoModalSheetDefaults.containerColor(),
                shape = CafesitoModalSheetDefaults.shape,
                scrimColor = CafesitoModalSheetDefaults.scrimColor,
                dragHandle = { CafesitoModalSheetDefaults.dragHandle() }
            ) {
                Column(Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp, bottom = 40.dp)) {
                    Text(
                        text = stringResource(id = R.string.add_pantry_add_photo_title).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp),
                        fontWeight = FontWeight.Bold
                    )
                    ModalMenuOption(
                        title = stringResource(id = R.string.add_pantry_take_photo),
                        icon = Icons.Default.PhotoCamera,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = {
                            if (permissionState.allPermissionsGranted) {
                                val uri = createTempImageUri(context)
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                                onTrackEvent("modal_close", bundleOf("modal_id" to "add_pantry_image_picker"))
                                showImagePickerSheet = false
                            } else {
                                permissionState.launchMultiplePermissionRequest()
                            }
                        }
                    )
                    ModalMenuOption(
                        title = stringResource(id = R.string.add_pantry_choose_gallery),
                        icon = Icons.Default.Collections,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = {
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            onTrackEvent("modal_close", bundleOf("modal_id" to "add_pantry_image_picker"))
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
                "specialty" -> Triple(stringResource(id = R.string.add_pantry_specialty), SPECIALTY_OPTIONS, false)
                "roast" -> Triple(stringResource(id = R.string.add_pantry_roast), ROAST_OPTIONS, false)
                "country" -> Triple(stringResource(id = R.string.add_pantry_countries), countries, true)
                "variety" -> Triple(stringResource(id = R.string.add_pantry_varieties), VARIETY_OPTIONS, true)
                "format" -> Triple(stringResource(id = R.string.add_pantry_format), FORMAT_OPTIONS, false)
                "process" -> Triple(stringResource(id = R.string.add_pantry_process), PROCESS_OPTIONS, true)
                "grind" -> Triple(stringResource(id = R.string.add_pantry_recommended_grind), GRIND_OPTIONS, false)
                else -> Triple("", emptyList(), false)
            }
            val optionBg = if (isDark) PureBlack else PureWhite
            ModalBottomSheet(
                onDismissRequest = { onTrackEvent("modal_close", bundleOf("modal_id" to "add_pantry_option_picker")); pickerOpen = null },
                containerColor = CafesitoModalSheetDefaults.containerColor(),
                shape = CafesitoModalSheetDefaults.shape,
                scrimColor = CafesitoModalSheetDefaults.scrimColor,
                dragHandle = { CafesitoModalSheetDefaults.dragHandle() }
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
                                    onTrackEvent("modal_close", bundleOf("modal_id" to "add_pantry_option_picker"))
                                    pickerOpen = null
                                },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) { Text(stringResource(id = R.string.common_apply)) }
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
                                        onTrackEvent("modal_close", bundleOf("modal_id" to "add_pantry_option_picker"))
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
                contentDescription = stringResource(id = R.string.common_open),
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
