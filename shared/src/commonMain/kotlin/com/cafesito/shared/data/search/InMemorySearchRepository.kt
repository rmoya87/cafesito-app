package com.cafesito.shared.data.search

import com.cafesito.shared.domain.search.SearchRepository
import com.cafesito.shared.domain.search.SearchResult
import kotlinx.coroutines.delay

class InMemorySearchRepository : SearchRepository {
    override suspend fun search(query: String): List<SearchResult> {
        delay(150)
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return emptyList()

        return demoResults()
            .filter { result ->
                result.title.lowercase().contains(normalized) ||
                    result.subtitle.lowercase().contains(normalized)
            }
    }

    private fun demoResults(): List<SearchResult> = listOf(
        SearchResult(id = "1", title = "Café de Colombia", subtitle = "Origen Huila"),
        SearchResult(id = "2", title = "Café de Etiopía", subtitle = "Notas florales"),
        SearchResult(id = "3", title = "Latte vainilla", subtitle = "Bebida con leche"),
        SearchResult(id = "4", title = "Cold Brew", subtitle = "Extracción en frío"),
        SearchResult(id = "5", title = "Affogato", subtitle = "Helado + espresso"),
    )
}
