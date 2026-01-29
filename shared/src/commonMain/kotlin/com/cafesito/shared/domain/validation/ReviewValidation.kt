package com.cafesito.shared.domain.validation

import com.cafesito.shared.core.ValidationError

class ValidateReviewInputUseCase(
    private val minRating: Float = 0.5f,
    private val maxRating: Float = 10f
) {
    operator fun invoke(rating: Float, comment: String): Result<Unit> {
        if (rating.isNaN() || rating < minRating || rating > maxRating) {
            return Result.failure(
                ValidationError(
                    field = "rating",
                    message = "Rating must be between $minRating and $maxRating."
                )
            )
        }
        if (comment.isBlank()) {
            return Result.failure(
                ValidationError(
                    field = "comment",
                    message = "Comment cannot be blank."
                )
            )
        }
        return Result.success(Unit)
    }
}
