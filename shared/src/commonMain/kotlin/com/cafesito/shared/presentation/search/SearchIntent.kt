package com.cafesito.shared.presentation.search

sealed interface SearchIntent {
    data class QueryChanged(val query: String) : SearchIntent
    data object Submit : SearchIntent
    data object Retry : SearchIntent
    data class ResultTapped(val id: String) : SearchIntent
}
