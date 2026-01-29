package com.cafesito.shared.domain

import com.cafesito.shared.domain.repository.GreetingRepository
import com.cafesito.shared.domain.usecase.GetGreetingUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetGreetingUseCaseTest {
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun returnsGreetingFromRepository() = runTest {
        val repository = object : GreetingRepository {
            override suspend fun fetchGreeting(userName: String): String = "Hola $userName"
        }
        val useCase = GetGreetingUseCase(repository)

        val result = useCase("Ana")

        assertEquals("Hola Ana", result)
    }
}
