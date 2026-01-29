package com.cafesito.shared.data.repository

import com.cafesito.shared.data.local.CustomCoffeeCache
import com.cafesito.shared.data.model.CustomCoffeeDto
import com.cafesito.shared.data.remote.SupabaseRemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CustomCoffeeRepository(
    private val remote: SupabaseRemoteDataSource,
    private val cache: CustomCoffeeCache
) {
    suspend fun getCustomCoffees(userId: Int): List<CustomCoffeeDto> = withContext(Dispatchers.Default) {
        val cached = cache.selectByUser(userId)
        if (cached.isNotEmpty()) {
            return@withContext cached
        }
        fetchAndCache(userId).getOrDefault(cached)
    }

    suspend fun refreshCustomCoffees(userId: Int): Result<List<CustomCoffeeDto>> = withContext(Dispatchers.Default) {
        fetchAndCache(userId)
    }

    private suspend fun fetchAndCache(userId: Int): Result<List<CustomCoffeeDto>> = runCatching {
        val remoteItems = remote.getCustomCoffees(userId)
        cache.replaceAll(userId, remoteItems)
        remoteItems
    }
}
