package com.cafesito.shared.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.cafesito.shared.data.local.CafesitoDatabase
import com.cafesito.shared.data.local.CustomCoffeeCache
import com.cafesito.shared.data.model.CustomCoffeeDto
import com.cafesito.shared.data.remote.SupabaseRemoteDataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomCoffeeRepositoryTest {
    @Test
    fun getCustomCoffeesReturnsCachedFirst() = kotlinx.coroutines.test.runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CafesitoDatabase.Schema.create(driver)
        val cache = CustomCoffeeCache(CafesitoDatabase(driver))
        val remote = FakeRemoteDataSource(::sampleCoffee)
        val repository = CustomCoffeeRepository(remote, cache)

        cache.replaceAll(1, listOf(sampleCoffee("cached")))

        val result = repository.getCustomCoffees(1)

        assertEquals("cached", result.first().id)
        assertTrue(remote.calls == 0)
    }

    @Test
    fun refreshCustomCoffeesUpdatesCache() = kotlinx.coroutines.test.runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CafesitoDatabase.Schema.create(driver)
        val cache = CustomCoffeeCache(CafesitoDatabase(driver))
        val remote = FakeRemoteDataSource(::sampleCoffee)
        val repository = CustomCoffeeRepository(remote, cache)

        val result = repository.refreshCustomCoffees(1)

        assertTrue(result.isSuccess)
        assertEquals("remote", cache.selectByUser(1).first().id)
    }


    private class FakeRemoteDataSource(
        private val factory: (String) -> CustomCoffeeDto
    ) : SupabaseRemoteDataSource {
        var calls: Int = 0

        override suspend fun signInWithGoogleIdToken(idToken: String): String? = "uid"
        override suspend fun signOut() = Unit
        override suspend fun getCustomCoffees(userId: Int): List<CustomCoffeeDto> {
            calls++
            return listOf(factory("remote"))
        }
        override suspend fun upsertCustomCoffee(coffee: CustomCoffeeDto) = Unit
        override suspend fun insertDiaryEntry(entry: com.cafesito.shared.data.model.DiaryEntryDto) = Unit
        override suspend fun getDiaryEntries(userId: Int): List<com.cafesito.shared.data.model.DiaryEntryDto> = emptyList()
        override suspend fun upsertPantryItem(item: com.cafesito.shared.data.model.PantryItemDto) = Unit
        override suspend fun getPantryItems(userId: Int): List<com.cafesito.shared.data.model.PantryItemDto> = emptyList()
        override suspend fun getFavorites(userId: Int): List<com.cafesito.shared.data.model.FavoriteDto> = emptyList()
        override suspend fun upsertFavorite(favorite: com.cafesito.shared.data.model.FavoriteDto) = Unit
        override suspend fun deleteFavorite(coffeeId: String, userId: Int) = Unit
        override suspend fun upsertReview(review: com.cafesito.shared.data.model.ReviewDto) = Unit
        override suspend fun getReviewByUserAndCoffee(userId: Int, coffeeId: String): com.cafesito.shared.data.model.ReviewDto? = null
    }

    private fun sampleCoffee(id: String) = CustomCoffeeDto(
        id = id,
        userId = 1,
        name = "Casa",
        brand = "Marca",
        specialty = "Arabica",
        roast = "Medium",
        variety = "Bourbon",
        country = "Colombia",
        hasCaffeine = true,
        format = "Grano",
        imageUrl = "https://example.com/coffee.png",
        totalGrams = 250
    )
}
