package com.cafesito.shared.presentation.search

import com.cafesito.shared.domain.search.CoffeeSummary
import com.cafesito.shared.domain.search.SearchFilters
import com.cafesito.shared.domain.search.SearchCoffeesUseCase
import com.cafesito.shared.core.CommonFlow
import com.cafesito.shared.core.asCommonFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SearchIntent {
    data class UpdateQuery(val query: String) : SearchIntent
    data class UpdateOrigins(val origins: Set<String>) : SearchIntent
    data class UpdateRoasts(val roasts: Set<String>) : SearchIntent
    data class UpdateMinRating(val minRating: Float) : SearchIntent
    data object SubmitSearch : SearchIntent
    data object ClearResults : SearchIntent
}

sealed interface SearchEffect {
    data class ShowError(val message: String) : SearchEffect
}

data class SearchUiState(
    val isLoading: Boolean = false,
    val filters: SearchFilters = SearchFilters(),
    val results: List<CoffeeSummary> = emptyList()
)

class SearchViewModel(
    private val searchCoffees: SearchCoffeesUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<SearchEffect>(extraBufferCapacity = 1)
    val effects = _effects.asSharedFlow()

    fun stateFlow(): CommonFlow<SearchUiState> = state.asCommonFlow()

    fun effectsFlow(): CommonFlow<SearchEffect> = effects.asCommonFlow()

    fun handle(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.UpdateQuery -> updateFilters { it.copy(query = intent.query) }
            is SearchIntent.UpdateOrigins -> updateFilters { it.copy(origins = intent.origins) }
            is SearchIntent.UpdateRoasts -> updateFilters { it.copy(roasts = intent.roasts) }
            is SearchIntent.UpdateMinRating -> updateFilters { it.copy(minRating = intent.minRating) }
            SearchIntent.SubmitSearch -> submitSearch()
            SearchIntent.ClearResults -> _state.update { it.copy(results = emptyList()) }
        }
    }

    fun close() {
        scope.coroutineContext[Job]?.cancel()
    }

    private fun updateFilters(transform: (SearchFilters) -> SearchFilters) {
        _state.update { current -> current.copy(filters = transform(current.filters)) }
    }

    private fun submitSearch() {
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = searchCoffees(_state.value.filters)
            result.fold(
                onSuccess = { items ->
                    _state.update { it.copy(isLoading = false, results = items) }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false) }
                    _effects.tryEmit(SearchEffect.ShowError(error.message ?: "Search failed"))
                }
            )
        }
    }
}
