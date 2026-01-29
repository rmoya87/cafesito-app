package com.cafesito.shared.data

import com.cafesito.shared.domain.model.Coffee
import com.cafesito.shared.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemorySearchRepository : SearchRepository {
    private val coffees = MutableStateFlow(
        listOf(
            Coffee(id = "1", name = "Cafesito House Blend", origin = "Colombia", rating = 4.6f),
            Coffee(id = "2", name = "Altura Espresso", origin = "Ethiopia", rating = 4.4f),
            Coffee(id = "3", name = "Rio Claro", origin = "Costa Rica", rating = 4.2f),
            Coffee(id = "4", name = "Sierra Negra", origin = "Mexico", rating = 4.1f)
        )
    )

    override fun publicCoffees(): Flow<List<Coffee>> = coffees
}
