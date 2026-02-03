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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val coffeeRepository: CoffeeRepository,
    private val socialRepository: SocialRepository,
    private val reviewRepository: ReviewRepository,
    private val notificationStore: TimelineNotificationStore
) : ViewModel() {
    private val validateReviewInput = ValidateReviewInputUseCase()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val deletedNotificationIds = MutableStateFlow<Set<String>>(emptySet())

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
            } catch (e: Exception) {
                Log.e("TimelineVM", "Error in refreshData", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // staticData - Consolidado con flujos de repositorios
    private val staticData = combine(
        userRepository.getActiveUserFlow().onEach { Log.d("TimelineVM", "ActiveUser emitted: ${it?.username}") },
        coffeeRepository.allCoffees.onStart { emit(emptyList()) },
        userRepository.getAllUsersFlow().onStart { emit(emptyList()) }
    ) { me, coffees, users -> 
        TimelineStaticData(me, coffees, users, emptyList()) // Recommendations se cargan por separado si es necesario
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // dynamicData
    private val dynamicData = combine(
        socialRepository.getAllPostsWithDetails().onEach { Log.d("TimelineVM", "Posts emitted: ${it.size}") },
        socialRepository.getAllReviewsWithAuthor().onEach { Log.d("TimelineVM", "Reviews emitted: ${it.size}") },
        userRepository.followingMap.onEach { Log.d("TimelineVM", "FollowingMap emitted: ${it.size} entries") }
    ) { posts, reviews, following -> 
        TimelineDynamicData(posts, reviews, following)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TimelineUiState> = combine(
        staticData,
        dynamicData
    ) { static, dynamic ->
        if (static?.activeUser == null) return@combine TimelineUiState.Loading

        val activeUser = static.activeUser
        val posts = dynamic.posts
        val reviews = dynamic.reviews
        val myFollowing = dynamic.following[activeUser.id] ?: emptySet()
        
        val isFollowingAnyone = myFollowing.isNotEmpty()
        val visibleUserIds = if (isFollowingAnyone) myFollowing + activeUser.id else emptySet()

        val postItems = posts
            .filter { !isFollowingAnyone || visibleUserIds.contains(it.post.userId) }
            .map { TimelineItem.PostItem(it) }

        val reviewItems = reviews
            .filter { !isFollowingAnyone || visibleUserIds.contains(it.review.userId) }
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
            recommendations = emptyList()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimelineUiState.Loading)

    private val localReadNotificationIds = MutableStateFlow<Set<Int>>(emptySet())
    private val notifiedNotificationIds = MutableStateFlow(notificationStore.getNotifiedIds())

    @OptIn(ExperimentalCoroutinesApi::class)
    val notifications: StateFlow<List<TimelineNotification>> = staticData
        .filterNotNull()
        .flatMapLatest { static ->
            val userId = static.activeUser?.id ?: return@flatMapLatest flowOf(emptyList<TimelineNotification>())
            userRepository.getNotificationsForUser(userId).map { entities ->
                entities.mapNotNull { it.toTimelineNotification(static.allUsers) }
            }
        }
        .combine(deletedNotificationIds) { list: List<TimelineNotification>, deletedIds: Set<String> ->
            list.filter { it.id !in deletedIds }.sortedByDescending { it.timestamp }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationIds: StateFlow<Set<String>> = combine(
        notifications,
        localReadNotificationIds
    ) { notifications: List<TimelineNotification>, localReadIds: Set<Int> ->
        notifications
            .filter { !it.isRead && it.notificationId !in localReadIds }
            .map { it.id }
            .toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val unreadCount: StateFlow<Int> = unreadNotificationIds
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val newUnreadNotifications: StateFlow<List<TimelineNotification>> = combine(
        notifications,
        unreadNotificationIds,
        notifiedNotificationIds
    ) { notifications: List<TimelineNotification>, unreadIds: Set<String>, notifiedIds: Set<String> ->
        notifications.filter { it.id in unreadIds && it.id !in notifiedIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFollowSuggestion(userId: Int) {
        viewModelScope.launch {
            val me = userRepository.getActiveUser() ?: return@launch
            userRepository.toggleFollow(me.id, userId)
        }
    }

    fun markNotificationRead(notification: TimelineNotification) {
        localReadNotificationIds.update { it + notification.notificationId }
        viewModelScope.launch {
            userRepository.markNotificationRead(notification.notificationId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val user = userRepository.getActiveUser() ?: return@launch
            userRepository.markAllNotificationsRead(user.id)
        }
    }

    fun deleteNotification(notification: TimelineNotification) {
        deletedNotificationIds.update { it + notification.id }
        viewModelScope.launch {
            userRepository.deleteNotification(notification.notificationId)
        }
    }

    fun markNotificationsNotified(notificationIds: Set<String>) {
        if (notificationIds.isEmpty()) return
        notifiedNotificationIds.update { it + notificationIds }
        notificationStore.addNotifiedIds(notificationIds)
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
