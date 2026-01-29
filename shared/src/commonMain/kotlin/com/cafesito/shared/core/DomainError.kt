package com.cafesito.shared.core

sealed class DomainError(message: String) : Throwable(message)

class ValidationError(
    val field: String,
    message: String
) : DomainError(message)
