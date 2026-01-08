package com.example.cafesito.ui.profile

import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.CoffeeRepository
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.domain.Post
import com.example.cafesito.domain.User
import com.example.cafesito.domain.allUsers
import com.example.cafesito.domain.currentUser
import com.example.cafesito.domain.samplePosts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val coffeeRepository: CoffeeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val userId: Int = checkNotNull(savedStateHandle["userId"])
    private val _userFlow = MutableStateFlow(allUsers.find { it.id == userId })
    private val _isEditing = MutableStateFlow(false)
    private val _emailError = MutableStateFlow<String?>(null)
    private val _showImagePicker = MutableStateFlow(false)

    private data class SubState(val isEditing: Boolean, val emailError: String?, val showImagePicker: Boolean)
    private val _uiSubState = combine(_isEditing, _emailError, _showImagePicker) { isEditing, emailError, showImagePicker ->
        SubState(isEditing, emailError, showImagePicker)
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        _userFlow, coffeeRepository.allCoffees, coffeeRepository.favorites, _uiSubState
    ) { user, allCoffees, favorites, subState ->
        if (user == null) {
            ProfileUiState.Error("Usuario no encontrado")
        } else {
            val userPosts = samplePosts.filter { it.user.id == userId }
            val favoriteCoffees = allCoffees.filter { coffeeDetails ->
                favorites.any { it.coffeeId == coffeeDetails.coffee.id }
            }
            ProfileUiState.Success(
                user = user,
                isCurrentUser = (userId == currentUser.id),
                followers = 250, // Mock
                following = 120, // Mock
                posts = userPosts,
                favoriteCoffees = favoriteCoffees,
                isEditing = subState.isEditing,
                emailError = subState.emailError,
                showImageSourceDialog = subState.showImagePicker
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState.Loading
    )

    fun toggleEditMode() {
        _isEditing.value = !_isEditing.value
        _emailError.value = null // Clear error on mode change
    }

    fun onSaveProfile(newAvatarUrl: String, newBio: String, newEmail: String) {
        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            _emailError.value = "El formato del email no es válido"
            return
        }
        
        _emailError.value = null
        val updatedUser = _userFlow.value?.copy(
            avatarUrl = newAvatarUrl,
            bio = newBio,
            email = newEmail
        )
        _userFlow.value = updatedUser

        val userIndex = allUsers.indexOfFirst { it.id == userId }
        if (userIndex != -1 && updatedUser != null) {
            allUsers[userIndex] = updatedUser
            // Propagate user changes to posts
            val updatedPosts = samplePosts.map {
                if (it.user.id == userId) it.copy(user = updatedUser) else it
            }
            samplePosts.clear()
            samplePosts.addAll(updatedPosts)
        }
        toggleEditMode()
    }
    
    fun onShowImagePicker() {
        _showImagePicker.value = true
    }
    
    fun onDismissImagePicker() {
        _showImagePicker.value = false
    }

    fun onAvatarChange(uri: Uri?) {
        if (uri == null) return
        val newAvatarUrl = uri.toString()
        _userFlow.value = _userFlow.value?.copy(avatarUrl = newAvatarUrl)
        onDismissImagePicker()
    }
}

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Error(val message: String) : ProfileUiState
    data class Success(
        val user: User,
        val isCurrentUser: Boolean,
        val followers: Int,
        val following: Int,
        val posts: List<Post>,
        val favoriteCoffees: List<CoffeeWithDetails>,
        val isEditing: Boolean,
        val emailError: String?,
        val showImageSourceDialog: Boolean
    ) : ProfileUiState
}
