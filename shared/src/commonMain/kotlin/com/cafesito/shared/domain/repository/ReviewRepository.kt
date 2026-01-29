package com.cafesito.shared.domain.repository

import com.cafesito.shared.core.Result
import com.cafesito.shared.domain.model.ReviewInput

interface ReviewRepository {
    suspend fun upsertReview(userId: Int, input: ReviewInput): Result<Unit>
}
