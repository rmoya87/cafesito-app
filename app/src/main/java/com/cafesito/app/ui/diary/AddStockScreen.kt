package com.cafesito.app.ui.diary

import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafesito.app.R
import com.cafesito.app.camera.NativeBarcodeScannerActivity
import com.cafesito.app.ui.components.*
import com.cafesito.app.ui.search.BarcodeActionIcon
import com.cafesito.app.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStockScreen(
    onBackClick: () -> Unit,
    onAddCustomClick: () -> Unit,
    onSuccess: () -> Unit,
    onSuccessWithCoffeeId: ((String) -> Unit)? = null,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val coffees by viewModel.availableCoffees.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    
    val filteredSuggestions = remember(coffees, searchQuery) {
        coffees.filter { 
            !it.coffee.isCustom && (
                it.coffee.nombre.contains(searchQuery, ignoreCase = true) || 
                it.coffee.marca.contains(searchQuery, ignoreCase = true) ||
                it.coffee.codigoBarras == searchQuery
            )
        }.sortedByDescending { it.isFavorite }
    }
    
    var selectedCoffeeId by remember { mutableStateOf<String?>(null) }
    var grams by remember { mutableStateOf("250") }
    var isSaving by remember { mutableStateOf(false) }
    val nativeScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val value = result.data?.getStringExtra(NativeBarcodeScannerActivity.EXTRA_BARCODE_VALUE)
        if (!value.isNullOrBlank()) {
            searchQuery = value
            val found = coffees.find { it.coffee.codigoBarras == value }
            if (found != null) selectedCoffeeId = found.coffee.id
        }
    }


    if (selectedCoffeeId != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { if (!isSaving) selectedCoffeeId = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.add_stock_title), 
                    style = MaterialTheme.typography.labelLarge, 
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                
                val gramsValue = grams.toFloatOrNull() ?: 250f
                Text(
                    text = stringResource(R.string.add_stock_weight_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                BasicTextField(
                    value = grams,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all { it.isDigit() }) {
                            grams = input
                        }
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
                Spacer(Modifier.height(12.dp))
                Slider(
                    value = gramsValue.coerceIn(0f, 2000f),
                    onValueChange = { grams = it.roundToInt().toString() },
                    valueRange = 0f..2000f,
                    enabled = !isSaving,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    )
                )

                Spacer(Modifier.height(32.dp))
                
                Button(
                    onClick = {
                        val coffeeId = selectedCoffeeId!!
                        val g = grams.toIntOrNull() ?: 250
                        isSaving = true
                        viewModel.addToPantry(coffeeId, g) {
                            isSaving = false
                            selectedCoffeeId = null
                            onSuccessWithCoffeeId?.invoke(coffeeId)
                            onSuccess()
                        }
                    }, 
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isSaving && grams.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.add_stock_save_button), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GlassyTopBar(
                title = stringResource(R.string.add_stock_select_title),
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = onAddCustomClick) {
                        Icon(Icons.Default.Bolt, contentDescription = "Añadir rápido", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.add_stock_search_placeholder)) },
                    shape = RoundedCornerShape(999.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = if (isDark) Color.Black else Color.White,
                        focusedContainerColor = if (isDark) Color.Black else Color.White
                    ),
                    singleLine = true,
                    leadingIcon = { 
                        Box(modifier = Modifier.padding(start = 4.dp)) {
                            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) 
                        }
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                nativeScannerLauncher.launch(Intent(context, NativeBarcodeScannerActivity::class.java))
                            },
                            modifier = Modifier.padding(end = 12.dp),
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
                        ) {
                            BarcodeActionIcon(
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
            }

            items(filteredSuggestions, key = { it.coffee.id }) { coffeeDetails ->
                CoffeePremiumRowItem(coffeeDetails) { 
                    selectedCoffeeId = coffeeDetails.coffee.id 
                }
            }
        }
    }
}
