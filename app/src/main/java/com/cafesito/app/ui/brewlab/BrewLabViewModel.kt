package com.cafesito.app.ui.brewlab

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.*
import com.cafesito.shared.domain.brew.BrewCaffeineInput
import com.cafesito.shared.domain.brew.BrewEngine
import com.cafesito.shared.domain.brew.BrewMethodProfile
import com.cafesito.shared.domain.brew.BrewSource
import com.cafesito.shared.domain.brew.BrewTimeProfile
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
    BREWING("Proceso en curso")
}

data class BrewMethod(
    val name: String,
    val grindSize: String,
    val tempRange: String,
    val iconResName: String = "maq_espresso"
)

data class BaristaTip(
    val label: String,
    val value: String,
    val iconKey: String
)

data class BrewPhaseInfo(
    val label: String,
    val instruction: String,
    val durationSeconds: Int,
    val isActive: Boolean = false
)

@HiltViewModel
class BrewLabViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val coffeeRepository: CoffeeRepository
) : ViewModel() {

    private val _currentStep = MutableStateFlow(BrewStep.CHOOSE_METHOD)
    val currentStep = _currentStep.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    // --- 1. CHOOSE METHOD ---
    val brewMethods = listOf(
        BrewMethod("Aeropress", "Fina/Media", "80-85C", iconResName = "maq_aeropress"),
        BrewMethod("Chemex", "Media-Gruesa", "92-94C", iconResName = "maq_chemex"),
        BrewMethod("Espresso", "Fina", "90-94C", iconResName = "maq_espresso"),
        BrewMethod("Goteo", "Media", "92-96C", iconResName = "maq_goteo"),
        BrewMethod("Hario V60", "Media-Fina", "91-93C", iconResName = "maq_hario_v60"),
        BrewMethod("Italiana", "Media-Fina", "95C", iconResName = "maq_italiana"),
        BrewMethod("Manual", "Media", "92-96C", iconResName = "maq_manual"),
        BrewMethod("Prensa francesa", "Gruesa (Sal)", "94-96C", iconResName = "maq_prensa_francesa"),
        BrewMethod("Sifon", "Media", "91-94C", iconResName = "maq_sifon"),
        BrewMethod("Turco", "Extrafina", "95C", iconResName = "maq_turco")
    )

    private val _selectedMethod = MutableStateFlow<BrewMethod?>(null)
    val selectedMethod = _selectedMethod.asStateFlow()

    val selectedMethodProfile: StateFlow<BrewMethodProfile> = _selectedMethod
        .map { method -> BrewEngine.methodProfileFor(method?.name.orEmpty()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BrewEngine.methodProfileFor(""))
    val selectedTimeProfile: StateFlow<BrewTimeProfile> = _selectedMethod
        .map { method -> BrewEngine.timeProfileFor(method?.name.orEmpty()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BrewEngine.timeProfileFor(""))

    fun selectMethod(method: BrewMethod) {
        _selectedMethod.value = method
        val profile = BrewEngine.methodProfileFor(method.name)
        val timeProfile = BrewEngine.timeProfileFor(method.name)
        _waterAmount.value = profile.defaultWaterMl.toFloat()
        _ratio.value = profile.defaultRatio.toFloat()
        _brewTimeSeconds.value = timeProfile.defaultSeconds
        _currentStep.value = BrewStep.CHOOSE_COFFEE
    }

    // --- 2. CHOOSE COFFEE ---
    val pantryItems = diaryRepository.getPantryItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableCoffees = coffeeRepository.allCoffees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPantryItem = MutableStateFlow<PantryItemWithDetails?>(null)
    val selectedPantryItem = _selectedPantryItem.asStateFlow()

    private val _selectedCoffee = MutableStateFlow<Coffee?>(null)
    val selectedCoffee = _selectedCoffee.asStateFlow()

    private val _isCoffeeFromPantry = MutableStateFlow(false)
    val isCoffeeFromPantry = _isCoffeeFromPantry.asStateFlow()

    fun refreshPantry() {
        diaryRepository.triggerRefresh()
    }

    /** Refresca lista de cafés y despensa para que el café recién creado desde Brew Lab aparezca y se pueda seleccionar. */
    fun refreshCoffeesAndPantry() {
        coffeeRepository.triggerRefresh()
        diaryRepository.triggerRefresh()
    }

    fun selectPantryItem(item: PantryItemWithDetails) {
        _selectedPantryItem.value = item
        _selectedCoffee.value = item.coffee
        _isCoffeeFromPantry.value = true
        _waterAmount.value = selectedMethodProfile.value.defaultWaterMl.toFloat()
        _hasTimerStarted.value = false
        _currentStep.value = BrewStep.CONFIGURATION
    }

    fun selectCoffeeFromCatalog(coffee: Coffee) {
        _selectedPantryItem.value = null
        _selectedCoffee.value = coffee
        _isCoffeeFromPantry.value = false
        _waterAmount.value = selectedMethodProfile.value.defaultWaterMl.toFloat()
        _hasTimerStarted.value = false
        _currentStep.value = BrewStep.CONFIGURATION
    }

    /** Llamado cuando el usuario cancela/cierra el flujo de añadir café sin guardar; vuelve a «Elige tu café». */
    fun onCoffeeAddedFromPantryFlow() {
        _currentStep.value = BrewStep.CHOOSE_COFFEE
        _searchQuery.value = ""
        refreshPantry()
    }

    // --- 3. CONFIGURATION ---
        private val _waterAmount = MutableStateFlow(250f)
    val waterAmount = _waterAmount.asStateFlow()

    private val _ratio = MutableStateFlow(16f)
    val ratio = _ratio.asStateFlow()
    private val _brewTimeSeconds = MutableStateFlow(180)
    val brewTimeSeconds = _brewTimeSeconds.asStateFlow()
    val isEspressoMethod = _selectedMethod
        .map { method -> method?.name?.contains("Espresso", ignoreCase = true) == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isRatioEditable = _selectedMethod
        .map { method ->
            val key = method?.name?.lowercase().orEmpty()
            !(key.contains("espresso") || key.contains("italiana") || key.contains("turco"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val isWaterEditable = isEspressoMethod
        .map { isEspresso -> !isEspresso }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val coffeeGrams = combine(_waterAmount, _ratio) { water, rat ->
        if (rat <= 0f) 0f else water / rat
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15.6f)

    val baristaTips = combine(_selectedMethod, _ratio, _waterAmount, coffeeGrams, _brewTimeSeconds) { method, ratio, water, grams, brewTime ->
        BrewEngine.baristaTipsForMethod(
            method = method?.name.orEmpty(),
            ratio = ratio.toDouble(),
            waterMl = water.toInt(),
            coffeeGrams = grams.toDouble(),
            brewTimeSeconds = brewTime
        ).map {
            BaristaTip(label = it.label, value = it.value, iconKey = it.iconKey)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val brewValuation = combine(_selectedMethod, _ratio, _waterAmount) { method, rat, water ->
        if (method == null) return@combine ""
        BrewEngine.brewAdvice(method.name, rat.toDouble(), water.toInt())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun setWaterAmount(value: Float) {
        val profile = selectedMethodProfile.value
        val clamped = value.coerceIn(profile.waterMinMl.toFloat(), profile.waterMaxMl.toFloat())
        _waterAmount.value = snapToStep(clamped, profile.waterStepMl.toFloat(), profile.waterMinMl.toFloat())
    }

    fun setRatio(value: Float) {
        if (!isRatioEditable.value) {
            _ratio.value = selectedMethodProfile.value.defaultRatio.toFloat()
            return
        }
        val profile = selectedMethodProfile.value
        val clamped = value.coerceIn(profile.ratioMin.toFloat(), profile.ratioMax.toFloat())
        _ratio.value = snapToStep(clamped, profile.ratioStep.toFloat(), profile.ratioMin.toFloat())
    }

    fun setCoffeeGrams(value: Float) {
        val safe = value.coerceIn(1f, 250f)
        if (isEspressoMethod.value) {
            val yieldMl = (safe * selectedMethodProfile.value.defaultRatio.toFloat()).toInt()
            _waterAmount.value = yieldMl.toFloat()
            _ratio.value = selectedMethodProfile.value.defaultRatio.toFloat()
            return
        }
        if (!isRatioEditable.value) {
            val waterMl = (safe * selectedMethodProfile.value.defaultRatio.toFloat()).toInt()
            _waterAmount.value = waterMl.toFloat()
            _ratio.value = selectedMethodProfile.value.defaultRatio.toFloat()
            return
        }
        val nextRatio = if (safe <= 0f) selectedMethodProfile.value.defaultRatio.toFloat() else _waterAmount.value / safe
        setRatio(nextRatio)
    }

    fun setBrewTimeSeconds(value: Int) {
        val p = selectedTimeProfile.value
        _brewTimeSeconds.value = value.coerceIn(p.minSeconds, p.maxSeconds)
    }

    private fun snapToStep(value: Float, step: Float, base: Float): Float {
        if (step <= 0f) return value
        val ticks = ((value - base) / step).toInt()
        val snappedDown = base + (ticks * step)
        val snappedUp = snappedDown + step
        return if (value - snappedDown >= snappedUp - value) snappedUp else snappedDown
    }
    fun startBrewing() {
        _timerSeconds.value = 0
        _hasTimerStarted.value = false
        _currentStep.value = BrewStep.BREWING
    }

    // --- 4. BREWING LOGIC ---
    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds = _timerSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning = _isTimerRunning.asStateFlow()

    private val _hasTimerStarted = MutableStateFlow(false)
    val hasTimerStarted = _hasTimerStarted.asStateFlow()

    private val _phaseEvent = MutableSharedFlow<Unit>()
    val phaseEvent = _phaseEvent.asSharedFlow()

    val phasesTimeline: StateFlow<List<BrewPhaseInfo>> = combine(_waterAmount, _selectedMethod, _brewTimeSeconds) { water, method, brewTime ->
        val isEspresso = method?.name?.contains("Espresso", ignoreCase = true) == true
        BrewEngine.timelineForMethod(
            method = method?.name.orEmpty(),
            waterMl = water.toInt(),
            espressoSeconds = if (isEspresso) brewTime else null,
            targetTotalSeconds = null
        ).map {
            BrewPhaseInfo(
                label = it.label,
                instruction = it.instruction,
                durationSeconds = it.durationSeconds
            )
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

    private data class BrewingAdviceContext(
        val method: BrewMethod?,
        val ratio: Float,
        val water: Float,
        val phaseIndex: Int,
        val timeline: List<BrewPhaseInfo>
    )

    val brewingProcessAdvice: StateFlow<String> = combine(
        _selectedMethod,
        _ratio,
        _waterAmount,
        currentPhaseIndex,
        phasesTimeline
    ) { method, ratio, water, phaseIndex, timeline ->
        BrewingAdviceContext(method, ratio, water, phaseIndex, timeline)
    }.combine(secondsRemainingInPhase) { ctx, remaining ->
        if (ctx.method == null || ctx.timeline.isEmpty()) return@combine ""
        val phaseLabel = ctx.timeline.getOrNull(ctx.phaseIndex)?.label.orEmpty()
        BrewEngine.brewingProcessAdvice(
            method = ctx.method.name,
            ratio = ctx.ratio.toDouble(),
            waterMl = ctx.water.toInt(),
            phaseLabel = phaseLabel,
            remainingInPhaseSeconds = remaining,
            brewTimeSeconds = if (isEspressoMethod.value) _brewTimeSeconds.value else null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private var timerJob: Job? = null

    fun toggleTimer() {
        if (!_hasTimerStarted.value) _hasTimerStarted.value = true
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
                    timerJob?.cancel()
                }
            }
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _timerSeconds.value = 0
        _hasTimerStarted.value = false
    }

    fun saveToDiary(onSuccess: () -> Unit) {
        val coffee = _selectedCoffee.value ?: return
        val isFromPantry = _isCoffeeFromPantry.value
        viewModelScope.launch {
            try {
                val grams = coffeeGrams.value.toInt()
                diaryRepository.addDiaryEntry(
                    coffeeId = coffee.id,
                    coffeeName = coffee.nombre,
                    coffeeBrand = coffee.marca,
                    caffeineAmount = BrewEngine.estimateCaffeineMg(
                        BrewCaffeineInput(
                            source = BrewSource.BREWLAB,
                            methodOrPreparation = _selectedMethod.value?.name.orEmpty(),
                            coffeeGrams = grams.toDouble(),
                            hasCaffeine = BrewEngine.hasCaffeineFromLabel(coffee.cafeina)
                        )
                    ),
                    amountMl = _waterAmount.value.toInt(),
                    coffeeGrams = grams,
                    preparationType = "Lab: ${_selectedMethod.value?.name} (${_selectedTaste.value})",
                    sizeLabel = BrewEngine.cupSizeLabelForAmountMl(_waterAmount.value.toInt()),
                    reduceFromPantry = isFromPantry
                )
                onSuccess()
                resetAll()
            } catch (e: Exception) {
                Log.e("BrewLab", "Error al guardar", e)
            }
        }
    }

    private val _selectedTaste = MutableStateFlow<String?>(null)
    val selectedTaste = _selectedTaste.asStateFlow()

    private val _dialInRecommendation = MutableStateFlow<String?>(null)
    val dialInRecommendation = _dialInRecommendation.asStateFlow()

    fun onTasteFeedback(taste: String) {
        _selectedTaste.value = taste
        _dialInRecommendation.value = when (taste) {
            "Amargo" -> "Sobre-extracción: muele más grueso o baja la temperatura 2°C para reducir el amargor."
            "Ácido" -> "Sub-extracción: muele más fino o vierte más lento para aumentar el contacto con el agua."
            "Equilibrado" -> "¡Perfil perfecto! Has logrado un equilibrio ideal entre dulzor, acidez y cuerpo."
            "Salado" -> "Muy sub-extraído: muele mucho más fino. Indica que el agua no ha extraído los azúcares."
            "Acuoso" -> "Poca intensidad: usa un ratio de café más alto o intenta una molienda un punto más fina."
            "Aspero" -> "Astringencia: evita remover en exceso y asegúrate de que el filtro esté bien lavado."
            "Dulce" -> "¡Extraordinario! Has resaltado los azúcares naturales del grano. Mantén estos ajustes."
            else -> null
        }
    }

    fun resetAll() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _timerSeconds.value = 0
        _selectedTaste.value = null
        _dialInRecommendation.value = null
        _selectedPantryItem.value = null
        _selectedCoffee.value = null
        _isCoffeeFromPantry.value = false
        _currentStep.value = BrewStep.CHOOSE_METHOD
    }

    fun backStep() {
        val current = _currentStep.value
        if (current == BrewStep.BREWING) {
            resetTimer()
        }
        _currentStep.value = when(current) {
            BrewStep.CHOOSE_COFFEE -> BrewStep.CHOOSE_METHOD
            BrewStep.CONFIGURATION -> BrewStep.CHOOSE_COFFEE
            BrewStep.BREWING -> BrewStep.CONFIGURATION
            else -> BrewStep.CHOOSE_METHOD
        }
    }

    /** True when brewing timer has reached total duration (user can then select taste and save). */
    val timerEnded: StateFlow<Boolean> = combine(timerSeconds, phasesTimeline) { seconds, timeline ->
        val total = timeline.sumOf { it.durationSeconds }
        total > 0 && seconds >= total
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
}




