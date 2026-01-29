package com.cafesito.shared.domain.model

data class ReviewInput(
    val coffeeId: String,
    val rating: Float,
    val comment: String,
    val timestampMs: Long,
    val imageUrl: String? = null,
    val method: String? = null,
    val ratio: String? = null,
    val waterTemp: Int? = null,
    val extractionTime: String? = null,
    val grindSize: String? = null,
    val reviewId: Long? = null
)
