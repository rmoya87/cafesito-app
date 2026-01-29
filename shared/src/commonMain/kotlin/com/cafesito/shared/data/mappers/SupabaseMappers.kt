package com.cafesito.shared.data.mappers

import com.cafesito.shared.data.remote.CustomCoffeeDto
import com.cafesito.shared.data.remote.DiaryEntryDto
import com.cafesito.shared.data.remote.DiaryEntryInsertDto
import com.cafesito.shared.data.remote.FavoriteDto
import com.cafesito.shared.data.remote.PantryItemDto
import com.cafesito.shared.data.remote.RatingDto
import com.cafesito.shared.data.remote.RatingUpsertDto
import com.cafesito.shared.domain.model.AuthSession
import com.cafesito.shared.domain.model.CustomCoffee
import com.cafesito.shared.domain.model.DiaryEntry
import com.cafesito.shared.domain.model.Favorite
import com.cafesito.shared.domain.model.PantryItem
import com.cafesito.shared.domain.model.Rating
import io.github.jan.supabase.gotrue.Session

fun Session.toDomain(): AuthSession = AuthSession(
    accessToken = accessToken,
    refreshToken = refreshToken,
    userId = user?.id.orEmpty()
)

fun CustomCoffeeDto.toDomain(): CustomCoffee = CustomCoffee(
    id = id,
    userId = userId,
    name = name,
    brand = brand,
    specialty = specialty,
    roast = roast,
    variety = variety,
    country = country,
    hasCaffeine = hasCaffeine,
    format = format,
    imageUrl = imageUrl,
    totalGrams = totalGrams
)

fun CustomCoffee.toDto(): CustomCoffeeDto = CustomCoffeeDto(
    id = id,
    userId = userId,
    name = name,
    brand = brand,
    specialty = specialty,
    roast = roast,
    variety = variety,
    country = country,
    hasCaffeine = hasCaffeine,
    format = format,
    imageUrl = imageUrl,
    totalGrams = totalGrams
)

fun DiaryEntryDto.toDomain(): DiaryEntry = DiaryEntry(
    id = id,
    userId = userId,
    coffeeId = coffeeId,
    coffeeName = coffeeName,
    coffeeBrand = coffeeBrand,
    caffeineAmount = caffeineAmount,
    amountMl = amountMl,
    coffeeGrams = coffeeGrams,
    preparationType = preparationType,
    timestamp = timestamp,
    type = type,
    externalId = externalId
)

fun DiaryEntry.toInsertDto(): DiaryEntryInsertDto = DiaryEntryInsertDto(
    userId = userId,
    coffeeId = coffeeId,
    coffeeName = coffeeName,
    coffeeBrand = coffeeBrand,
    caffeineAmount = caffeineAmount,
    amountMl = amountMl,
    coffeeGrams = coffeeGrams,
    preparationType = preparationType,
    timestamp = timestamp,
    type = type,
    externalId = externalId
)

fun PantryItemDto.toDomain(): PantryItem = PantryItem(
    coffeeId = coffeeId,
    userId = userId,
    gramsRemaining = gramsRemaining,
    totalGrams = totalGrams,
    lastUpdated = lastUpdated
)

fun PantryItem.toDto(): PantryItemDto = PantryItemDto(
    coffeeId = coffeeId,
    userId = userId,
    gramsRemaining = gramsRemaining,
    totalGrams = totalGrams,
    lastUpdated = lastUpdated
)

fun FavoriteDto.toDomain(isCustom: Boolean): Favorite = Favorite(
    coffeeId = coffeeId,
    userId = userId,
    isCustom = isCustom,
    savedAt = savedAt
)

fun Favorite.toDto(): FavoriteDto = FavoriteDto(
    coffeeId = coffeeId,
    userId = userId,
    savedAt = savedAt
)

fun RatingDto.toDomain(): Rating = Rating(
    coffeeId = coffeeId,
    userId = userId,
    rating = rating,
    comment = comment,
    imageUrl = imageUrl,
    timestamp = timestamp,
    method = method,
    ratio = ratio,
    waterTemp = waterTemp,
    extractionTime = extractionTime,
    grindSize = grindSize
)

fun Rating.toUpsertDto(): RatingUpsertDto = RatingUpsertDto(
    coffeeId = coffeeId,
    userId = userId,
    rating = rating,
    comment = comment,
    imageUrl = imageUrl,
    timestamp = timestamp,
    method = method,
    ratio = ratio,
    waterTemp = waterTemp,
    extractionTime = extractionTime,
    grindSize = grindSize
)
