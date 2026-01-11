package com.example.cafesito.ui.access

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
    private val userRepository: UserRepository
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
                val activeUser = userRepository.getActiveUser()
                if (activeUser != null) {
                    if (activeUser.username.isNotBlank()) {
                        _sessionState.value = SessionState.Authenticated
                    } else {
                        _sessionState.value = SessionState.Registered
                    }
                } else {
                    _sessionState.value = SessionState.NewUser
                }
            } catch (e: Exception) {
                Log.e("SessionViewModel", "Error comprobando sesión", e)
                _sessionState.value = SessionState.NewUser
            }
        }
    }
}
