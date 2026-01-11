package com.example.cafesito.ui.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.UserRepository
import com.example.cafesito.data.UserEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompleteProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    fun saveUserProfile(
        googleId: String,
        email: String,
        username: String,
        bio: String,
        avatarUrl: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            // Generamos un ID interno único basado en el hash del googleId
            val internalId = googleId.hashCode()
            
            val newUser = UserEntity(
                id = internalId,
                googleId = googleId,
                username = username,
                fullName = username, 
                avatarUrl = avatarUrl,
                email = email,
                bio = bio
            )
            
            // Guardado real en Room
            userRepository.upsertUser(newUser)
            onSuccess()
        }
    }
}
