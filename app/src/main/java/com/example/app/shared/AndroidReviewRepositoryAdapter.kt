package com.cafesito.app.shared

import com.cafesito.app.data.CoffeeRepository
import com.cafesito.app.data.ReviewEntity
import com.cafesito.shared.core.DomainError
import com.cafesito.shared.core.Result
import com.cafesito.shared.domain.model.ReviewInput
import com.cafesito.shared.domain.repository.ReviewRepository
import javax.inject.Inject

class AndroidReviewRepositoryAdapter @Inject constructor(
    private val coffeeRepository: CoffeeRepository
) : ReviewRepository {
    override suspend fun upsertReview(userId: Int, input: ReviewInput): Result<Unit> {
        return try {
            val review = ReviewEntity(
                id = input.reviewId ?: 0,
                coffeeId = input.coffeeId,
                userId = userId,
                rating = input.rating,
                comment = input.comment,
                imageUrl = input.imageUrl,
                timestamp = input.timestampMs,
                method = input.method,
                ratio = input.ratio,
                waterTemp = input.waterTemp,
                extractionTime = input.extractionTime,
                grindSize = input.grindSize
            )
            coffeeRepository.upsertReview(review)
            Result.Success(Unit)
        } catch (exception: Exception) {
            Result.Failure(DomainError.Unknown(exception.message ?: "Unknown error"))
        }
    }
}
