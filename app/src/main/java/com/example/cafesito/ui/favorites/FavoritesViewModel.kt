package com.example.cafesito.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.CoffeeRepository
import com.example.cafesito.data.CoffeeWithDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: CoffeeRepository
) : ViewModel() {

    // Repository.favorites gives List<LocalFavorite>. 
    // We need to fetch the CoffeeWithDetails for each.
    // Efficient way: Get all coffees and filter by IDs in favorites.
    // Or add a DAO method "getFavoriteCoffees".
    // For V1 (small data), combining "All Coffees" + "All Favorites" flows is acceptable.
    
    val uiState: StateFlow<FavoritesUiState> = combine(
        repository.allCoffees,
        repository.favorites
    ) { allCoffees, favorites ->
        val favoriteIds = favorites.map { it.coffeeId }.toSet()
        val favoriteCoffees = allCoffees.filter { it.coffee.id in favoriteIds }
        
        if (favoriteCoffees.isEmpty()) {
            FavoritesUiState.Empty
        } else {
            FavoritesUiState.Success(favoriteCoffees)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FavoritesUiState.Loading
    )

    fun removeFromFavorites(coffeeId: Int) {
        viewModelScope.launch {
            repository.toggleFavorite(coffeeId, false)
        }
    }
}

sealed interface FavoritesUiState {
    data object Loading : FavoritesUiState
    data object Empty: FavoritesUiState
    data class Success(val coffees: List<CoffeeWithDetails>) : FavoritesUiState
}
