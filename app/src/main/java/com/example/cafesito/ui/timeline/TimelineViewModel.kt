package com.example.cafesito.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafesito.data.*
import com.example.cafesito.ui.profile.UserReviewInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val coffeeRepository: CoffeeRepository,
    private val socialRepository: SocialRepository
) : ViewModel() {

    private val _refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<TimelineUiState> = combine(
        userRepository.getActiveUserFlow(), 
        socialRepository.getAllPostsWithDetails(),
        socialRepository.getAllReviewsWithAuthor(),
        coffeeRepository.allCoffees,
        userRepository.followingMap,
        _refreshTrigger
    ) { args: Array<Any?> ->
        // CASTING EXPLÍCITO: Necesario cuando combine tiene más de 5 flujos
        val activeUser = args[0] as? UserEntity
        val posts = args[1] as List<PostWithDetails>
        val reviews = args[2] as List<ReviewWithAuthor>
        val allCoffees = args[3] as List<CoffeeWithDetails>
        val followingMap = args[4] as Map<Int, Set<Int>>
        
        if (activeUser == null) return@combine TimelineUiState.Loading

        val myFollowing = followingMap[activeUser.id] ?: emptySet()
        val visibleUserIds = myFollowing + activeUser.id

        // 1. Filtrar y mapear Publicaciones reales de Room
        val postItems = posts
            .filter { visibleUserIds.contains(it.post.userId) }
            .map { TimelineItem.PostItem(it) }

        // 2. Filtrar y mapear Opiniones reales unidas con café y autor
        val reviewItems = reviews
            .filter { visibleUserIds.contains(it.review.userId) }
            .mapNotNull { reviewWithAuthor ->
                val coffeeDetails = allCoffees.find { it.coffee.id == reviewWithAuthor.review.coffeeId }
                coffeeDetails?.let {
                    TimelineItem.ReviewItem(
                        UserReviewInfo(
                            coffeeDetails = it,
                            review = reviewWithAuthor.review,
                            authorName = reviewWithAuthor.author.fullName
                        )
                    )
                }
            }

        val combinedItems = (postItems + reviewItems).sortedByDescending { it.timestamp }

        TimelineUiState.Success(
            items = combinedItems,
            suggestedUsers = emptyList(), // Las sugerencias ahora vendrán de Room
            myFollowingIds = myFollowing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimelineUiState.Loading)

    fun toggleFollowSuggestion(userId: Int) {
        viewModelScope.launch {
            val me = userRepository.getActiveUser() ?: return@launch
            userRepository.toggleFollow(me.id, userId)
        }
    }

    fun onAddComment(postId: String, text: String) {
        viewModelScope.launch {
            val user = userRepository.getActiveUser() ?: return@launch
            socialRepository.addComment(CommentEntity(postId = postId, userId = user.id, text = text, timestamp = System.currentTimeMillis()))
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            val user = userRepository.getActiveUser() ?: return@launch
            socialRepository.toggleLike(postId, user.id)
        }
    }
}

// Clase de apoyo para la UI integrada con el dominio
data class SuggestedUserInfo(
    val user: UserEntity,
    val followersCount: Int,
    val followingCount: Int
)

sealed class TimelineItem {
    abstract val timestamp: Long
    data class PostItem(val details: PostWithDetails) : TimelineItem() {
        override val timestamp: Long = details.post.timestamp
    }
    data class ReviewItem(val reviewInfo: UserReviewInfo) : TimelineItem() {
        override val timestamp: Long = reviewInfo.review.timestamp
    }
    data class FavoriteActionItem(val coffeeDetails: CoffeeWithDetails, override val timestamp: Long) : TimelineItem()
}

sealed interface TimelineUiState {
    data object Loading : TimelineUiState
    data class Success(
        val items: List<TimelineItem>,
        val suggestedUsers: List<com.example.cafesito.domain.SuggestedUserInfo>,
        val myFollowingIds: Set<Int>
    ) : TimelineUiState
}
