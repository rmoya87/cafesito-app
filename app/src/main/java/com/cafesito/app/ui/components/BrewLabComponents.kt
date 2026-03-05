package com.cafesito.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import coil.compose.AsyncImage
import com.cafesito.app.R
import com.cafesito.app.data.Coffee
import com.cafesito.app.data.CoffeeWithDetails
import com.cafesito.app.data.CommentWithAuthor
import com.cafesito.app.data.DiaryEntryEntity
import com.cafesito.app.data.PantryItemWithDetails
import com.cafesito.app.data.PostWithDetails
import com.cafesito.app.data.UserEntity
import com.cafesito.app.data.UserReviewInfo
import com.cafesito.app.ui.brewlab.BrewLabViewModel
import com.cafesito.app.ui.brewlab.BaristaTip
import com.cafesito.app.ui.brewlab.BrewMethod
import com.cafesito.app.ui.brewlab.BrewPhaseInfo
import com.cafesito.app.ui.diary.DiaryAnalytics
import com.cafesito.app.ui.diary.DiaryPeriod
import com.cafesito.app.ui.profile.ProfileUiState
import com.cafesito.app.ui.profile.ProfileViewModel
import com.cafesito.app.ui.search.BarcodeActionIcon
import com.cafesito.app.ui.theme.*
import com.cafesito.app.ui.timeline.CommentsViewModel
import com.cafesito.app.ui.timeline.TimelineNotification
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

// --- BREWLAB COMPONENTS ---

