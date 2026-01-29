package com.cafesito.shared.domain.search

class SearchUseCase(private val repository: SearchRepository) {
    suspend operator fun invoke(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        return repository.search(query.trim())
    }
}
