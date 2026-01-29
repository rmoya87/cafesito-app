package com.cafesito.shared.domain.validation

import com.cafesito.shared.core.DomainError
import com.cafesito.shared.core.Result
import com.cafesito.shared.domain.model.ReviewInput

class ReviewValidator {
    fun validate(input: ReviewInput): Result<ReviewInput> {
        val errors = buildList {
            if (input.coffeeId.isBlank()) {
                add(ReviewValidationError.MissingCoffeeId)
            }
            if (input.rating <= 0f || input.rating > 5f) {
                add(ReviewValidationError.RatingOutOfRange)
            }
            if (input.comment.isBlank()) {
                add(ReviewValidationError.CommentBlank)
            }
        }

        return if (errors.isEmpty()) {
            Result.Success(input)
        } else {
            Result.Failure(DomainError.Validation(errors))
        }
    }
}
