package com.cafesito.shared.domain.search

import com.cafesito.shared.domain.model.Coffee
import com.cafesito.shared.domain.repository.SearchRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class SearchViewModel(
    private val repository: SearchRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val query = MutableStateFlow("")
    private val recentSearches = MutableStateFlow<List<String>>(emptyList())
    private val isLoading = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val state: StateFlow<SearchState> = combine(
        repository.publicCoffees(),
        query,
        recentSearches,
        isLoading,
        errorMessage
    ) { coffees, currentQuery, recent, loading, error ->
        val filtered = filterCoffees(coffees, currentQuery)
        SearchState(
            query = currentQuery,
            coffees = filtered,
            isLoading = loading,
            recentSearches = recent,
            errorMessage = error,
            isPaginated = false
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), SearchState())

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun addSearchToHistory(term: String) {
        if (term.isBlank()) return
        recentSearches.update { current ->
            (listOf(term) + current.filterNot { it == term }).take(8)
        }
    }

    fun clearRecentSearches() {
        recentSearches.value = emptyList()
    }

    private fun filterCoffees(coffees: List<Coffee>, term: String): List<Coffee> {
        if (term.isBlank()) return coffees
        val normalized = term.trim()
        return coffees.filter { coffee ->
            coffee.name.contains(normalized, ignoreCase = true) ||
                coffee.origin.contains(normalized, ignoreCase = true)
        }
    }
}
