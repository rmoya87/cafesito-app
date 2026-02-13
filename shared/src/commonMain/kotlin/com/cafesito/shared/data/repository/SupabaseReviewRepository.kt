package com.cafesito.shared.data.repository

import com.cafesito.shared.data.model.ReviewDto
import com.cafesito.shared.data.remote.SupabaseRemoteDataSource
import com.cafesito.shared.domain.Review
import com.cafesito.shared.domain.User
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

    override suspend fun getReviewByUserAndCoffee(userId: Int, coffeeId: String): Result<Review?> = withContext(Dispatchers.Default) {
        runCatching {
            remote.getReviewByUserAndCoffee(userId, coffeeId)?.toDomain()
        }
    }
}

private fun Review.toDto(): ReviewDto = ReviewDto(
    id = id,
    coffeeId = coffeeId,
    userId = user.id,
    rating = rating,
    comment = comment,
    imageUrl = imageUrl,
    aroma = aroma,
    sabor = sabor,
    cuerpo = cuerpo,
    acidez = acidez,
    dulzura = dulzura,
    timestamp = timestamp
)

private fun ReviewDto.toDomain(): Review = Review(
    id = id,
    user = User(
        id = userId,
        username = "",
        fullName = "",
        avatarUrl = "",
        email = "",
        bio = null,
        favoriteCoffeeIds = emptyList()
    ),
    coffeeId = coffeeId,
    rating = rating,
    comment = comment,
    imageUrl = imageUrl,
    aroma = aroma,
    sabor = sabor,
    cuerpo = cuerpo,
    acidez = acidez,
    dulzura = dulzura,
    timestamp = timestamp
)
