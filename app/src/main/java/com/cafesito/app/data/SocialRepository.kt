package com.cafesito.app.data

import android.util.Log
import com.cafesito.app.ui.utils.ConnectivityObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
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

    fun triggerRefresh() {
        _refreshTrigger.tryEmit(Unit)
    }

    /** Posts/likes/comments/postCoffeeTags ya no se cargan desde Supabase; se devuelve solo caché local (vacía). */
    fun getAllPostsWithDetails(): Flow<List<PostWithDetails>> = _refreshTrigger
        .flatMapLatest {
            networkBoundResource(
                resourceKey = "all_posts",
                query = { socialDao.getAllPostsWithDetails() },
                fetch = {
                    SocialSyncPayload(
                        posts = emptyList(),
                        likes = emptyList(),
                        comments = emptyList(),
                        users = emptyList(),
                        coffeeTags = emptyList()
                    )
                },
                saveFetchResult = { payload ->
                    withContext(Dispatchers.IO) {
                        if (payload.posts.isEmpty()) {
                            socialDao.deleteAllComments()
                            socialDao.deleteAllLikes()
                            socialDao.deleteAllPostCoffeeTags()
                            socialDao.deleteAllPosts()
                        } else {
                            socialDao.insertPosts(payload.posts)
                            syncLikesWithRemote(payload.likes)
                            syncCommentsWithRemote(payload.comments)
                            socialDao.upsertPostCoffeeTags(payload.coffeeTags)
                            userRepository.insertUsers(payload.users)
                        }
                    }
                },
                shouldFetch = { true },
                scope = externalScope,
                connectivityObserver = connectivityObserver
            )
        }.flowOn(Dispatchers.IO)

    /** Posts ya no se cargan desde Supabase; solo se lee caché local (vacía). */
    fun getPostsByUserId(userId: Int): Flow<List<PostWithDetails>> = socialDao.getPostsByUserIdWithDetails(userId)
        .flowOn(Dispatchers.IO)

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
        // Funcionalidad social legacy (posts) deshabilitada: no-op.
        Log.w("SocialRepository", "createPost() llamado pero la funcionalidad social legacy está deshabilitada.")
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
        // Funcionalidad social legacy (likes) deshabilitada: no-op.
        @Suppress("UNUSED_PARAMETER")
        val ignored = LikeEntity(postId, userId)
        Log.w("SocialRepository", "toggleLike() llamado pero la funcionalidad social legacy está deshabilitada.")
    }

    suspend fun savePost(postId: String, userId: Int) = withContext(Dispatchers.IO) {
        // Funcionalidad social legacy (guardar post) deshabilitada: no-op.
        @Suppress("UNUSED_PARAMETER")
        val ignored = LikeEntity(postId, userId)
        Log.w("SocialRepository", "savePost() llamado pero la funcionalidad social legacy está deshabilitada.")
    }

    suspend fun addComment(comment: CommentEntity) = withContext(Dispatchers.IO) {
        // Funcionalidad social legacy (comentarios) deshabilitada: no-op.
        @Suppress("UNUSED_PARAMETER")
        val ignored = comment
        Log.w("SocialRepository", "addComment() llamado pero la funcionalidad social legacy está deshabilitada.")
    }

    suspend fun deleteComment(commentId: Int) = withContext(Dispatchers.IO) {
        // Funcionalidad social legacy (comentarios) deshabilitada: no-op.
        @Suppress("UNUSED_PARAMETER")
        val ignored = commentId
        Log.w("SocialRepository", "deleteComment() llamado pero la funcionalidad social legacy está deshabilitada.")
    }

    suspend fun updateComment(commentId: Int, newText: String) = withContext(Dispatchers.IO) {
        // Funcionalidad social legacy (comentarios) deshabilitada: no-op.
        @Suppress("UNUSED_PARAMETER")
        val ignoredId = commentId
        @Suppress("UNUSED_PARAMETER")
        val ignoredText = newText
        Log.w("SocialRepository", "updateComment() llamado pero la funcionalidad social legacy está deshabilitada.")
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
        // Funcionalidad social legacy (posts) deshabilitada: no-op.
        @Suppress("UNUSED_PARAMETER")
        val ignored = postId
        Log.w("SocialRepository", "deletePost() llamado pero la funcionalidad social legacy está deshabilitada.")
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

    /** Sincroniza solo reseñas; posts/likes/comments/postCoffeeTags ya no se cargan (código residual eliminado). */
    suspend fun syncSocialData() {
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            try {
                socialDao.deleteAllComments()
                socialDao.deleteAllLikes()
                socialDao.deleteAllPostCoffeeTags()
                socialDao.deleteAllPosts()

                val reviews = supabaseDataSource.getAllReviews()
                socialDao.upsertReviews(reviews)

                triggerRefresh()
            } catch (e: Exception) {
                Log.e("SocialRepository", "Error in syncSocialData", e)
            }
        }
    }
}
