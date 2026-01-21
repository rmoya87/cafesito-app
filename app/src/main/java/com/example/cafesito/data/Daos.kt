package com.example.cafesito.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CoffeeDao {
    @Transaction
    @Query("SELECT * FROM coffees ORDER BY nombre ASC")
    fun getAllCoffeesWithDetails(): Flow<List<CoffeeWithDetails>>

    @Transaction
    @Query("SELECT * FROM coffees WHERE isCustom = 0 ORDER BY nombre ASC")
    fun getPublicCoffeesWithDetails(): Flow<List<CoffeeWithDetails>>

    @Transaction
    @Query("SELECT * FROM coffees WHERE id = :id")
    fun getCoffeeWithDetailsById(id: String): Flow<CoffeeWithDetails?>

    @Transaction
    @Query("""
        SELECT * FROM coffees 
        WHERE isCustom = 0
        AND (:query IS NULL OR nombre LIKE '%' || :query || '%' OR marca LIKE '%' || :query || '%')
        AND (:origin IS NULL OR paisOrigen = :origin)
        AND (:roast IS NULL OR tueste = :roast)
        AND (:specialty IS NULL OR especialidad = :specialty)
        AND (:variety IS NULL OR variedadTipo LIKE '%' || :variety || '%')
        AND (:format IS NULL OR formato = :format)
        AND (:grind IS NULL OR moliendaRecomendada LIKE '%' || :grind || '%')
        ORDER BY nombre ASC
    """)
    fun getFilteredPublicCoffees(
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoffees(coffees: List<Coffee>)

    @Query("SELECT * FROM local_favorites")
    fun getLocalFavorites(): Flow<List<LocalFavorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: LocalFavorite)

    @Delete
    suspend fun deleteFavorite(favorite: LocalFavorite)

    // FAVORITOS CUSTOM
    @Query("SELECT * FROM local_favorites_custom")
    fun getLocalFavoritesCustom(): Flow<List<LocalFavoriteCustom>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteCustom(favorite: LocalFavoriteCustom)

    @Delete
    suspend fun deleteFavoriteCustom(favorite: LocalFavoriteCustom)

    @Query("SELECT * FROM reviews_db ORDER BY timestamp DESC")
    fun getAllReviews(): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReview(review: ReviewEntity)

    @Query("DELETE FROM reviews_db WHERE coffeeId = :coffeeId AND userId = :userId")
    suspend fun deleteReviewByUser(coffeeId: String, userId: Int)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users_db")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Query("SELECT * FROM users_db WHERE id = :userId")
    suspend fun getUserById(userId: Int): UserEntity?

    @Query("SELECT * FROM users_db WHERE googleId = :googleId LIMIT 1")
    suspend fun getUserByGoogleId(googleId: String): UserEntity?

    @Query("SELECT * FROM users_db WHERE googleId = :googleId LIMIT 1")
    fun getUserByGoogleIdFlow(googleId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users_db WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM follows")
    fun getAllFollows(): Flow<List<FollowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: FollowEntity)

    @Delete
    suspend fun deleteFollow(follow: FollowEntity)

    @Query("DELETE FROM users_db")
    suspend fun deleteAllUsers()
}

@Dao
interface SocialDao {
    @Transaction
    @Query("SELECT * FROM posts_db ORDER BY timestamp DESC")
    fun getAllPostsWithDetails(): Flow<List<PostWithDetails>>

    @Transaction
    @Query("SELECT * FROM posts_db WHERE userId = :userId ORDER BY timestamp DESC")
    fun getPostsByUserIdWithDetails(userId: Int): Flow<List<PostWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Delete
    suspend fun deletePost(post: PostEntity)

    @Transaction
    @Query("SELECT * FROM reviews_db ORDER BY timestamp DESC")
    fun getAllReviewsWithAuthor(): Flow<List<ReviewWithAuthor>>

    @Transaction
    @Query("SELECT * FROM reviews_db WHERE userId = :userId ORDER BY timestamp DESC")
    fun getReviewsByUserIdWithAuthor(userId: Int): Flow<List<ReviewWithAuthor>>

    @Transaction
    @Query("SELECT * FROM comments_db WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsWithAuthorForPost(postId: String): Flow<List<CommentWithAuthor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<CommentEntity>)

    @Query("DELETE FROM comments_db WHERE id = :commentId")
    suspend fun deleteComment(commentId: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLike(like: LikeEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLikes(likes: List<LikeEntity>)

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

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries WHERE userId = :userId ORDER BY timestamp DESC")
    fun getDiaryEntries(userId: Int): Flow<List<DiaryEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaryEntry(entry: DiaryEntryEntity)

    @Query("DELETE FROM diary_entries WHERE id = :entryId")
    suspend fun deleteDiaryEntryById(entryId: Long)

    @Query("SELECT * FROM pantry_items WHERE userId = :userId")
    fun getPantryItems(userId: Int): Flow<List<PantryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPantryItem(item: PantryItemEntity)

    @Query("DELETE FROM pantry_items WHERE coffeeId = :coffeeId AND userId = :userId")
    suspend fun deletePantryItem(coffeeId: String, userId: Int)
}
