package com.cafesito.shared.domain.usecase

import com.cafesito.shared.core.DomainError
import com.cafesito.shared.core.Result
import com.cafesito.shared.domain.model.ReviewInput
import com.cafesito.shared.domain.repository.ReviewRepository
import com.cafesito.shared.domain.repository.UserSessionRepository
import com.cafesito.shared.domain.validation.ReviewValidator

class SubmitReviewUseCase(
    private val reviewRepository: ReviewRepository,
    private val userSessionRepository: UserSessionRepository,
    private val reviewValidator: ReviewValidator
) {
    suspend operator fun invoke(input: ReviewInput): Result<Unit> {
        val validation = reviewValidator.validate(input)
        if (validation is Result.Failure) {
            return validation
        }
        val userId = userSessionRepository.activeUserId()
            ?: return Result.Failure(DomainError.NotAuthenticated)

        return reviewRepository.upsertReview(userId, input)
    }
}
