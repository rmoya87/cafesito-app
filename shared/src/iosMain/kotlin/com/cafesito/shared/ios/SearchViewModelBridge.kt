package com.cafesito.shared.ios

import com.cafesito.shared.presentation.search.SearchIntent
import com.cafesito.shared.presentation.search.SearchSideEffect
import com.cafesito.shared.presentation.search.SearchUiState
import com.cafesito.shared.presentation.search.SearchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SearchViewModelBridge(
    private val viewModel: SearchViewModel,
) {
    private val scope: CoroutineScope = MainScope()
    private val mutableSideEffects = MutableSharedFlow<SearchSideEffect>()
    val sideEffects: Flow<SearchSideEffect> = mutableSideEffects
    val stateBridge = FlowBridge(viewModel.state, SearchUiState())

    init {
        viewModel.sideEffects
            .onEach { effect -> mutableSideEffects.emit(effect) }
            .launchIn(scope)
    }

    fun onQueryChanged(query: String) {
        viewModel.onIntent(SearchIntent.QueryChanged(query))
    }

    fun onSubmit() {
        viewModel.onIntent(SearchIntent.Submit)
    }

    fun onRetry() {
        viewModel.onIntent(SearchIntent.Retry)
    }

    fun onResultTapped(id: String) {
        viewModel.onIntent(SearchIntent.ResultTapped(id))
    }

    fun clear() {
        stateBridge.clear()
        scope.cancel()
        viewModel.clear()
    }
}
