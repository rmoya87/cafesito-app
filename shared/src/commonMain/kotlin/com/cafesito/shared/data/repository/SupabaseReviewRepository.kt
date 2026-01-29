package com.cafesito.shared.data.repository

import com.cafesito.shared.data.model.ReviewDto
import com.cafesito.shared.data.remote.SupabaseRemoteDataSource
import com.cafesito.shared.domain.Review
import com.cafesito.shared.domain.repository.ReviewRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseReviewRepository(
    private val remote: SupabaseRemoteDataSource
) : ReviewRepository {
    override suspend fun submitReview(review: Review): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            remote.upsertReview(review.toDto())
        }
    }

    override suspend fun updateReview(review: Review): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            remote.upsertReview(review.toDto())
        }
    }
}

private fun Review.toDto(): ReviewDto = ReviewDto(
    coffeeId = coffeeId,
    userId = user.id,
    rating = rating,
    comment = comment,
    imageUrl = imageUrl,
    timestamp = timestamp
)
