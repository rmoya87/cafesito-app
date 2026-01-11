package com.example.cafesito.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Coffee::class, 
        LocalFavorite::class,
        ReviewEntity::class,
        UserEntity::class,
        FollowEntity::class,
        PostEntity::class,
        CommentEntity::class,
        LikeEntity::class
    ],
    version = 9, // INCREMENTADO A 9 PARA RESOLVER IllegalStateException
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coffeeDao(): CoffeeDao
    abstract fun userDao(): UserDao
    abstract fun socialDao(): SocialDao
}
