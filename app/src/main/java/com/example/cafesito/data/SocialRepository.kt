package com.example.cafesito.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepository @Inject constructor(
    private val socialDao: SocialDao,
    private val supabaseDataSource: SupabaseDataSource,
    private val userDao: UserDao
) {
    fun getAllPostsWithDetails(): Flow<List<PostWithDetails>> = socialDao.getAllPostsWithDetails()
    fun getAllReviewsWithAuthor(): Flow<List<ReviewWithAuthor>> = socialDao.getAllReviewsWithAuthor()
    fun getCommentsForPost(postId: String): Flow<List<CommentWithAuthor>> = socialDao.getCommentsWithAuthorForPost(postId)

    suspend fun createPost(post: PostEntity) {
        supabaseDataSource.insertPost(post)
        socialDao.insertPost(post)
    }

    suspend fun addComment(comment: CommentEntity) {
        supabaseDataSource.insertComment(comment)
        socialDao.insertComment(comment)

        val currentUser = userDao.getActiveUserSync() ?: return
        val post = socialDao.getAllPostsWithDetails().first().find { it.post.id == comment.postId }
        
        if (post != null && post.post.userId != currentUser.id) {
            val notification = NotificationEntity(
                userId = post.post.userId,
                type = "COMMENT",
                fromUsername = currentUser.username,
                message = "ha comentado tu publicación.",
                timestamp = System.currentTimeMillis(),
                relatedId = comment.postId
            )
            supabaseDataSource.insertNotification(notification)
        }
    }

    suspend fun toggleLike(postId: String, userId: Int) {
        val isCurrentlyLiked = socialDao.isPostLikedByUser(postId, userId).first()
        val currentUser = userDao.getActiveUserSync() ?: return

        if (isCurrentlyLiked) {
            supabaseDataSource.deleteLike(postId, userId)
            socialDao.deleteLike(LikeEntity(postId, userId))
        } else {
            val like = LikeEntity(postId, userId)
            supabaseDataSource.insertLike(like)
            socialDao.insertLike(like)

            val post = socialDao.getAllPostsWithDetails().first().find { it.post.id == postId }
            if (post != null && post.post.userId != currentUser.id) {
                val notification = NotificationEntity(
                    userId = post.post.userId,
                    type = "LIKE",
                    fromUsername = currentUser.username,
                    message = "le ha dado me gusta a tu publicación.",
                    timestamp = System.currentTimeMillis(),
                    relatedId = postId
                )
                supabaseDataSource.insertNotification(notification)
            }
        }
    }

    suspend fun syncSocialData() {
        val remotePosts = supabaseDataSource.getAllPosts()
        remotePosts.forEach { socialDao.insertPost(it) }
        
        val remoteLikes = supabaseDataSource.getAllLikes()
        remoteLikes.forEach { socialDao.insertLike(it) }
    }

    fun getNotifications(userId: Int): Flow<List<NotificationEntity>> = socialDao.getNotificationsForUser(userId)
    suspend fun markNotificationsAsRead(userId: Int) = socialDao.markAllAsRead(userId)
}
