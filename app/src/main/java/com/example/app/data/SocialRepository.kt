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
                    emit(emptyList())
                }
            }
        }.flowOn(Dispatchers.IO)

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
            externalScope.launch {
                try { supabaseDataSource.insertPost(post) } catch (e: Exception) { }
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

    suspend fun addComment(comment: CommentEntity) = withContext(Dispatchers.IO) {
        socialDao.insertComment(comment)
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch {
                try { supabaseDataSource.insertComment(comment) } catch (e: Exception) { }
            }
        }
        triggerRefresh()
    }

    suspend fun deleteComment(commentId: Int) = withContext(Dispatchers.IO) {
        socialDao.deleteComment(commentId)
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch { try { supabaseDataSource.deleteComment(commentId) } catch (e: Exception) { } }
        }
        triggerRefresh()
    }

    fun getCommentsForPost(postId: String): Flow<List<CommentWithAuthor>> = socialDao.getCommentsWithAuthorForPost(postId)

    suspend fun deletePost(postId: String) = withContext(Dispatchers.IO) {
        val post = socialDao.getAllPostsWithDetails().first().find { it.post.id == postId }?.post
        if (post != null) socialDao.deletePost(post)
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            try { 
                supabaseDataSource.deletePost(postId) 
                triggerRefresh()
            } catch (e: Exception) { }
        } else {
            triggerRefresh()
        }
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
                socialDao.insertLikes(likes)
                socialDao.insertComments(comments)
                socialDao.upsertReviews(reviews)
                
                triggerRefresh()
            } catch (e: Exception) {
                Log.e("SocialRepository", "Error in syncSocialData", e)
            }
        }
    }
}
