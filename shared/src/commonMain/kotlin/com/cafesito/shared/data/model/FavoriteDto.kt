package com.cafesito.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FavoriteDto(
    @SerialName("coffee_id") val coffeeId: String,
    @SerialName("user_id") val userId: Int,
    @SerialName("saved_at") val savedAt: Long = System.currentTimeMillis()
)
