package com.cafesito.app.ui.timeline

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.CoffeeRepository
import com.cafesito.app.data.CoffeeWithDetails
import com.cafesito.app.data.DiaryRepository
import com.cafesito.app.data.DiaryEntryEntity
import com.cafesito.app.data.PantryItemWithDetails
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
import com.cafesito.app.data.SupabaseDataSource
import com.cafesito.app.data.SyncManager
import com.cafesito.app.data.UserRepository
import com.cafesito.shared.domain.Review
import com.cafesito.shared.domain.SuggestedUserInfo
import com.cafesito.shared.domain.brew.BrewDiaryEntryForOrder
import com.cafesito.shared.domain.brew.getOrderedBrewMethods
import com.cafesito.shared.domain.User
import com.cafesito.shared.domain.repository.ReviewRepository
import com.cafesito.shared.domain.validation.ValidateReviewInputUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max
import kotlin.random.Random

@HiltViewModel
class HomeViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val coffeeRepository: CoffeeRepository,
    private val socialRepository: SocialRepository,
    private val syncManager: SyncManager,
    private val reviewRepository: ReviewRepository,
    private val diaryRepository: DiaryRepository,
    private val notificationStore: TimelineNotificationStore,
    private val supabaseDataSource: SupabaseDataSource
) : ViewModel() {
    private val validateReviewInput = ValidateReviewInputUseCase()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val deletedNotificationIds = MutableStateFlow<Set<String>>(emptySet())

    private val _isPublishingContent = MutableStateFlow(false)
    val isPublishingContent: StateFlow<Boolean> = _isPublishingContent.asStateFlow()
    private var publishingStartedAt: Long? = null
    private var publishingAutoHideJob: Job? = null

    private val pageSize = 10
    private val loadThreshold = 2

    private var latestBaseData: TimelineBaseData? = null
    private var hasLoadedOnce = false

    fun updateStock(pantryItemId: String, total: Int, remaining: Int) {
        viewModelScope.launch {
            diaryRepository.updatePantryStockById(pantryItemId, total, remaining)
            refreshData()
        }
    }

    fun removeFromPantry(pantryItemId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                diaryRepository.deletePantryItemById(pantryItemId)
                refreshData()
                onSuccess()
            } catch (e: Exception) {
                Log.e("TimelineVM", "Error removing from pantry", e)
            }
        }
    }

    fun markCoffeeAsFinished(pantryItemId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                diaryRepository.markCoffeeAsFinished(pantryItemId)
                refreshData()
                onSuccess()
            } catch (e: Exception) {
                Log.e("TimelineVM", "Error marking finished", e)
            }
        }
    }

    fun startPublishingContent() {
        publishingAutoHideJob?.cancel()
        publishingStartedAt = System.currentTimeMillis()
        _isPublishingContent.value = true
        publishingAutoHideJob = viewModelScope.launch {
            delay(8_000)
            if (_isPublishingContent.value) {
                stopPublishingContent()
            }
        }
    }

    private fun stopPublishingContent() {
        _isPublishingContent.value = false
        publishingStartedAt = null
        publishingAutoHideJob?.cancel()
        publishingAutoHideJob = null
    }

    private fun maybeFinishPublishing(data: TimelineBaseData) {
        if (!_isPublishingContent.value) return
        val startedAt = publishingStartedAt ?: return
        val lowerBound = startedAt - 10_000
        val hasOwnRecentContent = data.reviews.any {
            it.review.userId == data.activeUser.id && it.review.timestamp >= lowerBound
        }
        val isTimedOut = System.currentTimeMillis() - startedAt > 45_000
        if (hasOwnRecentContent || isTimedOut) {
            stopPublishingContent()
        }
    }

    suspend fun getUserIdByUsername(username: String): Int? {
        val cleanName = username.removePrefix("@").trim()
        if (cleanName.isEmpty()) return null
        return userRepository.getUserByUsername(cleanName)?.id
    }

    fun refreshData() {
        viewModelScope.launch {
            if (_isRefreshing.value) return@launch
            _isRefreshing.value = true
            Log.d("TimelineVM", "Triggering global refresh...")
            try {
                withContext(Dispatchers.IO) {
                    userRepository.restoreSessionFromSupabaseIfNeeded()
                    syncManager.syncUsersIfNeeded(force = true)
                    userRepository.syncFollows()
                    socialRepository.syncSocialData()
                    syncManager.syncCoffeesIfNeeded(force = true)
                    syncManager.syncDeferred()
                }
                if (_isPublishingContent.value) {
                    stopPublishingContent()
                }
            } catch (e: Exception) {
                Log.e("TimelineVM", "Error in refreshData", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeBaseData() {
        viewModelScope.launch {
            val dynamicDataWithFallback = combine(
                flowOf(emptyList<PostWithDetails>()),
                socialRepository.getAllReviewsWithAuthor().onStart { emit(emptyList()) },
                userRepository.followingMap.onStart { emit(emptyMap()) }
            ) { posts, reviews, following ->
                TimelineDynamicData(posts, reviews, following)
            }
            combine(
                staticData,
                dynamicDataWithFallback,
                diaryRepository.getPantryItems().onStart { emit(emptyList()) },
                diaryRepository.getDiaryEntries().onStart { emit(emptyList()) }
            ) { static, dynamic, pantryItems, diaryEntries ->
                val activeUser = static?.activeUser ?: return@combine null
                val pantryCoffeeIds = pantryItems.map { it.coffee.id }.toSet()
                TimelineBaseData(
                    activeUser = activeUser,
                    allCoffees = static.allCoffees,
                    allUsers = static.allUsers,
                    favorites = static.favorites,
                    userReviews = static.userReviews,
                    pantryCoffeeIds = pantryCoffeeIds,
                    pantryItems = pantryItems,
                    diaryEntries = diaryEntries,
                    posts = dynamic.posts,
                    reviews = dynamic.reviews,
                    following = dynamic.following
                )
            }
                .filterNotNull()
                .map { data ->
                    val pantryStockSignature = data.pantryItems.sumOf { it.pantryItem.gramsRemaining.toLong() } + data.pantryItems.sumOf { it.pantryItem.coffeeId.hashCode().toLong() }
                    TimelineDataKey(
                        userId = data.activeUser.id,
                        pantryCount = data.pantryCoffeeIds.size,
                        pantryStockSignature = pantryStockSignature,
                        diaryCount = data.diaryEntries.size,
                        coffeesCount = data.allCoffees.size,
                        usersCount = data.allUsers.size
                    ) to data
                }
                .distinctUntilChanged { old, new -> old.first == new.first }
                .debounce(250)
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
        flowOf(emptyList<PostWithDetails>()),
        socialRepository.getAllReviewsWithAuthor().onEach { Log.d("TimelineVM", "Reviews emitted: ${it.size}") },
        userRepository.followingMap.onEach { Log.d("TimelineVM", "FollowingMap emitted: ${it.size} entries") }
    ) { posts, reviews, following ->
        TimelineDynamicData(posts, reviews, following)
    }

    init {
        Log.d("HomeVM", "Initializing HomeViewModel")
        refreshData()
        observeBaseData()
    }

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private suspend fun loadInitialPage(data: TimelineBaseData) {
        if (!hasLoadedOnce) {
            _uiState.value = HomeUiState.Loading
        }
        val forOrder = data.diaryEntries.map { e ->
            BrewDiaryEntryForOrder(
                preparationType = e.preparationType,
                type = e.type,
                timestamp = e.timestamp
            )
        }
        val orderedBrewMethodNames = getOrderedBrewMethods(forOrder)
        val recommendations = buildCoffeeRecommendations(data)
        val suggestedUsers = buildSuggestedUsers(data)

        _uiState.value = HomeUiState.Success(
            items = emptyList(),
            suggestedUsers = suggestedUsers,
            myFollowingIds = data.following[data.activeUser.id] ?: emptySet(),
            activeUser = data.activeUser,
            allUsers = data.allUsers,
            recommendations = recommendations,
            recommendedTopics = emptyList(),
            pantryItems = data.pantryItems,
            orderedBrewMethodNames = orderedBrewMethodNames,
            meta = TimelineMeta(feedSource = TimelineFeedMode.GLOBAL),
            nextCursor = null,
            canLoadMore = false,
            isLoadingMore = false
        )
        hasLoadedOnce = true
    }

    private fun determineFeedMode(data: TimelineBaseData): TimelineFeedMode {
        val followingIds = data.following[data.activeUser.id].orEmpty()
        val hasOwnContent = data.reviews.any { it.review.userId == data.activeUser.id }
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

    private suspend fun buildCoffeeRecommendations(data: TimelineBaseData): List<CoffeeWithDetails> {
        // Recomendaciones "recomendadas" (fuente de verdad): RPC Supabase `get_coffee_recommendations`.
        // Si falla (sin red/500) usamos fallback local para no bloquear Home.
        val rpc = runCatching {
            withContext(Dispatchers.IO) {
                supabaseDataSource.getRecommendationsWithCache(data.activeUser.id)
            }
        }.getOrNull().orEmpty()
        if (rpc.isNotEmpty()) {
            val (allowCapsule, allowDecaf) = inferUserRecommendationConstraints(data)
            val byId = data.allCoffees.associateBy { it.coffee.id }
            val mapped = rpc.mapNotNull { coffee ->
                byId[coffee.id] ?: CoffeeWithDetails(coffee, null, emptyList())
            }.distinctBy { it.coffee.id }

            val filtered = mapped.filter { details ->
                val formato = normalize(details.coffee.formato)
                val cafeina = normalize(details.coffee.cafeina)
                val isCapsule = formato.contains("capsul")
                val isDecaf = isNoCaffeine(cafeina)
                (!isCapsule || allowCapsule) && (!isDecaf || allowDecaf)
            }
            val picked = filtered.take(10)
            if (picked.isNotEmpty()) return picked
        }

        return buildCoffeeRecommendationsLocal(data)
    }

    /**
     * Si el usuario nunca interactuó con cápsulas o descafeinado (favoritos, reseñas, despensa, diario),
     * lo filtramos de las recomendaciones para evitar "sorpresas" en Home.
     */
    private fun inferUserRecommendationConstraints(data: TimelineBaseData): Pair<Boolean, Boolean> {
        val favoriteIds = data.favorites.filter { it.userId == data.activeUser.id }.map { it.coffeeId }.toSet()
        val reviewedIds = data.userReviews.filter { it.userId == data.activeUser.id }.map { it.coffeeId }.toSet()
        val diaryIds = data.diaryEntries.mapNotNull { it.coffeeId }.toSet()
        val referenceIds = favoriteIds + reviewedIds + data.pantryCoffeeIds + diaryIds
        val referenceCoffees = data.allCoffees.asSequence()
            .filter { referenceIds.contains(it.coffee.id) }
            .map { it.coffee }
            .toList()

        val hasCapsuleSignal = referenceCoffees.any { normalize(it.formato).contains("capsul") }
        val hasDecafSignal = referenceCoffees.any { isNoCaffeine(normalize(it.cafeina)) }
        return hasCapsuleSignal to hasDecafSignal
    }

    private fun normalize(v: String?): String = v?.trim()?.lowercase() ?: ""

    private fun isNoCaffeine(cafeinaNormalized: String): Boolean {
        if (cafeinaNormalized.isBlank()) return false
        // En dataset se ven valores tipo "Sí/No" y también textos tipo "Sin cafeina/Descafeinado".
        return cafeinaNormalized == "no" ||
            cafeinaNormalized.contains("sin") && cafeinaNormalized.contains("cafe") ||
            cafeinaNormalized.contains("descaf")
    }

    private fun buildCoffeeRecommendationsLocal(data: TimelineBaseData): List<CoffeeWithDetails> {
        val favoriteIds = data.favorites.filter { it.userId == data.activeUser.id }.map { it.coffeeId }.toSet()
        val reviewedIds = data.userReviews.filter { it.userId == data.activeUser.id }.map { it.coffeeId }.toSet()
        val referenceIds = favoriteIds + reviewedIds + data.pantryCoffeeIds

        val referenceCoffees = data.allCoffees.filter { referenceIds.contains(it.coffee.id) }
        val preferenceTags = referenceCoffees.flatMap { coffee ->
            buildList {
                addAll(coffee.coffee.paisOrigen.toAtomizedList())
                addAll(coffee.coffee.tueste.toAtomizedList())
                addAll(coffee.coffee.especialidad.toAtomizedList())
                addAll(coffee.coffee.formato.toAtomizedList())
                coffee.coffee.proceso.trim().takeIf { it.isNotEmpty() }?.let { add(it) }
            }
        }.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

        val candidates = data.allCoffees.filter { !referenceIds.contains(it.coffee.id) }
        val similar = if (preferenceTags.isEmpty()) {
            candidates
        } else {
            candidates.filter { coffee ->
                val tags = (
                    coffee.coffee.paisOrigen.toAtomizedList() +
                    coffee.coffee.tueste.toAtomizedList() +
                    coffee.coffee.especialidad.toAtomizedList() +
                    coffee.coffee.formato.toAtomizedList() +
                    listOfNotNull(coffee.coffee.proceso.trim().takeIf { it.isNotEmpty() })
                ).map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                tags.any { it in preferenceTags }
            }
        }

        val seed = max(data.activeUser.id, 1) * 31 + similar.size
        return similar.shuffled(Random(seed)).take(10)
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
                val hydratedUsers = hydrateUsersForNotifications(
                    entities = entities,
                    knownUsers = static.allUsers
                )
                entities.mapNotNull { it.toTimelineNotification(hydratedUsers) }
            }
        }
        .combine(deletedNotificationIds) { list: List<TimelineNotification>, deletedIds: Set<String> ->
            list.filter { it.id !in deletedIds }.sortedByDescending { it.timestamp }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private suspend fun hydrateUsersForNotifications(
        entities: List<com.cafesito.app.data.NotificationEntity>,
        knownUsers: List<UserEntity>
    ): List<UserEntity> {
        if (entities.isEmpty()) return knownUsers
        val usersById = knownUsers.associateBy { it.id }.toMutableMap()
        val usersByUsername = knownUsers.associateBy { it.username.lowercase() }.toMutableMap()

        entities.forEach { notification ->
            val typeUpper = notification.type.uppercase()
            if (typeUpper == "FOLLOW" || typeUpper == "FIRST_COFFEE" || typeUpper == "FOLLOWED_FIRST_COFFEE") {
                val targetId = notification.relatedId?.toIntOrNull()
                if (targetId != null && !usersById.containsKey(targetId)) {
                    userRepository.getUserById(targetId)?.let { user ->
                        usersById[user.id] = user
                        usersByUsername[user.username.lowercase()] = user
                    }
                }
            }

            val usernameKey = notification.fromUsername.lowercase()
            if (!usersByUsername.containsKey(usernameKey)) {
                userRepository.getUserByUsername(notification.fromUsername)?.let { user ->
                    usersById[user.id] = user
                    usersByUsername[user.username.lowercase()] = user
                }
            }
        }
        return usersById.values.toList()
    }

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
            try {
                userRepository.toggleFollow(me.id, userId)
            } catch (e: Exception) {
                Log.e("TimelineVM", "toggleFollowSuggestion failed", e)
            }
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

    fun savePostFromNotification(notification: TimelineNotification) {
        viewModelScope.launch { markNotificationRead(notification) }
    }

    fun acceptListInvitation(invitationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { supabaseDataSource.acceptListInvitation(invitationId) }
                .onSuccess { markAllAsRead() }
        }
    }

    fun declineListInvitation(invitationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { supabaseDataSource.declineListInvitation(invitationId) }
        }
    }

    fun markNotificationsNotified(notificationIds: Set<String>) {
        if (notificationIds.isEmpty()) return
        notifiedNotificationIds.update { it + notificationIds }
        notificationStore.addNotifiedIds(notificationIds)
    }

    fun onAddComment(postId: String, text: String) { /* posts/comments removed */ }

    fun toggleLike(postId: String) { /* posts removed */ }
    fun deletePost(postId: String) { /* posts removed */ }
    fun updatePost(postId: String, newText: String, newImageUrl: String) { /* posts removed */ }

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
    val pantryCoffeeIds: Set<String>,
    val pantryItems: List<PantryItemWithDetails>,
    val diaryEntries: List<DiaryEntryEntity>,
    val posts: List<PostWithDetails>,
    val reviews: List<ReviewWithAuthor>,
    val following: Map<Int, Set<Int>>
)

private data class TimelineDataKey(
    val userId: Int,
    val pantryCount: Int,
    /** Firma de stock (cambia al descontar gramos en elaboración) para refrescar Timeline. */
    val pantryStockSignature: Long,
    val diaryCount: Int,
    val coffeesCount: Int,
    val usersCount: Int
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

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Success(
        val items: List<TimelineItem>,
        val suggestedUsers: List<SuggestedUserInfo>,
        val myFollowingIds: Set<Int>,
        val activeUser: UserEntity,
        val allUsers: List<UserEntity>,
        val recommendations: List<CoffeeWithDetails> = emptyList(),
        val recommendedTopics: List<String> = emptyList(),
        val pantryItems: List<PantryItemWithDetails> = emptyList(),
        val orderedBrewMethodNames: List<String> = emptyList(),
        val meta: TimelineMeta,
        val nextCursor: Long?,
        val canLoadMore: Boolean,
        val isLoadingMore: Boolean
    ) : HomeUiState
}
