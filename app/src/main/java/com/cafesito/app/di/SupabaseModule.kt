package com.cafesito.app.di

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.plugins.HttpTimeout
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json
import com.cafesito.app.BuildConfig

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @OptIn(SupabaseInternal::class)
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        ) {
            install(Postgrest)
            install(Auth) {
                alwaysAutoRefresh = true
            }
            install(Realtime)
            install(Storage)

            // Configurar el serializador para ser más flexible con los datos de la DB
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                encodeDefaults = true
                isLenient = true
            })

            // Configuración global de HTTP usando el plugin de Ktor
            httpConfig {
                install(HttpTimeout) {
                    requestTimeoutMillis = 30.seconds.inWholeMilliseconds
                    connectTimeoutMillis = 30.seconds.inWholeMilliseconds
                    socketTimeoutMillis = 30.seconds.inWholeMilliseconds
                }
            }
        }
    }
}
