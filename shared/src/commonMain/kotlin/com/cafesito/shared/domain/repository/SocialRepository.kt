package com.cafesito.shared.domain.repository

import com.cafesito.shared.domain.Post
import kotlinx.coroutines.flow.Flow

interface SocialRepository {
    fun posts(): Flow<List<Post>>
    suspend fun syncSocialData(): Result<Unit>
}
