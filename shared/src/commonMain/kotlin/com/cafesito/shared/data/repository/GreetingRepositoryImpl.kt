package com.cafesito.shared.data.repository

import com.cafesito.shared.core.DispatcherProvider
import com.cafesito.shared.data.local.GreetingCache
import com.cafesito.shared.data.remote.GreetingApi
import com.cafesito.shared.domain.repository.GreetingRepository
import kotlinx.coroutines.withContext

class GreetingRepositoryImpl(
    private val api: GreetingApi,
    private val cache: GreetingCache,
    private val dispatcherProvider: DispatcherProvider,
) : GreetingRepository {
    override suspend fun fetchGreeting(userName: String): String = withContext(dispatcherProvider.io) {
        cache.getGreeting(userName)?.let { return@withContext it }
        val greeting = api.fetchGreeting(userName)
        cache.saveGreeting(userName, greeting)
        greeting
    }
}
