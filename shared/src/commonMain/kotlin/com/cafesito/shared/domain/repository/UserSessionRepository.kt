package com.cafesito.shared.domain.repository

interface UserSessionRepository {
    suspend fun activeUserId(): Int?
}
