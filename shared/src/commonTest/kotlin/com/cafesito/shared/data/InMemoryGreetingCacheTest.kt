package com.cafesito.shared.data

import com.cafesito.shared.data.local.InMemoryGreetingCache
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemoryGreetingCacheTest {
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun storesAndRetrievesGreeting() = runTest {
        val cache = InMemoryGreetingCache()

        assertNull(cache.getGreeting("Ana"))

        cache.saveGreeting("Ana", "Hola Ana")

        assertEquals("Hola Ana", cache.getGreeting("Ana"))
    }
}
