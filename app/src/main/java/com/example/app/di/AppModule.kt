package com.cafesito.app.di

import android.content.Context
import androidx.room.Room
import com.cafesito.app.data.*
import com.cafesito.app.shared.AndroidReviewRepositoryAdapter
import com.cafesito.app.shared.AndroidUserSessionRepository
import com.cafesito.app.ui.utils.ConnectivityObserver
import com.cafesito.app.ui.utils.NetworkConnectivityObserver
import com.cafesito.shared.domain.repository.ReviewRepository
import com.cafesito.shared.domain.repository.UserSessionRepository
import com.cafesito.shared.domain.usecase.SubmitReviewUseCase
import com.cafesito.shared.domain.validation.ReviewValidator
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "cafesito_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideCoffeeDao(appDatabase: AppDatabase): CoffeeDao {
        return appDatabase.coffeeDao()
    }

    @Provides
    @Singleton
    fun provideUserDao(appDatabase: AppDatabase): UserDao {
        return appDatabase.userDao()
    }

    @Provides
    @Singleton
    fun provideSocialDao(appDatabase: AppDatabase): SocialDao {
        return appDatabase.socialDao()
    }

    @Provides
    @Singleton
    fun provideDiaryDao(appDatabase: AppDatabase): DiaryDao {
        return appDatabase.diaryDao()
    }

    @Provides
    @Singleton
    fun provideConnectivityObserver(
        @ApplicationContext context: Context
    ): ConnectivityObserver {
        return NetworkConnectivityObserver(context)
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): android.content.SharedPreferences {
        return context.getSharedPreferences("cafesito_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    fun provideReviewValidator(): ReviewValidator = ReviewValidator()

    @Provides
    fun provideReviewRepository(adapter: AndroidReviewRepositoryAdapter): ReviewRepository = adapter

    @Provides
    fun provideUserSessionRepository(adapter: AndroidUserSessionRepository): UserSessionRepository = adapter

    @Provides
    fun provideSubmitReviewUseCase(
        reviewRepository: ReviewRepository,
        userSessionRepository: UserSessionRepository,
        reviewValidator: ReviewValidator
    ): SubmitReviewUseCase = SubmitReviewUseCase(reviewRepository, userSessionRepository, reviewValidator)
}
