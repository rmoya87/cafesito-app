package com.cafesito.shared.domain.usecase

import com.cafesito.shared.domain.repository.GreetingRepository

class GetGreetingUseCase(
    private val repository: GreetingRepository,
) {
    suspend operator fun invoke(userName: String): String = repository.fetchGreeting(userName)
}
