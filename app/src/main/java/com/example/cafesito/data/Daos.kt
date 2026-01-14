package com.example.cafesito.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CoffeeDao {
    @Transaction
    @Query("SELECT * FROM coffees ORDER BY nombre ASC")
    fun getAllCoffeesWithDetails(): Flow<List<CoffeeWithDetails>>

    @Transaction
    @Query("SELECT * FROM coffees WHERE id = :id")
    fun getCoffeeWithDetailsById(id: String): Flow<CoffeeWithDetails?>

    @Transaction
    @Query("""
        SELECT * FROM coffees 
        WHERE (:query IS NULL OR nombre LIKE '%' || :query || '%' OR marca LIKE '%' || :query || '%')
        AND (:origin IS NULL OR paisOrigen = :origin)
        AND (:roast IS NULL OR tueste = :roast)
        AND (:specialty IS NULL OR especialidad = :specialty)
        AND (:variety IS NULL OR variedadTipo LIKE '%' || :variety || '%')
        AND (:format IS NULL OR formato = :format)
        AND (:grind IS NULL OR moliendaRecomendada LIKE '%' || :grind || '%')
        ORDER BY nombre ASC
    """)
    fun getFilteredCoffees(
        query: String?, 
        origin: String?,
        roast: String?,
        specialty: String?,
        variety: String?,
        format: String?,
        grind: String?
    ): Flow<List<CoffeeWithDetails>>

    @Query("SELECT * FROM coffees WHERE id = :id")
    suspend fun getCoffeeById(id: String): Coffee?

    @Query("SELECT COUNT(*) FROM coffees")
    suspend fun getCoffeeCount(): Int

    @Query("SELECT * FROM local_favorites")
    fun getLocalFavorites(): Flow<List<LocalFavorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: LocalFavorite)

    @Delete
    suspend fun deleteFavorite(favorite: LocalFavorite)

    @Query("SELECT * FROM reviews_db ORDER BY timestamp DESC")
    fun getAllReviews(): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReview(review: ReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoffee(coffee: Coffee)

    @Query("DELETE FROM coffees")
    suspend fun deleteAllCoffees()
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users_db")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Query("UPDATE users_db SET googleId = NULL WHERE id = :userId")
    suspend fun clearGoogleId(userId: Int)

    @Query("SELECT COUNT(*) FROM users_db")
    suspend fun getUserCount(): Int

    @Query("SELECT * FROM users_db WHERE id = :userId")
    suspend fun getUserById(userId: Int): UserEntity?

    @Query("SELECT * FROM users_db WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users_db WHERE googleId IS NOT NULL LIMIT 1")
    suspend fun getActiveUserSync(): UserEntity?

    @Query("SELECT * FROM users_db WHERE googleId IS NOT NULL LIMIT 1")
    fun getActiveUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM follows")
    fun getAllFollows(): Flow<List<FollowEntity>>

    @Query("SELECT * FROM follows")
    suspend fun getAllFollowsList(): List<FollowEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: FollowEntity)

    @Delete
    suspend fun deleteFollow(follow: FollowEntity)
}

@Dao
interface SocialDao {
    @Transaction
    @Query("SELECT * FROM posts_db ORDER BY timestamp DESC")
    fun getAllPostsWithDetails(): Flow<List<PostWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Delete
    suspend fun deletePost(post: PostEntity)

    @Transaction
    @Query("SELECT * FROM reviews_db ORDER BY timestamp DESC")
    fun getAllReviewsWithAuthor(): Flow<List<ReviewWithAuthor>>

    @Transaction
    @Query("SELECT * FROM comments_db WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsWithAuthorForPost(postId: String): Flow<List<CommentWithAuthor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLike(like: LikeEntity)

    @Delete
    suspend fun deleteLike(like: LikeEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM likes_db WHERE postId = :postId AND userId = :userId)")
    fun isPostLikedByUser(postId: String, userId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("SELECT * FROM notifications_db WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsForUser(userId: Int): Flow<List<NotificationEntity>>

    @Query("UPDATE notifications_db SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: Int)
}
