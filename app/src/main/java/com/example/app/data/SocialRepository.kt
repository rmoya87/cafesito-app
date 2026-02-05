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
                        val posts = supabaseDataSource.getAllPosts()
                        val likes = supabaseDataSource.getAllLikes()
                        val comments = supabaseDataSource.getAllComments()
                        val users = userRepository.getAllUsersList()
                        Triple(posts, Pair(likes, comments), users)
                    }
                },
                saveFetchResult = { (posts, interactions, users) ->
                    withContext(Dispatchers.IO) {
                        socialDao.insertPosts(posts)
                        socialDao.insertLikes(interactions.first)
                        socialDao.insertComments(interactions.second)
                        userRepository.insertUsers(users)
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
                    // Si no hay conexión y queremos "directo", no mostramos nada o error
                    emit(emptyList())
                }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun refreshReviews(coffeeId: String) = withContext(Dispatchers.IO) {
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            try {
                val reviews = supabaseDataSource.getReviewsByCoffeeId(coffeeId)
                val userIds = reviews.map { it.userId }.distinct()
                val users = userIds.mapNotNull { supabaseDataSource.getUserById(it) }
                userRepository.insertUsers(users)
                // socialDao.upsertReviews(reviews) // Eliminado para ir directo a Supabase
            } catch (e: Exception) {
                Log.e("SocialRepository", "Error refreshing reviews: ${e.message}")
            }
        }
    }

    suspend fun createPost(post: PostEntity) = withContext(Dispatchers.IO) {
        socialDao.insertPost(post)
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch {
                try { supabaseDataSource.insertPost(post) } catch (e: Exception) { }
            }
        }
    }

    suspend fun uploadImage(bucket: String, path: String, bytes: ByteArray): String {
        return supabaseDataSource.uploadImage(bucket, path, bytes)
    }

    suspend fun updatePost(postId: String, newComment: String, newImageUrl: String) = withContext(Dispatchers.IO) {
        val currentPosts = socialDao.getAllPostsWithDetails().first()
        val existing = currentPosts.find { it.post.id == postId }?.post ?: return@withContext
        val updated = existing.copy(comment = newComment, imageUrl = newImageUrl)
        socialDao.insertPost(updated)
        
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch {
                try { supabaseDataSource.insertPost(updated) } catch (e: Exception) { }
            }
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
    }

    suspend fun addComment(comment: CommentEntity) = withContext(Dispatchers.IO) {
        socialDao.insertComment(comment)
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch {
                try { supabaseDataSource.insertComment(comment) } catch (e: Exception) { }
            }
        }
    }

    suspend fun updateComment(commentId: Int, newText: String) = withContext(Dispatchers.IO) {
        // Implementación futura si es necesaria
    }

    suspend fun deleteComment(commentId: Int) = withContext(Dispatchers.IO) {
        socialDao.deleteComment(commentId)
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch { try { supabaseDataSource.deleteComment(commentId) } catch (e: Exception) { } }
        }
    }

    fun getCommentsForPost(postId: String): Flow<List<CommentWithAuthor>> = socialDao.getCommentsWithAuthorForPost(postId)

    suspend fun deletePost(postId: String) = withContext(Dispatchers.IO) {
        val post = socialDao.getAllPostsWithDetails().first().find { it.post.id == postId }?.post
        if (post != null) socialDao.deletePost(post)
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch { try { supabaseDataSource.deletePost(postId) } catch (e: Exception) { } }
        }
    }

    suspend fun getUserByUsername(username: String): UserEntity? = userRepository.getUserByUsername(username)

    suspend fun syncSocialData() {
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            try {
                val posts = supabaseDataSource.getAllPosts()
                val likes = supabaseDataSource.getAllLikes()
                val comments = supabaseDataSource.getAllComments()
                
                socialDao.insertPosts(posts)
                socialDao.insertLikes(likes)
                socialDao.insertComments(comments)
                
                triggerRefresh()
            } catch (e: Exception) {
                Log.e("SocialRepository", "Error in syncSocialData", e)
            }
        }
    }
}
