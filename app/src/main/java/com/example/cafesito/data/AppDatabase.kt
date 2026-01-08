package com.example.cafesito.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Coffee::class, 
        Origin::class, 
        ScoreSource::class, 
        SensoryProfile::class, 
        LocalFavorite::class,
        UserEntity::class,
        FollowEntity::class
    ],
    version = 4, // Incremented version for user and follow entities
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coffeeDao(): CoffeeDao
    abstract fun originDao(): OriginDao
    abstract fun scoreSourceDao(): ScoreSourceDao
    abstract fun userDao(): UserDao
}
