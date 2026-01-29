package com.cafesito.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomCoffeeDto(
    val id: String,
    @SerialName("user_id") val userId: Int,
    val name: String,
    val brand: String,
    val specialty: String,
    val roast: String? = null,
    val variety: String? = null,
    val country: String,
    @SerialName("has_caffeine") val hasCaffeine: Boolean,
    val format: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("total_grams") val totalGrams: Int = 250
)
