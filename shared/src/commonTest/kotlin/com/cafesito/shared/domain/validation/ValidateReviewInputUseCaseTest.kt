package com.cafesito.shared.domain.validation

import kotlin.test.Test
import kotlin.test.assertTrue

class ValidateReviewInputUseCaseTest {
    private val useCase = ValidateReviewInputUseCase()

    @Test
    fun acceptsValidRatingAndComment() {
        val result = useCase(rating = 7.5f, comment = "Buen café")
        assertTrue(result.isSuccess)
    }

    @Test
    fun rejectsBlankComment() {
        val result = useCase(rating = 6.0f, comment = "   ")
        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsOutOfRangeRating() {
        val result = useCase(rating = 11.0f, comment = "Demasiado")
        assertTrue(result.isFailure)
    }
}
