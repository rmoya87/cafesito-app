package com.cafesito.shared.presentation

import com.cafesito.shared.core.DispatcherProvider
import com.cafesito.shared.domain.usecase.GetGreetingUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GreetingUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class GreetingPresenter(
    private val useCase: GetGreetingUseCase,
    private val dispatcherProvider: DispatcherProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.default)
    private val _state = MutableStateFlow(GreetingUiState())
    val state: StateFlow<GreetingUiState> = _state.asStateFlow()

    fun loadGreeting(userName: String) {
        _state.value = GreetingUiState(isLoading = true)
        scope.launch {
            runCatching { useCase(userName) }
                .onSuccess { message ->
                    _state.value = GreetingUiState(message = message)
                }
                .onFailure { error ->
                    _state.value = GreetingUiState(error = error.message ?: "Unknown error")
                }
        }
    }

    fun clear() {
        scope.coroutineContext.cancel()
    }
}
