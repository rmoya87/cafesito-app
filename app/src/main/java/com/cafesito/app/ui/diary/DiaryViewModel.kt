package com.cafesito.app.ui.diary

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.*
import com.cafesito.shared.domain.diary.DiaryAnalyticsTargets
import com.cafesito.shared.domain.diary.DiaryPeriod as SharedDiaryPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

enum class DiaryPeriod { HOY, SEMANA, MES }

data class ChartEntry(
    val label: String,
    val caffeine: Int,
    val water: Int
)

data class DiaryAnalytics(
    val chartData: List<ChartEntry>,
    val waterCount: Int,
    val totalWaterMl: Int,
    val cupsCount: Int,
    val totalCaffeine: Int,
    val averageCaffeine: Int,
    /** Media de un día en los últimos 30 días (común Android/iOS/webapp). */
    val avgCaffeineLast30: Int,
    val avgCupsLast30: Float,
    val avgHydrationPctLast30: Int,
    /** Porcentaje de tendencia cafeína vs objetivo (lógica compartida con webApp). */
    val caffeineTrendPct: Int,
    /** Porcentaje de tendencia hidratación vs objetivo (lógica compartida con webApp). */
    val hydrationTrendPct: Int,
    /** Progreso hidratación 0..100 (lógica compartida con webApp). */
    val hydrationProgressPct: Int,
    val period: DiaryPeriod
)

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val coffeeRepository: CoffeeRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(DiaryPeriod.HOY)
    val selectedPeriod: StateFlow<DiaryPeriod> = _selectedPeriod.asStateFlow()

    private val calendarTodayStart: Long
        get() {
            val c = Calendar.getInstance()
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }
    private val _selectedDiaryDateMs = MutableStateFlow(calendarTodayStart)
    val selectedDiaryDateMs: StateFlow<Long> = _selectedDiaryDateMs.asStateFlow()

    fun setSelectedDiaryDateMs(dayStartMs: Long) { _selectedDiaryDateMs.value = dayStartMs }
    fun prevDay() {
        val c = Calendar.getInstance().apply { timeInMillis = _selectedDiaryDateMs.value }
        c.add(Calendar.DAY_OF_MONTH, -1)
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        _selectedDiaryDateMs.value = c.timeInMillis
    }
    fun nextDay() {
        if (_selectedDiaryDateMs.value >= calendarTodayStart) return
        val c = Calendar.getInstance().apply { timeInMillis = _selectedDiaryDateMs.value }
        c.add(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        _selectedDiaryDateMs.value = c.timeInMillis
    }

    private val allEntriesFlow = diaryRepository.getDiaryEntries()

    /** Todas las entradas del diario (sin filtrar por periodo), para el calendario y marcar días con café/agua. */
    val allDiaryEntries: StateFlow<List<DiaryEntryEntity>> = allEntriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val diaryEntries: StateFlow<List<DiaryEntryEntity>> = combine(
        allEntriesFlow,
        _selectedPeriod,
        _selectedDiaryDateMs
    ) { entries, period, selectedDateMs ->
        val calendar = Calendar.getInstance()
        val startTime = when (period) {
            DiaryPeriod.HOY -> selectedDateMs
            DiaryPeriod.SEMANA -> {
                calendar.timeInMillis = selectedDateMs
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            DiaryPeriod.MES -> {
                calendar.timeInMillis = selectedDateMs
                calendar.set(Calendar.DAY_OF_MONTH, 1); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
        }
        val endTime = when (period) {
            DiaryPeriod.HOY -> startTime + 86400000L
            DiaryPeriod.SEMANA -> startTime + 604800000L
            DiaryPeriod.MES -> {
                calendar.timeInMillis = startTime
                calendar.add(Calendar.MONTH, 1)
                calendar.timeInMillis
            }
        }
        entries
            .filter { it.timestamp >= startTime && it.timestamp < endTime }
            .sortedByDescending { it.timestamp }
    }
    .onEach { _isLoading.value = false } 
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pantryItems: StateFlow<List<PantryItemWithDetails>> = diaryRepository.getPantryItems()
        .onEach { _isLoading.value = false } 
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val finishedCoffees: StateFlow<List<FinishedCoffeeWithDetails>> = diaryRepository.getFinishedCoffees()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableCoffees: StateFlow<List<CoffeeWithDetails>> = coffeeRepository.allCoffees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val analytics: StateFlow<DiaryAnalytics?> = combine(allEntriesFlow, _selectedPeriod, _selectedDiaryDateMs) { entries, period, selectedDateMs ->
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()
        calendar.timeInMillis = now
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis

        val (currentEntries, previousEntries, averageValue) = when (period) {
            DiaryPeriod.HOY -> {
                val dayStart = selectedDateMs
                val dayEnd = dayStart + 86400000L
                val dailySums = entries.groupBy {
                    val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
                }.values.map { it.filter { e -> e.type == "CUP" }.sumOf { e -> e.caffeineAmount } }
                Triple(
                    entries.filter { it.timestamp in dayStart until dayEnd },
                    entries.filter { it.timestamp in (dayStart - 86400000) until dayStart },
                    if (dailySums.isEmpty()) 0 else dailySums.average().toInt()
                )
            }
            DiaryPeriod.SEMANA -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                val startOfWeek = calendar.timeInMillis
                val weeklySums = entries.groupBy { val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }; "${c.get(Calendar.YEAR)}-${c.get(Calendar.WEEK_OF_YEAR)}" }.values.map { it.filter { e -> e.type == "CUP" }.sumOf { e -> e.caffeineAmount } }
                Triple(entries.filter { it.timestamp >= startOfWeek }, entries.filter { it.timestamp in (startOfWeek - 604800000) until startOfWeek }, if(weeklySums.isEmpty()) 0 else weeklySums.average().toInt())
            }
            DiaryPeriod.MES -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.DAY_OF_MONTH, 1); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                val startOfMonth = calendar.timeInMillis
                val monthlySums = entries.groupBy { val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }; "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH)}" }.values.map { it.filter { e -> e.type == "CUP" }.sumOf { e -> e.caffeineAmount } }
                Triple(entries.filter { it.timestamp >= startOfMonth }, emptyList<DiaryEntryEntity>(), if(monthlySums.isEmpty()) 0 else monthlySums.average().toInt())
            }
        }

        val totalCaffeine = currentEntries.filter { it.type == "CUP" }.sumOf { it.caffeineAmount }
        val totalWaterMl = currentEntries.filter { it.type == "WATER" }.sumOf { it.amountMl }

        val sharedPeriod = when (period) {
            DiaryPeriod.HOY -> SharedDiaryPeriod.HOY
            DiaryPeriod.SEMANA -> SharedDiaryPeriod.SEMANA
            DiaryPeriod.MES -> SharedDiaryPeriod.MES
        }
        val caffeineTarget = DiaryAnalyticsTargets.caffeineTargetMg(sharedPeriod)
        val hydrationTarget = DiaryAnalyticsTargets.hydrationTargetMl(sharedPeriod)
        val caffeineTrendPct = DiaryAnalyticsTargets.trendPercent(totalCaffeine, caffeineTarget)
        val hydrationTrendPct = DiaryAnalyticsTargets.trendPercent(totalWaterMl, hydrationTarget)
        val hydrationProgressPct = DiaryAnalyticsTargets.hydrationProgressPercent(totalWaterMl, sharedPeriod)

        val dayMs = 86400000L
        val hydrationTargetDay = 2000
        val last30Avg = run {
            calendar.timeInMillis = now
            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            val todayStart = calendar.timeInMillis
            var sumCaffeine = 0; var sumCups = 0; var sumHydrationPct = 0
            for (i in 0 until 30) {
                val dayStart = todayStart - (29 - i) * dayMs
                val dayEnd = dayStart + dayMs
                val dayEntries = entries.filter { it.timestamp in dayStart until dayEnd }
                sumCaffeine += dayEntries.filter { it.type == "CUP" }.sumOf { it.caffeineAmount }
                sumCups += dayEntries.count { it.type == "CUP" }
                val waterMl = dayEntries.filter { it.type == "WATER" }.sumOf { it.amountMl }
                sumHydrationPct += (waterMl.toDouble() / hydrationTargetDay * 100).toInt().coerceIn(0, 100)
            }
            Triple(sumCaffeine / 30, sumCups / 30.0, sumHydrationPct / 30)
        }

        val chartData = when (period) {
            DiaryPeriod.HOY -> {
                val hourlyData = currentEntries.groupBy { 
                    Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.HOUR_OF_DAY)
                }
                (0..23).map { hour -> 
                    val caffeineVal = hourlyData[hour]?.filter { it.type == "CUP" }?.sumOf { it.caffeineAmount } ?: 0
                    val waterVal = hourlyData[hour]?.filter { it.type == "WATER" }?.sumOf { it.amountMl } ?: 0
                    ChartEntry(String.format("%02d:00", hour), caffeineVal, waterVal)
                }
            }
            DiaryPeriod.SEMANA -> {
                val weeklyData = currentEntries.groupBy { 
                    val day = Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.DAY_OF_WEEK)
                    if (day == Calendar.SUNDAY) 6 else day - 2
                }
                listOf("Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Dom").mapIndexed { i, d -> 
                    val caffeineVal = weeklyData[i]?.filter { it.type == "CUP" }?.sumOf { it.caffeineAmount } ?: 0
                    val waterVal = weeklyData[i]?.filter { it.type == "WATER" }?.sumOf { it.amountMl } ?: 0
                    ChartEntry(d, caffeineVal, waterVal)
                }
            }
            DiaryPeriod.MES -> {
                val currentCalendar = Calendar.getInstance()
                val maxDays = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                val monthlyData = currentEntries.groupBy { 
                    Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.DAY_OF_MONTH)
                }
                (1..maxDays).map { day -> 
                    val caffeineVal = monthlyData[day]?.filter { it.type == "CUP" }?.sumOf { it.caffeineAmount } ?: 0
                    val waterVal = monthlyData[day]?.filter { it.type == "WATER" }?.sumOf { it.amountMl } ?: 0
                    ChartEntry(day.toString(), caffeineVal, waterVal)
                }
            }
        }

        DiaryAnalytics(
            chartData = chartData,
            waterCount = currentEntries.count { it.type == "WATER" },
            totalWaterMl = totalWaterMl,
            cupsCount = currentEntries.count { it.type == "CUP" },
            totalCaffeine = totalCaffeine,
            averageCaffeine = averageValue,
            avgCaffeineLast30 = last30Avg.first,
            avgCupsLast30 = last30Avg.second.toFloat(),
            avgHydrationPctLast30 = last30Avg.third,
            caffeineTrendPct = caffeineTrendPct,
            hydrationTrendPct = hydrationTrendPct,
            hydrationProgressPct = hydrationProgressPct,
            period = period
        )
    }
    .onEach { _isLoading.value = false }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setPeriod(period: DiaryPeriod) { _selectedPeriod.value = period }
    
    fun refreshData(showLoader: Boolean = false) {
        if (showLoader) _isLoading.value = true
        viewModelScope.launch {
            try {
                diaryRepository.syncDiaryEntriesFromRemote()
                diaryRepository.syncPantryItems()
                diaryRepository.syncPendingDiaryEntries()
                diaryRepository.triggerRefresh()
            } catch (e: Exception) { /* best-effort */ }
            finally { if (showLoader) _isLoading.value = false }
            refreshDiaryWidget()
        }
    }

    suspend fun addCoffeeConsumption(
        coffeeId: String?, 
        coffeeName: String, 
        coffeeBrand: String,
        caffeineAmount: Int, 
        amountMl: Int, 
        coffeeGrams: Int,
        preparationType: String,
        sizeLabel: String? = null
    ) {
        diaryRepository.addDiaryEntry(coffeeId, coffeeName, coffeeBrand, caffeineAmount, "CUP", amountMl, coffeeGrams, preparationType, sizeLabel)
        refreshDiaryWidget()
    }
    
    suspend fun addWaterConsumption(amountMl: Int) {
            diaryRepository.addDiaryEntry(null, "Agua", "", 0, "WATER", amountMl, 0, "None", null)

        refreshDiaryWidget()
    }
    
    fun updateEntry(entry: DiaryEntryEntity) {
        viewModelScope.launch {
            diaryRepository.updateDiaryEntry(entry)
            refreshDiaryWidget()
        }
    }

    fun deleteEntry(entryId: Long) { 
        viewModelScope.launch { 
            diaryRepository.deleteDiaryEntry(entryId) 
            refreshDiaryWidget()
        } 
    }
    
    fun addToPantry(coffeeId: String, grams: Int, onSuccess: () -> Unit = {}) { 
        viewModelScope.launch { 
            try {
                diaryRepository.addToPantry(coffeeId, grams)
                refreshDiaryWidget()
                onSuccess()
            } catch (e: Exception) {
                Log.e("DIARY_VIEWMODEL", "Error adding to pantry", e)
            }
        } 
    }
    
    fun updateStock(coffeeId: String, total: Int, remaining: Int) {
        viewModelScope.launch {
            diaryRepository.updatePantryStockFull(coffeeId, total, remaining)
            refreshDiaryWidget()
        }
    }

    fun removeFromPantry(coffeeId: String, onSuccess: () -> Unit = {}) { 
        viewModelScope.launch { 
            try {
                diaryRepository.deletePantryItem(coffeeId)
                refreshDiaryWidget()
                onSuccess()
            } catch (e: Exception) {
                Log.e("DIARY_VIEWMODEL", "Error removing from pantry", e)
            }
        } 
    }

    fun markCoffeeAsFinished(coffeeId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                diaryRepository.markCoffeeAsFinished(coffeeId)
                refreshDiaryWidget()
                onSuccess()
            } catch (e: Exception) {
                Log.e("DIARY_VIEWMODEL", "Error marking coffee as finished", e)
            }
        }
    }
    
    fun saveCustomCoffee(
        name: String, brand: String, specialty: String, roast: String?, variety: String?,
        country: String, hasCaffeine: Boolean, format: String, totalGrams: Int,
        imageUri: Uri?, onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val imageBytes = imageUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }
                }
                diaryRepository.createCustomCoffeeAndAddToPantry(
                    name, brand, specialty, roast, variety, country, hasCaffeine, format, imageBytes, totalGrams
                )
                onSuccess()
            } catch (e: Exception) {
                Log.e("DIARY_VIEWMODEL", "Error al guardar café personalizado", e)
            }
        }
    }

    fun saveCustomCoffeeForDiary(
        name: String, brand: String, specialty: String, roast: String?, variety: String?,
        country: String, hasCaffeine: Boolean, format: String,
        imageUri: Uri?, totalGrams: Int = 250, onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val imageBytes = imageUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }
                }
                val coffeeId = diaryRepository.createCustomCoffeeAndAddToPantry(
                    name = name,
                    brand = brand,
                    specialty = specialty,
                    roast = roast,
                    variety = variety,
                    country = country,
                    hasCaffeine = hasCaffeine,
                    format = format,
                    imageBytes = imageBytes,
                    totalGrams = totalGrams
                )
                coffeeId?.let(onSuccess)
            } catch (e: Exception) {
                Log.e("DIARY_VIEWMODEL", "Error al guardar café", e)
            }
        }
    }

    fun updateCustomCoffee(
        id: String, name: String, brand: String, specialty: String, roast: String?,
        variety: String?, country: String, hasCaffeine: Boolean, format: String,
        imageUri: Uri?, totalGrams: Int, onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val imageBytes = imageUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }
                }
                diaryRepository.updateCustomCoffee(
                    id, name, brand, specialty, roast, variety, country, hasCaffeine, format, imageBytes, totalGrams
                )
                refreshDiaryWidget()
                onSuccess()
            } catch (e: Exception) {
                Log.e("DIARY_VIEWMODEL", "Error al actualizar café personalizado", e)
            }
        }
    }

    private fun refreshDiaryWidget() {
        // Widgets desactivados en esta rama.
    }
}
