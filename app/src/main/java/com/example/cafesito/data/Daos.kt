package com.example.cafesito.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CoffeeDao {
    @Transaction
    @Query("SELECT * FROM coffees")
    fun getAllCoffeesWithDetails(): Flow<List<CoffeeWithDetails>>

    @Transaction
    @Query("SELECT * FROM coffees WHERE id = :id")
    fun getCoffeeWithDetailsById(id: Int): Flow<CoffeeWithDetails?>

    @Transaction
    @Query("""
        SELECT * FROM coffees 
        WHERE (:query IS NULL OR name LIKE '%' || :query || '%' OR brandRoaster LIKE '%' || :query || '%')
        AND officialScore >= :minScore
        AND (:originId IS NULL OR originId = :originId)
    """)
    fun getFilteredCoffees(query: String?, minScore: Float, originId: Int?): Flow<List<CoffeeWithDetails>>

    @Query("SELECT * FROM coffees WHERE id = :id")
    suspend fun getCoffeeById(id: Int): Coffee?

    @Query("SELECT * FROM sensory_profiles WHERE coffeeId = :coffeeId")
    fun getSensoryProfile(coffeeId: Int): Flow<SensoryProfile?>

    @Query("SELECT * FROM local_favorites")
    fun getLocalFavorites(): Flow<List<LocalFavorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: LocalFavorite)

    @Delete
    suspend fun deleteFavorite(favorite: LocalFavorite)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoffee(coffee: Coffee): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSensoryProfile(profile: SensoryProfile)
}

@Dao
interface OriginDao {
    @Query("SELECT * FROM origins")
    fun getAllOrigins(): Flow<List<Origin>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrigins(origins: List<Origin>): List<Long>
}

@Dao
interface ScoreSourceDao {
    @Query("SELECT * FROM score_sources")
    fun getAllSources(): Flow<List<ScoreSource>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSources(sources: List<ScoreSource>): List<Long>
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
