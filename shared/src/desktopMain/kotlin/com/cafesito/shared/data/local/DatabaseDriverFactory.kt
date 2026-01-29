package com.cafesito.shared.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.cafesito.shared.cache.CoffeeCacheDatabase

class DesktopDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver("jdbc:sqlite:coffee_cache.db")
        CoffeeCacheDatabase.Schema.create(driver)
        return driver
    }
}
