package com.cafesito.shared.domain.search

interface SearchRepository {
    suspend fun search(filters: SearchFilters): Result<List<CoffeeSummary>>
}
