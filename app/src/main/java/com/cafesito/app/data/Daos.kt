package com.cafesito.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM active_session LIMIT 1")
    fun getActiveSession(): Flow<ActiveSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSession(session: ActiveSessionEntity)

    @Query("DELETE FROM active_session")
    suspend fun clearSession()
}

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

    @Query("SELECT * FROM coffees WHERE id = :id")
    suspend fun getCoffeeById(id: String): Coffee?

    @Query("SELECT * FROM coffees WHERE isCustom = 1 AND userId = :userId")
    suspend fun getCustomCoffeesByUserId(userId: Int): List<Coffee>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoffees(coffees: List<Coffee>): List<Long>

    @Query("DELETE FROM coffees WHERE id = :coffeeId")
    suspend fun deleteCoffeeById(coffeeId: String): Int

    @Query("SELECT * FROM local_favorites")
    fun getLocalFavorites(): Flow<List<LocalFavorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: LocalFavorite): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(favorites: List<LocalFavorite>): List<Long>

    @Delete
    suspend fun deleteFavorite(favorite: LocalFavorite): Int

    @Query("DELETE FROM local_favorites WHERE userId = :userId")
    suspend fun deleteFavoritesByUserId(userId: Int): Int

    @Query("SELECT * FROM reviews_db ORDER BY timestamp DESC")
    fun getAllReviews(): Flow<List<ReviewEntity>>


    @Query("SELECT * FROM coffee_sensory_profiles WHERE coffeeId = :coffeeId")
    fun getSensoryProfilesForCoffee(coffeeId: String): Flow<List<CoffeeSensoryProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSensoryProfile(profile: CoffeeSensoryProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSensoryProfiles(profiles: List<CoffeeSensoryProfileEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReview(review: ReviewEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReviews(reviews: List<ReviewEntity>): List<Long>

    @Query("DELETE FROM reviews_db WHERE coffeeId = :coffeeId AND userId = :userId")
    suspend fun deleteReviewByUser(coffeeId: String, userId: Int): Int
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users_db")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity): Long

    @Query("SELECT * FROM users_db WHERE id = :userId")
    suspend fun getUserById(userId: Int): UserEntity?

    @Query("SELECT * FROM users_db WHERE id = :userId")
    fun getUserByIdFlow(userId: Int): Flow<UserEntity?>

    @Query("SELECT * FROM users_db WHERE googleId = :googleId LIMIT 1")
    suspend fun getUserByGoogleId(googleId: String): UserEntity?

    @Query("SELECT * FROM users_db WHERE googleId = :googleId LIMIT 1")
    fun getUserByGoogleIdFlow(googleId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users_db WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM follows")
    fun getAllFollows(): Flow<List<FollowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: FollowEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollows(follows: List<FollowEntity>): List<Long>

    @Delete
    suspend fun deleteFollow(follow: FollowEntity): Int

    @Query("DELETE FROM users_db")
    suspend fun deleteAllUsers(): Int
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
    suspend fun insertPost(post: PostEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPostCoffeeTag(tag: PostCoffeeTagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPostCoffeeTags(tags: List<PostCoffeeTagEntity>)

    @Query("DELETE FROM post_coffee_tags WHERE postId = :postId")
    suspend fun deletePostCoffeeTag(postId: String)

    @Query("DELETE FROM comments_db WHERE postId NOT IN (:keepPostIds)")
    suspend fun deleteCommentsForPostsNotIn(keepPostIds: List<String>): Int

    @Query("DELETE FROM likes_db WHERE postId NOT IN (:keepPostIds)")
    suspend fun deleteLikesForPostsNotIn(keepPostIds: List<String>): Int

    @Query("DELETE FROM post_coffee_tags WHERE postId NOT IN (:keepPostIds)")
    suspend fun deletePostCoffeeTagsForPostsNotIn(keepPostIds: List<String>): Int

    @Query("DELETE FROM posts_db WHERE id NOT IN (:keepPostIds)")
    suspend fun deletePostsNotIn(keepPostIds: List<String>): Int

    @Query("DELETE FROM comments_db")
    suspend fun deleteAllComments(): Int

    @Query("DELETE FROM likes_db")
    suspend fun deleteAllLikes(): Int

    @Query("DELETE FROM post_coffee_tags")
    suspend fun deleteAllPostCoffeeTags(): Int

    @Query("DELETE FROM posts_db")
    suspend fun deleteAllPosts(): Int

    @Delete
    suspend fun deletePost(post: PostEntity): Int

    @Transaction
    @Query("SELECT * FROM reviews_db ORDER BY timestamp DESC")
    fun getAllReviewsWithAuthor(): Flow<List<ReviewWithAuthor>>

    @Transaction
    @Query("SELECT * FROM reviews_db WHERE coffeeId = :coffeeId ORDER BY timestamp DESC")
    fun getReviewsWithAuthorForCoffee(coffeeId: String): Flow<List<ReviewWithAuthor>>

    @Transaction
    @Query("SELECT * FROM comments_db WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsWithAuthorForPost(postId: String): Flow<List<CommentWithAuthor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<CommentEntity>): List<Long>

    @Query("SELECT id FROM comments_db")
    suspend fun getAllCommentIds(): List<Int>

    @Query("DELETE FROM comments_db WHERE id IN (:commentIds)")
    suspend fun deleteCommentsByIds(commentIds: List<Int>): Int

    @Query("DELETE FROM comments_db WHERE id = :commentId")
    suspend fun deleteComment(commentId: Int): Int

    @Query("UPDATE comments_db SET text = :newText WHERE id = :commentId")
    suspend fun updateComment(commentId: Int, newText: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLike(like: LikeEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLikes(likes: List<LikeEntity>): List<Long>

    @Query("SELECT * FROM likes_db")
    suspend fun getAllLikesList(): List<LikeEntity>

    @Delete
    suspend fun deleteLike(like: LikeEntity): Int

    @Query("SELECT EXISTS(SELECT 1 FROM likes_db WHERE postId = :postId AND userId = :userId)")
    fun isPostLikedByUser(postId: String, userId: Int): Flow<Boolean>


    @Query("SELECT * FROM coffee_sensory_profiles WHERE coffeeId = :coffeeId")
    fun getSensoryProfilesForCoffee(coffeeId: String): Flow<List<CoffeeSensoryProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSensoryProfile(profile: CoffeeSensoryProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSensoryProfiles(profiles: List<CoffeeSensoryProfileEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReview(review: ReviewEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReviews(reviews: List<ReviewEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("SELECT * FROM notifications_db WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsForUser(userId: Int): Flow<List<NotificationEntity>>

    @Query("UPDATE notifications_db SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: Int): Int

    @Query("UPDATE notifications_db SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: Int): Int

    @Query("DELETE FROM notifications_db WHERE id = :notificationId")
    suspend fun deleteNotification(notificationId: Int): Int
}

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries WHERE userId = :userId ORDER BY timestamp DESC")
    fun getDiaryEntries(userId: Int): Flow<List<DiaryEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaryEntry(entry: DiaryEntryEntity): Long

    @Query("SELECT * FROM diary_entries WHERE id = :entryId LIMIT 1")
    suspend fun getDiaryEntryById(entryId: Long): DiaryEntryEntity?

    @Query("DELETE FROM diary_entries WHERE id = :entryId")
    suspend fun deleteDiaryEntryById(entryId: Long): Int

    @Query("SELECT * FROM pantry_items WHERE userId = :userId ORDER BY lastUpdated DESC")
    fun getPantryItems(userId: Int): Flow<List<PantryItemEntity>>

    @Query("SELECT * FROM pantry_items WHERE id = :id")
    suspend fun getPantryItemById(id: String): PantryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPantryItem(item: PantryItemEntity): Long

    @Query("DELETE FROM pantry_items WHERE userId = :userId AND id NOT IN (:keepIds)")
    suspend fun deletePantryItemsForUserNotIn(userId: Int, keepIds: List<String>): Int

    @Query("DELETE FROM pantry_items WHERE userId = :userId")
    suspend fun deleteAllPantryItemsForUser(userId: Int): Int

    @Query("DELETE FROM pantry_items WHERE id = :id")
    suspend fun deletePantryItemById(id: String): Int

    @Query("DELETE FROM diary_entries WHERE userId = :userId AND id NOT IN (:keepIds)")
    suspend fun deleteDiaryEntriesForUserNotIn(userId: Int, keepIds: List<Long>): Int

    @Query("DELETE FROM diary_entries WHERE userId = :userId")
    suspend fun deleteAllDiaryEntriesForUser(userId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPendingDiarySync(item: PendingDiarySyncEntity)

    @Query("SELECT * FROM pending_diary_sync ORDER BY createdAt ASC")
    suspend fun getPendingDiarySyncEntries(): List<PendingDiarySyncEntity>

    @Query("DELETE FROM pending_diary_sync WHERE localEntryId = :localEntryId")
    suspend fun deletePendingDiarySync(localEntryId: Long)
}

@Dao
interface FinishedCoffeeDao {
    @Query("SELECT * FROM finished_coffees WHERE userId = :userId ORDER BY finished_at DESC")
    fun getFinishedCoffeesByUser(userId: Int): Flow<List<FinishedCoffeeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinishedCoffee(entity: FinishedCoffeeEntity): Long

    @Query("DELETE FROM finished_coffees WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM finished_coffees WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Int): Int
}
