package com.example.cafesito.ui.profile

import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.CoffeeRepository
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.domain.Comment
import com.example.cafesito.domain.Post
import com.example.cafesito.domain.Review
import com.example.cafesito.domain.User
import com.example.cafesito.domain.allUsers
import com.example.cafesito.domain.currentUser
import com.example.cafesito.domain.samplePosts
import com.example.cafesito.domain.sampleReviews
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val _activeCommentPost = MutableStateFlow<Post?>(null)
    private val _postToDelete = MutableStateFlow<Post?>(null)
    private val _postToEdit = MutableStateFlow<Post?>(null)
    private val _refreshTrigger = MutableStateFlow(0)

    private data class SubState(
        val isEditing: Boolean, 
        val emailError: String?, 
        val showImagePicker: Boolean,
        val activeCommentPost: Post?,
        val postToDelete: Post?,
        val postToEdit: Post?,
        val refresh: Int
    )
    
    private val _uiSubState = combine(
        _isEditing, _emailError, _showImagePicker, _activeCommentPost, _postToDelete, _postToEdit, _refreshTrigger
    ) { params ->
        SubState(params[0] as Boolean, params[1] as String?, params[2] as Boolean, 
                 params[3] as Post?, params[4] as Post?, params[5] as Post?, params[6] as Int)
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        _userFlow, coffeeRepository.allCoffees, coffeeRepository.favorites, _uiSubState
    ) { user, allCoffees, localFavorites, subState ->
        if (user == null) {
            ProfileUiState.Error("Usuario no encontrado")
        } else {
            val isMe = (userId == currentUser.id)
            val userPosts = samplePosts.filter { it.user.id == userId }
            
            val favoriteCoffees = if (isMe) {
                allCoffees.filter { details -> 
                    localFavorites.any { it.coffeeId == details.coffee.id }
                }
            } else {
                allCoffees.filter { details ->
                    user.favoriteCoffeeIds.contains(details.coffee.id)
                }
            }
            
            val myFavoriteIds = localFavorites.map { it.coffeeId }.toSet()

            // JOIN LOGIC: Map user's reviews to coffee info
            val userReviews = sampleReviews.value.filter { it.user.id == userId }.mapNotNull { review ->
                allCoffees.find { it.coffee.id == review.coffeeId }?.let { coffeeDetails ->
                    UserReviewInfo(coffeeDetails, review)
                }
            }

            ProfileUiState.Success(
                user = user,
                isCurrentUser = isMe,
                followers = 250,
                following = 120,
                posts = userPosts,
                favoriteCoffees = favoriteCoffees,
                userReviews = userReviews,
                myFavoriteIds = myFavoriteIds,
                isEditing = subState.isEditing,
                emailError = subState.emailError,
                showImageSourceDialog = subState.showImagePicker,
                activeCommentPost = subState.activeCommentPost,
                postToDelete = subState.postToDelete,
                postToEdit = subState.postToEdit
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState.Loading
    )

    fun toggleEditMode() {
        _isEditing.value = !_isEditing.value
        _emailError.value = null
    }

    fun onSaveProfile(newAvatarUrl: String, newBio: String, newEmail: String) {
        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            _emailError.value = "El formato del email no es válido"
            return
        }
        _emailError.value = null
        val updatedUser = _userFlow.value?.copy(avatarUrl = newAvatarUrl, bio = newBio, email = newEmail)
        _userFlow.value = updatedUser
        val userIndex = allUsers.indexOfFirst { it.id == userId }
        if (userIndex != -1 && updatedUser != null) {
            allUsers[userIndex] = updatedUser
            val updatedPosts = samplePosts.map {
                if (it.user.id == userId) it.copy(user = updatedUser) else it
            }
            samplePosts.clear()
            samplePosts.addAll(updatedPosts)
        }
        _refreshTrigger.value++
        toggleEditMode()
    }

    fun onShowImagePicker() = run { _showImagePicker.value = true }
    fun onDismissImagePicker() = run { _showImagePicker.value = false }
    fun onAvatarChange(uri: Uri?) {
        uri?.let { _userFlow.value = _userFlow.value?.copy(avatarUrl = it.toString()) }
        onDismissImagePicker()
    }

    fun onCommentClick(post: Post) { _activeCommentPost.value = post }
    fun onDismissComments() { _activeCommentPost.value = null }
    fun onAddComment(post: Post, text: String) {
        val postIndex = samplePosts.indexOfFirst { it.id == post.id }
        if (postIndex != -1) {
            val updatedComments = post.comments.toMutableList().apply { add(Comment(currentUser, text)) }
            val updatedPost = post.copy(comments = updatedComments)
            samplePosts[postIndex] = updatedPost
            _activeCommentPost.value = updatedPost
            _refreshTrigger.value++
        }
    }

    fun requestDeletePost(post: Post) { _postToDelete.value = post }
    fun dismissDeleteDialog() { _postToDelete.value = null }
    fun confirmDeletePost() {
        _postToDelete.value?.let { post ->
            samplePosts.removeIf { it.id == post.id }
            _refreshTrigger.value++
        }
        dismissDeleteDialog()
    }

    fun requestEditPost(post: Post) { _postToEdit.value = post }
    fun dismissEditPost() { _postToEdit.value = null }
    fun savePostEdit(post: Post, newComment: String) {
        val index = samplePosts.indexOfFirst { it.id == post.id }
        if (index != -1) {
            samplePosts[index] = post.copy(comment = newComment)
            _refreshTrigger.value++
        }
        dismissEditPost()
    }

    fun onToggleFavorite(coffeeId: Int, isCurrentlyFavorite: Boolean) {
        viewModelScope.launch {
            coffeeRepository.toggleFavorite(coffeeId, isCurrentlyFavorite)
            _refreshTrigger.value++
        }
    }
}

data class UserReviewInfo(val coffeeDetails: CoffeeWithDetails, val review: Review)

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
        val userReviews: List<UserReviewInfo>,
        val myFavoriteIds: Set<Int>,
        val isEditing: Boolean,
        val emailError: String?,
        val showImageSourceDialog: Boolean,
        val activeCommentPost: Post?,
        val postToDelete: Post?,
        val postToEdit: Post?
    ) : ProfileUiState
}
