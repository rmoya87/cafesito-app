package com.cafesito.app.ui.timeline

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.*
import com.cafesito.shared.domain.Review
import com.cafesito.shared.domain.SuggestedUserInfo
import com.cafesito.shared.domain.User
import com.cafesito.shared.domain.repository.ReviewRepository
import com.cafesito.shared.domain.validation.ValidateReviewInputUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val coffeeRepository: CoffeeRepository,
    private val socialRepository: SocialRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {
    private val validateReviewInput = ValidateReviewInputUseCase()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        Log.d("TimelineVM", "Initializing TimelineViewModel")
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            if (_isRefreshing.value) return@launch
            _isRefreshing.value = true
            Log.d("TimelineVM", "Triggering global refresh...")
            try {
                userRepository.syncUsers()
                socialRepository.syncSocialData()
                Log.d("TimelineVM", "Refresh trigger sent successfully")
            } catch (e: Exception) {
                Log.e("TimelineVM", "Error in refreshData", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // ✅ Mejorado: staticData con logs y mayor tolerancia
    private val staticData = combine(
        userRepository.getActiveUserFlow().onEach { Log.d("TimelineVM", "ActiveUser emitted: ${it?.username}") },
        coffeeRepository.allCoffees.onStart { emit(emptyList()) },
        userRepository.getAllUsersFlow().onStart { emit(emptyList()) },
        coffeeRepository.getRecommendations().onStart { emit(emptyList()) }
    ) { me, coffees, users, reco -> 
        TimelineStaticData(me, coffees, users, reco)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ✅ Mejorado: dynamicData con logs
    private val dynamicData = combine(
        socialRepository.getAllPostsWithDetails().onEach { Log.d("TimelineVM", "Posts emitted: ${it.size}") },
        socialRepository.getAllReviewsWithAuthor().onEach { Log.d("TimelineVM", "Reviews emitted: ${it.size}") },
        userRepository.followingMap.onEach { Log.d("TimelineVM", "FollowingMap emitted: ${it.size} entries") }
    ) { posts, reviews, following -> 
        TimelineDynamicData(posts, reviews, following)
    }

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<TimelineUiState> = combine(
        staticData,
        dynamicData
    ) { static, dynamic ->
        Log.d("TimelineVM", "Combining UI State... staticUserPresent=${static?.activeUser != null}")
        
        if (static?.activeUser == null) {
            return@combine TimelineUiState.Loading
        }

        val activeUser = static.activeUser
        val posts = dynamic.posts
        val reviews = dynamic.reviews
        val myFollowing = dynamic.following[activeUser.id] ?: emptySet()
        
        // FILTRO: Solo yo y a los que sigo
        val visibleUserIds = myFollowing + activeUser.id

        val postItems = posts
            .filter { visibleUserIds.contains(it.post.userId) }
            .map { TimelineItem.PostItem(it) }

        val reviewItems = reviews
            .filter { visibleUserIds.contains(it.review.userId) }
            .mapNotNull { reviewWithAuthor ->
                val coffeeDetails = static.allCoffees.find { it.coffee.id == reviewWithAuthor.review.coffeeId }
                coffeeDetails?.let {
                    TimelineItem.ReviewItem(
                        UserReviewInfo(
                            coffeeDetails = it,
                            review = reviewWithAuthor.review,
                            authorName = reviewWithAuthor.author.fullName,
                            authorAvatarUrl = reviewWithAuthor.author.avatarUrl
                        )
                    )
                }
            }

        val combinedItems = (postItems + reviewItems).sortedByDescending { it.timestamp }
        Log.d("TimelineVM", "Final Items Count: ${combinedItems.size}")

        val suggestedUsers = static.allUsers
            .filter { it.id != activeUser.id && !myFollowing.contains(it.id) }
            .map { entity ->
                SuggestedUserInfo(
                    user = User(
                        id = entity.id,
                        username = entity.username,
                        fullName = entity.fullName,
                        avatarUrl = entity.avatarUrl,
                        email = entity.email,
                        bio = entity.bio
                    ),
                    followersCount = dynamic.following.values.count { it.contains(entity.id) },
                    followingCount = dynamic.following[entity.id]?.size ?: 0
                )
            }
            .take(10)

        TimelineUiState.Success(
            items = combinedItems,
            suggestedUsers = suggestedUsers,
            myFollowingIds = myFollowing,
            activeUser = activeUser,
            recommendations = static.recommendations
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimelineUiState.Loading)

    // ... Resto de funciones (toggleLike, deletePost, etc) permanecen igual
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

    fun deletePost(postId: String) {
        viewModelScope.launch { socialRepository.deletePost(postId) }
    }

    fun updatePost(postId: String, newText: String, newImageUrl: String) {
        viewModelScope.launch { socialRepository.updatePost(postId, newText, newImageUrl) }
    }

    fun deleteReview(coffeeId: String) {
        viewModelScope.launch {
            val user = userRepository.getActiveUser() ?: return@launch
            coffeeRepository.deleteReview(coffeeId, user.id)
        }
    }

    fun updateReview(coffeeId: String, rating: Float, comment: String, imageUrl: String?) {
        viewModelScope.launch {
            val validation = validateReviewInput(rating, comment)
            if (validation.isFailure) return@launch
            val user = userRepository.getActiveUser() ?: return@launch
            val result = reviewRepository.updateReview(
                Review(
                    user = user.toDomainUser(),
                    coffeeId = coffeeId,
                    rating = rating,
                    comment = comment,
                    imageUrl = imageUrl,
                    timestamp = System.currentTimeMillis()
                )
            )
            if (result.isFailure) return@launch
            coffeeRepository.triggerRefresh()
        }
    }
}

private fun UserEntity.toDomainUser(): User = User(
    id = id,
    username = username,
    fullName = fullName,
    avatarUrl = avatarUrl,
    email = email,
    bio = bio
)

private data class TimelineStaticData(
    val activeUser: UserEntity?,
    val allCoffees: List<CoffeeWithDetails>,
    val allUsers: List<UserEntity>,
    val recommendations: List<CoffeeWithDetails>
)

private data class TimelineDynamicData(
    val posts: List<PostWithDetails>,
    val reviews: List<ReviewWithAuthor>,
    val following: Map<Int, Set<Int>>
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
    data class Error(val message: String) : TimelineUiState
    data class Success(
        val items: List<TimelineItem>,
        val suggestedUsers: List<SuggestedUserInfo>,
        val myFollowingIds: Set<Int>,
        val activeUser: UserEntity,
        val recommendations: List<CoffeeWithDetails> = emptyList()
    ) : TimelineUiState
}
