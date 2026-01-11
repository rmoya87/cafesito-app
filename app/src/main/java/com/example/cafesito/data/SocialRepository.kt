package com.example.cafesito.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepository @Inject constructor(
    private val socialDao: SocialDao
) {
    fun getAllPostsWithDetails(): Flow<List<PostWithDetails>> = socialDao.getAllPostsWithDetails()

    fun getAllReviewsWithAuthor(): Flow<List<ReviewWithAuthor>> = socialDao.getAllReviewsWithAuthor()

    suspend fun createPost(post: PostEntity) = socialDao.insertPost(post)

    fun getCommentsForPost(postId: String): Flow<List<CommentEntity>> = socialDao.getCommentsForPost(postId)

    suspend fun addComment(comment: CommentEntity) = socialDao.insertComment(comment)

    fun isPostLikedByUser(postId: String, userId: Int): Flow<Boolean> = socialDao.isPostLikedByUser(postId, userId)

    suspend fun toggleLike(postId: String, userId: Int) {
        val isCurrentlyLiked = socialDao.isPostLikedByUser(postId, userId).first()
        if (isCurrentlyLiked) {
            socialDao.deleteLike(LikeEntity(postId, userId))
        } else {
            socialDao.insertLike(LikeEntity(postId, userId))
        }
    }
}
