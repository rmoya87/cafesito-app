package com.cafesito.shared.domain.model

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String
)

data class CustomCoffee(
    val id: String,
    val userId: Int,
    val name: String,
    val brand: String,
    val specialty: String,
    val roast: String? = null,
    val variety: String? = null,
    val country: String,
    val hasCaffeine: Boolean,
    val format: String,
    val imageUrl: String,
    val totalGrams: Int = 250
)

data class DiaryEntry(
    val id: Long = 0,
    val userId: Int,
    val coffeeId: String? = null,
    val coffeeName: String,
    val coffeeBrand: String = "",
    val caffeineAmount: Int,
    val amountMl: Int = 250,
    val coffeeGrams: Int = 15,
    val preparationType: String = "Espresso",
    val timestamp: Long,
    val type: String = "CUP",
    val externalId: String? = null
)

data class PantryItem(
    val coffeeId: String,
    val userId: Int,
    val gramsRemaining: Int,
    val totalGrams: Int,
    val lastUpdated: Long
)

data class Favorite(
    val coffeeId: String,
    val userId: Int,
    val isCustom: Boolean,
    val savedAt: Long
)

data class Rating(
    val coffeeId: String,
    val userId: Int,
    val rating: Float,
    val comment: String,
    val imageUrl: String? = null,
    val timestamp: Long,
    val method: String? = null,
    val ratio: String? = null,
    val waterTemp: Int? = null,
    val extractionTime: String? = null,
    val grindSize: String? = null
)
