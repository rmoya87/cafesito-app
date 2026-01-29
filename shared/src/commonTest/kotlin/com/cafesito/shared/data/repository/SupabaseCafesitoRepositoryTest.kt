package com.cafesito.shared.data.repository

import com.cafesito.shared.core.DataResult
import com.cafesito.shared.data.remote.CustomCoffeeDto
import com.cafesito.shared.data.remote.DiaryEntryDto
import com.cafesito.shared.data.remote.DiaryEntryInsertDto
import com.cafesito.shared.data.remote.FavoriteDto
import com.cafesito.shared.data.remote.PantryItemDto
import com.cafesito.shared.data.remote.RatingDto
import com.cafesito.shared.data.remote.RatingUpsertDto
import com.cafesito.shared.data.remote.SupabaseRemoteDataSource
import com.cafesito.shared.domain.model.CustomCoffee
import com.cafesito.shared.domain.model.DiaryEntry
import com.cafesito.shared.domain.model.PantryItem
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SupabaseCafesitoRepositoryTest {
    @Test
    fun `addDiaryEntry stores and returns entry`() = runTest {
        val remote = FakeSupabaseRemoteDataSource()
        val repository = SupabaseCafesitoRepository(remote)

        val entry = DiaryEntry(
            userId = 42,
            coffeeId = "coffee-1",
            coffeeName = "Test",
            coffeeBrand = "Brand",
            caffeineAmount = 120,
            timestamp = 1234L
        )

        val result = repository.addDiaryEntry(entry)

        assertTrue(result is DataResult.Success)
        val saved = (result as DataResult.Success).data
        assertEquals(1L, saved.id)
        assertEquals(1, remote.diaryEntries.size)
    }

    @Test
    fun `upsertPantryItem delegates to remote data source`() = runTest {
        val remote = FakeSupabaseRemoteDataSource()
        val repository = SupabaseCafesitoRepository(remote)

        val item = PantryItem(
            coffeeId = "coffee-1",
            userId = 12,
            gramsRemaining = 200,
            totalGrams = 250,
            lastUpdated = 999L
        )

        val result = repository.upsertPantryItem(item)

        assertTrue(result is DataResult.Success)
        assertEquals(1, remote.pantryItems.size)
        assertEquals("coffee-1", remote.pantryItems.first().coffeeId)
    }

    @Test
    fun `getCustomCoffees maps remote data`() = runTest {
        val remote = FakeSupabaseRemoteDataSource().apply {
            customCoffees.add(
                CustomCoffeeDto(
                    id = "custom-1",
                    userId = 7,
                    name = "Custom",
                    brand = "Local",
                    specialty = "Special",
                    country = "CO",
                    hasCaffeine = true,
                    format = "Beans",
                    imageUrl = "url"
                )
            )
        }
        val repository = SupabaseCafesitoRepository(remote)

        val result = repository.getCustomCoffees(7)

        assertTrue(result is DataResult.Success)
        val coffees = (result as DataResult.Success).data
        assertEquals(listOf("custom-1"), coffees.map(CustomCoffee::id))
    }
}

private class FakeSupabaseRemoteDataSource : SupabaseRemoteDataSource {
    val customCoffees = mutableListOf<CustomCoffeeDto>()
    val diaryEntries = mutableListOf<DiaryEntryDto>()
    val pantryItems = mutableListOf<PantryItemDto>()
    val favorites = mutableListOf<FavoriteDto>()
    val ratings = mutableListOf<RatingDto>()

    override suspend fun getCustomCoffees(userId: Int): List<CustomCoffeeDto> =
        customCoffees.filter { it.userId == userId }

    override suspend fun insertCustomCoffee(dto: CustomCoffeeDto) {
        customCoffees.add(dto)
    }

    override suspend fun updateCustomCoffee(dto: CustomCoffeeDto) {
        val index = customCoffees.indexOfFirst { it.id == dto.id && it.userId == dto.userId }
        if (index != -1) customCoffees[index] = dto
    }

    override suspend fun deleteCustomCoffee(id: String, userId: Int) {
        customCoffees.removeAll { it.id == id && it.userId == userId }
    }

    override suspend fun getDiaryEntries(userId: Int): List<DiaryEntryDto> =
        diaryEntries.filter { it.userId == userId }

    override suspend fun insertDiaryEntry(dto: DiaryEntryInsertDto): DiaryEntryDto {
        val saved = DiaryEntryDto(
            id = (diaryEntries.size + 1).toLong(),
            userId = dto.userId,
            coffeeId = dto.coffeeId,
            coffeeName = dto.coffeeName,
            coffeeBrand = dto.coffeeBrand,
            caffeineAmount = dto.caffeineAmount,
            amountMl = dto.amountMl,
            coffeeGrams = dto.coffeeGrams,
            preparationType = dto.preparationType,
            timestamp = dto.timestamp,
            type = dto.type,
            externalId = dto.externalId
        )
        diaryEntries.add(saved)
        return saved
    }

    override suspend fun deleteDiaryEntry(entryId: Long) {
        diaryEntries.removeAll { it.id == entryId }
    }

    override suspend fun getPantryItems(userId: Int): List<PantryItemDto> =
        pantryItems.filter { it.userId == userId }

    override suspend fun upsertPantryItem(dto: PantryItemDto) {
        pantryItems.removeAll { it.coffeeId == dto.coffeeId && it.userId == dto.userId }
        pantryItems.add(dto)
    }

    override suspend fun deletePantryItem(coffeeId: String, userId: Int) {
        pantryItems.removeAll { it.coffeeId == coffeeId && it.userId == userId }
    }

    override suspend fun getFavorites(userId: Int, isCustom: Boolean): List<FavoriteDto> =
        favorites.filter { it.userId == userId }

    override suspend fun upsertFavorite(dto: FavoriteDto, isCustom: Boolean) {
        favorites.removeAll { it.coffeeId == dto.coffeeId && it.userId == dto.userId }
        favorites.add(dto)
    }

    override suspend fun deleteFavorite(coffeeId: String, userId: Int, isCustom: Boolean) {
        favorites.removeAll { it.coffeeId == coffeeId && it.userId == userId }
    }

    override suspend fun getRatings(coffeeId: String): List<RatingDto> =
        ratings.filter { it.coffeeId == coffeeId }

    override suspend fun upsertRating(dto: RatingUpsertDto) {
        ratings.removeAll { it.coffeeId == dto.coffeeId && it.userId == dto.userId }
        ratings.add(
            RatingDto(
                id = (ratings.size + 1).toLong(),
                coffeeId = dto.coffeeId,
                userId = dto.userId,
                rating = dto.rating,
                comment = dto.comment,
                imageUrl = dto.imageUrl,
                timestamp = dto.timestamp,
                method = dto.method,
                ratio = dto.ratio,
                waterTemp = dto.waterTemp,
                extractionTime = dto.extractionTime,
                grindSize = dto.grindSize
            )
        )
    }

    override suspend fun deleteRating(coffeeId: String, userId: Int) {
        ratings.removeAll { it.coffeeId == coffeeId && it.userId == userId }
    }
}
