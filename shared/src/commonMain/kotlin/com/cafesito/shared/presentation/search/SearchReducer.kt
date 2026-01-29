package com.cafesito.shared.presentation.search

import com.cafesito.shared.domain.search.SearchResult

class SearchReducer {
    fun reduce(
        currentState: SearchUiState,
        mutation: SearchMutation,
    ): SearchUiState {
        return when (mutation) {
            is SearchMutation.QueryUpdated -> currentState.copy(
                query = mutation.query,
                errorMessage = null,
            )
            SearchMutation.SearchStarted -> currentState.copy(
                isLoading = true,
                errorMessage = null,
            )
            is SearchMutation.SearchSucceeded -> currentState.copy(
                isLoading = false,
                results = mutation.results.map { it.toUi() },
                errorMessage = null,
            )
            is SearchMutation.SearchFailed -> currentState.copy(
                isLoading = false,
                errorMessage = mutation.message,
                results = emptyList(),
            )
            SearchMutation.Cleared -> SearchUiState()
        }
    }
}

sealed interface SearchMutation {
    data class QueryUpdated(val query: String) : SearchMutation
    data object SearchStarted : SearchMutation
    data class SearchSucceeded(val results: List<SearchResult>) : SearchMutation
    data class SearchFailed(val message: String) : SearchMutation
    data object Cleared : SearchMutation
}

private fun SearchResult.toUi(): SearchUiItem = SearchUiItem(
    id = id,
    title = title,
    subtitle = subtitle,
)
