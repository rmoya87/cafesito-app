package com.example.cafesito.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

enum class DiaryPeriod { HOY, SEMANA, MES }

data class DiaryAnalytics(
    val chartData: List<Pair<String, Int>>,
    val waterCount: Int,
    val cupsCount: Int,
    val totalCaffeine: Int,
    val averageCaffeine: Int,
    val comparisonPercentage: Int, // e.g. +10 or -5
    val period: DiaryPeriod
)

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val coffeeRepository: CoffeeRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(DiaryPeriod.HOY)
    val selectedPeriod = _selectedPeriod.asStateFlow()

    // All entries for the user to calculate averages
    private val allEntriesFlow = diaryRepository.getDiaryEntries()

    val diaryEntries: StateFlow<List<DiaryEntryEntity>> = combine(
        allEntriesFlow,
        _selectedPeriod
    ) { entries, period ->
        val calendar = Calendar.getInstance()
        
        when (period) {
            DiaryPeriod.HOY -> {
                val startOfDay = calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                entries.filter { it.timestamp >= startOfDay }
            }
            DiaryPeriod.SEMANA -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfWeek = calendar.timeInMillis
                entries.filter { it.timestamp >= startOfWeek }
            }
            DiaryPeriod.MES -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfMonth = calendar.timeInMillis
                entries.filter { it.timestamp >= startOfMonth }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pantryItems: StateFlow<List<PantryItemWithDetails>> = diaryRepository.getPantryItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableCoffees: StateFlow<List<CoffeeWithDetails>> = coffeeRepository.allCoffees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val analytics: StateFlow<DiaryAnalytics?> = combine(allEntriesFlow, _selectedPeriod) { entries, period ->
        if (entries.isEmpty()) {
            return@combine DiaryAnalytics(emptyList(), 0, 0, 0, 0, 0, period)
        }

        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()
        
        val (currentEntries, previousEntries, averageValue) = when (period) {
            DiaryPeriod.HOY -> {
                val startOfToday = calendar.apply { 
                    timeInMillis = now
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) 
                }.timeInMillis
                val startOfYesterday = startOfToday - (24 * 60 * 60 * 1000)
                
                val today = entries.filter { it.timestamp >= startOfToday }
                val yesterday = entries.filter { it.timestamp in startOfYesterday until startOfToday }
                
                // Average daily consumption (historical)
                val dailySums = entries.groupBy { 
                    Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.DAY_OF_YEAR) 
                }.values.map { it.sumOf { entry -> entry.caffeineAmount } }
                
                Triple(today, yesterday, dailySums.average().toInt())
            }
            DiaryPeriod.SEMANA -> {
                val startOfThisWeek = calendar.apply {
                    timeInMillis = now
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val startOfLastWeek = startOfThisWeek - (7 * 24 * 60 * 60 * 1000)
                
                val thisWeek = entries.filter { it.timestamp >= startOfThisWeek }
                val lastWeek = entries.filter { it.timestamp in startOfLastWeek until startOfThisWeek }
                
                // Average weekly consumption (historical)
                val weeklySums = entries.groupBy { 
                    Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.WEEK_OF_YEAR) 
                }.values.map { it.sumOf { entry -> entry.caffeineAmount } }
                
                Triple(thisWeek, lastWeek, weeklySums.average().toInt())
            }
            DiaryPeriod.MES -> {
                val startOfThisMonth = calendar.apply {
                    timeInMillis = now
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                calendar.add(Calendar.MONTH, -1)
                val startOfLastMonth = calendar.timeInMillis
                
                val thisMonth = entries.filter { it.timestamp >= startOfThisMonth }
                val lastMonth = entries.filter { it.timestamp in startOfLastMonth until startOfThisMonth }
                
                // Average monthly consumption (historical)
                val monthlySums = entries.groupBy { 
                    Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.MONTH) 
                }.values.map { it.sumOf { entry -> entry.caffeineAmount } }
                
                Triple(thisMonth, lastMonth, monthlySums.average().toInt())
            }
        }

        val totalCaffeine = currentEntries.sumOf { it.caffeineAmount }
        val prevTotal = previousEntries.sumOf { it.caffeineAmount }
        
        val comparison = if (prevTotal == 0) 0 else {
            ((totalCaffeine - prevTotal).toFloat() / prevTotal * 100).toInt()
        }

        val waterCount = currentEntries.count { it.type == "WATER" }
        val cupsCount = currentEntries.count { it.type == "CUP" }

        val chartData = when (period) {
            DiaryPeriod.HOY -> (0..23).map { hour ->
                val label = String.format("%02d:00", hour)
                val amount = currentEntries.filter { 
                    Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.HOUR_OF_DAY) == hour
                }.sumOf { it.caffeineAmount }
                label to amount
            }
            DiaryPeriod.SEMANA -> {
                val days = listOf("Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Dom")
                (0..6).map { i ->
                    val amount = currentEntries.filter {
                        val entryCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        val dayOfWeek = entryCal.get(Calendar.DAY_OF_WEEK)
                        val adjustedDay = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
                        adjustedDay == i
                    }.sumOf { it.caffeineAmount }
                    days[i] to amount
                }
            }
            DiaryPeriod.MES -> {
                val maxDay = calendar.apply { timeInMillis = now }.getActualMaximum(Calendar.DAY_OF_MONTH)
                (1..maxDay).map { day ->
                    val amount = currentEntries.filter {
                        Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.DAY_OF_MONTH) == day
                    }.sumOf { it.caffeineAmount }
                    day.toString() to amount
                }
            }
        }

        DiaryAnalytics(chartData, waterCount, cupsCount, totalCaffeine, averageValue, comparison, period)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setPeriod(period: DiaryPeriod) {
        _selectedPeriod.value = period
    }

    fun addCoffeeConsumption(coffeeId: String?, coffeeName: String, caffeineAmount: Int) {
        viewModelScope.launch {
            diaryRepository.addDiaryEntry(coffeeId, coffeeName, caffeineAmount, "CUP")
        }
    }

    fun addWaterConsumption() {
        viewModelScope.launch {
            diaryRepository.addDiaryEntry(null, "Agua", 0, "WATER")
        }
    }

    fun deleteEntry(entryId: Int) {
        viewModelScope.launch {
            diaryRepository.deleteDiaryEntry(entryId)
        }
    }

    fun addToPantry(coffeeId: String, grams: Int) {
        viewModelScope.launch {
            diaryRepository.addToPantry(coffeeId, grams)
        }
    }

    fun removeFromPantry(coffeeId: String) {
        viewModelScope.launch {
            diaryRepository.deletePantryItem(coffeeId)
        }
    }
}
