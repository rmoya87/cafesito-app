package com.example.cafesito.ui.profile

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val coffeeRepository: CoffeeRepository,
    private val socialRepository: SocialRepository
) : ViewModel() {

    // Si el userId es 0, significa que queremos cargar el perfil del usuario logueado actualmente
    private val requestedUserId: Int = savedStateHandle["userId"] ?: 0
    private val _isEditing = MutableStateFlow(false)
    private val _emailError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProfileUiState> = combine(
        userRepository.getActiveUserFlow(), // Quién soy yo
        socialRepository.getAllPostsWithDetails(),
        coffeeRepository.allCoffees,
        coffeeRepository.allReviews,
        userRepository.followingMap,
        coffeeRepository.favorites, // Mis favoritos reales de Room
        _isEditing,
        _emailError
    ) { args: Array<Any?> ->
        val activeUser = args[0] as? UserEntity
        val allPosts = args[1] as List<PostWithDetails>
        val allCoffees = args[2] as List<CoffeeWithDetails>
        val allReviews = args[3] as List<ReviewEntity>
        val followingMap = args[4] as Map<Int, Set<Int>>
        val myFavorites = args[5] as List<LocalFavorite>
        val isEditing = args[6] as Boolean
        val emailError = args[7] as? String

        // Determinamos qué perfil mostrar
        val targetUserId = if (requestedUserId == 0) activeUser?.id ?: 0 else requestedUserId
        val targetUser = if (targetUserId == activeUser?.id) activeUser 
                         else userRepository.getUserById(targetUserId)

        if (targetUser == null) return@combine ProfileUiState.Error("Usuario no encontrado")

        val userPosts = allPosts.filter { it.post.userId == targetUserId }
        
        // Obtenemos detalles de cafés favoritos reales para este perfil
        val favoriteCoffees = if (targetUserId == activeUser?.id) {
            val favIds = myFavorites.map { it.coffeeId }.toSet()
            allCoffees.filter { favIds.contains(it.coffee.id) }
        } else emptyList()

        val userReviews = allReviews.filter { it.userId == targetUserId }.mapNotNull { review ->
            allCoffees.find { it.coffee.id == review.coffeeId }?.let { coffee ->
                UserReviewInfo(coffee, review, targetUser.fullName)
            }
        }

        ProfileUiState.Success(
            user = targetUser,
            posts = userPosts,
            favoriteCoffees = favoriteCoffees,
            userReviews = userReviews,
            followers = followingMap.values.count { it.contains(targetUserId) },
            following = followingMap[targetUserId]?.size ?: 0,
            isFollowing = activeUser?.let { followingMap[it.id]?.contains(targetUserId) } ?: false,
            isCurrentUser = targetUserId == activeUser?.id,
            isEditing = isEditing,
            emailError = emailError,
            myFavoriteIds = myFavorites.map { it.coffeeId }.toSet()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState.Loading)

    fun toggleEditMode() {
        _isEditing.value = !_isEditing.value
    }

    fun onSaveProfile(avatarUrl: String, bio: String, email: String) {
        viewModelScope.launch {
            val activeUser = userRepository.getActiveUser() ?: return@launch
            val updatedUser = activeUser.copy(
                avatarUrl = avatarUrl,
                bio = bio,
                email = email
            )
            userRepository.upsertUser(updatedUser)
            _isEditing.value = false
        }
    }

    fun toggleFollow() {
        viewModelScope.launch {
            val me = userRepository.getActiveUser() ?: return@launch
            userRepository.toggleFollow(me.id, targetId = requestedUserId)
        }
    }

    fun onToggleFavorite(coffeeId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            coffeeRepository.toggleFavorite(coffeeId, isFavorite)
        }
    }

    fun onAvatarChange(uri: Uri?) {
        // En una implementación real, aquí subiríamos la imagen a un servidor
    }
}

data class UserReviewInfo(
    val coffeeDetails: CoffeeWithDetails, 
    val review: ReviewEntity, 
    val authorName: String?
)

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Error(val message: String) : ProfileUiState
    data class Success(
        val user: UserEntity,
        val posts: List<PostWithDetails>,
        val favoriteCoffees: List<CoffeeWithDetails>,
        val userReviews: List<UserReviewInfo>,
        val followers: Int,
        val following: Int,
        val isFollowing: Boolean,
        val isCurrentUser: Boolean,
        val isEditing: Boolean,
        val emailError: String?,
        val myFavoriteIds: Set<String>
    ) : ProfileUiState
}
