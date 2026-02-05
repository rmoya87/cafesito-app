package com.cafesito.shared.domain.repository

import com.cafesito.shared.domain.Review

interface ReviewRepository {
    suspend fun submitReview(review: Review): Result<Unit>
    suspend fun updateReview(review: Review): Result<Unit>
    suspend fun getReviewByUserAndCoffee(userId: Int, coffeeId: String): Result<Review?>
}
