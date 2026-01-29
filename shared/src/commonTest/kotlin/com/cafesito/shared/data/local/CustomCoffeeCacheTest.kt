package com.cafesito.shared.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.cafesito.shared.data.model.CustomCoffeeDto
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomCoffeeCacheTest {
    @Test
    fun replaceAllOverwritesUserCache() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CafesitoDatabase.Schema.create(driver)
        val database = CafesitoDatabase(driver)
        val cache = CustomCoffeeCache(database)

        val first = listOf(sampleCoffee(id = "1"))
        cache.replaceAll(1, first)
        assertEquals(1, cache.selectByUser(1).size)

        val second = listOf(sampleCoffee(id = "2"))
        cache.replaceAll(1, second)
        val stored = cache.selectByUser(1)

        assertEquals(1, stored.size)
        assertEquals("2", stored.first().id)
    }

    private fun sampleCoffee(id: String) = CustomCoffeeDto(
        id = id,
        userId = 1,
        name = "Casa",
        brand = "Marca",
        specialty = "Arabica",
        roast = "Medium",
        variety = "Bourbon",
        country = "Colombia",
        hasCaffeine = true,
        format = "Grano",
        imageUrl = "https://example.com/coffee.png",
        totalGrams = 250
    )
}
