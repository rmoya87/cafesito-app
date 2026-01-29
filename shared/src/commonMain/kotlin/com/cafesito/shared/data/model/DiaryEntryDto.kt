package com.cafesito.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiaryEntryDto(
    val id: Long? = null,
    @SerialName("user_id") val userId: Int,
    @SerialName("coffee_id") val coffeeId: String? = null,
    @SerialName("coffee_name") val coffeeName: String,
    @SerialName("coffee_brand") val coffeeBrand: String = "",
    @SerialName("caffeine_mg") val caffeineAmount: Int,
    @SerialName("amount_ml") val amountMl: Int = 250,
    @SerialName("coffee_grams") val coffeeGrams: Int = 15,
    @SerialName("preparation_type") val preparationType: String = "Espresso",
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "CUP",
    @SerialName("external_id") val externalId: String? = null
)
