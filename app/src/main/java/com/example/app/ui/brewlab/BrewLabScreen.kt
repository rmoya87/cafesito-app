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
                        LaunchedEffect(step) { viewModel.refreshPantry() }
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
