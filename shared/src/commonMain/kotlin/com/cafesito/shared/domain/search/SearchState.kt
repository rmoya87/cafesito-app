package com.cafesito.shared.domain.search

import com.cafesito.shared.domain.model.Coffee

data class SearchState(
    val query: String = "",
    val coffees: List<Coffee> = emptyList(),
    val isLoading: Boolean = false,
    val recentSearches: List<String> = emptyList(),
    val errorMessage: String? = null,
    val isPaginated: Boolean = false
)
