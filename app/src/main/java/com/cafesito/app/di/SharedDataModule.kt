package com.cafesito.app.di

import com.cafesito.shared.data.remote.SupabaseRemoteDataSource
import com.cafesito.shared.data.remote.SupabaseRemoteDataSourceImpl
import com.cafesito.shared.data.repository.SupabaseReviewRepository
import com.cafesito.shared.domain.repository.ReviewRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SharedDataModule {
    @Provides
    @Singleton
    fun provideSupabaseRemoteDataSource(
        supabaseClient: SupabaseClient
    ): SupabaseRemoteDataSource = SupabaseRemoteDataSourceImpl(supabaseClient)

    @Provides
    @Singleton
    fun provideReviewRepository(
        remote: SupabaseRemoteDataSource
    ): ReviewRepository = SupabaseReviewRepository(remote)
}
