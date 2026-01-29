package com.cafesito.shared.presentation.search

import com.cafesito.shared.core.DispatcherProvider
import com.cafesito.shared.core.SharedViewModel
import com.cafesito.shared.domain.search.SearchUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val dispatcherProvider: DispatcherProvider,
    private val reducer: SearchReducer,
    private val searchUseCase: SearchUseCase,
) : SharedViewModel() {

    private val mutableState = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = mutableState.asStateFlow()

    private val mutableSideEffects = MutableSharedFlow<SearchSideEffect>()
    val sideEffects = mutableSideEffects.asSharedFlow()

    fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> {
                updateState(SearchMutation.QueryUpdated(intent.query))
            }
            SearchIntent.Submit -> performSearch()
            SearchIntent.Retry -> performSearch()
            is SearchIntent.ResultTapped -> emitSideEffect(
                SearchSideEffect.NavigateToDetail(intent.id)
            )
        }
    }

    private fun performSearch() {
        val query = state.value.query
        if (query.isBlank()) {
            updateState(SearchMutation.Cleared)
            emitSideEffect(SearchSideEffect.ShowMessage("Ingresa una búsqueda"))
            return
        }

        updateState(SearchMutation.SearchStarted)
        scope.launch(dispatcherProvider.io) {
            runCatching { searchUseCase(query) }
                .onSuccess { results ->
                    updateState(SearchMutation.SearchSucceeded(results))
                }
                .onFailure {
                    updateState(SearchMutation.SearchFailed("No pudimos cargar resultados"))
                    emitSideEffect(SearchSideEffect.ShowMessage("Error al buscar"))
                }
        }
    }

    private fun updateState(mutation: SearchMutation) {
        mutableState.value = reducer.reduce(mutableState.value, mutation)
    }

    private fun emitSideEffect(effect: SearchSideEffect) {
        scope.launch {
            mutableSideEffects.emit(effect)
        }
    }
}
