package com.cafesito.shared.data.local

import com.cafesito.shared.cache.CoffeeCacheDatabase

class CoffeeCacheDatabaseFactory(
    private val driverFactory: DatabaseDriverFactory
) {
    fun createDatabase(): CoffeeCacheDatabase {
        return CoffeeCacheDatabase(driverFactory.createDriver())
    }
}
