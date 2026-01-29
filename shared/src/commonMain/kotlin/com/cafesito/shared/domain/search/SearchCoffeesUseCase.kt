package com.cafesito.shared.domain.search

class SearchCoffeesUseCase(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(filters: SearchFilters): Result<List<CoffeeSummary>> {
        return repository.search(filters)
    }
}
