package com.example.cafesito.ui.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.UserDao
import com.example.cafesito.data.UserEntity
import com.example.cafesito.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompleteProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userDao: UserDao
) : ViewModel() {

    private val _usernameError = MutableStateFlow<String?>(null)
    val usernameError = _usernameError.asStateFlow()

    fun saveUserProfile(
        googleId: String,
        email: String,
        username: String,
        bio: String,
        avatarUrl: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            // 1. Validar espacios
            if (username.contains(" ")) {
                _usernameError.value = "El nombre de usuario no puede contener espacios"
                return@launch
            }

            // 2. Validar unicidad en DB
            val existingUser = userDao.getUserByUsername(username.trim())
            if (existingUser != null) {
                _usernameError.value = "Este nombre de usuario ya está en uso"
                return@launch
            }

            _usernameError.value = null

            val internalId = googleId.hashCode()
            val newUser = UserEntity(
                id = internalId,
                googleId = googleId,
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
