package com.cafesito.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PantryItemDto(
    @SerialName("coffee_id") val coffeeId: String,
    @SerialName("user_id") val userId: Int,
    @SerialName("grams_remaining") val gramsRemaining: Int,
    @SerialName("total_grams") val totalGrams: Int,
    @SerialName("last_updated") val lastUpdated: Long = System.currentTimeMillis()
)
