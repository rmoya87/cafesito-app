package com.example.cafesito.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.CoffeeRepository
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.data.ReviewEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: CoffeeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val coffeeId: String = checkNotNull(savedStateHandle["coffeeId"])
    private val currentUserId = 1 // rmoya (Simulado)

    val uiState: StateFlow<DetailUiState> = repository.getCoffeeWithDetailsById(coffeeId)
        .map { coffee ->
            if (coffee != null) {
                val myReview = coffee.reviews.find { it.userId == currentUserId }
                DetailUiState.Success(coffee, myReview)
            } else DetailUiState.Error
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
            repository.toggleFavorite(coffeeId, currentStatus)
        }
    }

    fun submitReview(rating: Float, comment: String) {
        viewModelScope.launch {
            val review = ReviewEntity(
                coffeeId = coffeeId,
                userId = currentUserId,
                rating = rating,
                comment = comment,
                timestamp = System.currentTimeMillis()
            )
            repository.upsertReview(review)
        }
    }
}

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data object Error : DetailUiState
    data class Success(val coffee: CoffeeWithDetails, val userReview: ReviewEntity?) : DetailUiState
}
