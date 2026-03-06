package com.cafesito.app.ui.brewlab

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafesito.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrewLabScreen(
    onNavigateToDiary: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onAddToPantryClick: () -> Unit = {},
    onCreateCoffeeClick: () -> Unit = {},
    createdCoffeeId: String? = null,
    onCreatedCoffeeConsumed: () -> Unit = {},
    viewModel: BrewLabViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val step by viewModel.currentStep.collectAsState()
    val selectedMethod by viewModel.selectedMethod.collectAsState()
    val selectedCoffee by viewModel.selectedCoffee.collectAsState()
    
    val water by viewModel.waterAmount.collectAsState()
    val ratio by viewModel.ratio.collectAsState()
    val coffeeGrams by viewModel.coffeeGrams.collectAsState()
    val isEspressoMethod by viewModel.isEspressoMethod.collectAsState()
    val isRatioEditable by viewModel.isRatioEditable.collectAsState()
    val isWaterEditable by viewModel.isWaterEditable.collectAsState()
    val brewTimeSeconds by viewModel.brewTimeSeconds.collectAsState()
    val timeProfile by viewModel.selectedTimeProfile.collectAsState()
    val valuation by viewModel.brewValuation.collectAsState()
    val methodProfile by viewModel.selectedMethodProfile.collectAsState()
    val baristaTips by viewModel.baristaTips.collectAsState()
    
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val phasesTimeline by viewModel.phasesTimeline.collectAsState()
    val currentPhaseIndex by viewModel.currentPhaseIndex.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val hasTimerStarted by viewModel.hasTimerStarted.collectAsState()
    val remainingSeconds by viewModel.secondsRemainingInPhase.collectAsState()
    val brewingProcessAdvice by viewModel.brewingProcessAdvice.collectAsState()
    
    val recommendation by viewModel.dialInRecommendation.collectAsState()
    val selectedTaste by viewModel.selectedTaste.collectAsState()
    val timerEnded by viewModel.timerEnded.collectAsState()
    val pantryItems by viewModel.pantryItems.collectAsState()
    val allCoffees by viewModel.availableCoffees.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }

    LaunchedEffect(Unit) {
        viewModel.phaseEvent.collect {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        }
    }

    LaunchedEffect(createdCoffeeId, allCoffees, pantryItems) {
        val newCoffeeId = createdCoffeeId ?: return@LaunchedEffect
        val coffeeWithDetails = allCoffees.find { it.coffee.id == newCoffeeId }
        if (coffeeWithDetails != null) {
            viewModel.selectCoffeeFromCatalog(coffeeWithDetails.coffee)
            onCreatedCoffeeConsumed()
            return@LaunchedEffect
        }
        val pantryItem = pantryItems.find { it.coffee.id == newCoffeeId }
        if (pantryItem != null) {
            viewModel.selectPantryItem(pantryItem)
            onCreatedCoffeeConsumed()
            return@LaunchedEffect
        }
        viewModel.refreshCoffeesAndPantry()
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
                    if (step == BrewStep.CONFIGURATION) {
                        IconButton(onClick = { viewModel.startBrewing() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Empezar preparación")
                        }
                    }
                    if (step == BrewStep.BREWING && timerEnded) {
                        val canSave = selectedCoffee != null && selectedTaste != null
                        Text(
                            text = "Guardar",
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable(enabled = canSave) { viewModel.saveToDiary { onNavigateToDiary() } },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (canSave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
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
                        LaunchedEffect(step) { viewModel.refreshPantry() }
                        ChooseCoffeeStep(
                            pantryItems = pantryItems,
                            allCoffees = allCoffees,
                            searchQuery = searchQuery,
                            onSearchQueryChange = viewModel::onSearchQueryChanged,
                            onAddToPantryClick = onAddToPantryClick,
                            onCreateCoffeeClick = onCreateCoffeeClick,
                            onCoffeeSelected = { coffee, fromPantry ->
                                if (fromPantry) {
                                    val item = pantryItems.find { it.coffee.id == coffee.id }
                                    if (item != null) viewModel.selectPantryItem(item)
                                } else {
                                    viewModel.selectCoffeeFromCatalog(coffee)
                                }
                            }
                        )
                    }
                    BrewStep.CONFIGURATION -> ConfigStep(
                        methodName = selectedMethod?.name,
                        isEspressoMethod = isEspressoMethod,
                        isRatioEditable = isRatioEditable,
                        isWaterEditable = isWaterEditable,
                        brewTimeSeconds = brewTimeSeconds,
                        timeProfile = timeProfile,
                        methodProfile = methodProfile,
                        water = water,
                        ratio = ratio,
                        coffeeGrams = coffeeGrams,
                        valuation = valuation,
                        baristaTips = baristaTips,
                        viewModel = viewModel
                    )
                    BrewStep.BREWING -> PreparationStep(timerSeconds, remainingSeconds, phasesTimeline, currentPhaseIndex, brewingProcessAdvice, isTimerRunning, hasTimerStarted, selectedTaste, recommendation, viewModel)
                }
            }
        }
    }
}
