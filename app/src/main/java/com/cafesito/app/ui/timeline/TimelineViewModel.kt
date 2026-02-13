package com.cafesito.app.ui.timeline

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.CoffeeRepository
import com.cafesito.app.data.CoffeeWithDetails
import com.cafesito.app.data.CommentEntity
import com.cafesito.app.data.PostWithDetails
import com.cafesito.app.data.ReviewWithAuthor
import com.cafesito.app.data.SocialRepository
import com.cafesito.app.data.TimelineFeedItem
import com.cafesito.app.data.TimelineFeedMode
import com.cafesito.app.data.TimelineMeta
import com.cafesito.app.data.TimelinePage
import com.cafesito.app.data.TimelineReasonCode
import com.cafesito.app.data.UserEntity
import com.cafesito.app.data.UserReviewInfo
import com.cafesito.app.data.UserRepository
import com.cafesito.shared.domain.Review
import com.cafesito.shared.domain.SuggestedUserInfo
import com.cafesito.shared.domain.User
import com.cafesito.shared.domain.repository.ReviewRepository
import com.cafesito.shared.domain.validation.ValidateReviewInputUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max
import kotlin.random.Random

@HiltViewModel
class TimelineViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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

    private val _isPublishingContent = MutableStateFlow(false)
    val isPublishingContent: StateFlow<Boolean> = _isPublishingContent.asStateFlow()
    private var publishingStartedAt: Long? = null

    private val pageSize = 10
    private val loadThreshold = 2

    private var latestBaseData: TimelineBaseData? = null
    private var hasLoadedOnce = false


    fun startPublishingContent() {
        publishingStartedAt = System.currentTimeMillis()
        _isPublishingContent.value = true
    }

    private fun maybeFinishPublishing(data: TimelineBaseData) {
        if (!_isPublishingContent.value) return
        val startedAt = publishingStartedAt ?: return
        val lowerBound = startedAt - 10_000
        val hasOwnRecentContent = data.posts.any {
            it.post.userId == data.activeUser.id && it.post.timestamp >= lowerBound
        } || data.reviews.any {
            it.review.userId == data.activeUser.id && it.review.timestamp >= lowerBound
        }
        val isTimedOut = System.currentTimeMillis() - startedAt > 45_000
        if (hasOwnRecentContent || isTimedOut) {
            _isPublishingContent.value = false
            publishingStartedAt = null
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            if (_isRefreshing.value) return@launch
            _isRefreshing.value = true
            Log.d("TimelineVM", "Triggering global refresh...")
            try {
                userRepository.syncUsers()
                socialRepository.syncSocialData()
                coffeeRepository.syncCoffees()
            } catch (e: Exception) {
                Log.e("TimelineVM", "Error in refreshData", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun observeBaseData() {
        viewModelScope.launch {
            combine(
                staticData,
                dynamicData
            ) { static, dynamic ->
                val activeUser = static?.activeUser ?: return@combine null
                TimelineBaseData(
                    activeUser = activeUser,
                    allCoffees = static.allCoffees,
                    allUsers = static.allUsers,
                    favorites = static.favorites,
                    userReviews = static.userReviews,
                    posts = dynamic.posts,
                    reviews = dynamic.reviews,
                    following = dynamic.following
                )
            }
                .filterNotNull()
                .map { data ->
                    val interactionsCount = data.posts.sumOf { it.likes.size + it.comments.size }
                    val postsDigest = data.posts.sumOf { it.post.hashCode() }
                    val key = TimelineDataKey(
                        userId = data.activeUser.id,
                        postsCount = data.posts.size,
                        reviewsCount = data.reviews.size,
                        followingCount = data.following[data.activeUser.id]?.size ?: 0,
                        usersCount = data.allUsers.size,
                        coffeesCount = data.allCoffees.size,
                        favoritesCount = data.favorites.size,
                        interactionsCount = interactionsCount,
                        postsDigest = postsDigest
                    )
                    key to data
                }
                .distinctUntilChanged { old, new -> old.first == new.first }
                .debounce(300)
                .collectLatest { (_, data) ->
                    latestBaseData = data
                    loadInitialPage(data)
                }
        }
    }

    private val staticData = combine(
        userRepository.getActiveUserFlow().onEach { Log.d("TimelineVM", "ActiveUser emitted: ${it?.username}") },
        coffeeRepository.allCoffees.onStart { emit(emptyList()) },
        userRepository.getAllUsersFlow().onStart { emit(emptyList()) },
        coffeeRepository.favorites.onStart { emit(emptyList()) },
        coffeeRepository.allReviews.onStart { emit(emptyList()) }
    ) { me, coffees, users, favorites, reviews ->
        TimelineStaticData(me, coffees, users, favorites, reviews)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val dynamicData = combine(
        socialRepository.getAllPostsWithDetails().onEach { Log.d("TimelineVM", "Posts emitted: ${it.size}") },
        socialRepository.getAllReviewsWithAuthor().onEach { Log.d("TimelineVM", "Reviews emitted: ${it.size}") },
        userRepository.followingMap.onEach { Log.d("TimelineVM", "FollowingMap emitted: ${it.size} entries") }
    ) { posts, reviews, following ->
        TimelineDynamicData(posts, reviews, following)
    }

    init {
        Log.d("TimelineVM", "Initializing TimelineViewModel")
        refreshData()
        observeBaseData()
    }

    private val _uiState = MutableStateFlow<TimelineUiState>(TimelineUiState.Loading)
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    private suspend fun loadInitialPage(data: TimelineBaseData) {
        if (!hasLoadedOnce) {
            _uiState.value = TimelineUiState.Loading
        }
        val mode = determineFeedMode(data)

        val page = try {
            socialRepository.getTimeline(
                viewerId = data.activeUser.id,
                cursor = null,
                limit = pageSize,
                mode = mode
            )
        } catch (e: Exception) {
            _uiState.value = TimelineUiState.Error(e.message ?: "Error al cargar timeline")
            return
        }

        val mappedItems = mapTimelineItems(page.items, data.allCoffees)
        val suggestedUsers = buildSuggestedUsers(data)
        val recommendations = buildCoffeeRecommendations(data)
        val recommendedTopics = buildRecommendedTopics(data)

        if (mappedItems.isEmpty()) {
            logEmptyTimeline(data.activeUser.id, mode, page)
        }

        _uiState.value = TimelineUiState.Success(
            items = mappedItems,
            suggestedUsers = suggestedUsers,
            myFollowingIds = data.following[data.activeUser.id] ?: emptySet(),
            activeUser = data.activeUser,
            recommendations = recommendations,
            recommendedTopics = recommendedTopics,
            meta = page.meta,
            nextCursor = page.nextCursor,
            canLoadMore = page.nextCursor != null,
            isLoadingMore = false
        )
        maybeFinishPublishing(data)
        hasLoadedOnce = true
    }

    fun loadMoreIfNeeded(index: Int) {
        val current = _uiState.value as? TimelineUiState.Success ?: return
        if (current.isLoadingMore || !current.canLoadMore) return
        if (index < current.items.size - loadThreshold) return

        val data = latestBaseData ?: return
        viewModelScope.launch {
            val nextCursor = current.nextCursor ?: return@launch
            _uiState.value = current.copy(isLoadingMore = true)

            val page = try {
                socialRepository.getTimeline(
                    viewerId = data.activeUser.id,
                    cursor = nextCursor,
                    limit = pageSize,
                    mode = determineFeedMode(data)
                )
            } catch (e: Exception) {
                _uiState.value = TimelineUiState.Error(e.message ?: "Error al cargar más")
                return@launch
            }

            val newItems = mapTimelineItems(page.items, data.allCoffees)
            val combinedItems = (current.items + newItems).distinctBy { it.stableKey }

            _uiState.value = current.copy(
                items = combinedItems,
                nextCursor = page.nextCursor,
                canLoadMore = page.nextCursor != null && newItems.isNotEmpty(),
                isLoadingMore = false
            )
        }
    }

    private fun determineFeedMode(data: TimelineBaseData): TimelineFeedMode {
        val followingIds = data.following[data.activeUser.id].orEmpty()
        val hasOwnContent = data.posts.any { it.post.userId == data.activeUser.id } ||
            data.reviews.any { it.review.userId == data.activeUser.id }
        return if (followingIds.isNotEmpty() || hasOwnContent) {
            TimelineFeedMode.FOLLOWING
        } else {
            TimelineFeedMode.GLOBAL
        }
    }

    private fun mapTimelineItems(
        items: List<TimelineFeedItem>,
        coffees: List<CoffeeWithDetails>
    ): List<TimelineItem> {
        return items.mapNotNull { item ->
            when (item) {
                is TimelineFeedItem.Post -> TimelineItem.PostItem(item.details)
                is TimelineFeedItem.Review -> {
                    val coffeeDetails = coffees.find { it.coffee.id == item.details.review.coffeeId }
                    coffeeDetails?.let {
                        TimelineItem.ReviewItem(
                            UserReviewInfo(
                                coffeeDetails = it,
                                review = item.details.review,
                                authorName = item.details.author?.fullName,
                                authorAvatarUrl = item.details.author?.avatarUrl
                            )
                        )
                    }
                }
            }
        }
    }

    private fun buildSuggestedUsers(data: TimelineBaseData): List<SuggestedUserInfo> {
        val myFollowing = data.following[data.activeUser.id].orEmpty()
        val relatedIds = if (myFollowing.isEmpty()) {
            emptySet()
        } else {
            myFollowing.flatMap { followedId -> data.following[followedId].orEmpty() }.toSet()
        }

        val excludedIds = myFollowing + data.activeUser.id
        val candidates = data.allUsers.filter { it.id !in excludedIds }

        val friendsOfFriends = if (relatedIds.isEmpty()) {
            emptyList()
        } else {
            candidates.filter { relatedIds.contains(it.id) }
        }

        val recentActivityCutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val activityScores = mutableMapOf<Int, Int>()
        val recentPosts = data.posts.filter { it.post.timestamp >= recentActivityCutoff }
        for (post in recentPosts) {
            val authorId = post.post.userId
            activityScores[authorId] = (activityScores[authorId] ?: 0) + 1
        }
        val recentReviews = data.reviews.filter { it.review.timestamp >= recentActivityCutoff }
        for (review in recentReviews) {
            val authorId = review.review.userId
            activityScores[authorId] = (activityScores[authorId] ?: 0) + 1
        }

        val sortedByActivity = candidates.sortedWith(
            compareByDescending<UserEntity> { activityScores[it.id] ?: 0 }
                .thenByDescending { data.following.values.count { followers -> followers.contains(it.id) } }
                .thenBy { it.username }
        )

        val selection = if (myFollowing.isEmpty()) {
            sortedByActivity
        } else {
            val fallback = sortedByActivity.filterNot { candidate ->
                friendsOfFriends.any { it.id == candidate.id }
            }
            (friendsOfFriends + fallback)
        }

        return selection.distinctBy { it.id }.take(10).map { entity ->
            SuggestedUserInfo(
                user = User(
                    id = entity.id,
                    username = entity.username,
                    fullName = entity.fullName,
                    avatarUrl = entity.avatarUrl,
                    email = entity.email,
                    bio = entity.bio
                ),
                followersCount = data.following.values.count { it.contains(entity.id) },
                followingCount = data.following[entity.id]?.size ?: 0
            )
        }
    }

    private fun buildCoffeeRecommendations(data: TimelineBaseData): List<CoffeeWithDetails> {
        val favoriteIds = data.favorites.filter { it.userId == data.activeUser.id }.map { it.coffeeId }.toSet()
        val reviewedIds = data.userReviews.filter { it.userId == data.activeUser.id }.map { it.coffeeId }.toSet()
        val interactedIds = favoriteIds + reviewedIds

        val interactionCoffees = data.allCoffees.filter { interactedIds.contains(it.coffee.id) }
        val preferenceTags = interactionCoffees.flatMap { coffee ->
            buildList {
                addAll(coffee.coffee.paisOrigen.toAtomizedList())
                addAll(coffee.coffee.tueste.toAtomizedList())
                addAll(coffee.coffee.especialidad.toAtomizedList())
                addAll(coffee.coffee.formato.toAtomizedList())
            }
        }.toSet()

        val candidates = data.allCoffees.filter { !interactedIds.contains(it.coffee.id) }
        val filtered = if (preferenceTags.isEmpty()) {
            candidates
        } else {
            candidates.filter { coffee ->
                val tags = coffee.coffee.paisOrigen.toAtomizedList() +
                    coffee.coffee.tueste.toAtomizedList() +
                    coffee.coffee.especialidad.toAtomizedList() +
                    coffee.coffee.formato.toAtomizedList()
                tags.any { it in preferenceTags }
            }
        }

        val seed = max(data.activeUser.id, 1) * 31 + filtered.size
        return filtered.shuffled(Random(seed)).take(10)
    }

    private fun buildRecommendedTopics(data: TimelineBaseData): List<String> {
        val favoriteIds = data.favorites.filter { it.userId == data.activeUser.id }.map { it.coffeeId }.toSet()
        val reviewedIds = data.userReviews.filter { it.userId == data.activeUser.id }.map { it.coffeeId }.toSet()
        val interactedIds = favoriteIds + reviewedIds
        val interactionCoffees = data.allCoffees.filter { interactedIds.contains(it.coffee.id) }
        val preferenceTags = interactionCoffees.flatMap { coffee ->
            buildList {
                addAll(coffee.coffee.especialidad.toAtomizedList())
                addAll(coffee.coffee.tueste.toAtomizedList())
                addAll(coffee.coffee.paisOrigen.toAtomizedList())
                addAll(coffee.coffee.formato.toAtomizedList())
            }
        }.filter { it.isNotBlank() }

        val fallbackTags = data.allCoffees.flatMap { coffee ->
            coffee.coffee.especialidad.toAtomizedList() + coffee.coffee.tueste.toAtomizedList()
        }.filter { it.isNotBlank() }

        return (preferenceTags.ifEmpty { fallbackTags }).distinct().take(12)
    }

    private fun String?.toAtomizedList(): List<String> =
        this?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

    private fun logEmptyTimeline(viewerId: Int, mode: TimelineFeedMode, page: TimelinePage) {
        val reason = page.meta.reasonCode ?: TimelineReasonCode.NO_POSTS_GLOBAL
        Log.w(
            "TimelineTelemetry",
            "Empty feed: viewer_user_id=$viewerId feed_type=$mode " +
                "applied_fallbacks=${page.meta.fallbacksUsed} reason_code=$reason"
        )
    }

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
        viewModelScope.launch {
            val imageToPersist = resolvePersistableImageUrl(newImageUrl, "posts") ?: newImageUrl
            socialRepository.updatePost(postId, newText, imageToPersist)
        }
    }

    fun deleteReview(coffeeId: String) {
        viewModelScope.launch {
            val user = userRepository.getActiveUser() ?: return@launch
            coffeeRepository.deleteReview(coffeeId, user.id)
            socialRepository.syncSocialData()
        }
    }

    fun updateReview(coffeeId: String, rating: Float, comment: String, imageUrl: String?) {
        viewModelScope.launch {
            val validation = validateReviewInput(rating, comment)
            if (validation.isFailure) return@launch
            val user = userRepository.getActiveUser() ?: return@launch
            val imageToPersist = resolvePersistableImageUrl(imageUrl, "reviews")
            val result = reviewRepository.updateReview(
                Review(
                    user = user.toDomainUser(),
                    coffeeId = coffeeId,
                    rating = rating,
                    comment = comment,
                    imageUrl = imageToPersist,
                    timestamp = System.currentTimeMillis()
                )
            )
            if (result.isFailure) return@launch
            coffeeRepository.triggerRefresh()
            socialRepository.syncSocialData()
        }
    }

    private suspend fun resolvePersistableImageUrl(rawUrl: String?, bucket: String): String? {
        if (rawUrl.isNullOrBlank()) return rawUrl
        if (!rawUrl.startsWith("content://") && !rawUrl.startsWith("file://")) return rawUrl

        return withContext(Dispatchers.IO) {
            try {
                val bytes = context.contentResolver.openInputStream(Uri.parse(rawUrl))?.use { it.readBytes() }
                    ?: return@withContext rawUrl
                val path = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
                socialRepository.uploadImage(bucket, path, bytes)
            } catch (e: Exception) {
                Log.e("TimelineVM", "No se pudo subir imagen local", e)
                rawUrl
            }
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
    val favorites: List<com.cafesito.app.data.LocalFavorite>,
    val userReviews: List<com.cafesito.app.data.ReviewEntity>
)

private data class TimelineDynamicData(
    val posts: List<PostWithDetails>,
    val reviews: List<ReviewWithAuthor>,
    val following: Map<Int, Set<Int>>
)

private data class TimelineBaseData(
    val activeUser: UserEntity,
    val allCoffees: List<CoffeeWithDetails>,
    val allUsers: List<UserEntity>,
    val favorites: List<com.cafesito.app.data.LocalFavorite>,
    val userReviews: List<com.cafesito.app.data.ReviewEntity>,
    val posts: List<PostWithDetails>,
    val reviews: List<ReviewWithAuthor>,
    val following: Map<Int, Set<Int>>
)

private data class TimelineDataKey(
    val userId: Int,
    val postsCount: Int,
    val reviewsCount: Int,
    val followingCount: Int,
    val usersCount: Int,
    val coffeesCount: Int,
    val favoritesCount: Int,
    val interactionsCount: Int,
    val postsDigest: Int
)

sealed class TimelineItem {
    abstract val timestamp: Long
    abstract val stableKey: String
    data class PostItem(val details: PostWithDetails) : TimelineItem() {
        override val timestamp: Long = details.post.timestamp
        override val stableKey: String = "post_${details.post.id}"
    }
    data class ReviewItem(val reviewInfo: UserReviewInfo) : TimelineItem() {
        override val timestamp: Long = reviewInfo.review.timestamp
        override val stableKey: String = "review_${reviewInfo.review.id}"
    }
    data class FavoriteActionItem(val coffeeDetails: CoffeeWithDetails, override val timestamp: Long) : TimelineItem() {
        override val stableKey: String = "fav_$timestamp"
    }
}

sealed interface TimelineUiState {
    data object Loading : TimelineUiState
    data class Error(val message: String) : TimelineUiState
    data class Success(
        val items: List<TimelineItem>,
        val suggestedUsers: List<SuggestedUserInfo>,
        val myFollowingIds: Set<Int>,
        val activeUser: UserEntity,
        val recommendations: List<CoffeeWithDetails> = emptyList(),
        val recommendedTopics: List<String> = emptyList(),
        val meta: TimelineMeta,
        val nextCursor: Long?,
        val canLoadMore: Boolean,
        val isLoadingMore: Boolean
    ) : TimelineUiState
}
