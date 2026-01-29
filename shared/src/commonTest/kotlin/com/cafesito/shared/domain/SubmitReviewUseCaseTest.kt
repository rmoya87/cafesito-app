package com.cafesito.shared.domain

import com.cafesito.shared.core.DomainError
import com.cafesito.shared.core.Result
import com.cafesito.shared.domain.model.ReviewInput
import com.cafesito.shared.domain.repository.ReviewRepository
import com.cafesito.shared.domain.repository.UserSessionRepository
import com.cafesito.shared.domain.usecase.SubmitReviewUseCase
import com.cafesito.shared.domain.validation.ReviewValidator
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubmitReviewUseCaseTest {
    @Test
    fun `returns not authenticated when no user`() = runBlocking {
        val useCase = SubmitReviewUseCase(
            reviewRepository = FakeReviewRepository(),
            userSessionRepository = FakeUserSessionRepository(null),
            reviewValidator = ReviewValidator()
        )

        val result = useCase(
            ReviewInput(
                coffeeId = "coffee-1",
                rating = 4f,
                comment = "Nice",
                timestampMs = 100L
            )
        )

        assertTrue(result is Result.Failure)
        assertEquals(DomainError.NotAuthenticated, (result as Result.Failure).error)
    }

    @Test
    fun `submits review when valid`() = runBlocking {
        val repository = FakeReviewRepository()
        val useCase = SubmitReviewUseCase(
            reviewRepository = repository,
            userSessionRepository = FakeUserSessionRepository(7),
            reviewValidator = ReviewValidator()
        )

        val input = ReviewInput(
            coffeeId = "coffee-1",
            rating = 4f,
            comment = "Nice",
            timestampMs = 100L
        )

        val result = useCase(input)

        assertTrue(result is Result.Success)
        assertEquals(7, repository.lastUserId)
        assertEquals(input, repository.lastInput)
    }
}

private class FakeReviewRepository : ReviewRepository {
    var lastUserId: Int? = null
    var lastInput: ReviewInput? = null

    override suspend fun upsertReview(userId: Int, input: ReviewInput): Result<Unit> {
        lastUserId = userId
        lastInput = input
        return Result.Success(Unit)
    }
}

private class FakeUserSessionRepository(private val userId: Int?) : UserSessionRepository {
    override suspend fun activeUserId(): Int? = userId
}
