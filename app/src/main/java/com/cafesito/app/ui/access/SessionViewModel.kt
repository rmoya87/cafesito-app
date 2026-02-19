package com.cafesito.app.ui.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SessionState {
    val userId: Int? get() = null

    data object Loading : SessionState
    data object NotAuthenticated : SessionState
    data class Authenticated(override val userId: Int) : SessionState
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            userRepository.restoreSessionFromSupabaseIfNeeded()
        }
    }

    /**
     * Observa el estado de la sesión de forma reactiva.
     * Usamos WhileSubscribed(5000) para permitir que el estado sobreviva a cambios de configuración
     * o bloqueos de pantalla, pero evitando ejecuciones costosas en el hilo principal durante el inicio frío.
     */
    val sessionState: StateFlow<SessionState> = userRepository.getActiveUserFlow()
        .map { user ->
            if (user != null && user.username.isNotBlank()) {
                SessionState.Authenticated(user.id)
            } else {
                SessionState.NotAuthenticated
            }
        }
        .onStart { emit(SessionState.Loading) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SessionState.Loading
        )

    fun refreshSession() {
        userRepository.triggerRefresh()
    }
}
