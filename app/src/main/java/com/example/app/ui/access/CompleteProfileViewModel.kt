package com.cafesito.app.ui.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.UserDao
import com.cafesito.app.data.UserEntity
import com.cafesito.app.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompleteProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userDao: UserDao,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _usernameError = MutableStateFlow<String?>(null)
    val usernameError = _usernameError.asStateFlow()

    fun saveUserProfile(
        googleId: String, // Este es el ID de Google (numérico)
        email: String,
        username: String,
        bio: String,
        avatarUrl: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (username.contains(" ")) {
                _usernameError.value = "El nombre de usuario no puede contener espacios"
                return@launch
            }

            val existingUser = userDao.getUserByUsername(username.trim())
            if (existingUser != null) {
                _usernameError.value = "Este nombre de usuario ya está en uso"
                return@launch
            }

            _usernameError.value = null

            // OBTENEMOS EL ID REAL DE SUPABASE AUTH (UUID)
            // Este es el que necesitamos para que las políticas RLS funcionen.
            val supabaseAuthId = supabaseClient.auth.currentUserOrNull()?.id ?: googleId

            val newUser = UserEntity(
                id = googleId.hashCode(), // Mantenemos el hashCode para el ID primario de Room
                googleId = supabaseAuthId, // USAMOS EL UUID DE SUPABASE PARA LA SEGURIDAD
                username = username.trim(),
                fullName = username.trim(), 
                avatarUrl = avatarUrl,
                email = email,
                bio = bio
            )
            
            userRepository.upsertUser(newUser)
            onSuccess()
        }
    }

    fun clearError() { _usernameError.value = null }
}
