package com.example.cafesito.ui.access

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SessionState {
    data object Loading : SessionState
    data object NotAuthenticated : SessionState
    data object Authenticated : SessionState
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState = _sessionState.asStateFlow()

    init {
        checkSession()
    }

    fun refreshSession() {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            try {
                val currentUid = supabaseClient.auth.currentUserOrNull()?.id
                
                if (currentUid != null) {
                    val activeUser = userRepository.getActiveUser()
                    if (activeUser != null && activeUser.username.isNotBlank()) {
                        _sessionState.value = SessionState.Authenticated
                    } else {
                        // Si hay sesión pero el perfil no está listo, lo tratamos como No Autenticado
                        // para que pase por el flujo de Login/CompleteProfile si es necesario
                        _sessionState.value = SessionState.NotAuthenticated
                    }
                } else {
                    _sessionState.value = SessionState.NotAuthenticated
                }
            } catch (e: Exception) {
                Log.e("SessionViewModel", "Error comprobando sesión", e)
                _sessionState.value = SessionState.NotAuthenticated
            }
        }
    }
}
