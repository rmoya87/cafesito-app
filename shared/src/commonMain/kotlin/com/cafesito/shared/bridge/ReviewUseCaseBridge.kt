package com.cafesito.shared.bridge

import com.cafesito.shared.core.Result
import com.cafesito.shared.domain.model.ReviewInput
import com.cafesito.shared.domain.usecase.SubmitReviewUseCase

class ReviewUseCaseBridge(
    private val submitReviewUseCase: SubmitReviewUseCase
) {
    suspend fun submitReview(input: ReviewInput): Result<Unit> = submitReviewUseCase(input)
}
