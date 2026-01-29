package com.cafesito.shared.data.repository

import com.cafesito.shared.core.DataResult
import com.cafesito.shared.core.safeCall
import com.cafesito.shared.data.mappers.toDomain
import com.cafesito.shared.domain.model.AuthSession
import com.cafesito.shared.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.IDToken

class SupabaseAuthRepository(
    private val client: SupabaseClient,
    private val timeoutMs: Long = 10_000L
) : AuthRepository {
    override suspend fun signInWithGoogleIdToken(idToken: String): DataResult<AuthSession> {
        return safeCall(timeoutMs) {
            client.auth.signInWith(IDToken) {
                token = idToken
            }
            val session = client.auth.currentSessionOrNull()
                ?: error("No session after sign-in")
            session.toDomain()
        }
    }

    override suspend fun currentSession(): DataResult<AuthSession?> {
        return safeCall(timeoutMs) {
            client.auth.currentSessionOrNull()?.toDomain()
        }
    }

    override suspend fun signOut(): DataResult<Unit> {
        return safeCall(timeoutMs) {
            client.auth.signOut()
        }
    }
}
