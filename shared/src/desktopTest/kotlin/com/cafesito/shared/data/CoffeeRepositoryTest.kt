package com.cafesito.shared.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.cafesito.shared.cache.CoffeeCacheDatabase
import com.cafesito.shared.core.CachePolicy
import com.cafesito.shared.core.TimeProvider
import com.cafesito.shared.data.local.SqlDelightCoffeeCache
import com.cafesito.shared.data.remote.CoffeeRemoteDataSource
import com.cafesito.shared.domain.model.Coffee
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CoffeeRepositoryTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Test
    fun `returns fresh data when cache is empty`() = runTest {
        val database = createDatabase()
        val cache = SqlDelightCoffeeCache(database)
        val remote = FakeCoffeeRemoteDataSource(
            listOf(Coffee(id = "1", name = "Pacamara", origin = "El Salvador"))
        )
        val timeProvider = FakeTimeProvider(startMillis = 1_000)
        val repository = CoffeeRepositoryImpl(cache, remote, dispatcher, timeProvider)

        val results = repository.observeCoffees(CachePolicy(staleAfterMillis = 500))
            .take(1)
            .toList()

        assertEquals(1, results.size)
        assertTrue(results.first().isFresh)
        assertEquals("Pacamara", results.first().data.first().name)
    }

    @Test
    fun `emits stale cache then refreshes`() = runTest {
        val database = createDatabase()
        val cache = SqlDelightCoffeeCache(database)
        val remote = FakeCoffeeRemoteDataSource(
            listOf(Coffee(id = "2", name = "Geisha", origin = "Panama"))
        )
        val timeProvider = FakeTimeProvider(startMillis = 2_000)
        val repository = CoffeeRepositoryImpl(cache, remote, dispatcher, timeProvider)

        cache.replaceAll(
            coffees = listOf(Coffee(id = "1", name = "Bourbon", origin = "Guatemala")),
            updatedAt = 1_000
        )

        val results = repository.observeCoffees(CachePolicy(staleAfterMillis = 500))
            .take(2)
            .toList()

        assertEquals(2, results.size)
        assertFalse(results.first().isFresh)
        assertEquals("Bourbon", results.first().data.first().name)
        assertTrue(results.last().isFresh)
        assertEquals("Geisha", results.last().data.first().name)
    }

    private fun createDatabase(): CoffeeCacheDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CoffeeCacheDatabase.Schema.create(driver)
        return CoffeeCacheDatabase(driver)
    }

    private class FakeCoffeeRemoteDataSource(
        private val coffees: List<Coffee>
    ) : CoffeeRemoteDataSource {
        override suspend fun fetchCoffees(): List<Coffee> = coffees
    }

    private class FakeTimeProvider(
        private val startMillis: Long
    ) : TimeProvider {
        override fun nowMillis(): Long = startMillis
    }
}
