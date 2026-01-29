package com.cafesito.shared.data.local

import com.cafesito.shared.domain.model.Coffee

interface CoffeeCache {
    suspend fun getAll(): List<Coffee>
    suspend fun lastUpdatedAt(): Long?
    suspend fun replaceAll(coffees: List<Coffee>, updatedAt: Long)
}