@Composable
fun ChooseMethodStep(methods: List<BrewMethod>, onSelect: (BrewMethod) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val chunkedMethods = methods.chunked(2)
        items(chunkedMethods) { rowMethods ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                rowMethods.forEach { method ->
                    MethodCard(method, Modifier.weight(1f)) { onSelect(method) }
                }
                if (rowMethods.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun MethodCard(method: BrewMethod, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val resId = remember(method.iconResName) {
        context.resources.getIdentifier(method.iconResName, "drawable", context.packageName)
    }

    PremiumCard(
        modifier = modifier
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (resId != 0) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = method.name,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Icon(Icons.Default.CoffeeMaker, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = method.name.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ChooseCoffeeStep(
    pantryItems: List<PantryItemWithDetails>,
    allCoffees: List<CoffeeWithDetails>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddNotFoundClick: () -> Unit,
    onCoffeeSelected: (Coffee, Boolean) -> Unit
) {
    val filteredPantry = if (searchQuery.isBlank()) pantryItems else pantryItems.filter {
        it.coffee.nombre.contains(searchQuery, true) || it.coffee.marca.contains(searchQuery, true)
    }
    val filteredSuggestions = if (searchQuery.isBlank()) allCoffees.take(10) else allCoffees.filter {
        it.coffee.nombre.contains(searchQuery, true) || it.coffee.marca.contains(searchQuery, true)
    }.take(10)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "TU DESPENSA",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(12.dp))
            if (filteredPantry.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (searchQuery.isBlank()) "Tu despensa está vacía." else "No hay coincidencias en tu despensa.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredPantry, key = { it.coffee.id + "_pantry" }) { item ->
                        PantryPremiumMiniCard(item) { onCoffeeSelected(item.coffee, true) }
                    }
                    item(key = "brewlab-add-pantry-card") {
                        PantryAddActionCard(onClick = onAddNotFoundClick)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(20.dp))
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("SUGERENCIAS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onAddNotFoundClick) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("Crear mi café", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Buscar café...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(999.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }

        if (filteredSuggestions.isEmpty()) {
            item {
                Text(
                    "No hay sugerencias disponibles.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            items(filteredSuggestions, key = { it.coffee.id }) { item ->
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    SimpleCoffeeSelectionCard(item.coffee) { onCoffeeSelected(item.coffee, false) }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun SimpleCoffeeSelectionCard(coffee: Coffee, onClick: () -> Unit) {
    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = coffee.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = coffee.nombre.toCoffeeNameFormat(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                @Suppress("DEPRECATION")
                Text(
                    text = coffee.marca.toCoffeeBrandFormat(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun ConfigStep(
    method: BrewMethod?,
    water: Float,
    ratio: Float,
    coffeeGrams: Float,
    valuation: String,
    baristaTips: List<BaristaTip>,
    viewModel: BrewLabViewModel
) {
    val waterBlue = WaterBlue
    var waterDraft by remember { mutableStateOf(water.roundToInt().toString()) }
    var coffeeDraft by remember { mutableStateOf(formatDecimalWithComma(coffeeGrams)) }
    LaunchedEffect(water) {
        waterDraft = water.roundToInt().toString()
    }
    LaunchedEffect(coffeeGrams) {
        coffeeDraft = formatDecimalWithComma(coffeeGrams)
    }
    val ratioProfile = remember(ratio) {
        when {
            ratio <= 13f -> "INTENSO"
            ratio <= 15f -> "INTENSO"
            ratio <= 17f -> "EQUILIBRADO"
            ratio <= 19f -> "LIGERO"
            else -> "MUY LIGERO"
        }
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier
            .weight(1f)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(24.dp))
            SectionHeader("AJUSTES TÉCNICOS")

            PremiumCard {
                Column(Modifier.padding(24.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        if (method?.hasWaterAdjustment == true) {
                            BrewMetricInputBlock(
                                label = "Agua (ml)",
                                value = waterDraft,
                                valueColor = waterBlue,
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f),
                                onValueChange = { raw ->
                                    val filtered = raw.filter { it.isDigit() }
                                    waterDraft = filtered
                                    filtered.toFloatOrNull()?.let { parsed ->
                                        viewModel.setWaterAmount(parsed.coerceIn(50f, 1000f))
                                    }
                                }
                            )
                            Spacer(Modifier.width(14.dp))
                        }
                        BrewMetricInputBlock(
                            label = "Café (g)",
                            value = coffeeDraft,
                            valueColor = MaterialTheme.colorScheme.primary,
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f),
                            onValueChange = { raw ->
                                val normalized = raw.replace(",", ".")
                                if (normalized.count { it == '.' } > 1) return@BrewMetricInputBlock
                                if (normalized.any { !it.isDigit() && it != '.' }) return@BrewMetricInputBlock
                                coffeeDraft = raw
                                normalized.toFloatOrNull()?.let { parsed ->
                                    viewModel.setCoffeeGrams(parsed)
                                }
                            }
                        )
                    }

                    if (method?.hasWaterAdjustment == true) {
                        Spacer(Modifier.height(32.dp))
                        @Suppress("DEPRECATION")
                        Text("CANTIDAD DE AGUA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = water, onValueChange = { viewModel.setWaterAmount(it) }, valueRange = 50f..1000f,
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.outline, activeTrackColor = waterBlue, inactiveTrackColor = MaterialTheme.colorScheme.outline)
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    val label = "RATIO 1:${ratio.roundToInt()} · $ratioProfile"
                    val range = if (method?.hasWaterAdjustment == true) 10f..20f else 14f..22f

                    @Suppress("DEPRECATION")
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = ratio, onValueChange = { viewModel.setRatio(it) }, valueRange = range,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.outline, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.outline)
                    )

                    if (valuation.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = valuation,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            @Suppress("DEPRECATION")
            Text("CONSEJOS DEL BARISTA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            val baristaColumns = remember(baristaTips) { baristaTips.chunked(3) }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(baristaColumns) { columnTips ->
                    Column(
                        modifier = Modifier.widthIn(min = 220.dp, max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        columnTips.forEach { tip ->
                            BaristaTipPill(tip = tip, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }

    }
}

@Composable
fun PreparationStep(
    timerSeconds: Int,
    remainingSeconds: Int,
    timeline: List<BrewPhaseInfo>,
    currentPhaseIndex: Int,
    isTimerRunning: Boolean,
    hasTimerStarted: Boolean,
    viewModel: BrewLabViewModel
) {
    val haptic = LocalHapticFeedback.current
    val currentPhase = timeline.getOrNull(currentPhaseIndex) ?: BrewPhaseInfo("Listo", "Proceso completado.", 0)
    val nextPhase = timeline.getOrNull(currentPhaseIndex + 1)
    val totalSeconds = timeline.sumOf { it.durationSeconds }.coerceAtLeast(1)
    val totalProgress = (timerSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)

    val timerScale by animateFloatAsState(
        targetValue = if (remainingSeconds <= 5 && isTimerRunning) 1.1f else 1f,
        animationSpec = if (remainingSeconds <= 5 && isTimerRunning) {
            infiniteRepeatable(
                animation = tween(500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(300)
        },
        label = "timerScale"
    )

    LaunchedEffect(currentPhaseIndex) {
        if (hasTimerStarted) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PremiumCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp)
            ) {
                Box {
                    WaterWaveAnimation(
                        progress = totalProgress,
                        color = LocalCaramelAccent.current.copy(alpha = 0.05f),
                        modifier = Modifier.matchParentSize()
                    )

                    Column {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = currentPhase.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val maxFontSize = 120.sp
                                val minFontSize = 84.sp
                                val fontSize = when {
                                    maxWidth < 320.dp -> minFontSize
                                    maxWidth < 360.dp -> 96.sp
                                    else -> maxFontSize
                                }

                                Text(
                                    text = String.format("%02d:%02d", remainingSeconds / 60, remainingSeconds % 60),
                                    style = MaterialTheme.typography.displayLarge.copy(fontSize = fontSize),
                                    fontWeight = FontWeight.Black,
                                    color = if (remainingSeconds <= 5 && isTimerRunning) ElectricRed else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .graphicsLayer(scaleX = timerScale, scaleY = timerScale)
                                )
                            }

                            val nextLabel = nextPhase?.label ?: "Finalizar"
                            Text(
                                text = "Siguiente: $nextLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(24.dp))

                            BrewTimeline(phases = timeline, elapsedTotalSeconds = timerSeconds)

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                @Suppress("DEPRECATION")
                                Text(
                                    text = String.format("TOTAL %02d:%02d", totalSeconds / 60, totalSeconds % 60),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                @Suppress("DEPRECATION")
                                Text(
                                    text = String.format("%02d:%02d", timerSeconds / 60, timerSeconds % 60),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                .padding(24.dp)
                        ) {
                            Text(
                                text = currentPhase.instruction,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (hasTimerStarted) {
                    OutlinedButton(
                        onClick = { viewModel.resetTimer() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("REINICIAR", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { viewModel.toggleTimer() },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTimerRunning) ElectricRed else MaterialTheme.colorScheme.primary,
                        contentColor = if (isTimerRunning) Color.White else if (isSystemInDarkTheme()) Color.Black else MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(
                        if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isTimerRunning) Color.White else if (isSystemInDarkTheme()) Color.Black else MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isTimerRunning) "PAUSAR" else "INICIAR",
                        fontWeight = FontWeight.Bold,
                        color = if (isTimerRunning) Color.White else if (isSystemInDarkTheme()) Color.Black else MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun PhaseDurationLabel(phase: BrewPhaseInfo) {
    @Suppress("DEPRECATION")
    Text(
        text = "${phase.durationSeconds}s",
        style = MaterialTheme.typography.labelSmall,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
fun BrewTimeline(phases: List<BrewPhaseInfo>, elapsedTotalSeconds: Int) {
    val totalSeconds = phases.sumOf { it.durationSeconds }.coerceAtLeast(1)
    val coffeeBrown = LocalCaramelAccent.current
    val softGray = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Column(Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            phases.forEach { phase ->
                val weight = (phase.durationSeconds.toFloat() / totalSeconds).coerceAtLeast(0.05f)
                Box(
                    modifier = Modifier.weight(weight),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    PhaseDurationLabel(phase)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier
            .fillMaxWidth()
            .height(12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            var elapsedBeforeThisPhase = 0
            phases.forEach { phase ->
                val weight = (phase.durationSeconds.toFloat() / totalSeconds).coerceAtLeast(0.05f)
                val phaseProgress = if (elapsedTotalSeconds <= elapsedBeforeThisPhase) {
                    0f
                } else if (elapsedTotalSeconds >= elapsedBeforeThisPhase + phase.durationSeconds) {
                    1f
                } else {
                    (elapsedTotalSeconds - elapsedBeforeThisPhase).toFloat() / phase.durationSeconds
                }

                Box(
                    Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(softGray)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(phaseProgress)
                            .fillMaxHeight()
                            .background(coffeeBrown)
                    )
                }
                elapsedBeforeThisPhase += phase.durationSeconds
            }
        }
    }
}

@Composable
fun ResultStep(
    selectedTaste: String?,
    recommendation: String?,
    selectedCoffee: Coffee?,
    viewModel: BrewLabViewModel,
    onNavigateToDiary: () -> Unit
) {
    val tastes = listOf(
        "Amargo" to Icons.Default.LocalFireDepartment,
        "Ácido" to Icons.Default.Science,
        "Equilibrado" to Icons.Default.Verified,
        "Salado" to Icons.Default.Waves,
        "Acuoso" to Icons.Default.WaterDrop,
        "Aspero" to Icons.Default.Grain,
        "Dulce" to Icons.Default.Favorite
    )

    Column(Modifier.fillMaxSize()) {
        @Suppress("DEPRECATION")
        Column(Modifier
            .weight(1f)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(24.dp))

            PremiumCard {
                Column(Modifier.padding(24.dp)) {
                    Text(
                        text = "¿QUÉ SABOR HAS OBTENIDO?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        tastes.chunked(2).forEach { rowTastes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowTastes.forEach { (label, icon) ->
                                    TasteChip(
                                        label = label.uppercase(),
                                        icon = icon,
                                        isSelected = selectedTaste == label,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        viewModel.onTasteFeedback(label)
                                    }
                                }
                                if (rowTastes.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = recommendation != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(Modifier.height(32.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Column(Modifier.padding(24.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.width(12.dp))
                                        @Suppress("DEPRECATION")
                                        Text(
                                            "Recomendación",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        text = recommendation ?: "",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 24.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }

        BottomActionContainer {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { viewModel.resetAll() },
                    Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text("REINICIAR", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Button(
                    onClick = { viewModel.saveToDiary { onNavigateToDiary() } },
                    enabled = selectedCoffee != null && selectedTaste != null,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = if (isSystemInDarkTheme()) Color.Black else MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        "GUARDAR EN DIARIO",
                        fontWeight = FontWeight.Bold,
                        color = if (isSystemInDarkTheme()) Color.Black else MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun TasteChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        shadowElevation = if (isSelected) 3.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            @Suppress("DEPRECATION")
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PantrySelectionCard(item: PantryItemWithDetails, onClick: () -> Unit) {
    val progress = if (item.pantryItem.totalGrams > 0) item.pantryItem.gramsRemaining.toFloat() / item.pantryItem.totalGrams else 0f

    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.coffee.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(text = item.coffee.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                @Suppress("DEPRECATION")
                Text(text = item.coffee.marca.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun BottomActionContainer(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 80.dp),
        content = content
    )
}

@Composable
fun SectionHeader(title: String) {
    @Suppress("DEPRECATION")
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun DataBlock(label: String, value: String, valueColor: Color) {
    Column {
        @Suppress("DEPRECATION")
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = valueColor)
    }
}

@Composable
fun BrewMetricInputBlock(
    label: String,
    value: String,
    valueColor: Color,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    Column(modifier = modifier) {
        @Suppress("DEPRECATION")
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Transparent)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = valueColor
                ),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.widthIn(min = 92.dp, max = 170.dp)
            )
        }
    }
}

private fun formatDecimalWithComma(value: Float): String {
    return String.format(Locale.US, "%.1f", value).replace('.', ',')
}

@Composable
fun TasteOption(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
    ) {
        @Suppress("DEPRECATION")
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 14.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun WaterWaveAnimation(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = WaterBlue.copy(alpha = 0.15f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val waveHeight = 10.dp.toPx()
        val baseFillHeight = height * (1f - progress.coerceIn(0f, 1f))

        val path = Path()
        path.moveTo(0f, baseFillHeight)

        for (x in 0..width.toInt()) {
            val progressX = x.toFloat() / width
            val y = baseFillHeight + waveHeight * sin((progressX * 2.0 * PI + waveOffset.toDouble())).toFloat()
            path.lineTo(x.toFloat(), y)
        }

        path.lineTo(width, height)
        path.lineTo(0f, height)
        path.close()

        clipRect {
            drawPath(path, color = color)
        }
    }
}

@Composable
private fun baristaTipIcon(iconKey: String): ImageVector {
    return when (iconKey) {
        "grind" -> Icons.Default.Grain
        "thermostat" -> Icons.Default.Thermostat
        "water" -> Icons.Default.WaterDrop
        "clock" -> Icons.Default.AccessTime
        "coffee" -> Icons.Default.Coffee
        else -> Icons.Default.AutoAwesome
    }
}

@Composable
fun BaristaTipPill(tip: BaristaTip, modifier: Modifier = Modifier) {
    val icon = baristaTipIcon(tip.iconKey)
    PremiumCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .heightIn(min = 52.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier
                .size(26.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                @Suppress("DEPRECATION")
                Text(text = tip.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                @Suppress("DEPRECATION")
                Text(
                    text = tip.value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
