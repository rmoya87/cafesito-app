package com.cafesito.app.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.CommentEntity
import com.cafesito.app.data.CommentWithAuthor
import com.cafesito.app.data.SocialRepository
import com.cafesito.app.data.UserEntity
import com.cafesito.app.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _mentionQuery = MutableStateFlow("")
    
    private val _currentPostId = MutableStateFlow<String?>(null)
    
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val comments: StateFlow<List<CommentWithAuthor>> = _currentPostId
        .filterNotNull()
        .flatMapLatest { socialRepository.getCommentsForPost(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeUser = userRepository.getActiveUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val mentionSuggestions: StateFlow<List<UserEntity>> = _mentionQuery
        .debounce(200)
        .flatMapLatest { query ->
            if (query.isEmpty()) flowOf(emptyList())
            else userRepository.getAllUsersFlow().map { users ->
                users.filter { it.username.contains(query, ignoreCase = true) }.take(5)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setPostId(postId: String) {
        _currentPostId.value = postId
    }

    fun onTextChanged(text: String) {
        val lastWord = text.split(" ").lastOrNull() ?: ""
        if (lastWord.startsWith("@") && lastWord.length > 1) {
            _mentionQuery.value = lastWord.removePrefix("@")
        } else {
            _mentionQuery.value = ""
        }
    }

    fun addComment(postId: String, text: String) {
        viewModelScope.launch {
            val user = userRepository.getActiveUser() ?: return@launch
            socialRepository.addComment(CommentEntity(
                postId = postId,
                userId = user.id,
                text = text,
                timestamp = System.currentTimeMillis()
            ))
        }
    }

    fun deleteComment(commentId: Int) {
        viewModelScope.launch {
            socialRepository.deleteComment(commentId)
        }
    }

    fun updateComment(commentId: Int, newText: String) {
        viewModelScope.launch {
            socialRepository.updateComment(commentId, newText)
        }
    }

    suspend fun getUserIdByUsername(username: String): Int? {
        val cleanName = username.removePrefix("@").filter { it.isLetterOrDigit() || it == '_' }
        return userRepository.getUserByUsername(cleanName)?.id
    }
}
