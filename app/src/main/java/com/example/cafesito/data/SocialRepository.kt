package com.example.cafesito.data

import android.util.Log
import com.example.cafesito.ui.utils.ConnectivityObserver
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepository @Inject constructor(
    private val socialDao: SocialDao,
    private val supabaseDataSource: SupabaseDataSource,
    private val userRepository: UserRepository,
    private val connectivityObserver: ConnectivityObserver
) {
    private val _refreshTrigger = MutableStateFlow(0L)

    private suspend fun ensureConnected() {
        if (connectivityObserver.observe().first() != ConnectivityObserver.Status.Available) {
            throw NoConnectivityException("No hay conexión a internet.")
        }
    }

    fun triggerRefresh() {
        _refreshTrigger.value++
    }

    /**
     * Flujo reactivo que combina la carga inicial con actualizaciones Realtime de Supabase.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getAllPostsWithDetails(): Flow<List<PostWithDetails>> {
        val realtimeUpdates = merge(
            supabaseDataSource.subscribeToLikes(),
            supabaseDataSource.subscribeToComments()
        ).onEach { Log.d("REALTIME", "Cambio detectado social: $it") }.map { Unit }

        // Utilizamos merge de los triggers (manual + realtime) mapeados a Unit
        return merge(_refreshTrigger.map { Unit }, realtimeUpdates)
            .flatMapLatest {
                flow {
                    try {
                        ensureConnected()
                        val remotePosts = supabaseDataSource.getAllPosts()
                        val remoteLikes = supabaseDataSource.getAllLikes()
                        val remoteComments = supabaseDataSource.getAllComments()
                        val users = userRepository.getAllUsersList()

                        val details = remotePosts.map { post ->
                            val author = users.find { it.id == post.userId } ?: UserEntity(post.userId, null, "Usuario", "Usuario", "", "", "")
                            PostWithDetails(
                                post = post,
                                author = author,
                                likes = remoteLikes.filter { it.postId == post.id },
                                comments = remoteComments.filter { it.postId == post.id }
                            )
                        }
                        emit(details)
                    } catch (e: Exception) {
                        Log.e("SOCIAL_REPO", "Error fetching social data", e)
                        emit(emptyList())
                    }
                }
            }.flowOn(Dispatchers.IO)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getPostsByUserId(userId: Int): Flow<List<PostWithDetails>> {
        val realtimeLikes = supabaseDataSource.subscribeToLikes().map { Unit }
        
        return merge(_refreshTrigger.map { Unit }, realtimeLikes)
            .flatMapLatest {
                flow {
                    try {
                        ensureConnected()
                        val remotePosts = supabaseDataSource.getAllPosts().filter { it.userId == userId }
                        val remoteLikes = supabaseDataSource.getAllLikes()
                        val remoteComments = supabaseDataSource.getAllComments()
                        val author = userRepository.getUserById(userId) ?: UserEntity(userId, null, "Usuario", "Usuario", "", "", "")

                        val details = remotePosts.map { post ->
                            PostWithDetails(
                                post = post,
                                author = author,
                                likes = remoteLikes.filter { it.postId == post.id },
                                comments = remoteComments.filter { it.postId == post.id }
                            )
                        }
                        emit(details)
                    } catch (e: Exception) {
                        emit(emptyList())
                    }
                }
            }.flowOn(Dispatchers.IO)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getAllReviewsWithAuthor(): Flow<List<ReviewWithAuthor>> = _refreshTrigger.flatMapLatest {
        flow {
            try {
                ensureConnected()
                val reviews = supabaseDataSource.getAllReviews()
                val users = userRepository.getAllUsersList()
                val result = reviews.mapNotNull { review ->
                    users.find { it.id == review.userId }?.let { author ->
                        ReviewWithAuthor(review, author)
                    }
                }
                emit(result)
            } catch (e: Exception) {
                emit(emptyList())
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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getCommentsForPost(postId: String): Flow<List<CommentWithAuthor>> {
        val realtimeComments = supabaseDataSource.subscribeToComments().map { Unit }
        
        return merge(_refreshTrigger.map { Unit }, realtimeComments)
            .flatMapLatest {
                flow {
                    try {
                        ensureConnected()
                        val comments = supabaseDataSource.getCommentsForPost(postId)
                        val users = userRepository.getAllUsersList()
                        val result = comments.map { comment ->
                            val author = users.find { it.id == comment.userId } ?: UserEntity(comment.userId, null, "Usuario", "", "", "", "")
                            CommentWithAuthor(comment, author)
                        }
                        emit(result)
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
