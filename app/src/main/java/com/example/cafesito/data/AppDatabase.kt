package com.example.cafesito.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Coffee::class, Origin::class, ScoreSource::class, SensoryProfile::class, LocalFavorite::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coffeeDao(): CoffeeDao
    abstract fun originDao(): OriginDao
    abstract fun scoreSourceDao(): ScoreSourceDao
}
