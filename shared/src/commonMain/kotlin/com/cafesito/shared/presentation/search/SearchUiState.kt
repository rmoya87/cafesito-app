package com.cafesito.shared.presentation.search

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<SearchUiItem> = emptyList(),
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean = results.isEmpty() && !isLoading && errorMessage == null
}

data class SearchUiItem(
    val id: String,
    val title: String,
    val subtitle: String,
)
