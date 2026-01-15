package com.example.cafesito.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class DiaryAnalytics(
    val weeklyCaffeine: List<Pair<String, Int>>,
    val waterCount: Int,
    val totalCaffeineToday: Int
)

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val coffeeRepository: CoffeeRepository
) : ViewModel() {

    val diaryEntries: StateFlow<List<DiaryEntryEntity>> = diaryRepository.getDiaryEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pantryItems: StateFlow<List<PantryItemWithDetails>> = diaryRepository.getPantryItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableCoffees: StateFlow<List<CoffeeWithDetails>> = coffeeRepository.allCoffees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val analytics: StateFlow<DiaryAnalytics?> = diaryEntries.map { entries ->
        if (entries.isEmpty()) return@map null
        
        val calendar = Calendar.getInstance()
        val today = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        val todayEntries = entries.filter { it.timestamp >= today }
        val waterCount = todayEntries.count { it.type == "WATER" }
        val caffeineToday = todayEntries.sumOf { it.caffeineAmount }

        // Analítica semanal simple
        val days = listOf("D", "L", "M", "X", "J", "V", "S")
        val weekly = (0..6).map { i ->
            val dayStart = today - (i * 24 * 60 * 60 * 1000)
            val dayEnd = dayStart + (24 * 60 * 60 * 1000)
            val dayName = days[Calendar.getInstance().apply { timeInMillis = dayStart }.get(Calendar.DAY_OF_WEEK) - 1]
            dayName to entries.filter { it.timestamp in dayStart until dayEnd }.sumOf { it.caffeineAmount }
        }.reversed()

        DiaryAnalytics(weekly, waterCount, caffeineToday)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
