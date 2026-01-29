package com.cafesito.shared.domain.search

import kotlinx.serialization.Serializable

@Serializable
data class CoffeeSummary(
    val id: String,
    val name: String,
    val brand: String,
    val origin: String? = null,
    val roast: String? = null,
    val rating: Float = 0f,
    val imageUrl: String? = null
)

@Serializable
data class SearchFilters(
    val query: String = "",
    val origins: Set<String> = emptySet(),
    val roasts: Set<String> = emptySet(),
    val minRating: Float = 0f
)
