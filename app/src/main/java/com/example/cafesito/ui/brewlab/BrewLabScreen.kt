package com.example.cafesito.ui.brewlab

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cafesito.data.PantryItemWithDetails
import com.example.cafesito.ui.components.*
import com.example.cafesito.ui.theme.*
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrewLabScreen(
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onNavigateToDiary: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    viewModel: BrewLabViewModel = hiltViewModel()
) {
    val step by viewModel.currentStep.collectAsState()
    val selectedMethod by viewModel.selectedMethod.collectAsState()
    val selectedItem by viewModel.selectedPantryItem.collectAsState()
    
    val water by viewModel.waterAmount.collectAsState()
    val ratio by viewModel.ratio.collectAsState()
    val coffeeGrams by viewModel.coffeeGrams.collectAsState()
    
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val phasesTimeline by viewModel.phasesTimeline.collectAsState()
    val currentPhaseIndex by viewModel.currentPhaseIndex.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
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
        containerColor = SoftOffWhite,
        topBar = { 
            GlassyTopBar(
                title = step.title,
                onBackClick = if (step != BrewStep.CHOOSE_METHOD) { { viewModel.backStep() } } else null,
                scrollBehavior = scrollBehavior
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
                    BrewStep.CHOOSE_COFFEE -> ChooseCoffeeStep(pantryItems) { viewModel.selectPantryItem(it) }
                    BrewStep.CONFIGURATION -> ConfigStep(selectedMethod, water, ratio, coffeeGrams, viewModel) { viewModel.startBrewing() }
                    BrewStep.BREWING -> PreparationStep(timerSeconds, remainingSeconds, phasesTimeline, currentPhaseIndex, isTimerRunning, viewModel)
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
        items(methods.chunked(2)) { rowMethods ->
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
                Icon(Icons.Default.CoffeeMaker, null, tint = CaramelAccent, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = method.name.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = EspressoDeep,
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
                    Text("No tienes café en tu despensa", color = Color.Gray)
                }
            }
        } else {
            items(items) { item ->
                PantrySelectionCard(item) { onSelect(item) }
            }
        }
    }
}

