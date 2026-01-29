package com.cafesito.shared.domain

import com.cafesito.shared.core.DomainError
import com.cafesito.shared.core.Result
import com.cafesito.shared.domain.model.ReviewInput
import com.cafesito.shared.domain.validation.ReviewValidationError
import com.cafesito.shared.domain.validation.ReviewValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReviewValidatorTest {
    private val validator = ReviewValidator()

    @Test
    fun `accepts valid review`() {
        val input = ReviewInput(
            coffeeId = "coffee-1",
            rating = 4.5f,
            comment = "Muy buen café",
            timestampMs = 1234L
        )

        val result = validator.validate(input)

        assertTrue(result is Result.Success)
    }

    @Test
    fun `returns multiple errors for invalid input`() {
        val input = ReviewInput(
            coffeeId = "",
            rating = 0f,
            comment = " ",
            timestampMs = 1234L
        )

        val result = validator.validate(input)

        assertTrue(result is Result.Failure)
        val error = (result as Result.Failure).error as DomainError.Validation
        assertEquals(
            listOf(
                ReviewValidationError.MissingCoffeeId,
                ReviewValidationError.RatingOutOfRange,
                ReviewValidationError.CommentBlank
            ),
            error.errors
        )
    }
}
