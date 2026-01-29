package com.cafesito.shared.domain.search

interface SearchRepository {
    suspend fun search(query: String): List<SearchResult>
}