@Composable
fun ConfigStep(method: BrewMethod?, water: Float, ratio: Float, coffeeGrams: Float, viewModel: BrewLabViewModel, onNext: () -> Unit) {
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
                        DataBlock("CAFÉ", "${String.format("%.1f", coffeeGrams)} g", CaramelAccent)
                    }
                    
                    if (method?.hasWaterAdjustment == true) {
                        Spacer(Modifier.height(32.dp))
                        Text("CANTIDAD DE AGUA", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Slider(
                            value = water, onValueChange = { viewModel.setWaterAmount(it) }, valueRange = 50f..1000f,
                            colors = SliderDefaults.colors(thumbColor = waterBlue, activeTrackColor = waterBlue, inactiveTrackColor = Color.LightGray)
                        )
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    val label = if (method?.hasWaterAdjustment == true) "RATIO (INTENSIDAD)" else "DOSIS DE CAFÉ"
                    val range = if (method?.hasWaterAdjustment == true) 10f..20f else 14f..22f
                    
                    Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Slider(
                        value = ratio, onValueChange = { viewModel.setRatio(it) }, valueRange = range,
                        colors = SliderDefaults.colors(thumbColor = CaramelAccent, activeTrackColor = CaramelAccent, inactiveTrackColor = Color.LightGray)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            
            Text("CONSEJOS DEL BARISTA", style = MaterialTheme.typography.labelLarge, color = CaramelAccent)
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
                colors = ButtonDefaults.buttonColors(containerColor = EspressoDeep), 
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("EMPEZAR PREPARACIÓN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
    viewModel: BrewLabViewModel
) {
    val currentPhase = timeline.getOrNull(currentPhaseIndex) ?: BrewPhaseInfo("Listo", "Proceso completado.", 0)
    val hasNextPhase = currentPhaseIndex < timeline.size - 1
    
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).padding(horizontal = 24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            
            PremiumCard {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().size(220.dp).clip(CircleShape).background(CreamLight).border(BorderStroke(1.dp, BorderLight), CircleShape), contentAlignment = Alignment.Center) {
                        WaterWaveAnimation(progress = if (isTimerRunning) (timerSeconds % 180) / 180f else 0f, modifier = Modifier.fillMaxSize())
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = String.format("%02d:%02d", timerSeconds / 60, timerSeconds % 60), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black, color = EspressoDeep)
                            Text(currentPhase.label.uppercase(), style = MaterialTheme.typography.labelMedium, color = CaramelAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    if (isTimerRunning && hasNextPhase) {
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            color = CaramelAccent.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "PRÓXIMO PASO EN ${remainingSeconds}S",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CaramelAccent,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Surface(color = SoftOffWhite, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, BorderLight)) {
                        Text(text = currentPhase.instruction, style = MaterialTheme.typography.bodyLarge, color = EspressoDeep, modifier = Modifier.padding(20.dp), textAlign = TextAlign.Center, lineHeight = 24.sp)
                    }
                }
            }
        }

        BottomActionContainer {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = { viewModel.resetAll() }, 
                    modifier = Modifier.weight(1f).height(56.dp), 
                    shape = RoundedCornerShape(28.dp), 
                    border = BorderStroke(1.dp, EspressoDeep),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EspressoDeep)
                ) {
                    Text("CANCELAR", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { viewModel.toggleTimer() }, 
                    modifier = Modifier.weight(1f).height(56.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = if (isTimerRunning) ElectricRed else EspressoDeep), 
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isTimerRunning) "PAUSAR" else "INICIAR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ResultStep(selectedTaste: String?, recommendation: String?, selectedItem: PantryItemWithDetails?, viewModel: BrewLabViewModel, onNavigateToDiary: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(24.dp))
            
            PremiumCard {
                Column(Modifier.padding(24.dp)) {
                    Text("¿QUÉ SABOR HAS OBTENIDO?", style = MaterialTheme.typography.labelLarge, color = EspressoDeep, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TasteOption("AMARGO", isSelected = selectedTaste == "Amargo", Modifier.weight(1f)) { viewModel.onTasteFeedback("Amargo") }
                        TasteOption("ÓPTIMO", isSelected = selectedTaste == "Equilibrado", Modifier.weight(1f)) { viewModel.onTasteFeedback("Equilibrado") }
                        TasteOption("ÁCIDO", isSelected = selectedTaste == "Ácido", Modifier.weight(1f)) { viewModel.onTasteFeedback("Ácido") }
                    }
                    
                    AnimatedVisibility(visible = recommendation != null) {
                        Column {
                            Spacer(Modifier.height(24.dp))
                            Surface(color = CaramelAccent.copy(alpha = 0.05f), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, CaramelAccent.copy(alpha = 0.1f))) {
                                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = CaramelAccent, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(16.dp))
                                    Text(recommendation ?: "", style = MaterialTheme.typography.bodyMedium, color = EspressoDeep, lineHeight = 20.sp)
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Consejo: Deja reposar tu café 5 minutos antes de la primera cata para percibir mejor su dulzura natural.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        BottomActionContainer {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { viewModel.resetAll() }, Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, EspressoDeep)) {
                    Text("REINICIAR", fontWeight = FontWeight.Bold, color = EspressoDeep)
                }
                Button(onClick = { viewModel.saveToDiary { onNavigateToDiary() } }, enabled = selectedItem != null && selectedTaste != null, modifier = Modifier.weight(1.2f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = CaramelAccent, disabledContainerColor = Color.LightGray), shape = RoundedCornerShape(28.dp)) {
                    Text("GUARDAR EN MI DIARIO", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
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
                Text(text = item.coffee.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = item.coffee.marca.uppercase(), style = MaterialTheme.typography.labelSmall, color = CaramelAccent)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = CaramelAccent,
                    trackColor = BorderLight
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}

@Composable
fun BottomActionContainer(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
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
        color = CaramelAccent,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun DataBlock(label: String, value: String, valueColor: Color = EspressoDeep) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = valueColor)
    }
}

@Composable
fun TasteOption(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) CaramelAccent else Color.White,
        border = BorderStroke(1.dp, if (isSelected) CaramelAccent else BorderLight)
    ) {
        Text(
            text = label, 
            modifier = Modifier.padding(vertical = 14.dp), 
            textAlign = TextAlign.Center, 
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else EspressoDeep
        )
    }
}

@Composable
fun WaterWaveAnimation(progress: Float, modifier: Modifier = Modifier) {
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
            drawPath(path, color = Color(0xFF2196F3).copy(alpha = 0.15f))
        }
    }
}

@Composable
fun LocalDetailBlock(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    PremiumCard(modifier = modifier) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(CaramelAccent.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = CaramelAccent, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 8.sp)
                Text(text = value.uppercase(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
