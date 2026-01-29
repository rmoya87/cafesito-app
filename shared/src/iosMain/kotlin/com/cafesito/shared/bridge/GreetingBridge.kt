package com.cafesito.shared.bridge

import com.cafesito.shared.core.DefaultDispatcherProvider
import com.cafesito.shared.data.local.InMemoryGreetingCache
import com.cafesito.shared.data.remote.KtorGreetingApi
import com.cafesito.shared.data.repository.GreetingRepositoryImpl
import com.cafesito.shared.domain.usecase.GetGreetingUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class GreetingBridge(
    baseUrl: String,
) {
    private val dispatcherProvider = DefaultDispatcherProvider()
    private val client = HttpClient(Darwin)
    private val api = KtorGreetingApi(client, baseUrl)
    private val cache = InMemoryGreetingCache()
    private val repository = GreetingRepositoryImpl(api, cache, dispatcherProvider)
    private val useCase = GetGreetingUseCase(repository)
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.default)

    fun loadGreeting(userName: String, callback: (String?, String?) -> Unit) {
        scope.launch {
            runCatching { useCase(userName) }
                .onSuccess { callback(it, null) }
                .onFailure { callback(null, it.message ?: "Unknown error") }
        }
    }

    fun clear() {
        scope.coroutineContext.cancel()
        client.close()
    }
}
