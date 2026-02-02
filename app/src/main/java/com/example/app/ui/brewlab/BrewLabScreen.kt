package com.cafesito.app.ui.brewlab

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cafesito.app.data.PantryItemWithDetails
import com.cafesito.app.ui.components.*
import com.cafesito.app.ui.theme.*
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrewLabScreen(
    onNavigateToDiary: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onAddCoffeeClick: () -> Unit = {},
    viewModel: BrewLabViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val step by viewModel.currentStep.collectAsState()
    val selectedMethod by viewModel.selectedMethod.collectAsState()
    val selectedItem by viewModel.selectedPantryItem.collectAsState()
    
    val water by viewModel.waterAmount.collectAsState()
    val ratio by viewModel.ratio.collectAsState()
    val coffeeGrams by viewModel.coffeeGrams.collectAsState()
    val valuation by viewModel.brewValuation.collectAsState()
    
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val phasesTimeline by viewModel.phasesTimeline.collectAsState()
    val currentPhaseIndex by viewModel.currentPhaseIndex.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val hasTimerStarted by viewModel.hasTimerStarted.collectAsState()
    val remainingSeconds by viewModel.secondsRemainingInPhase.collectAsState()
    
    val recommendation by viewModel.dialInRecommendation.collectAsState()
    val selectedTaste by viewModel.selectedTaste.collectAsState()
    val pantryItems by viewModel.pantryItems.collectAsState()

    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }

    LaunchedEffect(Unit) {
        viewModel.phaseEvent.collect {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { 
            GlassyTopBar(
                title = step.title,
                onBackClick = if (step != BrewStep.CHOOSE_METHOD) { { viewModel.backStep() } } else null,
                scrollBehavior = scrollBehavior,
                actions = {
                    if (step == BrewStep.CHOOSE_COFFEE) {
                        IconButton(
                            onClick = onAddCoffeeClick,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape
                            ) {
                                Icon(
                                    Icons.Default.Add, 
                                    contentDescription = "Añadir café", 
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                }
            ) 
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "StepTransition",
                modifier = Modifier.fillMaxSize()
            ) { currentStep ->
                when (currentStep) {
                    BrewStep.CHOOSE_METHOD -> ChooseMethodStep(viewModel.brewMethods) { viewModel.selectMethod(it) }
                    BrewStep.CHOOSE_COFFEE -> {
                        LaunchedEffect(Unit) { viewModel.refreshPantry() }
                        ChooseCoffeeStep(pantryItems) { viewModel.selectPantryItem(it) }
                    }
                    BrewStep.CONFIGURATION -> ConfigStep(selectedMethod, water, ratio, coffeeGrams, valuation, viewModel) { viewModel.startBrewing() }
                    BrewStep.BREWING -> PreparationStep(timerSeconds, remainingSeconds, phasesTimeline, currentPhaseIndex, isTimerRunning, hasTimerStarted, viewModel)
                    BrewStep.RESULT -> ResultStep(selectedTaste, recommendation, selectedItem, viewModel, onNavigateToDiary)
                }
            }
        }
    }
}

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
        modifier = modifier.height(140.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
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
fun ChooseCoffeeStep(items: List<PantryItemWithDetails>, onSelect: (PantryItemWithDetails) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (items.isEmpty()) {
            item { 
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tienes café en tu despensa", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(items, key = { it.coffee.id }) { item ->
                PantrySelectionCard(item) { onSelect(item) }
            }
        }
    }
}

@Composable
fun ConfigStep(method: BrewMethod?, water: Float, ratio: Float, coffeeGrams: Float, valuation: String, viewModel: BrewLabViewModel, onNext: () -> Unit) {
    val waterBlue = Color(0xFF2196F3)
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(24.dp))
            SectionHeader("AJUSTES TÉCNICOS")
            
            PremiumCard {
                Column(Modifier.padding(24.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        if (method?.hasWaterAdjustment == true) {
                            DataBlock("AGUA", "${water.roundToInt()} ml", waterBlue)
                        }
                        DataBlock("CAFÉ", "${String.format("%.1f", coffeeGrams)} g", MaterialTheme.colorScheme.primary)
                    }
                    
                    if (method?.hasWaterAdjustment == true) {
                        Spacer(Modifier.height(32.dp))
                        Text("CANTIDAD DE AGUA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = water, onValueChange = { viewModel.setWaterAmount(it) }, valueRange = 50f..1000f,
                            colors = SliderDefaults.colors(thumbColor = waterBlue, activeTrackColor = waterBlue, inactiveTrackColor = MaterialTheme.colorScheme.outline)
                        )
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    val label = if (method?.hasWaterAdjustment == true) "RATIO (INTENSIDAD)" else "DOSIS DE CAFÉ"
                    val range = if (method?.hasWaterAdjustment == true) 10f..20f else 14f..22f
                    
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = ratio, onValueChange = { viewModel.setRatio(it) }, valueRange = range,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.outline)
                    )

                    if (valuation.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
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
            
            Text("CONSEJOS DEL BARISTA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LocalDetailBlock("MOLIENDA", method?.grindSize ?: "Media", Icons.Default.Grain, Modifier.weight(1f))
                LocalDetailBlock("TEMPERATURA", method?.tempRange ?: "92-96°C", Icons.Default.Thermostat, Modifier.weight(1f))
            }
            Spacer(Modifier.height(40.dp))
        }

        BottomActionContainer {
            Button(
                onClick = onNext, 
                modifier = Modifier.fillMaxWidth().height(56.dp), 
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), 
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("EMPEZAR PREPARACIÓN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onPrimary)
            }
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

    // Efecto háptico al cambiar de fase
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
                    // Fondo de ola sutil
                    WaterWaveAnimation(
                        progress = totalProgress,
                        color = CaramelAccent.copy(alpha = 0.05f),
                        modifier = Modifier.matchParentSize()
                    )

                    Column {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = currentPhase.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                text = String.format("%02d:%02d", remainingSeconds / 60, remainingSeconds % 60),
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 120.sp),
                                fontWeight = FontWeight.Black,
                                color = if (remainingSeconds <= 5 && isTimerRunning) ElectricRed else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .graphicsLayer(scaleX = timerScale, scaleY = timerScale)
                            )
                            
                            val nextLabel = nextPhase?.label ?: "Finalizar"
                            Text(
                                text = "Siguiente: $nextLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(Modifier.height(24.dp))
                            
                            // Barra de progreso segmentada rica en información
                            BrewTimeline(phases = timeline, elapsedTotalSeconds = timerSeconds)
                            
                            Spacer(Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = String.format("TOTAL %02d:%02d", totalSeconds / 60, totalSeconds % 60),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format("%02d:%02d", timerSeconds / 60, timerSeconds % 60),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        // Footer con instrucciones (Gris suave adaptable a modo noche)
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
        }

        BottomActionContainer {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (hasTimerStarted) {
                    OutlinedButton(
                        onClick = { viewModel.resetTimer() }, 
                        modifier = Modifier.weight(1f).height(56.dp), 
                        shape = RoundedCornerShape(28.dp), 
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("REINICIAR", fontWeight = FontWeight.Bold)
                    }
                }
                
                Button(
                    onClick = { viewModel.toggleTimer() }, 
                    modifier = Modifier.weight(1f).height(56.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = if (isTimerRunning) ElectricRed else MaterialTheme.colorScheme.primary), 
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = if (isTimerRunning) Color.White else MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isTimerRunning) "PAUSAR" else "INICIAR", fontWeight = FontWeight.Bold, color = if (isTimerRunning) Color.White else MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
fun PhaseDurationLabel(phase: BrewPhaseInfo) {
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
    val coffeeBrown = CaramelAccent
    val softGray = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        // Tiempos por fase ENCIMA de la barra (con el mismo espaciado que la barra)
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

        // Barra de progreso segmentada
        Row(Modifier.fillMaxWidth().height(12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
fun ResultStep(selectedTaste: String?, recommendation: String?, selectedItem: PantryItemWithDetails?, viewModel: BrewLabViewModel, onNavigateToDiary: () -> Unit) {
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
        Column(Modifier.weight(1f).padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
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
                    Modifier.weight(1f).height(56.dp), 
                    shape = RoundedCornerShape(28.dp), 
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text("REINICIAR", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Button(
                    onClick = { viewModel.saveToDiary { onNavigateToDiary() } }, 
                    enabled = selectedItem != null && selectedTaste != null, 
                    modifier = Modifier.weight(1.3f).height(56.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, disabledContainerColor = MaterialTheme.colorScheme.outline), 
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("GUARDAR EN DIARIO", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
fun TasteChip(
    label: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

// --- SHARED COMPONENTS ---

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
                modifier = Modifier.size(70.dp).clip(RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(text = item.coffee.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                Text(text = item.coffee.marca.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
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
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 100.dp),
        content = content
    )
}

@Composable
fun SectionHeader(title: String) {
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
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = valueColor)
    }
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
    color: Color = Color(0xFF2196F3).copy(alpha = 0.15f)
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
            val y = baseFillHeight + waveHeight * sin(x / width * 2 * Math.PI + waveOffset).toFloat()
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
fun LocalDetailBlock(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    PremiumCard(modifier = modifier) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                Text(text = value.uppercase(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
