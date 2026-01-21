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
        NotificationEntity::class,
        DiaryEntryEntity::class,
        PantryItemEntity::class,
        CustomCoffeeEntity::class
    ],
    version = 23, // INCREMENTADO DE 22 A 23
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coffeeDao(): CoffeeDao
    abstract fun userDao(): UserDao
    abstract fun socialDao(): SocialDao
    abstract fun diaryDao(): DiaryDao
}
