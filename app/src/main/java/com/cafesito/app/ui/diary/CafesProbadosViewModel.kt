package com.cafesito.app.ui.diary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.DiaryEntryEntity
import com.cafesito.app.data.DiaryRepository
import com.cafesito.app.data.CoffeeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class CafesProbadosViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val coffeeRepository: CoffeeRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _selectedCountryFilter = MutableStateFlow(savedStateHandle.get<String>("selected_country"))
    val selectedCountryFilter: StateFlow<String?> = _selectedCountryFilter

    fun setSelectedCountry(country: String?) {
        _selectedCountryFilter.update { country }
        savedStateHandle["selected_country"] = country
    }

    /** Fuerza recarga de diario y catálogo para rellenar el listado si al volver está vacío. */
    fun triggerRefresh() {
        diaryRepository.triggerRefresh()
        coffeeRepository.triggerRefresh()
    }

    private fun isCupEntry(e: DiaryEntryEntity): Boolean =
        e.type.equals(DiaryRepository.TYPE_CUP, ignoreCase = true) ||
            (e.type != DiaryRepository.TYPE_WATER && e.preparationType.startsWith("Lab:"))

    /** Cafés probados (primera vez por café): diario + lista unificada (catálogo + custom). */
    val coffeesWithFirstTried: StateFlow<List<TriedCoffeeItem>> = combine(
        diaryRepository.getDiaryEntries(),
        coffeeRepository.allCoffeesIncludingCustom
    ) { entries, coffees ->
        val coffeeEntries = entries.filter { isCupEntry(it) }
        val coffeeById = coffees.associate { it.coffee.id to it.coffee }
        val byCoffeeId = mutableMapOf<String, Long>()
        coffeeEntries.forEach { e ->
            e.coffeeId?.let { id ->
                val ts = e.timestamp
                if (ts < (byCoffeeId[id] ?: Long.MAX_VALUE)) byCoffeeId[id] = ts
            }
        }
        byCoffeeId.mapNotNull { (id, firstMs) ->
            coffeeById[id]?.let { TriedCoffeeItem(it, firstMs) }
        }.sortedBy { it.firstTriedMs }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}
