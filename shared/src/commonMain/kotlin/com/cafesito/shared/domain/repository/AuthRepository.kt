package com.cafesito.shared.domain.repository

import com.cafesito.shared.core.DataResult
import com.cafesito.shared.domain.model.AuthSession

interface AuthRepository {
    suspend fun signInWithGoogleIdToken(idToken: String): DataResult<AuthSession>
    suspend fun currentSession(): DataResult<AuthSession?>
    suspend fun signOut(): DataResult<Unit>
}
