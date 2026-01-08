package com.example.cafesito.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CoffeeDao {
    @Transaction
    @Query("""
        SELECT * FROM coffees
        WHERE (:query = '' OR name LIKE '%' || :query || '%' OR brandRoaster LIKE '%' || :query || '%')
        AND officialScore >= :minScore
        AND (:originId IS NULL OR originId = :originId)
    """)
    fun getFilteredCoffees(query: String, minScore: Float, originId: Int?): Flow<List<CoffeeWithDetails>>

    @Transaction
    @Query("SELECT * FROM coffees")
    fun getAllCoffees(): Flow<List<CoffeeWithDetails>>

    @Transaction
    @Query("SELECT * FROM coffees WHERE id = :id")
    fun getCoffeeById(id: Int): Flow<CoffeeWithDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoffee(coffee: Coffee): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSensoryProfile(profile: SensoryProfile)
    
    // Favorites
    @Query("SELECT * FROM local_favorites")
    fun getFavorites(): Flow<List<LocalFavorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: LocalFavorite)

    @Query("DELETE FROM local_favorites WHERE coffeeId = :coffeeId")
    suspend fun removeFavorite(coffeeId: Int)
}

@Dao
interface OriginDao {
    @Query("SELECT * FROM origins")
    fun getAllOrigins(): Flow<List<Origin>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrigin(origin: Origin): Long
}

@Dao
interface ScoreSourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScoreSource(source: ScoreSource): Long
}

// Data Class for joining tables easily in UI
data class CoffeeWithDetails(
    @androidx.room.Embedded val coffee: Coffee,
    @androidx.room.Relation(
        parentColumn = "originId",
        entityColumn = "id"
    )
    val origin: Origin?,
    @androidx.room.Relation(
        parentColumn = "scoreSourceId",
        entityColumn = "id"
    )
    val scoreSource: ScoreSource?,
    @androidx.room.Relation(
        parentColumn = "id",
        entityColumn = "coffeeId"
    )
    val sensoryProfile: SensoryProfile?
)
