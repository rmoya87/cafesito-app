package com.example.cafesito.ui.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

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
        val errorMessage: String?,
        val usernameError: String?,
        val myFavoriteIds: Set<String>,
        val activeUser: UserEntity?
    ) : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val coffeeRepository: CoffeeRepository,
    private val socialRepository: SocialRepository,
    private val socialDao: SocialDao,
    private val userDao: UserDao
) : ViewModel() {

    private val requestedUserId: Int = savedStateHandle["userId"] ?: 0
    private val _isEditing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _usernameError = MutableStateFlow<String?>(null)
    private val _temporaryAvatarUri = MutableStateFlow<Uri?>(null)

    private val activeUserFlow = userRepository.getActiveUserFlow()
    
    // Flujo del usuario objetivo
    private val targetUserFlow = if (requestedUserId == 0) {
        activeUserFlow
    } else {
        userRepository.getAllUsersFlow().map { users ->
            users.find { it.id == requestedUserId }
        }.distinctUntilChanged()
    }

    // Flujo de posts filtrado por la BASE DE DATOS
    private val userPostsFlow = targetUserFlow.flatMapLatest { user ->
        if (user != null) {
            // Escuchamos los posts de la DB local que son sincronizados desde Supabase
            socialDao.getPostsByUserIdWithDetails(user.id)
        } else flowOf(emptyList())
    }

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<ProfileUiState> = combine(
        activeUserFlow,
        targetUserFlow,
        userPostsFlow,
        coffeeRepository.allCoffees,
        coffeeRepository.allReviews,
        userRepository.followingMap,
        coffeeRepository.favorites,
        _isEditing,
        _error,
        _usernameError,
        _temporaryAvatarUri
    ) { args ->
        val activeUser = args[0] as? UserEntity
        val targetUser = args[1] as? UserEntity
        val userPosts = args[2] as List<PostWithDetails>
        val allCoffees = args[3] as List<CoffeeWithDetails>
        val allReviews = args[4] as List<ReviewEntity>
        val followingMap = args[5] as Map<Int, Set<Int>>
        val myFavorites = args[6] as List<LocalFavorite>
        val isEditing = args[7] as Boolean
        val generalError = args[8] as? String
        val usernameError = args[9] as? String
        val tempAvatar = args[10] as? Uri

        if (targetUser == null) return@combine ProfileUiState.Loading

        val displayUser = if (isEditing && tempAvatar != null) {
            targetUser.copy(avatarUrl = tempAvatar.toString())
        } else targetUser

        ProfileUiState.Success(
            user = displayUser,
            posts = userPosts, 
            favoriteCoffees = if (targetUser.id == activeUser?.id) {
                val favIds = myFavorites.map { it.coffeeId }.toSet()
                allCoffees.filter { favIds.contains(it.coffee.id) }
            } else emptyList(),
            userReviews = allReviews.filter { it.userId == targetUser.id }.mapNotNull { review ->
                allCoffees.find { it.coffee.id == review.coffeeId }?.let { coffee ->
                    UserReviewInfo(coffee, review, targetUser.fullName, targetUser.avatarUrl)
                }
            },
            followers = followingMap.values.count { it.contains(targetUser.id) },
            following = followingMap[targetUser.id]?.size ?: 0,
            isFollowing = activeUser?.let { followingMap[it.id]?.contains(targetUser.id) } ?: false,
            isCurrentUser = targetUser.id == activeUser?.id,
            isEditing = isEditing,
            errorMessage = generalError,
            usernameError = usernameError,
            myFavoriteIds = myFavorites.map { it.coffeeId }.toSet(),
            activeUser = activeUser
        )
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState.Loading)

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Sincronizamos usuarios primero para tener las referencias
                userRepository.syncUsers()
                // Forzamos la sincronización de posts desde Supabase a Room
                socialRepository.syncSocialData()
                
                if (requestedUserId != 0) {
                    userRepository.getUserById(requestedUserId)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error sincronizando datos del perfil", e)
            }
        }
    }

    fun toggleEditMode() { _isEditing.value = !_isEditing.value }
    fun onAvatarChange(uri: Uri?) { _temporaryAvatarUri.value = uri }

    fun onSaveProfile(username: String, fullName: String, bio: String, email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val activeUser = userRepository.getActiveUser() ?: return@launch
                val updatedUser = activeUser.copy(
                    username = username.trim(),
                    fullName = fullName,
                    avatarUrl = _temporaryAvatarUri.value?.toString() ?: activeUser.avatarUrl,
                    bio = bio,
                    email = email
                )
                userRepository.upsertUser(updatedUser)
                _isEditing.value = false
            } catch (e: Exception) {
                _error.value = "Error al guardar"
            }
        }
    }

    fun toggleFollow() {
        viewModelScope.launch(Dispatchers.IO) {
            val me = userRepository.getActiveUser() ?: return@launch
            userRepository.toggleFollow(me.id, targetId = requestedUserId)
        }
    }

    fun onToggleFavorite(coffeeId: String, isFavorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { coffeeRepository.toggleFavorite(coffeeId, isFavorite) }
    }

    fun onToggleLike(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val me = userRepository.getActiveUser() ?: return@launch
            socialRepository.toggleLike(postId, me.id)
        }
    }

    fun onAddComment(postId: String, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val me = userRepository.getActiveUser() ?: return@launch
            socialRepository.addComment(CommentEntity(postId = postId, userId = me.id, text = text, timestamp = System.currentTimeMillis()))
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch(Dispatchers.IO) { socialRepository.deletePost(postId) }
    }

    fun updatePost(postId: String, newText: String, newImageUrl: String) {
        viewModelScope.launch(Dispatchers.IO) { socialRepository.updatePost(postId, newText, newImageUrl) }
    }

    fun deleteReview(coffeeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val me = userRepository.getActiveUser() ?: return@launch
            coffeeRepository.deleteReview(coffeeId, me.id)
        }
    }

    fun updateReview(coffeeId: String, rating: Float, comment: String, imageUrl: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val me = userRepository.getActiveUser() ?: return@launch
            coffeeRepository.upsertReview(ReviewEntity(coffeeId = coffeeId, userId = me.id, rating = rating, comment = comment, imageUrl = imageUrl, timestamp = System.currentTimeMillis()))
        }
    }
}
