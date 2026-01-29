package com.cafesito.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReviewDto(
    val id: Long? = null,
    @SerialName("coffee_id") val coffeeId: String,
    @SerialName("user_id") val userId: Int,
    val rating: Float,
    val comment: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val timestamp: Long,
    val method: String? = null,
    val ratio: String? = null,
    @SerialName("water_temp") val waterTemp: Int? = null,
    @SerialName("extraction_time") val extractionTime: String? = null,
    @SerialName("grind_size") val grindSize: String? = null
)
