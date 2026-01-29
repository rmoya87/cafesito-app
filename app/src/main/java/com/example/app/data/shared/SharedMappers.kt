package com.cafesito.app.data.shared

import com.cafesito.app.data.CustomCoffeeEntity
import com.cafesito.app.data.DiaryEntryEntity
import com.cafesito.app.data.PantryItemEntity
import com.cafesito.shared.domain.model.CustomCoffee
import com.cafesito.shared.domain.model.DiaryEntry
import com.cafesito.shared.domain.model.PantryItem

fun DiaryEntryEntity.toDomain(): DiaryEntry = DiaryEntry(
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

fun DiaryEntry.toEntity(): DiaryEntryEntity = DiaryEntryEntity(
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

fun PantryItemEntity.toDomain(): PantryItem = PantryItem(
    coffeeId = coffeeId,
    userId = userId,
    gramsRemaining = gramsRemaining,
    totalGrams = totalGrams,
    lastUpdated = lastUpdated
)

fun PantryItem.toEntity(): PantryItemEntity = PantryItemEntity(
    coffeeId = coffeeId,
    userId = userId,
    gramsRemaining = gramsRemaining,
    totalGrams = totalGrams,
    lastUpdated = lastUpdated
)

fun CustomCoffeeEntity.toDomain(): CustomCoffee = CustomCoffee(
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

fun CustomCoffee.toEntity(): CustomCoffeeEntity = CustomCoffeeEntity(
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
