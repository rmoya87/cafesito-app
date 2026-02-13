package com.cafesito.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Coffee::class, 
        LocalFavorite::class,
        LocalFavoriteCustom::class,
        ReviewEntity::class,
        UserEntity::class,
        FollowEntity::class,
        PostEntity::class,
        CommentEntity::class,
        LikeEntity::class,
        NotificationEntity::class,
        DiaryEntryEntity::class,
        PantryItemEntity::class,
        CustomCoffeeEntity::class,
        UserTokenEntity::class,
        ActiveSessionEntity::class
    ],
    version = 28, // INCREMENTADO DE 27 A 28 para resolver conflictos de integridad de datos
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coffeeDao(): CoffeeDao
    abstract fun userDao(): UserDao
    abstract fun socialDao(): SocialDao
    abstract fun diaryDao(): DiaryDao
    abstract fun sessionDao(): SessionDao
}
