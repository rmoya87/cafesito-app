package com.cafesito.app.ui.diary

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.*
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
    val comparisonPercentage: Int,
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

    private val allEntriesFlow = diaryRepository.getDiaryEntries()

    val diaryEntries: StateFlow<List<DiaryEntryEntity>> = combine(
        allEntriesFlow,
        _selectedPeriod
    ) { entries, period ->
        val calendar = Calendar.getInstance()
        when (period) {
            DiaryPeriod.HOY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            }
            DiaryPeriod.SEMANA -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            }
            DiaryPeriod.MES -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            }
        }
        val startTime = calendar.timeInMillis
        entries.filter { it.timestamp >= startTime }
    }
    .onEach { _isLoading.value = false } 
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pantryItems: StateFlow<List<PantryItemWithDetails>> = diaryRepository.getPantryItems()
        .onEach { _isLoading.value = false } 
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableCoffees: StateFlow<List<CoffeeWithDetails>> = coffeeRepository.allCoffees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val analytics: StateFlow<DiaryAnalytics?> = combine(allEntriesFlow, _selectedPeriod) { entries, period ->
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()
        
        val (currentEntries, previousEntries, averageValue) = when (period) {
            DiaryPeriod.HOY -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                val startOfToday = calendar.timeInMillis
                val dailySums = entries.groupBy { 
                    val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
                }.values.map { it.filter { e -> e.type == "CUP" }.sumOf { e -> e.caffeineAmount } }
                Triple(entries.filter { it.timestamp >= startOfToday }, entries.filter { it.timestamp in (startOfToday - 86400000) until startOfToday }, if(dailySums.isEmpty()) 0 else dailySums.average().toInt())
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
        
        val prevTotal = previousEntries.filter { it.type == "CUP" }.sumOf { it.caffeineAmount }
        val comp = if (prevTotal == 0) 0 else ((totalCaffeine - prevTotal).toFloat() / prevTotal * 100).toInt()

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
            comparisonPercentage = comp, 
            period = period
        )
    }
    .onEach { _isLoading.value = false }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setPeriod(period: DiaryPeriod) { _selectedPeriod.value = period }
    
    fun refreshData() {
        _isLoading.value = true
        diaryRepository.triggerRefresh()
    }

    suspend fun addCoffeeConsumption(
        coffeeId: String?, 
        coffeeName: String, 
        coffeeBrand: String,
        caffeineAmount: Int, 
        amountMl: Int, 
        coffeeGrams: Int,
        preparationType: String
    ) {
        diaryRepository.addDiaryEntry(coffeeId, coffeeName, coffeeBrand, caffeineAmount, "CUP", amountMl, coffeeGrams, preparationType)
    }
    
    suspend fun addWaterConsumption(amountMl: Int) {
        diaryRepository.addDiaryEntry(null, "Agua", "", 0, "WATER", amountMl, 0, "None")
    }
    
    fun deleteEntry(entryId: Long) { 
        viewModelScope.launch { 
            diaryRepository.deleteDiaryEntry(entryId) 
        } 
    }
    
    fun addToPantry(coffeeId: String, grams: Int, onSuccess: () -> Unit = {}) { 
        viewModelScope.launch { 
            try {
                diaryRepository.addToPantry(coffeeId, grams)
                onSuccess()
            } catch (e: Exception) {
                Log.e("DIARY_VIEWMODEL", "Error adding to pantry", e)
            }
        } 
    }
    
    fun updateStock(coffeeId: String, total: Int, remaining: Int) {
        viewModelScope.launch {
            diaryRepository.updatePantryStockFull(coffeeId, total, remaining)
        }
    }

    fun removeFromPantry(coffeeId: String, onSuccess: () -> Unit = {}) { 
        viewModelScope.launch { 
            try {
                diaryRepository.deletePantryItem(coffeeId)
                onSuccess()
            } catch (e: Exception) {
                Log.e("DIARY_VIEWMODEL", "Error removing from pantry", e)
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
                onSuccess()
            } catch (e: Exception) {
                Log.e("DIARY_VIEWMODEL", "Error al actualizar café personalizado", e)
            }
        }
    }
}
