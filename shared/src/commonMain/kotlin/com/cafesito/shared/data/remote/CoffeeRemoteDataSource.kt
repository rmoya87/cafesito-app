package com.cafesito.shared.data.remote

import com.cafesito.shared.domain.model.Coffee

interface CoffeeRemoteDataSource {
    suspend fun fetchCoffees(): List<Coffee>
}
