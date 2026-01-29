package com.cafesito.shared.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.cafesito.shared.cache.CoffeeCacheDatabase

class AndroidDatabaseDriverFactory(private val context: Context) : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = CoffeeCacheDatabase.Schema,
            context = context,
            name = "coffee_cache.db"
        )
    }
}
