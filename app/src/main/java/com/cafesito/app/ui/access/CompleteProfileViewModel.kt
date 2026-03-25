package com.cafesito.app.ui.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _usernameError = MutableStateFlow<String?>(null)
    val usernameError = _usernameError.asStateFlow()

    fun saveUserProfile(
        googleId: String, // UUID de Supabase o fallback
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

            val trimmedUsername = username.trim()
            
            // OBTENEMOS EL ID REAL DE SUPABASE AUTH (UUID)
            val supabaseAuthId = supabaseClient.auth.currentUserOrNull()?.id ?: googleId

            // Verificamos si el username ya existe (en local o remoto)
            val existingUserByUsername = userRepository.getUserByUsername(trimmedUsername)
            if (existingUserByUsername != null && existingUserByUsername.googleId != supabaseAuthId) {
                _usernameError.value = "Este nombre de usuario ya está en uso"
                return@launch
            }

            _usernameError.value = null

            // Intentamos recuperar el usuario existente por UUID para mantener el ID entero si ya existía
            val existingUser = userRepository.getUserByGoogleId(supabaseAuthId)
            val userId = existingUser?.id ?: googleId.hashCode()

            val newUser = UserEntity(
                id = userId,
                googleId = supabaseAuthId,
                username = trimmedUsername,
                fullName = trimmedUsername,
                avatarUrl = avatarUrl,
                email = email,
                bio = bio,
                onboardingStatus = null,
                onboardingCompletedAt = null,
                onboardingSkippedAt = null,
                appTourSkippedAt = null,
                appTourDismissedSteps = null
            )
            
            userRepository.upsertUser(newUser)
            userRepository.setSession(userId)
            onSuccess()
        }
    }

    fun clearError() { _usernameError.value = null }
}
