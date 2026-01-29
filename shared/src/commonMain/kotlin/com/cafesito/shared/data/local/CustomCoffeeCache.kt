package com.cafesito.shared.data.local

import com.cafesito.shared.data.model.CustomCoffeeDto

class CustomCoffeeCache(private val database: CafesitoDatabase) {
    private val queries = database.customCoffeeCacheQueries

    fun selectByUser(userId: Int): List<CustomCoffeeDto> {
        return queries.selectByUser(userId.toLong()).executeAsList().map { row ->
            CustomCoffeeDto(
                id = row.id,
                userId = row.user_id.toInt(),
                name = row.name,
                brand = row.brand,
                specialty = row.specialty,
                roast = row.roast,
                variety = row.variety,
                country = row.country,
                hasCaffeine = row.has_caffeine != 0L,
                format = row.format,
                imageUrl = row.image_url,
                totalGrams = row.total_grams.toInt()
            )
        }
    }

    fun replaceAll(userId: Int, items: List<CustomCoffeeDto>) {
        queries.transaction {
            queries.clearByUser(userId.toLong())
            items.forEach { item ->
                queries.upsert(
                    id = item.id,
                    user_id = item.userId.toLong(),
                    name = item.name,
                    brand = item.brand,
                    specialty = item.specialty,
                    roast = item.roast,
                    variety = item.variety,
                    country = item.country,
                    has_caffeine = if (item.hasCaffeine) 1 else 0,
                    format = item.format,
                    image_url = item.imageUrl,
                    total_grams = item.totalGrams.toLong()
                )
            }
        }
    }
}
