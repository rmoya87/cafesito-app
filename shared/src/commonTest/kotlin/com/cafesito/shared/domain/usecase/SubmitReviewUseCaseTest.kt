package com.cafesito.shared.domain.usecase

import com.cafesito.shared.domain.Review
import com.cafesito.shared.domain.User
import com.cafesito.shared.domain.repository.ReviewRepository
import kotlin.test.Test
import kotlin.test.assertTrue

class SubmitReviewUseCaseTest {
    @Test
    fun returnsFailureWhenValidationFails() = kotlinx.coroutines.test.runTest {
        val repo = FakeReviewRepository()
        val useCase = SubmitReviewUseCase(repo)
        val review = Review(
            user = sampleUser(),
            coffeeId = "coffee-1",
            rating = 0f,
            comment = ""
        )

        val result = useCase(review)

        assertTrue(result.isFailure)
    }

    @Test
    fun submitsWhenValidationSucceeds() = kotlinx.coroutines.test.runTest {
        val repo = FakeReviewRepository()
        val useCase = SubmitReviewUseCase(repo)
        val review = Review(
            user = sampleUser(),
            coffeeId = "coffee-1",
            rating = 8f,
            comment = "Excelente"
        )

        val result = useCase(review)

        assertTrue(result.isSuccess)
        assertTrue(repo.wasCalled)
    }

    private fun sampleUser() = User(
        id = 1,
        username = "tester",
        fullName = "Test User",
        avatarUrl = "https://example.com/avatar.png",
        email = "test@example.com",
        bio = null
    )

    private class FakeReviewRepository : ReviewRepository {
        var wasCalled: Boolean = false

        override suspend fun submitReview(review: Review): Result<Unit> {
            wasCalled = true
            return Result.success(Unit)
        }

        override suspend fun updateReview(review: Review): Result<Unit> {
            wasCalled = true
            return Result.success(Unit)
        }
        override suspend fun getReviewByUserAndCoffee(userId: Int, coffeeId: String): Result<Review?> =
            Result.success(null)
    }
}
