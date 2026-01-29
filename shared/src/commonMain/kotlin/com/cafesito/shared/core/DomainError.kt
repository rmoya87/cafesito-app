package com.cafesito.shared.core

import com.cafesito.shared.domain.validation.ReviewValidationError

sealed class DomainError {
    data class Validation(val errors: List<ReviewValidationError>) : DomainError()
    data object NotAuthenticated : DomainError()
    data class Unknown(val message: String) : DomainError()
}
