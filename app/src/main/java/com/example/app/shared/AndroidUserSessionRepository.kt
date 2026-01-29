package com.cafesito.app.shared

import com.cafesito.app.data.UserRepository
import com.cafesito.shared.domain.repository.UserSessionRepository
import javax.inject.Inject

class AndroidUserSessionRepository @Inject constructor(
    private val userRepository: UserRepository
) : UserSessionRepository {
    override suspend fun activeUserId(): Int? = userRepository.getActiveUser()?.id
}
