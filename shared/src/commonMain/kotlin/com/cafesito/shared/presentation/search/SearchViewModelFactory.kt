package com.cafesito.shared.presentation.search

import com.cafesito.shared.domain.search.CoffeeSummary
import com.cafesito.shared.domain.search.SearchCoffeesUseCase
import com.cafesito.shared.domain.search.SearchFilters
import com.cafesito.shared.domain.search.SearchRepository

class SearchViewModelFactory(private val repository: SearchRepository) {
    fun create(): SearchViewModel = SearchViewModel(SearchCoffeesUseCase(repository))
}

class SearchSampleRepository : SearchRepository {
    override suspend fun search(filters: SearchFilters): Result<List<CoffeeSummary>> {
        val query = filters.query.trim()
        val base = listOf(
            CoffeeSummary(
                id = "sample-1",
                name = "Cafesito House Blend",
                brand = "Cafesito",
                origin = "Colombia",
                roast = "Medium",
                rating = 4.6f,
                imageUrl = null
            ),
            CoffeeSummary(
                id = "sample-2",
                name = "Latino Espresso",
                brand = "Cafesito",
                origin = "Brazil",
                roast = "Dark",
                rating = 4.2f,
                imageUrl = null
            )
        )
        if (query.isBlank()) {
            return Result.success(base)
        }
        return Result.success(base.filter { it.name.contains(query, ignoreCase = true) })
    }
}
