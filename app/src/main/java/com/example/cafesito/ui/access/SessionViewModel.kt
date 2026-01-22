package com.example.cafesito.ui.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

sealed interface SessionState {
    data object Loading : SessionState
    data object NotAuthenticated : SessionState
    data object Authenticated : SessionState
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    /**
     * Observa el estado de la sesión de forma reactiva.
     * Si el usuario se desloguea, getActiveUserFlow() emitirá null y
     * automáticamente pasaremos a NotAuthenticated.
     */
    val sessionState: StateFlow<SessionState> = userRepository.getActiveUserFlow()
        .map { user ->
            if (user != null && user.username.isNotBlank()) {
                SessionState.Authenticated
            } else {
                SessionState.NotAuthenticated
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SessionState.Loading
        )

    fun refreshSession() {
        userRepository.triggerRefresh()
    }
}
