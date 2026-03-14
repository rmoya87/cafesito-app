package com.cafesito.app.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.DiaryEntryEntity
import com.cafesito.app.data.DiaryRepository
import com.cafesito.app.data.CoffeeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CafesProbadosViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val coffeeRepository: CoffeeRepository
) : ViewModel() {

    private fun isCupEntry(e: DiaryEntryEntity): Boolean =
        e.type.equals(DiaryRepository.TYPE_CUP, ignoreCase = true) ||
            (e.type != DiaryRepository.TYPE_WATER && e.preparationType.startsWith("Lab:"))

    /** Cafés probados (primera vez por café) usando TODAS las entradas del diario. */
    val coffeesWithFirstTried: StateFlow<List<TriedCoffeeItem>> = combine(
        diaryRepository.getDiaryEntries(),
        coffeeRepository.allCoffees
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
