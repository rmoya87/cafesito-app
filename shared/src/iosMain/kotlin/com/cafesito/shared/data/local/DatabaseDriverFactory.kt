package com.cafesito.shared.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.cafesito.shared.cache.CoffeeCacheDatabase

class IosDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = CoffeeCacheDatabase.Schema,
            name = "coffee_cache.db"
        )
    }
}
