package com.cafesito.shared.domain.usecase

import com.cafesito.shared.domain.Review
import com.cafesito.shared.domain.repository.ReviewRepository
import com.cafesito.shared.domain.validation.ValidateReviewInputUseCase

class SubmitReviewUseCase(
    private val reviewRepository: ReviewRepository,
    private val validateReviewInput: ValidateReviewInputUseCase = ValidateReviewInputUseCase()
) {
    suspend operator fun invoke(review: Review): Result<Unit> {
        val validation = validateReviewInput(review.rating, review.comment)
        if (validation.isFailure) return validation
        return reviewRepository.submitReview(review)
    }
}
