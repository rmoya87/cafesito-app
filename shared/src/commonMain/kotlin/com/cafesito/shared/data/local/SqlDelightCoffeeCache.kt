package com.cafesito.shared.data.local

import com.cafesito.shared.cache.CoffeeCacheDatabase
import com.cafesito.shared.domain.model.Coffee

class SqlDelightCoffeeCache(
    private val database: CoffeeCacheDatabase
) : CoffeeCache {
    private val queries = database.coffeeCacheQueries

    override suspend fun getAll(): List<Coffee> {
        return queries.selectAll().executeAsList().map {
            Coffee(
                id = it.id,
                name = it.name,
                origin = it.origin
            )
        }
    }

    override suspend fun lastUpdatedAt(): Long? {
        return queries.selectLastUpdatedAt().executeAsOneOrNull()?.MAX
    }

    override suspend fun replaceAll(coffees: List<Coffee>, updatedAt: Long) {
        queries.transaction {
            queries.deleteAll()
            coffees.forEach { coffee ->
                queries.upsert(
                    id = coffee.id,
                    name = coffee.name,
                    origin = coffee.origin,
                    updated_at = updatedAt
                )
            }
        }
    }
}
