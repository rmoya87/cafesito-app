package com.example.cafesito.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.CoffeeRepository
import com.example.cafesito.data.CoffeeWithDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CoffeeRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _minScore = MutableStateFlow(0f)
    val minScore = _minScore.asStateFlow()

    private val _selectedOrigin = MutableStateFlow<String?>(null)
    val selectedOrigin = _selectedOrigin.asStateFlow()

    val origins: StateFlow<List<String>> = repository.allCoffees
        .map { list -> list.map { it.coffee.paisOrigen }.filterNotNull().distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val filters = combine(_searchQuery, _minScore, _selectedOrigin) { query, score, origin ->
        Triple(query, score, origin)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = filters.flatMapLatest { (query, score, origin) ->
        repository.getFilteredCoffees(
            query = query,
            origin = origin
        ).map { coffees: List<CoffeeWithDetails> -> 
            val filtered = if (score > 0) {
                coffees.filter { it.averageRating >= score }
            } else {
                coffees
            }
            HomeUiState.Success(filtered) as HomeUiState
        }.catch { e -> 
            emit(HomeUiState.Error(e.message ?: "Error desconocido")) 
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState.Loading
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun applyFilters(minScore: Float, origin: String?) {
        _minScore.value = minScore
        _selectedOrigin.value = origin
    }

    fun toggleFavorite(coffeeId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(coffeeId, currentStatus)
        }
    }
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val coffees: List<CoffeeWithDetails>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
