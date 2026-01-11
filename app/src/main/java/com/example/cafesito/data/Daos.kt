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

    @Query("SELECT COUNT(*) FROM users_db")
    suspend fun getUserCount(): Int

    @Query("SELECT * FROM users_db WHERE id = :userId")
    suspend fun getUserById(userId: Int): UserEntity?

    @Query("SELECT * FROM follows")
    fun getAllFollows(): Flow<List<FollowEntity>>

    @Query("SELECT * FROM follows")
    suspend fun getAllFollowsList(): List<FollowEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: FollowEntity)

    @Delete
    suspend fun deleteFollow(follow: FollowEntity)
}
