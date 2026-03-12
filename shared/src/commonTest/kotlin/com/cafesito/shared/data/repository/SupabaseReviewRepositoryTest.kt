package com.cafesito.shared.data.repository

import com.cafesito.shared.data.model.CustomCoffeeDto
import com.cafesito.shared.data.model.DiaryEntryDto
import com.cafesito.shared.data.model.FavoriteDto
import com.cafesito.shared.data.model.PantryItemDto
import com.cafesito.shared.data.model.ReviewDto
import com.cafesito.shared.data.remote.SupabaseRemoteDataSource
import com.cafesito.shared.domain.Review
import com.cafesito.shared.domain.User
import kotlin.test.Test
import kotlin.test.assertTrue

class SupabaseReviewRepositoryTest {
    @Test
    fun submitReviewReturnsSuccess() = kotlinx.coroutines.test.runTest {
        val remote = FakeRemoteDataSource()
        val repository = SupabaseReviewRepository(remote)

        val result = repository.submitReview(sampleReview())

        assertTrue(result.isSuccess)
        assertTrue(remote.didUpsertReview)
    }

    @Test
    fun submitReviewReturnsFailureOnError() = kotlinx.coroutines.test.runTest {
        val remote = FakeRemoteDataSource(shouldThrow = true)
        val repository = SupabaseReviewRepository(remote)

        val result = repository.submitReview(sampleReview())

        assertTrue(result.isFailure)
    }

    private fun sampleReview() = Review(
        user = User(
            id = 1,
            username = "tester",
            fullName = "Test User",
            avatarUrl = "https://example.com/avatar.png",
            email = "test@example.com",
            bio = null
        ),
        coffeeId = "coffee-1",
        rating = 8f,
        comment = "Excelente",
        imageUrl = "https://example.com/review.png",
        timestamp = 1_700_000_000_000
    )

    private class FakeRemoteDataSource(
        private val shouldThrow: Boolean = false
    ) : SupabaseRemoteDataSource {
        var didUpsertReview: Boolean = false

        override suspend fun signInWithGoogleIdToken(idToken: String): String? = "uid"
        override suspend fun signOut() = Unit
        override suspend fun getCustomCoffees(userId: Int): List<CustomCoffeeDto> = emptyList()
        override suspend fun upsertCustomCoffee(coffee: CustomCoffeeDto) = Unit
        override suspend fun insertDiaryEntry(entry: DiaryEntryDto) = Unit
        override suspend fun getDiaryEntries(userId: Int): List<DiaryEntryDto> = emptyList()
        override suspend fun upsertPantryItem(item: PantryItemDto) = Unit
        override suspend fun getPantryItems(userId: Int): List<PantryItemDto> = emptyList()
        override suspend fun getFavorites(userId: Int): List<FavoriteDto> = emptyList()
        override suspend fun upsertFavorite(favorite: FavoriteDto) = Unit
        override suspend fun deleteFavorite(coffeeId: String, userId: Int) = Unit

        override suspend fun upsertReview(review: ReviewDto) {
            if (shouldThrow) error("boom")
            didUpsertReview = true
        }
        override suspend fun getReviewByUserAndCoffee(userId: Int, coffeeId: String): ReviewDto? = null
    }
}
