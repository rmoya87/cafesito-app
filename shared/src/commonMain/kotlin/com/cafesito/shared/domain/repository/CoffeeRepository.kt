package com.cafesito.shared.domain.repository

import com.cafesito.shared.core.CachePolicy
import com.cafesito.shared.core.CacheResult
import com.cafesito.shared.domain.model.Coffee
import kotlinx.coroutines.flow.Flow

interface CoffeeRepository {
    fun observeCoffees(policy: CachePolicy): Flow<CacheResult<List<Coffee>>>
}
