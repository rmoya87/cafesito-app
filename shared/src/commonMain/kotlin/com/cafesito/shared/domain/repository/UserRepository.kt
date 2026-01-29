package com.cafesito.shared.domain.repository

import com.cafesito.shared.domain.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun activeUser(): Flow<User?>
    fun allUsers(): Flow<List<User>>
    fun followingMap(): Flow<Map<Int, Set<Int>>>
    suspend fun syncUsers(): Result<Unit>
}
