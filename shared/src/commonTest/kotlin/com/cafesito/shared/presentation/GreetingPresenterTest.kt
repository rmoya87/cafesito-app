package com.cafesito.shared.presentation

import com.cafesito.shared.TestDispatcherProvider
import com.cafesito.shared.domain.repository.GreetingRepository
import com.cafesito.shared.domain.usecase.GetGreetingUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GreetingPresenterTest {
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun emitsGreetingAfterLoad() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val presenter = GreetingPresenter(
            useCase = GetGreetingUseCase(
                repository = object : GreetingRepository {
                    override suspend fun fetchGreeting(userName: String): String = "Hola $userName"
                },
            ),
            dispatcherProvider = TestDispatcherProvider(dispatcher),
        )

        presenter.loadGreeting("Ana")
        advanceUntilIdle()

        assertEquals("Hola Ana", presenter.state.value.message)
    }
}
