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
    private val socialRepository: SocialRepository,
    private val userDao: UserDao
) : ViewModel() {

    private val requestedUserId: Int = savedStateHandle["userId"] ?: 0
    private val _isEditing = MutableStateFlow(false)
    private val _emailError = MutableStateFlow<String?>(null)
    private val _usernameError = MutableStateFlow<String?>(null)
    private val _temporaryAvatarUri = MutableStateFlow<Uri?>(null)

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<ProfileUiState> = combine(
        userRepository.getActiveUserFlow(), 
        socialRepository.getAllPostsWithDetails(),
        coffeeRepository.allCoffees,
        coffeeRepository.allReviews,
        userRepository.followingMap,
        coffeeRepository.favorites, 
        _isEditing,
        _emailError,
        _usernameError,
        _temporaryAvatarUri
    ) { args: Array<Any?> ->
        val activeUser = args[0] as? UserEntity
        val allPosts = args[1] as List<PostWithDetails>
        val allCoffees = args[2] as List<CoffeeWithDetails>
        val allReviews = args[3] as List<ReviewEntity>
        val followingMap = args[4] as Map<Int, Set<Int>>
        val myFavorites = args[5] as List<LocalFavorite>
        val isEditing = args[6] as Boolean
        val emailError = args[7] as? String
        val usernameError = args[8] as? String
        val tempAvatar = args[9] as? Uri

        val targetUserId = if (requestedUserId == 0) activeUser?.id ?: 0 else requestedUserId
        val targetUser = if (targetUserId == activeUser?.id) activeUser 
                         else userRepository.getUserById(targetUserId)

        if (targetUser == null) return@combine ProfileUiState.Error("Usuario no encontrado")

        val displayUser = if (isEditing && tempAvatar != null) {
            targetUser.copy(avatarUrl = tempAvatar.toString())
        } else targetUser

        ProfileUiState.Success(
            user = displayUser,
            posts = allPosts.filter { it.post.userId == targetUserId },
            favoriteCoffees = if (targetUserId == activeUser?.id) {
                val favIds = myFavorites.map { it.coffeeId }.toSet()
                allCoffees.filter { favIds.contains(it.coffee.id) }
            } else emptyList(),
            userReviews = allReviews.filter { it.userId == targetUserId }.mapNotNull { review ->
                allCoffees.find { it.coffee.id == review.coffeeId }?.let { coffee ->
                    UserReviewInfo(coffee, review, targetUser.fullName, targetUser.avatarUrl)
                }
            },
            followers = followingMap.values.count { it.contains(targetUserId) },
            following = followingMap[targetUserId]?.size ?: 0,
            isFollowing = activeUser?.let { followingMap[it.id]?.contains(targetUserId) } ?: false,
            isCurrentUser = targetUserId == activeUser?.id,
            isEditing = isEditing,
            emailError = emailError,
            usernameError = usernameError,
            myFavoriteIds = myFavorites.map { it.coffeeId }.toSet()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState.Loading)

    fun toggleEditMode() { 
        _isEditing.value = !_isEditing.value 
        if (!_isEditing.value) {
            _temporaryAvatarUri.value = null
            _usernameError.value = null
        }
    }

    fun onAvatarChange(uri: Uri?) { _temporaryAvatarUri.value = uri }

    fun onSaveProfile(username: String, fullName: String, bio: String, email: String) {
        viewModelScope.launch {
            val activeUser = userRepository.getActiveUser() ?: return@launch
            
            // 1. Validar espacios
            if (username.contains(" ")) {
                _usernameError.value = "El nombre de usuario no puede tener espacios"
                return@launch
            }

            // 2. Validar unicidad (si ha cambiado el nombre)
            if (username != activeUser.username) {
                val existing = userDao.getUserByUsername(username.trim())
                if (existing != null) {
                    _usernameError.value = "Este nombre de usuario ya existe"
                    return@launch
                }
            }

            _usernameError.value = null
            val updatedUser = activeUser.copy(
                username = username.trim(),
                fullName = fullName,
                avatarUrl = _temporaryAvatarUri.value?.toString() ?: activeUser.avatarUrl,
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
        viewModelScope.launch { coffeeRepository.toggleFavorite(coffeeId, isFavorite) }
    }

    fun onToggleLike(postId: String) {
        viewModelScope.launch {
            val me = userRepository.getActiveUser() ?: return@launch
            socialRepository.toggleLike(postId, me.id)
        }
    }

    fun onAddComment(postId: String, text: String) {
        viewModelScope.launch {
            val me = userRepository.getActiveUser() ?: return@launch
            socialRepository.addComment(CommentEntity(
                postId = postId,
                userId = me.id,
                text = text,
                timestamp = System.currentTimeMillis()
            ))
        }
    }
}

data class UserReviewInfo(val coffeeDetails: CoffeeWithDetails, val review: ReviewEntity, val authorName: String?, val authorAvatarUrl: String?)

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
        val usernameError: String?,
        val myFavoriteIds: Set<String>
    ) : ProfileUiState
}
