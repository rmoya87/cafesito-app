package com.cafesito.shared.ios

import com.cafesito.shared.core.DefaultDispatcherProvider
import com.cafesito.shared.data.search.InMemorySearchRepository
import com.cafesito.shared.domain.search.SearchUseCase
import com.cafesito.shared.presentation.search.SearchReducer
import com.cafesito.shared.presentation.search.SearchViewModel

object SharedFactory {
    fun createSearchViewModelBridge(): SearchViewModelBridge {
        val dispatcherProvider = DefaultDispatcherProvider()
        val repository = InMemorySearchRepository()
        val useCase = SearchUseCase(repository)
        val reducer = SearchReducer()
        val viewModel = SearchViewModel(dispatcherProvider, reducer, useCase)
        return SearchViewModelBridge(viewModel)
    }
}
