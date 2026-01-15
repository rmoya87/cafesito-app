package com.example.cafesito.data

import android.util.Log
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepository @Inject constructor(
    private val socialDao: SocialDao,
    private val supabaseDataSource: SupabaseDataSource,
    private val userRepository: UserRepository
) {
    // Señal para forzar recarga
    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    fun triggerRefresh() {
        _refreshTrigger.tryEmit(Unit)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getAllPostsWithDetails(): Flow<List<PostWithDetails>> = _refreshTrigger.flatMapLatest {
        flow {
            try {
                val remotePosts = supabaseDataSource.getAllPosts()
                val remoteLikes = supabaseDataSource.getAllLikes()
                val remoteComments = supabaseDataSource.getAllComments()
                val users = userRepository.getAllUsersList()

                val details = remotePosts.map { post ->
                    val author = users.find { it.id == post.userId } ?: UserEntity(post.userId, "", "Usuario", "Usuario", "", "", "")
                    PostWithDetails(
                        post = post,
                        author = author,
                        likes = remoteLikes.filter { it.postId == post.id },
                        comments = remoteComments.filter { it.postId == post.id }
                    )
                }
                emit(details)
            } catch (e: Exception) {
                Log.e("SOCIAL_REPO", "Error fetching from Supabase", e)
                emit(emptyList())
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getPostsByUserId(userId: Int): Flow<List<PostWithDetails>> = _refreshTrigger.flatMapLatest {
        flow {
            try {
                val remotePosts = supabaseDataSource.getAllPosts().filter { it.userId == userId }
                val remoteLikes = supabaseDataSource.getAllLikes()
                val remoteComments = supabaseDataSource.getAllComments()
                val author = userRepository.getUserById(userId) ?: UserEntity(userId, "", "Usuario", "Usuario", "", "", "")

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
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getAllReviewsWithAuthor(): Flow<List<ReviewWithAuthor>> = _refreshTrigger.flatMapLatest {
        flow {
            try {
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
        supabaseDataSource.insertPost(post)
        triggerRefresh()
    }

    suspend fun deletePost(postId: String) {
        supabaseDataSource.deletePost(postId)
        triggerRefresh()
    }

    suspend fun updatePost(postId: String, newComment: String, newImageUrl: String) {
        val posts = supabaseDataSource.getAllPosts()
        val existing = posts.find { it.id == postId } ?: return
        val updated = existing.copy(comment = newComment, imageUrl = newImageUrl)
        supabaseDataSource.insertPost(updated)
        triggerRefresh()
    }

    suspend fun addComment(comment: CommentEntity) {
        supabaseDataSource.insertComment(comment)
        triggerRefresh()
        
        val posts = supabaseDataSource.getAllPosts()
        val post = posts.find { it.id == comment.postId }
        val currentUser = userRepository.getActiveUser()
        
        if (post != null && currentUser != null && post.userId != currentUser.id) {
            try {
                supabaseDataSource.insertNotification(NotificationEntity(
                    userId = post.userId,
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
            supabaseDataSource.deleteComment(commentId)
            triggerRefresh()
        } catch (e: Exception) { }
    }

    suspend fun updateComment(commentId: Int, newText: String) {
        try {
            val comments = supabaseDataSource.getAllComments()
            val existing = comments.find { it.id == commentId } ?: return
            val updated = existing.copy(text = newText)
            supabaseDataSource.upsertComment(updated)
            triggerRefresh()
        } catch (e: Exception) { }
    }

    suspend fun toggleLike(postId: String, userId: Int) {
        val likes = supabaseDataSource.getAllLikes()
        val isLiked = likes.any { it.postId == postId && it.userId == userId }

        if (isLiked) {
            supabaseDataSource.deleteLike(postId, userId)
        } else {
            supabaseDataSource.insertLike(LikeEntity(postId, userId))
            val posts = supabaseDataSource.getAllPosts()
            val post = posts.find { it.id == postId }
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
        return supabaseDataSource.uploadImage(bucket, path, bytes)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getCommentsForPost(postId: String): Flow<List<CommentWithAuthor>> = _refreshTrigger.flatMapLatest {
        flow {
            try {
                val comments = supabaseDataSource.getCommentsForPost(postId)
                val users = userRepository.getAllUsersList()
                val result = comments.map { comment ->
                    val author = users.find { it.id == comment.userId } ?: UserEntity(comment.userId, "", "Usuario", "", "", "", "")
                    CommentWithAuthor(comment, author)
                }
                emit(result)
            } catch (e: Exception) {
                emit(emptyList())
            }
        }
    }

    suspend fun syncSocialData() {
        // Mantenemos por compatibilidad, pero triggerRefresh es lo principal ahora
        triggerRefresh()
    }
}
