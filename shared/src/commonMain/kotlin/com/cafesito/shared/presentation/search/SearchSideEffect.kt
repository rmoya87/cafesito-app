package com.cafesito.shared.presentation.search

sealed interface SearchSideEffect {
    data class NavigateToDetail(val id: String) : SearchSideEffect
    data class ShowMessage(val message: String) : SearchSideEffect
}
