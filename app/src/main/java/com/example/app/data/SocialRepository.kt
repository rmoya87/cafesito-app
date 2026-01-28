package com.cafesito.app.data

import android.util.Log
import com.cafesito.app.ui.utils.ConnectivityObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Singleton
class SocialRepository @Inject constructor(
    private val socialDao: SocialDao,
    private val supabaseDataSource: SupabaseDataSource,
    private val userRepository: UserRepository,
    private val connectivityObserver: ConnectivityObserver
) {
    private val _refreshTrigger = MutableStateFlow(0L)

    // --- CACHÉ EN MEMORIA ---
    private var _cachedPosts: List<PostWithDetails>? = null
    private var _cachedReviews: List<ReviewWithAuthor>? = null

    private suspend fun ensureConnected() {
        if (connectivityObserver.observe().first() != ConnectivityObserver.Status.Available) {
            throw NoConnectivityException("No hay conexión a internet.")
        }
    }

    fun triggerRefresh() {
        _cachedPosts = null
        _cachedReviews = null
        _refreshTrigger.value++
    }

    /**
     * Flujo reactivo que combina la carga inicial con actualizaciones Realtime de Supabase.
     * Implementa paralelización y caché.
     */
    fun getAllPostsWithDetails(forceRefresh: Boolean = false): Flow<List<PostWithDetails>> {
        if (forceRefresh) _cachedPosts = null

        val realtimeUpdates = merge(
            supabaseDataSource.subscribeToLikes(),
            supabaseDataSource.subscribeToComments()
        ).map { Unit }

        return merge(_refreshTrigger.map { Unit }, realtimeUpdates)
            .debounce(300)
            .flatMapLatest {
                flow {
                    _cachedPosts?.let { emit(it); if (!forceRefresh) return@flow }

                    try {
                        ensureConnected()
                        supervisorScope {
                            val postsDef = async { supabaseDataSource.getAllPosts() }
                            val likesDef = async { supabaseDataSource.getAllLikes() }
                            val commentsDef = async { supabaseDataSource.getAllComments() }
                            val usersDef = async { userRepository.getAllUsersList() }

                            val remotePosts = postsDef.await()
                            val remoteLikes = likesDef.await()
                            val remoteComments = commentsDef.await()
                            val users = usersDef.await()

                            val details = withContext(Dispatchers.Default) {
                                remotePosts.map { post ->
                                    val author = users.find { it.id == post.userId } ?: UserEntity(post.userId, null, "Usuario", "Usuario", "", "", "")
                                    PostWithDetails(
                                        post = post,
                                        author = author,
                                        likes = remoteLikes.filter { it.postId == post.id },
                                        comments = remoteComments.filter { it.postId == post.id }
                                    )
                                }
                            }
                            _cachedPosts = details
                            emit(details)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e("SOCIAL_REPO", "Error fetching social data", e)
                        emit(_cachedPosts ?: emptyList())
                    }
                }
            }.flowOn(Dispatchers.IO)
    }

    fun getPostsByUserId(userId: Int): Flow<List<PostWithDetails>> {
        return merge(_refreshTrigger.map { Unit }, supabaseDataSource.subscribeToLikes().map { Unit })
            .debounce(300)
            .flatMapLatest {
                flow {
                    try {
                        ensureConnected()
                        supervisorScope {
                            // ✅ OPTIMIZACIÓN: Filtrado en servidor
                            val postsDef = async { supabaseDataSource.getPostsByUserId(userId) }
                            val likesDef = async { supabaseDataSource.getAllLikes() }
                            val commentsDef = async { supabaseDataSource.getAllComments() }
                            val authorDef = async { userRepository.getUserById(userId) }

                            val filteredPosts = postsDef.await()
                            val remoteLikes = likesDef.await()
                            val remoteComments = commentsDef.await()
                            val author = authorDef.await() ?: UserEntity(userId, null, "Usuario", "Usuario", "", "", "")

                            val details = withContext(Dispatchers.Default) {
                                filteredPosts.map { post ->
                                    PostWithDetails(
                                        post = post,
                                        author = author,
                                        likes = remoteLikes.filter { it.postId == post.id },
                                        comments = remoteComments.filter { it.postId == post.id }
                                    )
                                }
                            }
                            emit(details)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        emit(emptyList())
                    }
                }
            }.flowOn(Dispatchers.IO)
    }

    fun getAllReviewsWithAuthor(): Flow<List<ReviewWithAuthor>> = _refreshTrigger.debounce(300).flatMapLatest {
        flow {
            _cachedReviews?.let { emit(it); return@flow }
            try {
                ensureConnected()
                supervisorScope {
                    val reviewsDef = async { supabaseDataSource.getAllReviews() }
                    val usersDef = async { userRepository.getAllUsersList() }

                    val reviews = reviewsDef.await()
                    val users = usersDef.await()

                    val result = withContext(Dispatchers.Default) {
                        reviews.mapNotNull { review ->
                            users.find { it.id == review.userId }?.let { author ->
                                ReviewWithAuthor(review, author)
                            }
                        }
                    }
                    _cachedReviews = result
                    emit(result)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emit(_cachedReviews ?: emptyList())
            }
        }
    }

    suspend fun createPost(post: PostEntity) {
        ensureConnected()
        supabaseDataSource.insertPost(post)
        triggerRefresh()
    }

    suspend fun deletePost(postId: String) {
        ensureConnected()
        supabaseDataSource.deletePost(postId)
        triggerRefresh()
    }

    suspend fun updatePost(postId: String, newComment: String, newImageUrl: String) {
        ensureConnected()
        val posts = supabaseDataSource.getAllPosts()
        val existing = posts.find { it.id == postId } ?: return
        val updated = existing.copy(comment = newComment, imageUrl = newImageUrl)
        supabaseDataSource.insertPost(updated)
        triggerRefresh()
    }

    suspend fun addComment(comment: CommentEntity) {
        ensureConnected()
        supabaseDataSource.insertComment(comment)
        triggerRefresh()
        
        val currentUser = userRepository.getActiveUser()
        val postOwnerId = supabaseDataSource.getAllPosts().find { it.id == comment.postId }?.userId
        if (postOwnerId != null && currentUser != null && postOwnerId != currentUser.id) {
            try {
                supabaseDataSource.insertNotification(NotificationEntity(
                    userId = postOwnerId,
                    type = "COMMENT",
                    fromUsername = currentUser.username,
                    message = "ha comentado tu publicación.",
                    timestamp = System.currentTimeMillis(),
                    relatedId = comment.postId
                ))
            } catch (e: Exception) { }
        }
    }

    suspend fun deleteComment(commentId: Int) {
        try {
            ensureConnected()
            supabaseDataSource.deleteComment(commentId)
            triggerRefresh()
        } catch (e: Exception) { }
    }

    suspend fun updateComment(commentId: Int, newText: String) {
        try {
            ensureConnected()
            val comments = supabaseDataSource.getAllComments()
            val existing = comments.find { it.id == commentId } ?: return
            val updated = existing.copy(text = newText)
            supabaseDataSource.upsertComment(updated)
            triggerRefresh()
        } catch (e: Exception) { }
    }

    suspend fun toggleLike(postId: String, userId: Int) {
        ensureConnected()
        val likes = supabaseDataSource.getAllLikes()
        val isLiked = likes.any { it.postId == postId && it.userId == userId }

        if (isLiked) {
            supabaseDataSource.deleteLike(postId, userId)
        } else {
            supabaseDataSource.insertLike(LikeEntity(postId, userId))
            val post = supabaseDataSource.getAllPosts().find { it.id == postId }
            val currentUser = userRepository.getActiveUser()
            if (post != null && currentUser != null && post.userId != currentUser.id) {
                try {
                    supabaseDataSource.insertNotification(NotificationEntity(
                        userId = post.userId,
                        type = "LIKE",
                        fromUsername = currentUser.username,
                        message = "le ha dado me gusta a tu publicación.",
                        timestamp = System.currentTimeMillis(),
                        relatedId = postId
                    ))
                } catch (e: Exception) { }
            }
        }
        triggerRefresh()
    }

    suspend fun uploadImage(bucket: String, path: String, bytes: ByteArray): String {
        ensureConnected()
        return supabaseDataSource.uploadImage(bucket, path, bytes)
    }

    fun getCommentsForPost(postId: String): Flow<List<CommentWithAuthor>> {
        val realtimeComments = supabaseDataSource.subscribeToComments().map { Unit }
        
        return merge(_refreshTrigger.map { Unit }, realtimeComments)
            .debounce(300)
            .flatMapLatest {
                flow {
                    try {
                        ensureConnected()
                        val comments = supabaseDataSource.getCommentsForPost(postId)
                        val users = userRepository.getAllUsersList()
                        val result = withContext(Dispatchers.Default) {
                            comments.map { comment ->
                                val author = users.find { it.id == comment.userId } ?: UserEntity(comment.userId, null, "Usuario", "", "", "", "")
                                CommentWithAuthor(comment, author)
                            }
                        }
                        emit(result)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        emit(emptyList())
                    }
                }
            }.flowOn(Dispatchers.IO)
    }

    suspend fun syncSocialData() {
        triggerRefresh()
    }
}
