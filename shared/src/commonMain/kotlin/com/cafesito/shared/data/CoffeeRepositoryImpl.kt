package com.cafesito.shared.data

import com.cafesito.shared.core.CachePolicy
import com.cafesito.shared.core.CacheResult
import com.cafesito.shared.core.TimeProvider
import com.cafesito.shared.data.local.CoffeeCache
import com.cafesito.shared.data.remote.CoffeeRemoteDataSource
import com.cafesito.shared.domain.model.Coffee
import com.cafesito.shared.domain.repository.CoffeeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class CoffeeRepositoryImpl(
    private val cache: CoffeeCache,
    private val remote: CoffeeRemoteDataSource,
    private val dispatcher: CoroutineDispatcher,
    private val timeProvider: TimeProvider
) : CoffeeRepository {
    override fun observeCoffees(policy: CachePolicy): Flow<CacheResult<List<Coffee>>> = flow {
        val cached = withContext(dispatcher) { cache.getAll() }
        val lastUpdatedAt = withContext(dispatcher) { cache.lastUpdatedAt() }
        val isStale = lastUpdatedAt == null ||
            timeProvider.nowMillis() - lastUpdatedAt > policy.staleAfterMillis

        if (cached.isNotEmpty()) {
            emit(CacheResult(cached, isFresh = !isStale))
        }

        if (cached.isEmpty() || isStale) {
            val fresh = withContext(dispatcher) { remote.fetchCoffees() }
            withContext(dispatcher) {
                cache.replaceAll(fresh, updatedAt = timeProvider.nowMillis())
            }
            emit(CacheResult(fresh, isFresh = true))
        }
    }
}
