package com.cafesito.shared.data.remote

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

@Serializable
data class DiaryEntryDto(
    val id: Long = 0,
    @SerialName("user_id") val userId: Int,
    @SerialName("coffee_id") val coffeeId: String? = null,
    @SerialName("coffee_name") val coffeeName: String,
    @SerialName("coffee_brand") val coffeeBrand: String = "",
    @SerialName("caffeine_mg") val caffeineAmount: Int,
    @SerialName("amount_ml") val amountMl: Int = 250,
    @SerialName("coffee_grams") val coffeeGrams: Int = 15,
    @SerialName("preparation_type") val preparationType: String = "Espresso",
    val timestamp: Long,
    val type: String = "CUP",
    @SerialName("external_id") val externalId: String? = null
)

@Serializable
data class DiaryEntryInsertDto(
    @SerialName("user_id") val userId: Int,
    @SerialName("coffee_id") val coffeeId: String? = null,
    @SerialName("coffee_name") val coffeeName: String,
    @SerialName("coffee_brand") val coffeeBrand: String = "",
    @SerialName("caffeine_mg") val caffeineAmount: Int,
    @SerialName("amount_ml") val amountMl: Int,
    @SerialName("coffee_grams") val coffeeGrams: Int,
    @SerialName("preparation_type") val preparationType: String,
    val timestamp: Long,
    val type: String,
    @SerialName("external_id") val externalId: String? = null
)

@Serializable
data class PantryItemDto(
    @SerialName("coffee_id") val coffeeId: String,
    @SerialName("user_id") val userId: Int,
    @SerialName("grams_remaining") val gramsRemaining: Int,
    @SerialName("total_grams") val totalGrams: Int,
    @SerialName("last_updated") val lastUpdated: Long
)

@Serializable
data class FavoriteDto(
    @SerialName("coffee_id") val coffeeId: String,
    @SerialName("user_id") val userId: Int,
    @SerialName("saved_at") val savedAt: Long
)

@Serializable
data class RatingDto(
    val id: Long = 0,
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

@Serializable
data class RatingUpsertDto(
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
