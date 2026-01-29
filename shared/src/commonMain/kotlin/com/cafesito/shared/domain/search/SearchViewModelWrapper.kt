package com.cafesito.shared.domain.search

import com.cafesito.shared.core.di.ServiceLocator
import com.cafesito.shared.core.flow.CStateFlow
import com.cafesito.shared.core.flow.asCStateFlow

class SearchViewModelWrapper(
    private val viewModel: SearchViewModel = ServiceLocator.searchViewModel()
) {
    val state: CStateFlow<SearchState> = viewModel.state.asCStateFlow()

    fun onQueryChange(query: String) {
        viewModel.onQueryChange(query)
    }

    fun addSearchToHistory(term: String) {
        viewModel.addSearchToHistory(term)
    }

    fun clearRecentSearches() {
        viewModel.clearRecentSearches()
    }
}
