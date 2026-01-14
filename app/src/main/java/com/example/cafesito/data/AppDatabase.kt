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
        LikeEntity::class,
        NotificationEntity::class
    ],
    version = 12, // INCREMENTADO DE 11 A 12
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coffeeDao(): CoffeeDao
    abstract fun userDao(): UserDao
    abstract fun socialDao(): SocialDao
}
