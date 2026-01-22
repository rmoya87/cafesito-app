package com.example.cafesito.ui.brewlab

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.*
import com.example.cafesito.ui.utils.CaffeineCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BrewStep(val title: String) {
    CHOOSE_METHOD("Elaboración"),
    CHOOSE_COFFEE("Elige tu café"),
    CONFIGURATION("Configuración"),
    BREWING("Proceso en curso"),
    RESULT("Resultado")
}

data class BrewMethod(
    val name: String,
    val grindSize: String,
    val tempRange: String,
    val hasWaterAdjustment: Boolean = true,
    val defaultRatio: Float = 16f,
    val iconResName: String = "maq_espresso"
)

data class BrewPhaseInfo(
    val label: String,
    val instruction: String,
    val durationSeconds: Int,
    val isActive: Boolean = false
)

@HiltViewModel
class BrewLabViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository
) : ViewModel() {

    private val _currentStep = MutableStateFlow(BrewStep.CHOOSE_METHOD)
    val currentStep = _currentStep.asStateFlow()

    // --- 1. CHOOSE METHOD ---
    val brewMethods = listOf(
        BrewMethod("Aeropress", "Fina/Media", "85-90°C", defaultRatio = 14f, iconResName = "maq_aeropress"),
        BrewMethod("Chemex", "Media-Gruesa", "92-96°C", iconResName = "maq_chemex"),
        BrewMethod("Espresso", "Fina", "90-94°C", hasWaterAdjustment = false, defaultRatio = 18f, iconResName = "maq_espresso"),
        BrewMethod("Goteo", "Media", "92-96°C", iconResName = "maq_goteo"),
        BrewMethod("Hario V60", "Media-Fina", "92-96°C", iconResName = "maq_hario_v60"),
        BrewMethod("Italiana", "Media-Fina", "95°C", hasWaterAdjustment = false, defaultRatio = 20f, iconResName = "maq_italiana"),
        BrewMethod("Manual", "Media", "92-96°C", iconResName = "maq_manual"),
        BrewMethod("Prensa francesa", "Gruesa", "92-96°C", defaultRatio = 15f, iconResName = "maq_prensa_francesa"),
        BrewMethod("Sifón", "Media", "92-96°C", iconResName = "maq_sifon"),
        BrewMethod("Turco", "Extrafina", "95°C", iconResName = "maq_turco")
    )

    private val _selectedMethod = MutableStateFlow<BrewMethod?>(null)
    val selectedMethod = _selectedMethod.asStateFlow()

    fun selectMethod(method: BrewMethod) {
        _selectedMethod.value = method
        _ratio.value = method.defaultRatio
        _currentStep.value = BrewStep.CHOOSE_COFFEE
    }

    // --- 2. CHOOSE COFFEE ---
    val pantryItems = diaryRepository.getPantryItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPantryItem = MutableStateFlow<PantryItemWithDetails?>(null)
    val selectedPantryItem = _selectedPantryItem.asStateFlow()

    fun selectPantryItem(item: PantryItemWithDetails) {
        _selectedPantryItem.value = item
        _currentStep.value = BrewStep.CONFIGURATION
    }

    // --- 3. CONFIGURATION ---
    private val _waterAmount = MutableStateFlow(250f)
    val waterAmount = _waterAmount.asStateFlow()

    private val _ratio = MutableStateFlow(16f)
    val ratio = _ratio.asStateFlow()

    val coffeeGrams = combine(_waterAmount, _ratio, _selectedMethod) { water, rat, method ->
        if (method?.hasWaterAdjustment == false) {
            rat 
        } else {
            if (rat == 0f) 0f else water / rat
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15.6f)

    fun setWaterAmount(value: Float) { _waterAmount.value = value }
    fun setRatio(value: Float) { _ratio.value = value }

    fun startBrewing() {
        _timerSeconds.value = 0
        _currentStep.value = BrewStep.BREWING
    }

    // --- 4. BREWING LOGIC (Precise Phasing) ---
    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds = _timerSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning = _isTimerRunning.asStateFlow()

    private val _phaseEvent = MutableSharedFlow<Unit>()
    val phaseEvent = _phaseEvent.asSharedFlow()

    val phasesTimeline: StateFlow<List<BrewPhaseInfo>> = combine(_waterAmount, _selectedMethod) { water, method ->
        when (method?.name) {
            "Espresso" -> listOf(
                BrewPhaseInfo("Extracción", "Presión constante. Busca 36-40g en taza.", 25)
            )
            "Prensa francesa" -> listOf(
                BrewPhaseInfo("Infusión", "Vierte todo el agua y mantén la tapa puesta.", 240)
            )
            "Aeropress" -> listOf(
                BrewPhaseInfo("Blooming", "Vierte 50ml y remueve suavemente.", 30),
                BrewPhaseInfo("Infusión", "Añade el resto y espera el tiempo de inmersión.", 90),
                BrewPhaseInfo("Presión", "Presiona el émbolo con fuerza constante.", 30)
            )
            "Italiana" -> {
                val boilTime = (120 + (water * 0.2)).toInt()
                listOf(
                    BrewPhaseInfo("Calentamiento", "Espera a que el agua suba por el embudo.", boilTime),
                    BrewPhaseInfo("Extracción", "Baja el fuego cuando el café empiece a salir.", 40)
                )
            }
            "Turco" -> listOf(
                BrewPhaseInfo("Calentamiento", "Calienta a fuego lento hasta la primera espuma.", 120),
                BrewPhaseInfo("Espuma 1", "Retira brevemente y vuelve al fuego.", 20),
                BrewPhaseInfo("Espuma 2", "Repite el proceso para más cuerpo.", 20),
                BrewPhaseInfo("Espuma 3", "Último ciclo antes de servir.", 20)
            )
            "Sifón" -> listOf(
                BrewPhaseInfo("Ascenso", "El agua debe subir a la cámara superior.", 90),
                BrewPhaseInfo("Mezcla", "Añade el café y remueve en círculos.", 60),
                BrewPhaseInfo("Filtrado", "Retira el calor para el efecto vacío.", 45)
            )
            else -> { // Pour-overs (V60, Chemex, Goteo, Manual)
                val totalPourTime = (120 + (water - 250) * 0.18).toInt().coerceIn(90, 300)
                listOf(
                    BrewPhaseInfo("Pre-infusión", "Vierte ${(water/10).toInt()}ml para liberar gases.", 30),
                    BrewPhaseInfo("Primer vertido", "Vierte hasta el 60% (${(water * 0.6).toInt()}ml).", (totalPourTime * 0.4).toInt()),
                    BrewPhaseInfo("Segundo vertido", "Completa hasta los ${water.toInt()}ml suavemente.", (totalPourTime * 0.6).toInt())
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentPhaseIndex: StateFlow<Int> = combine(_timerSeconds, phasesTimeline) { seconds, timeline ->
        var elapsed = 0
        var index = timeline.size - 1
        for (i in timeline.indices) {
            if (seconds < elapsed + timeline[i].durationSeconds) {
                index = i
                break
            }
            elapsed += timeline[i].durationSeconds
        }
        index
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val secondsRemainingInPhase: StateFlow<Int> = combine(_timerSeconds, phasesTimeline) { seconds, timeline ->
        var elapsed = 0
        var remaining = 0
        for (phase in timeline) {
            if (seconds < elapsed + phase.durationSeconds) {
                remaining = (elapsed + phase.durationSeconds) - seconds
                break
            }
            elapsed += phase.durationSeconds
        }
        remaining
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private var timerJob: Job? = null

    fun toggleTimer() {
        if (_isTimerRunning.value) {
            timerJob?.cancel()
            _isTimerRunning.value = false
        } else {
            _isTimerRunning.value = true
            startTimerLogic()
        }
    }

    private fun startTimerLogic() {
        timerJob = viewModelScope.launch {
            var lastIdx = currentPhaseIndex.value
            while (_isTimerRunning.value) {
                delay(1000)
                _timerSeconds.value++
                
                if (currentPhaseIndex.value != lastIdx) {
                    lastIdx = currentPhaseIndex.value
                    _phaseEvent.emit(Unit)
                }
                
                val totalDuration = phasesTimeline.value.sumOf { it.durationSeconds }
                if (_timerSeconds.value >= totalDuration && totalDuration > 0) {
                    _isTimerRunning.value = false
                    _currentStep.value = BrewStep.RESULT
                    timerJob?.cancel()
                }
            }
        }
    }

    fun saveToDiary(onSuccess: () -> Unit) {
        val item = _selectedPantryItem.value ?: return
        viewModelScope.launch {
            try {
                val grams = coffeeGrams.value.toInt()
                diaryRepository.addDiaryEntry(
                    coffeeId = item.coffee.id,
                    coffeeName = item.coffee.nombre,
                    coffeeBrand = item.coffee.marca,
                    caffeineAmount = CaffeineCalculator.calculate("Americano", grams, true),
                    amountMl = _waterAmount.value.toInt(),
                    coffeeGrams = grams,
                    preparationType = "Lab: ${_selectedMethod.value?.name} (${_selectedTaste.value})"
                )
                onSuccess()
                resetAll()
            } catch (e: Exception) {
                Log.e("BrewLab", "Error al guardar", e)
            }
        }
    }

    // --- 5. RESULT ---
    private val _selectedTaste = MutableStateFlow<String?>(null)
    val selectedTaste = _selectedTaste.asStateFlow()

    private val _dialInRecommendation = MutableStateFlow<String?>(null)
    val dialInRecommendation = _dialInRecommendation.asStateFlow()

    fun onTasteFeedback(taste: String) {
        _selectedTaste.value = taste
        _dialInRecommendation.value = when (taste) {
            "Amargo" -> "Muele más grueso o baja la temperatura 2°C."
            "Ácido" -> "Muele más fino o vierte más lento."
            "Equilibrado" -> "¡Excelente receta!"
            else -> null
        }
    }

    fun resetAll() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _timerSeconds.value = 0
        _selectedTaste.value = null
        _dialInRecommendation.value = null
        _currentStep.value = BrewStep.CHOOSE_METHOD
    }

    fun backStep() {
        _currentStep.value = when(_currentStep.value) {
            BrewStep.CHOOSE_COFFEE -> BrewStep.CHOOSE_METHOD
            BrewStep.CONFIGURATION -> BrewStep.CHOOSE_COFFEE
            BrewStep.BREWING -> BrewStep.CONFIGURATION
            BrewStep.RESULT -> BrewStep.BREWING
            else -> BrewStep.CHOOSE_METHOD
        }
    }
}
