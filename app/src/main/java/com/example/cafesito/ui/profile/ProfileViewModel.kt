package com.example.cafesito.ui.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.*
import com.example.cafesito.ui.utils.ConnectivityObserver
import com.example.cafesito.health.HealthConnectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.exceptions.HttpRequestException
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
        val activeUser: UserEntity?,
        val sensoryProfile: Map<String, Float>,
        val healthConnectEnabled: Boolean
    ) : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val coffeeRepository: CoffeeRepository,
    private val socialRepository: SocialRepository,
    private val diaryRepository: DiaryRepository,
    private val connectivityObserver: ConnectivityObserver,
    val healthConnectRepository: HealthConnectRepository
) : ViewModel() {

    private val requestedUserId: Int = savedStateHandle["userId"] ?: 0
    private val _isEditing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _usernameError = MutableStateFlow<String?>(null)
    private val _temporaryAvatarUri = MutableStateFlow<Uri?>(null)
    private val _healthConnectEnabled = MutableStateFlow(healthConnectRepository.isEnabled())

    private val activeUserFlow = userRepository.getActiveUserFlow()
    
    // ✅ OPTIMIZACIÓN: Usar getUserByIdFlow para evitar cargar todos los usuarios
    private val targetUserFlow = if (requestedUserId == 0) {
        activeUserFlow
    } else {
        userRepository.getUserByIdFlow(requestedUserId)
    }.distinctUntilChanged()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val userPostsFlow = targetUserFlow.flatMapLatest { user ->
        if (user != null) {
            socialRepository.getPostsByUserId(user.id)
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
        _temporaryAvatarUri,
        connectivityObserver.observe(),
        _healthConnectEnabled
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
        val connectivityStatus = args[11] as ConnectivityObserver.Status
        val healthConnectEnabled = args[12] as Boolean

        if (connectivityStatus != ConnectivityObserver.Status.Available) {
            return@combine ProfileUiState.Error("No hay conexión a Internet. Por favor, revisa tu conexión.")
        }

        if (generalError != null) {
            return@combine ProfileUiState.Error(generalError)
        }
        if (targetUser == null) return@combine ProfileUiState.Loading

        val displayUser = if (isEditing && tempAvatar != null) {
            targetUser.copy(avatarUrl = tempAvatar.toString())
        } else targetUser

        val userReviewsForSensory = allReviews.filter { it.userId == targetUser.id }
        val reviewedCoffees = userReviewsForSensory.mapNotNull { review ->
            allCoffees.find { it.coffee.id == review.coffeeId }?.coffee
        }
        
        val userFavs = if (targetUser.id == (activeUser?.id ?: -1)) {
            val favIds = myFavorites.map { it.coffeeId }.toSet()
            allCoffees.filter { favIds.contains(it.coffee.id) }.map { it.coffee }
        } else emptyList()

        val allRelevantCoffees = (reviewedCoffees + userFavs).distinctBy { it.id }

        val sensoryProfile = if (allRelevantCoffees.isEmpty()) {
            mapOf("Aroma" to 5f, "Sabor" to 5f, "Cuerpo" to 5f, "Acidez" to 5f, "Dulzura" to 5f)
        } else {
            mapOf(
                "Aroma" to allRelevantCoffees.map { it.aroma }.average().toFloat(),
                "Sabor" to allRelevantCoffees.map { it.sabor }.average().toFloat(),
                "Cuerpo" to allRelevantCoffees.map { it.cuerpo }.average().toFloat(),
                "Acidez" to allRelevantCoffees.map { it.acidez }.average().toFloat(),
                "Dulzura" to allRelevantCoffees.map { it.dulzura }.average().toFloat()
            )
        }

        ProfileUiState.Success(
            user = displayUser,
            posts = userPosts, 
            favoriteCoffees = if (targetUser.id == (activeUser?.id ?: -1)) {
                val favIds = myFavorites.map { it.coffeeId }.toSet()
                allCoffees.filter { favIds.contains(it.coffee.id) }
            } else emptyList(),
            userReviews = userReviewsForSensory.mapNotNull { review ->
                allCoffees.find { it.coffee.id == review.coffeeId }?.let { coffee ->
                    UserReviewInfo(coffee, review, targetUser.fullName, targetUser.avatarUrl)
                }
            },
            followers = followingMap.values.count { it.contains(targetUser.id) },
            following = followingMap[targetUser.id]?.size ?: 0,
            isFollowing = activeUser?.let { followingMap[it.id]?.contains(targetUser.id) } ?: false,
            isCurrentUser = targetUser.id == (activeUser?.id ?: -1),
            isEditing = isEditing,
            errorMessage = generalError,
            usernameError = usernameError,
            myFavoriteIds = myFavorites.map { it.coffeeId }.toSet(),
            activeUser = activeUser,
            sensoryProfile = sensoryProfile,
            healthConnectEnabled = healthConnectEnabled
        )
    }
    .catch { e -> 
        Log.e("ProfileViewModel", "Error in UI state flow", e)
        emit(ProfileUiState.Error("Se ha producido un error inesperado."))
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState.Loading)

    init {
        // ✅ ELIMINADO: El refresco automático al inicio que duplicaba peticiones
        // El refresco ahora se gestiona solo cuando hay cambios de conectividad críticos
        // o manualmente por el usuario.
        viewModelScope.launch {
            connectivityObserver.observe().drop(1).collect { // drop(1) evita el primer evento al suscribirse
                if (it == ConnectivityObserver.Status.Available) {
                    refreshData()
                }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _error.value = null 
                userRepository.triggerRefresh()
                socialRepository.triggerRefresh()
                diaryRepository.syncExternalHealthData()
            } catch (e: HttpRequestException) {
                Log.e("ProfileViewModel", "Error de red al sincronizar", e)
                _error.value = "No hay conexión a Internet. Por favor, revisa tu conexión."
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error sincronizando datos del perfil", e)
                _error.value = "Ha ocurrido un error inesperado al cargar el perfil."
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

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
        }
    }

    fun toggleFollow() {
        viewModelScope.launch(Dispatchers.IO) {
            val me = userRepository.getActiveUser() ?: return@launch
            val targetId = if (requestedUserId == 0) me.id else requestedUserId
            userRepository.toggleFollow(me.id, targetId = targetId)
        }
    }

    fun onToggleFavorite(coffeeId: String, isFavorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { 
            try {
                coffeeRepository.toggleFavorite(coffeeId, isFavorite)
            } catch (e: Exception) {
                _error.value = "Error al actualizar favorito"
            }
        }
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

    fun onToggleHealthConnect(enabled: Boolean) {
        viewModelScope.launch {
            healthConnectRepository.setEnabled(enabled)
            _healthConnectEnabled.value = enabled
        }
    }
}
