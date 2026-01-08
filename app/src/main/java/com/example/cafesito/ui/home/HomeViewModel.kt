package com.example.cafesito.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.CoffeeRepository
import com.example.cafesito.data.CoffeeWithDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    private val _selectedOriginId = MutableStateFlow<Int?>(null)
    val selectedOriginId = _selectedOriginId.asStateFlow()

    // Available filters
    val origins = repository.origins.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val filters = combine(_searchQuery, _minScore, _selectedOriginId) { query, score, originId ->
        Triple(query, score, originId)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = filters.flatMapLatest { (query, score, originId) ->
        repository.getFilteredCoffees(query, score, originId)
            .map<List<CoffeeWithDetails>, HomeUiState> { coffees -> 
                HomeUiState.Success(coffees) 
            }
            .catch { emit(HomeUiState.Error(it.message ?: "An unknown error occurred")) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState.Loading
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun applyFilters(minScore: Float, originId: Int?) {
        _minScore.value = minScore
        _selectedOriginId.value = originId
    }

    fun toggleFavorite(coffeeId: Int, currentStatus: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(coffeeId, !currentStatus) // Simplified logic
        }
    }
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val coffees: List<CoffeeWithDetails>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
