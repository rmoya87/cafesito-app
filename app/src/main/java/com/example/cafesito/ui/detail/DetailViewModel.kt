package com.example.cafesito.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.CoffeeRepository
import com.example.cafesito.data.CoffeeWithDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: CoffeeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val coffeeId: Int = checkNotNull(savedStateHandle["coffeeId"])

    val uiState: StateFlow<DetailUiState> = repository.getCoffeeById(coffeeId)
        .map { coffee ->
            if (coffee != null) DetailUiState.Success(coffee) else DetailUiState.Error
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DetailUiState.Loading
        )

    val isFavorite: StateFlow<Boolean> = repository.favorites
        .map { list -> list.any { it.coffeeId == coffeeId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleFavorite(currentStatus: Boolean) {
        viewModelScope.launch {
            // If currentStatus is true, we want to remove it. Repository handle the logic.
            repository.toggleFavorite(coffeeId, currentStatus)
        }
    }
}

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data object Error : DetailUiState
    data class Success(val coffee: CoffeeWithDetails) : DetailUiState
}
