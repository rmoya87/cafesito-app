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
    data object NewUser : SessionState
    data object Registered : SessionState
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
                // 1. Verificamos si hay un usuario autenticado en el motor de Auth de Supabase
                val currentUid = supabaseClient.auth.currentUserOrNull()?.id
                
                if (currentUid != null) {
                    // 2. Si hay sesión, descargamos el perfil de Supabase para validar si está completo
                    val activeUser = userRepository.getActiveUser()
                    if (activeUser != null && activeUser.username.isNotBlank()) {
                        _sessionState.value = SessionState.Authenticated
                    } else {
                        // Sesión activa pero perfil sin completar (ej: falta el username)
                        _sessionState.value = SessionState.Registered
                    }
                } else {
                    // No hay sesión en Supabase (o ha expirado)
                    _sessionState.value = SessionState.NewUser
                }
            } catch (e: Exception) {
                Log.e("SessionViewModel", "Error comprobando sesión", e)
                _sessionState.value = SessionState.NewUser
            }
        }
    }
}
