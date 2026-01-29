package com.cafesito.shared.data.local

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryGreetingCache : GreetingCache {
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, String>()

    override suspend fun getGreeting(userName: String): String? = mutex.withLock {
        cache[userName]
    }

    override suspend fun saveGreeting(userName: String, greeting: String) {
        mutex.withLock {
            cache[userName] = greeting
        }
    }
}
