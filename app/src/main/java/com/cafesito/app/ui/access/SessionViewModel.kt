package com.cafesito.app.ui.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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

    /**
     * Observa el estado de la sesión de forma reactiva.
     * Primero restauramos la sesión desde Supabase (si existe token pero no hay fila en active_session)
     * y solo después emitimos desde getActiveUserFlow(), para que la detección de usuario sea correcta
     * en Timeline, Perfil y resto de pantallas.
     */
    val sessionState: StateFlow<SessionState> = flow {
        emit(SessionState.Loading)
        userRepository.restoreSessionFromSupabaseIfNeeded()
        emitAll(
            userRepository.getActiveUserFlow().map { user ->
                if (user != null && user.username.isNotBlank()) {
                    SessionState.Authenticated(user.id)
                } else {
                    SessionState.NotAuthenticated
                }
            }
        )
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
