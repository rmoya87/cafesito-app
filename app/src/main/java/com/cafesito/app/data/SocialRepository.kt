package com.cafesito.app.data

import android.util.Log
import com.cafesito.app.ui.utils.ConnectivityObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Singleton
class SocialRepository @Inject constructor(
    private val socialDao: SocialDao,
    private val supabaseDataSource: SupabaseDataSource,
    private val userRepository: UserRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val externalScope: CoroutineScope
) {
    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    init {
        externalScope.launch {
            supabaseDataSource.subscribeToLikes()
                .catch { }
                .collect {
                    triggerRefresh()
                }
        }
        externalScope.launch {
            supabaseDataSource.subscribeToComments()
                .catch { }
                .collect {
                    triggerRefresh()
                }
        }
    }

    fun triggerRefresh() {
        _refreshTrigger.tryEmit(Unit)
    }

    fun getAllPostsWithDetails(): Flow<List<PostWithDetails>> = _refreshTrigger
        .flatMapLatest {
            networkBoundResource(
                resourceKey = "all_posts",
                query = { socialDao.getAllPostsWithDetails() },
                fetch = {
                    supervisorScope {
                        val postsDeferred = async { supabaseDataSource.getAllPosts() }
                        val likesDeferred = async { supabaseDataSource.getAllLikes() }
                        val commentsDeferred = async { supabaseDataSource.getAllComments() }
                        val usersDeferred = async { userRepository.getAllUsersList() }
                        val coffeeTagsDeferred = async { supabaseDataSource.getAllPostCoffeeTags() }

                        SocialSyncPayload(
                            posts = postsDeferred.await(),
                            likes = likesDeferred.await(),
                            comments = commentsDeferred.await(),
                            users = usersDeferred.await(),
                            coffeeTags = coffeeTagsDeferred.await()
                        )
                    }
                },
                saveFetchResult = { payload ->
                    withContext(Dispatchers.IO) {
                        socialDao.insertPosts(payload.posts)
                        syncLikesWithRemote(payload.likes)
                        syncCommentsWithRemote(payload.comments)
                        socialDao.upsertPostCoffeeTags(payload.coffeeTags)
                        userRepository.insertUsers(payload.users)
                    }
                },
                shouldFetch = { true },
                scope = externalScope,
                connectivityObserver = connectivityObserver
            )
        }.flowOn(Dispatchers.IO)

    fun getPostsByUserId(userId: Int): Flow<List<PostWithDetails>> = socialDao.getPostsByUserIdWithDetails(userId)
        .onStart {
            if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
                externalScope.launch {
                    try {
                        val posts = supabaseDataSource.getPostsByUserId(userId)
                        socialDao.insertPosts(posts)
                    } catch (e: Exception) { }
                }
            }
        }.flowOn(Dispatchers.IO)

    fun getAllReviewsWithAuthor(): Flow<List<ReviewWithAuthor>> = socialDao.getAllReviewsWithAuthor()

    fun getReviewsForCoffee(coffeeId: String): Flow<List<ReviewWithAuthor>> = _refreshTrigger
        .flatMapLatest {
            flow {
                if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
                    try {
                        val reviews = supabaseDataSource.getReviewsByCoffeeId(coffeeId)
                        val userIds = reviews.map { it.userId }.distinct()
                        val users = userIds.mapNotNull { supabaseDataSource.getUserById(it) }
                        val userMap = users.associateBy { it.id }
                        
                        val result = reviews.mapNotNull { review ->
                            userMap[review.userId]?.let { user ->
                                ReviewWithAuthor(review, user)
                            }
                        }
                        emit(result)
                    } catch (e: Exception) {
                        Log.e("SocialRepository", "Error fetching reviews from Supabase: ${e.message}")
                        emit(emptyList())
                    }
                } else {
                    emit(emptyList())
                }
            }
        }.flowOn(Dispatchers.IO)

    fun getSensoryProfilesForCoffee(coffeeId: String): Flow<List<CoffeeSensoryProfileEntity>> = _refreshTrigger
        .flatMapLatest {
            flow {
                val local = socialDao.getSensoryProfilesForCoffee(coffeeId).first()
                emit(local)
                if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
                    try {
                        val remote = supabaseDataSource.getSensoryProfilesByCoffeeId(coffeeId)
                        socialDao.upsertSensoryProfiles(remote)
                        emit(remote)
                    } catch (e: Exception) {
                        Log.e("SocialRepository", "Error fetching sensory profiles: ${e.message}")
                    }
                }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun upsertSensoryProfile(profile: CoffeeSensoryProfileEntity) = withContext(Dispatchers.IO) {
        socialDao.upsertSensoryProfile(profile)
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            try {
                supabaseDataSource.upsertSensoryProfile(profile)
            } catch (e: Exception) {
                Log.e("SocialRepository", "Error upserting sensory profile: ${e.message}")
            }
        }
        triggerRefresh()
    }

    suspend fun upsertReview(review: ReviewEntity) = withContext(Dispatchers.IO) {
        // Guardamos en Supabase primero para asegurar persistencia
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            try {
                supabaseDataSource.upsertReview(review)
            } catch (e: Exception) {
                Log.e("SocialRepository", "Error upserting review to Supabase: ${e.message}")
            }
        }
        // Actualizamos caché local (Room) si existe la DAO para ello
        socialDao.upsertReviews(listOf(review))
        triggerRefresh()
    }

    suspend fun createPost(post: PostEntity) = withContext(Dispatchers.IO) {
        socialDao.insertPost(post)
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            try {
                supabaseDataSource.insertPost(post)
            } catch (e: Exception) {
                Log.e("SocialRepository", "Error creating post in Supabase", e)
            }
        }
        userRepository.touchUserInteraction()
        triggerRefresh()
    }

    suspend fun upsertPostCoffeeTag(tag: PostCoffeeTagEntity?) = withContext(Dispatchers.IO) {
        if (tag == null) return@withContext
        socialDao.upsertPostCoffeeTag(tag)
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            try {
                supabaseDataSource.upsertPostCoffeeTag(tag)
            } catch (e: Exception) {
                Log.e("SocialRepository", "Error upserting post coffee tag in Supabase", e)
            }
        }
        triggerRefresh()
    }

    suspend fun uploadImage(bucket: String, path: String, bytes: ByteArray): String {
        return supabaseDataSource.uploadImage(bucket, path, bytes)
    }

    suspend fun updatePost(postId: String, newComment: String, newImageUrl: String) = withContext(Dispatchers.IO) {
        val currentPosts = socialDao.getAllPostsWithDetails().first()
        val existing = currentPosts.find { it.post.id == postId }?.post ?: return@withContext
        val updated = existing.copy(comment = newComment, imageUrl = newImageUrl)
        
        // Actualizar local inmediatamente para UI rápida
        socialDao.insertPost(updated)
        triggerRefresh()
        
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            // No usamos externalScope aquí para asegurar que la edición termine antes de refrescar si es posible,
            // o usamos supervisorScope si queremos paralelismo protegido.
            try { 
                supabaseDataSource.insertPost(updated) 
                // Refrescamos tras éxito en Supabase
                triggerRefresh()
            } catch (e: Exception) { 
                Log.e("SocialRepository", "Error updating post in Supabase", e)
            }
        } else {
            triggerRefresh()
        }
    }

    suspend fun toggleLike(postId: String, userId: Int) = withContext(Dispatchers.IO) {
        val like = LikeEntity(postId, userId)
        val isLiked = socialDao.isPostLikedByUser(postId, userId).first()
        
        if (isLiked) socialDao.deleteLike(like) else socialDao.insertLike(like)

        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch {
                try {
                    if (isLiked) supabaseDataSource.deleteLike(postId, userId)
                    else supabaseDataSource.insertLike(like)
                } catch (e: Exception) { }
            }
        }
        triggerRefresh()
    }

    suspend fun savePost(postId: String, userId: Int) = withContext(Dispatchers.IO) {
        val alreadyLiked = socialDao.isPostLikedByUser(postId, userId).first()
        if (alreadyLiked) return@withContext

        val like = LikeEntity(postId, userId)
        socialDao.insertLike(like)

        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch {
                try {
                    supabaseDataSource.insertLike(like)
                } catch (_: Exception) {
                    // Best-effort en background; el refresco reintentará en siguiente sync.
                }
            }
        }
        triggerRefresh()
    }

    suspend fun addComment(comment: CommentEntity) = withContext(Dispatchers.IO) {
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch {
                try {
                    val stored = supabaseDataSource.insertComment(
                        CommentInsert(
                            postId = comment.postId,
                            userId = comment.userId,
                            text = comment.text,
                            timestamp = comment.timestamp
                        )
                    )
                    socialDao.insertComment(stored)
                    val mentionedUsernames = notifyMentionsIfNeeded(stored)
                    val postOwnerId = notifyPostOwnerIfNeeded(stored)
                    notifyCommentParticipantsIfNeeded(
                        comment = stored,
                        postOwnerId = postOwnerId,
                        excludedUsernames = mentionedUsernames
                    )
                } catch (e: Exception) {
                    socialDao.insertComment(comment)
                }
                userRepository.touchUserInteraction()
                triggerRefresh()
            }
        } else {
            socialDao.insertComment(comment)
            triggerRefresh()
        }
    }

    suspend fun deleteComment(commentId: Int) = withContext(Dispatchers.IO) {
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            try {
                supabaseDataSource.deleteComment(commentId)
                socialDao.deleteComment(commentId)
            } catch (e: Exception) {
                triggerRefresh()
                return@withContext
            }
        } else {
            socialDao.deleteComment(commentId)
        }
        triggerRefresh()
    }

    suspend fun updateComment(commentId: Int, newText: String) = withContext(Dispatchers.IO) {
        socialDao.updateComment(commentId, newText)
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch { try { supabaseDataSource.updateComment(commentId, newText) } catch (e: Exception) { } }
        }
        triggerRefresh()
    }

    fun getCommentsForPost(postId: String): Flow<List<CommentWithAuthor>> = socialDao.getCommentsWithAuthorForPost(postId)

    private data class SocialSyncPayload(
        val posts: List<PostEntity>,
        val likes: List<LikeEntity>,
        val comments: List<CommentEntity>,
        val users: List<UserEntity>,
        val coffeeTags: List<PostCoffeeTagEntity>
    )

    private suspend fun syncCommentsWithRemote(remoteComments: List<CommentEntity>) {
        socialDao.insertComments(remoteComments)
        val remoteIds = remoteComments.map { it.id }.toSet()
        val staleLocalIds = socialDao.getAllCommentIds()
            .filter { localId -> localId > 0 && localId !in remoteIds }
        if (staleLocalIds.isNotEmpty()) {
            socialDao.deleteCommentsByIds(staleLocalIds)
        }
    }

    private suspend fun syncLikesWithRemote(remoteLikes: List<LikeEntity>) {
        socialDao.insertLikes(remoteLikes)
        val remoteLikeKeys = remoteLikes
            .map { it.postId to it.userId }
            .toSet()
        val staleLocalLikes = socialDao.getAllLikesList()
            .filter { localLike -> (localLike.postId to localLike.userId) !in remoteLikeKeys }
        staleLocalLikes.forEach { staleLike ->
            socialDao.deleteLike(staleLike)
        }
    }

    suspend fun deletePost(postId: String) = withContext(Dispatchers.IO) {
        val post = socialDao.getAllPostsWithDetails().first().find { it.post.id == postId }?.post
        if (post != null) socialDao.deletePost(post)
        socialDao.deletePostCoffeeTag(postId)
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            try {
                supabaseDataSource.deletePostCoffeeTag(postId)
                supabaseDataSource.deletePost(postId)
                triggerRefresh()
            } catch (e: Exception) { }
        } else {
            triggerRefresh()
        }
    }

    private suspend fun notifyPostOwnerIfNeeded(comment: CommentEntity): Int? {
        val postOwnerId = socialDao.getAllPostsWithDetails()
            .first()
            .firstOrNull { it.post.id == comment.postId }
            ?.post
            ?.userId
            ?: return null
        if (postOwnerId == comment.userId) return postOwnerId

        val author = userRepository.getUserById(comment.userId) ?: return null
        supabaseDataSource.insertNotification(
            NotificationEntity(
                userId = postOwnerId,
                type = "COMMENT",
                fromUsername = author.username,
                message = "te ha dejado un comentario",
                timestamp = System.currentTimeMillis(),
                relatedId = "${comment.postId}:${comment.id}"
            )
        )
        return postOwnerId
    }

    private suspend fun notifyMentionsIfNeeded(comment: CommentEntity): Set<String> {
        val mentionUsernames = extractMentions(comment.text)
        if (mentionUsernames.isEmpty()) return emptySet()
        val author = userRepository.getUserById(comment.userId) ?: return emptySet()
        val mentions = mentionUsernames.filterNot { it.equals(author.username, ignoreCase = true) }
        if (mentions.isEmpty()) return emptySet()

        mentions.forEach { username ->
            val user = userRepository.getUserByUsername(username) ?: return@forEach
            if (user.id == author.id) return@forEach
            supabaseDataSource.insertNotification(
                NotificationEntity(
                    userId = user.id,
                    type = "MENTION",
                    fromUsername = author.username,
                    message = comment.text,
                    timestamp = System.currentTimeMillis(),
                    relatedId = "${comment.postId}:${comment.id}"
                )
            )
        }
        return mentions.map { it.lowercase() }.toSet()
    }

    private suspend fun notifyCommentParticipantsIfNeeded(
        comment: CommentEntity,
        postOwnerId: Int?,
        excludedUsernames: Set<String>
    ) {
        val author = userRepository.getUserById(comment.userId) ?: return
        val excludedLower = excludedUsernames.map { it.lowercase() }.toSet()
        val participants = socialDao.getCommentsWithAuthorForPost(comment.postId)
            .first()
            .mapNotNull { it.author }
            .filter { participant ->
                participant.id != author.id &&
                    participant.id != postOwnerId &&
                    !participant.username.equals(author.username, ignoreCase = true) &&
                    !excludedLower.contains(participant.username.lowercase())
            }
            .distinctBy { it.id }

        participants.forEach { participant ->
            supabaseDataSource.insertNotification(
                NotificationEntity(
                    userId = participant.id,
                    type = "COMMENT",
                    fromUsername = author.username,
                    message = "respondió a una publicación",
                    timestamp = System.currentTimeMillis(),
                    relatedId = "${comment.postId}:${comment.id}"
                )
            )
        }
    }

    private fun extractMentions(text: String): Set<String> {
        val regex = Regex("@([A-Za-z0-9._-]{2,30})")
        return regex.findAll(text)
            .map { it.groupValues[1] }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    suspend fun getTimeline(
        viewerId: Int,
        cursor: Long?,
        limit: Int,
        mode: TimelineFeedMode
    ): TimelinePage = withContext(Dispatchers.IO) {
        val posts = socialDao.getAllPostsWithDetails().first()
        val reviews = socialDao.getAllReviewsWithAuthor().first()
        val following = userRepository.followingMap.first()[viewerId].orEmpty()

        TimelineEngine.getTimeline(
            posts = posts,
            reviews = reviews,
            followingIds = following,
            viewerId = viewerId,
            cursor = cursor,
            limit = limit,
            mode = mode
        )
    }

    suspend fun syncSocialData() {
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            try {
                val posts = supabaseDataSource.getAllPosts()
                val likes = supabaseDataSource.getAllLikes()
                val comments = supabaseDataSource.getAllComments()
                val reviews = supabaseDataSource.getAllReviews()
                
                socialDao.insertPosts(posts)
                syncLikesWithRemote(likes)
                syncCommentsWithRemote(comments)
                socialDao.upsertReviews(reviews)
                
                triggerRefresh()
            } catch (e: Exception) {
                Log.e("SocialRepository", "Error in syncSocialData", e)
            }
        }
    }
}
