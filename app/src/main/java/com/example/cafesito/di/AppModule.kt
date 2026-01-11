package com.example.cafesito.di

import android.content.Context
import androidx.room.Room
import com.example.cafesito.data.AppDatabase
import com.example.cafesito.data.CoffeeDao
import com.example.cafesito.data.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "cafesito_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideCoffeeDao(db: AppDatabase): CoffeeDao {
        return db.coffeeDao()
    }

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao {
        return db.userDao()
    }
}
