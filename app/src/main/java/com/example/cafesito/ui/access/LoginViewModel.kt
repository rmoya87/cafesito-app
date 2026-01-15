package com.example.cafesito.ui.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun handleGoogleIdToken(
        idToken: String,
        onSuccess: (String, Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val supabaseUuid = userRepository.signInWithSupabase(idToken)
            
            if (supabaseUuid != null) {
                // Verificamos si el usuario ya tiene un perfil en Supabase
                val existingUser = userRepository.getUserByGoogleId(supabaseUuid)
                
                if (existingUser != null && existingUser.username.isNotBlank()) {
                    // El usuario ya existe, lo guardamos localmente para la sesión
                    userRepository.upsertLocalOnly(existingUser)
                    _isLoading.value = false
                    onSuccess(supabaseUuid, false) // isNewUser = false
                } else {
                    // Usuario nuevo o perfil incompleto
                    _isLoading.value = false
                    onSuccess(supabaseUuid, true) // isNewUser = true
                }
            } else {
                _isLoading.value = false
                onError("Error al vincular con la base de datos de Cafesito")
            }
        }
    }
}
