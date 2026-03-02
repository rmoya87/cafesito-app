package com.cafesito.app.ui.brewlab

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.*
import com.cafesito.app.ui.utils.CaffeineCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale
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
        BrewMethod("Aeropress", "Fina/Media", "80-85°C", defaultRatio = 14f, iconResName = "maq_aeropress"),
        BrewMethod("Chemex", "Media-Gruesa", "92-94°C", iconResName = "maq_chemex"),
        BrewMethod("Espresso", "Fina", "90-94°C", hasWaterAdjustment = false, defaultRatio = 18f, iconResName = "maq_espresso"),
        BrewMethod("Goteo", "Media", "92-96°C", iconResName = "maq_goteo"),
        BrewMethod("Hario V60", "Media-Fina", "91-93°C", iconResName = "maq_hario_v60"),
        BrewMethod("Italiana", "Media-Fina", "95°C", hasWaterAdjustment = false, defaultRatio = 20f, iconResName = "maq_italiana"),
        BrewMethod("Manual", "Media", "92-96°C", iconResName = "maq_manual"),
        BrewMethod("Prensa francesa", "Gruesa (Sal)", "94-96°C", defaultRatio = 15f, iconResName = "maq_prensa_francesa"),
        BrewMethod("Sifón", "Media", "91-94°C", iconResName = "maq_sifon"),
        BrewMethod("Turco", "Extrafina", "95°C", iconResName = "maq_turco")
    )

    private val _selectedMethod = MutableStateFlow<BrewMethod?>(null)
    val selectedMethod = _selectedMethod.asStateFlow()

    val baristaTips = _selectedMethod
        .map { method -> buildBaristaTips(method) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), buildBaristaTips(null))

    fun selectMethod(method: BrewMethod) {
        _selectedMethod.value = method
        _ratio.value = method.defaultRatio
        _currentStep.value = BrewStep.CHOOSE_COFFEE
    }

    private fun buildBaristaTips(method: BrewMethod?): List<BaristaTip> {
        val methodName = normalizeMethodName(method?.name.orEmpty())
        val grind = method?.grindSize ?: "Media"
        val temp = method?.tempRange ?: "92-96°C"
        val defaults = listOf(
            BaristaTip("MOLIENDA", grind, "grind"),
            BaristaTip("TEMPERATURA", temp, "thermostat"),
            BaristaTip("RATIO", "1:15 a 1:17", "coffee"),
            BaristaTip("BLOOM", "30-45s con 2x de agua", "water"),
            BaristaTip("VERTIDO", "Constante y en espiral", "water"),
            BaristaTip("TIEMPO", "2:30-3:30", "clock"),
            BaristaTip("AJUSTE ACIDEZ", "Muele más fino", "grind"),
            BaristaTip("AJUSTE AMARGOR", "Muele más grueso", "grind")
        )
        if (methodName.isBlank()) return defaults

        return when {
            methodName.contains("espresso") -> listOf(
                BaristaTip("MOLIENDA", "Fina", "grind"),
                BaristaTip("TEMPERATURA", "90-94°C", "thermostat"),
                BaristaTip("RATIO", "1:2 (ej. 18g -> 36g)", "coffee"),
                BaristaTip("TIEMPO", "25-32s", "clock"),
                BaristaTip("DISTRIBUCIÓN", "Nivela antes del tamp", "coffee"),
                BaristaTip("PREINFUSIÓN", "Suave para evitar canalización", "water"),
                BaristaTip("AJUSTE RÁPIDO", "Si corre rápido, más fino", "grind"),
                BaristaTip("AJUSTE LENTO", "Si se ahoga, más grueso", "grind")
            )
            methodName.contains("italiana") -> listOf(
                BaristaTip("MOLIENDA", "Media-fina", "grind"),
                BaristaTip("AGUA", "Caliente en base", "water"),
                BaristaTip("FUEGO", "Medio-bajo", "thermostat"),
                BaristaTip("CORTE", "Retira al primer burbujeo", "clock"),
                BaristaTip("FILTRO", "No compactar café", "coffee"),
                BaristaTip("RATIO", "Más café para más cuerpo", "coffee"),
                BaristaTip("AMARGOR", "Evita fuego alto", "thermostat")
            )
            methodName.contains("aeropress") -> listOf(
                BaristaTip("MOLIENDA", "Fina-media", "grind"),
                BaristaTip("TEMPERATURA", "85-92°C", "thermostat"),
                BaristaTip("INFUSIÓN", "1:30-2:00", "clock"),
                BaristaTip("PRESION", "Suave y constante", "coffee"),
                BaristaTip("REMOVIDO", "1-2 agitaciones suaves", "water"),
                BaristaTip("PAPEL", "Más limpieza en taza", "coffee"),
                BaristaTip("METAL", "Más cuerpo y textura", "coffee")
            )
            methodName.contains("chemex") -> listOf(
                BaristaTip("MOLIENDA", "Media-gruesa", "grind"),
                BaristaTip("TEMPERATURA", "93-96°C", "thermostat"),
                BaristaTip("FILTRO", "Enjuague generoso", "water"),
                BaristaTip("TIEMPO", "3:30-4:30", "clock"),
                BaristaTip("VERTIDO", "Pausado, sin colapsar filtro", "water"),
                BaristaTip("RATIO", "1:15 a 1:16", "coffee"),
                BaristaTip("AJUSTE LENTO", "Si drena lento, más grueso", "grind")
            )
            methodName.contains("prensa") -> listOf(
                BaristaTip("MOLIENDA", "Gruesa y uniforme", "grind"),
                BaristaTip("TEMPERATURA", "93-96°C", "thermostat"),
                BaristaTip("INFUSIÓN", "4:00", "clock"),
                BaristaTip("PRENSADO", "Lento, sin golpear", "coffee"),
                BaristaTip("COSTRA", "Romper y retirar espuma", "water"),
                BaristaTip("RATIO", "1:14 a 1:16", "coffee"),
                BaristaTip("DECANTAR", "Servir al terminar", "clock")
            )
            methodName.contains("sifon") -> listOf(
                BaristaTip("MOLIENDA", "Media", "grind"),
                BaristaTip("TEMPERATURA", "91-94°C", "thermostat"),
                BaristaTip("AGITACION", "Suave y breve", "water"),
                BaristaTip("BAJADA", "45-60s al vacío", "clock"),
                BaristaTip("HERVOR", "Controlado, no violento", "thermostat"),
                BaristaTip("CONTACTO", "1:30-2:30 total", "clock"),
                BaristaTip("FILTRO", "Limpio para evitar rancidez", "coffee")
            )
            methodName.contains("turco") -> listOf(
                BaristaTip("MOLIENDA", "Extra fina", "grind"),
                BaristaTip("FUEGO", "Muy bajo", "thermostat"),
                BaristaTip("ESPUMA", "3 levantamientos", "coffee"),
                BaristaTip("AGUA", "Casi ebullición, no hervir", "water"),
                BaristaTip("REMOVIDO", "Solo al inicio", "water"),
                BaristaTip("DESCANSO", "Breve antes de servir", "clock"),
                BaristaTip("DENSIDAD", "Taza corta y concentrada", "coffee")
            )
            methodName.contains("goteo") -> listOf(
                BaristaTip("MOLIENDA", "Media", "grind"),
                BaristaTip("RATIO", "55-65g por litro", "coffee"),
                BaristaTip("TEMPERATURA", temp, "thermostat"),
                BaristaTip("SERVICIO", "Consumir recién hecho", "clock"),
                BaristaTip("FILTRO", "Enjuagar antes de usar", "water"),
                BaristaTip("CARGA", "Nivelar cama de café", "coffee"),
                BaristaTip("PLACA", "Evitar sobrecalentamiento", "thermostat")
            )
            methodName.contains("hario") || methodName.contains("v60") || methodName.contains("manual") -> listOf(
                BaristaTip("MOLIENDA", "Media-fina", "grind"),
                BaristaTip("BLOOM", "30-45s con 2x de agua", "water"),
                BaristaTip("TEMPERATURA", "92-96°C", "thermostat"),
                BaristaTip("TIEMPO", "2:30-3:15", "clock"),
                BaristaTip("RATIO", "1:15 a 1:17", "coffee"),
                BaristaTip("VERTIDO", "Pulsos cortos y constantes", "water"),
                BaristaTip("AJUSTE ACIDEZ", "Muele más fino", "grind"),
                BaristaTip("AJUSTE AMARGOR", "Muele más grueso", "grind")
            )
            else -> defaults
        }
    }

    private fun normalizeMethodName(value: String): String {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
        return normalized.replace("\\p{Mn}+".toRegex(), "").lowercase(Locale.ROOT)
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

    fun selectPantryItem(item: PantryItemWithDetails) {
        _selectedPantryItem.value = item
        _selectedCoffee.value = item.coffee
        _isCoffeeFromPantry.value = true
        _waterAmount.value = 250f
        _hasTimerStarted.value = false
        _currentStep.value = BrewStep.CONFIGURATION
    }

    fun selectCoffeeFromCatalog(coffee: Coffee) {
        _selectedPantryItem.value = null
        _selectedCoffee.value = coffee
        _isCoffeeFromPantry.value = false
        _waterAmount.value = 250f
        _hasTimerStarted.value = false
        _currentStep.value = BrewStep.CONFIGURATION
    }

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

    val coffeeGrams = combine(_waterAmount, _ratio, _selectedMethod) { water, rat, method ->
        if (method?.hasWaterAdjustment == false) {
            rat 
        } else {
            if (rat == 0f) 0f else water / rat
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15.6f)

    // VALORACIÓN DINÁMICA MEJORADA
    val brewValuation = combine(_selectedMethod, _ratio, _waterAmount) { method, rat, water ->
        if (method == null) return@combine ""
        
        val intensity = when (method.name) {
            "Espresso" -> when {
                rat < 16f -> "Perfil sedoso y cuerpo ligero"
                rat < 20f -> "Equilibrio perfecto de dulzor"
                else -> "Cuerpo intenso y textura densa"
            }
            "Italiana" -> when {
                rat < 18f -> "Sabor suave y retrogusto limpio"
                rat < 21f -> "Intensidad clásica con mucho cuerpo"
                else -> "Textura muy robusta y concentrada"
            }
            else -> when {
                rat < 13f -> "Cuerpo muy pesado y sabores potentes"
                rat < 15f -> "Notas dulces muy marcadas y untuosas"
                rat < 17.5f -> "Extracción equilibrada y balanceada"
                rat < 19f -> "Mayor claridad aromática y cuerpo sutil"
                else -> "Perfil muy ligero con notas acuosas"
            }
        }

        val volumeDesc = when {
            water < 150f -> "en formato de taza corta e intensa."
            water < 300f -> "en una taza estándar ideal para el día a día."
            else -> "en formato diseñado para compartir."
        }

        "$intensity $volumeDesc"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun setWaterAmount(value: Float) { _waterAmount.value = value }
    fun setRatio(value: Float) { _ratio.value = value }
    fun setCoffeeGrams(value: Float) {
        val safe = value.coerceIn(1f, 250f)
        if (_selectedMethod.value?.hasWaterAdjustment == false) {
            _ratio.value = safe
            return
        }
        val nextRatio = (_waterAmount.value / safe).coerceIn(10f, 20f)
        _ratio.value = nextRatio
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

    val phasesTimeline: StateFlow<List<BrewPhaseInfo>> = combine(_waterAmount, _selectedMethod) { water, method ->
        when (method?.name) {
            "Espresso" -> listOf(
                BrewPhaseInfo("Extracción", "Aplica presión constante. Vigila el flujo: debe ser como un hilo de miel. Busca obtener unos 36-40g de líquido final.", 25)
            )
            "Prensa francesa" -> listOf(
                BrewPhaseInfo("Inmersión", "Vierte todo el agua caliente uniformemente sobre el café. Coloca la tapa sin presionar para mantener el calor.", 240)
            )
            "Aeropress" -> listOf(
                BrewPhaseInfo("Pre-infusión", "Vierte unos 50ml de agua para humedecer todo el café. Remueve suavemente 3 veces para asegurar una extracción uniforme.", 30),
                BrewPhaseInfo("Infusión", "Añade el resto del agua. Deja que el café repose e interactúe con el agua para extraer todos sus sabores.", 90),
                BrewPhaseInfo("Presión", "Presiona el émbolo hacia abajo con una fuerza firme y constante. Escucha el 'sssh' final y detente.", 30)
            )
            "Italiana" -> {
                val boilTime = (120 + (water * 0.2)).toInt()
                listOf(
                    BrewPhaseInfo("Calentamiento", "Mantén el fuego medio-bajo. El agua en la base empezará a crear presión para subir por la chimenea.", boilTime),
                    BrewPhaseInfo("Extracción", "Cuando el café empiece a salir, baja el fuego o retíralo. Escucha el burbujeo suave y detente antes del chorro final.", 40)
                )
            }
            "Turco" -> listOf(
                BrewPhaseInfo("Infusión", "Calienta a fuego muy lento hasta que veas que se forma una espuma densa y oscura en la superficie (crema).", 120),
                BrewPhaseInfo("Levantamiento 1", "Retira el cezve del fuego justo antes de que hierva. Deja que la espuma baje un poco y vuelve al fuego.", 20),
                BrewPhaseInfo("Levantamiento 2", "Repite el proceso: deja que suba la espuma por segunda vez para intensificar el cuerpo y sabor.", 20),
                BrewPhaseInfo("Toque Final", "Último ciclo de espuma. El café turco se caracteriza por su densidad y su sedimento único.", 20)
            )
            "Sifón" -> listOf(
                BrewPhaseInfo("Ascenso", "La presión enviará el agua a la cámara superior. Espera a que se estabilice antes de añadir el café.", 90),
                BrewPhaseInfo("Mezcla", "Añade el café molido y remueve en círculos suavemente. Asegúrate de que todo el café esté sumergido.", 60),
                BrewPhaseInfo("Efecto Vacío", "Retira la fuente de calor. El enfriamiento creará un vacío que filtrará el café hacia abajo a través del filtro.", 45)
            )
            else -> { // Pour-overs
                val totalPourTime = (120 + (water - 250) * 0.18).toInt().coerceIn(90, 300)
                listOf(
                    BrewPhaseInfo("Pre-infusión", "Vierte unos ${(water/10).toInt()}ml. Verás burbujas: es el CO2 liberándose para que el agua penetre mejor.", 30),
                    BrewPhaseInfo("Desarrollo de Sabor", "Vierte en espiral desde el centro hacia afuera hasta los ${(water * 0.6).toInt()}ml. Mantén un flujo constante.", (totalPourTime * 0.4).toInt()),
                    BrewPhaseInfo("Cuerpo y Dulzor", "Añade el agua restante hasta los ${water.toInt()}ml. Hazlo con suavidad para finalizar la extracción limpiamente.", (totalPourTime * 0.6).toInt())
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
                    _currentStep.value = BrewStep.RESULT
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
                    caffeineAmount = CaffeineCalculator.calculate(
                        type = "Americano",
                        grams = grams,
                        isFromPantry = true,
                        isDecaf = coffee.cafeina.equals("No", ignoreCase = true)
                    ),
                    amountMl = _waterAmount.value.toInt(),
                    coffeeGrams = grams,
                    preparationType = "Lab: ${_selectedMethod.value?.name} (${_selectedTaste.value})",
                    reduceFromPantry = isFromPantry
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
            "Amargo" -> "Sobre-extracción: Muele más grueso o baja la temperatura 2°C para reducir el amargor."
            "Ácido" -> "Sub-extracción: Muele más fino o vierte más lento para aumentar el contacto con el agua."
            "Equilibrado" -> "¡Perfil perfecto! Has logrado un equilibrio ideal entre dulzor, acidez y cuerpo."
            "Salado" -> "Muy sub-extraído: Muele mucho más fino. Indica que el agua no ha extraído los azúcares."
            "Acuoso" -> "Poca intensidad: Usa un ratio de café más alto o intenta una molienda un punto más fina."
            "Aspero" -> "Astringencia: Evita remover en exceso y asegúrate de que el filtro esté bien lavado."
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
        _currentStep.value = when(_currentStep.value) {
            BrewStep.CHOOSE_COFFEE -> BrewStep.CHOOSE_METHOD
            BrewStep.CONFIGURATION -> BrewStep.CHOOSE_COFFEE
            BrewStep.BREWING -> BrewStep.CONFIGURATION
            BrewStep.RESULT -> BrewStep.BREWING
            else -> BrewStep.CHOOSE_METHOD
        }
    }
}
