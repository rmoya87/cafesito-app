package com.cafesito.shared.domain.repository

import com.cafesito.shared.domain.model.Coffee
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    fun publicCoffees(): Flow<List<Coffee>>
}
