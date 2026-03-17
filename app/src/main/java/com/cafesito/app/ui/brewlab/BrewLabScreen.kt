package com.cafesito.app.ui.brewlab

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrewLabScreen(
    onNavigateToDiary: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onAddToPantryClick: () -> Unit = {},
    onCreateCoffeeClick: () -> Unit = {},
    onSelectCoffeeClick: () -> Unit = {},
    onTrackEvent: (String, Bundle) -> Unit = { _, _ -> },
    createdCoffeeId: String? = null,
    onCreatedCoffeeConsumed: () -> Unit = {},
    appliedSelectionId: String? = null,
    appliedSelectionFromPantry: Boolean = false,
    appliedSelectionPantryItemId: String? = null,
    onConsumeSelection: () -> Unit = {},
    viewModel: BrewLabViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val step by viewModel.currentStep.collectAsState()
    val selectedMethod by viewModel.selectedMethod.collectAsState()
    val selectedCoffee by viewModel.selectedCoffee.collectAsState()
    val selectedPantryItem by viewModel.selectedPantryItem.collectAsState()

    val ratio by viewModel.ratio.collectAsState()
    val coffeeGrams by viewModel.coffeeGrams.collectAsState()
    val displayCoffeeGrams by viewModel.displayCoffeeGrams.collectAsState()
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

    val brewMethods by viewModel.brewMethods.collectAsState(initial = emptyList())
    val brewTimerEnabled by viewModel.brewTimerEnabled.collectAsState()
    val canGoNext by viewModel.canGoNextFromMainStep.collectAsState()
    val canSaveForResult by viewModel.canSaveForResult.collectAsState()
    val drinkType by viewModel.drinkType.collectAsState()
    val selectedSizeLabel by viewModel.selectedSizeLabel.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.clearSelectedCoffeeOnEnter()
        viewModel.refreshPantry()
    }

    LaunchedEffect(appliedSelectionId) {
        val id = appliedSelectionId ?: return@LaunchedEffect
        if (appliedSelectionFromPantry) {
            val item = appliedSelectionPantryItemId?.let { pid -> pantryItems.find { it.pantryItem.id == pid } }
                ?: pantryItems.find { it.coffee.id == id }
            if (item != null) viewModel.selectPantryItem(item)
        } else {
            val c = allCoffees.find { it.coffee.id == id }?.coffee
            if (c != null) viewModel.selectCoffeeFromCatalog(c)
        }
        onConsumeSelection()
    }

    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        } catch (_: Exception) {
            null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.phaseEvent.collect {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
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
                    if (step == BrewStep.CHOOSE_METHOD) {
                        IconButton(
                            onClick = {
                                onTrackEvent("button_click", bundleOf("button_id" to "brew_next_step"))
                                viewModel.goNextFromMainStep(onNavigateToDiary)
                            },
                            enabled = canGoNext
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Siguiente",
                                tint = if (canGoNext) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.LightGray
                            )
                        }
                    }
                    if (step == BrewStep.BREWING && timerEnded) {
                        TextButton(onClick = {
                            onTrackEvent("button_click", bundleOf("button_id" to "brew_save_to_diary"))
                            viewModel.saveToDiary { onNavigateToDiary() }
                        }) {
                            Text("Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                    if (step == BrewStep.RESULT) {
                        TextButton(
                            onClick = {
                                onTrackEvent("button_click", bundleOf("button_id" to "brew_save_to_diary"))
                                viewModel.saveToDiary { onNavigateToDiary() }
                            },
                            enabled = canSaveForResult
                        ) {
                            Text("Guardar", fontWeight = FontWeight.Bold)
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
                    BrewStep.CHOOSE_METHOD -> {
                        if (brewMethods.isEmpty()) {
                            BrewLabLoadingContent()
                        } else {
                            BrewLabMainStepContent(
                        brewMethods = brewMethods,
                        selectedMethod = selectedMethod,
                        onSelectMethod = viewModel::selectMethod,
                        selectedCoffee = selectedCoffee,
                        selectedPantryItem = selectedPantryItem,
                        onSelectCoffeeClick = {
                            onTrackEvent("button_click", bundleOf("button_id" to "brew_select_coffee"))
                            onSelectCoffeeClick()
                        },
                        pantryItems = pantryItems,
                        allCoffees = allCoffees,
                        searchQuery = searchQuery,
                        onSearchQueryChange = viewModel::onSearchQueryChanged,
                        onAddToPantryClick = {
                            onTrackEvent("button_click", bundleOf("button_id" to "brew_add_to_pantry"))
                            onAddToPantryClick()
                        },
                        onCreateCoffeeClick = {
                            onTrackEvent("button_click", bundleOf("button_id" to "brew_create_coffee"))
                            onCreateCoffeeClick()
                        },
                        onCoffeeSelected = { _, _ -> },
                        drinkType = drinkType,
                        onDrinkTypeChange = viewModel::setDrinkType,
                        selectedSizeLabel = selectedSizeLabel,
                        onSizeSelected = viewModel::setSelectedSize,
                        isEspressoMethod = isEspressoMethod,
                        isRatioEditable = isRatioEditable,
                        isWaterEditable = isWaterEditable,
                        brewTimeSeconds = brewTimeSeconds,
                        timeProfile = timeProfile,
                        methodProfile = methodProfile,
                        ratio = ratio,
                        coffeeGrams = displayCoffeeGrams,
                        valuation = valuation,
                        baristaTips = baristaTips,
                        brewTimerEnabled = brewTimerEnabled,
                        onBrewTimerEnabledChange = viewModel::setBrewTimerEnabled,
                        viewModel = viewModel
                            )
                        }
                    }
                    BrewStep.BREWING -> PreparationStep(timerSeconds, remainingSeconds, phasesTimeline, currentPhaseIndex, brewingProcessAdvice, isTimerRunning, hasTimerStarted, selectedTaste, recommendation, viewModel)
                    BrewStep.RESULT -> ResultStep(
                        selectedTaste = selectedTaste,
                        recommendation = recommendation,
                        onSave = { viewModel.saveToDiary { onNavigateToDiary() } },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrewLabSelectCoffeeScreen(
    onBack: () -> Unit,
    onCoffeeSelected: (coffeeId: String, fromPantry: Boolean, pantryItemId: String?) -> Unit,
    onAddToPantryClick: () -> Unit,
    onCreateCoffeeClick: () -> Unit,
    viewModel: BrewLabViewModel = hiltViewModel()
) {
    val pantryItems by viewModel.pantryItems.collectAsState()
    val allCoffees by viewModel.availableCoffees.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshPantry() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Selecciona café", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
        ChooseCoffeeStep(
            pantryItems = pantryItems,
            allCoffees = allCoffees,
            searchQuery = searchQuery,
            onSearchQueryChange = viewModel::onSearchQueryChanged,
            onAddToPantryClick = onAddToPantryClick,
            onCreateCoffeeClick = onCreateCoffeeClick,
            onCoffeeSelected = { coffee, fromPantry, pantryItemId -> onCoffeeSelected(coffee.id, fromPantry, pantryItemId) }
        )
        }
    }
}
