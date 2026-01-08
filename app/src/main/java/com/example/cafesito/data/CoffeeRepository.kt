package com.example.cafesito.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoffeeRepository @Inject constructor(
    private val coffeeDao: CoffeeDao,
    private val originDao: OriginDao,
    private val scoreSourceDao: ScoreSourceDao
) {
    val allCoffees: Flow<List<CoffeeWithDetails>> = coffeeDao.getAllCoffeesWithDetails()
    val origins: Flow<List<Origin>> = originDao.getAllOrigins()
    val favorites: Flow<List<LocalFavorite>> = coffeeDao.getLocalFavorites()

    suspend fun getCoffeeById(id: Int): Coffee? = coffeeDao.getCoffeeById(id)
    
    fun getCoffeeWithDetailsById(id: Int): Flow<CoffeeWithDetails?> = coffeeDao.getCoffeeWithDetailsById(id)

    fun getFilteredCoffees(query: String?, minScore: Float, originId: Int?): Flow<List<CoffeeWithDetails>> = 
        coffeeDao.getFilteredCoffees(query, minScore, originId)

    fun getSensoryProfile(coffeeId: Int): Flow<SensoryProfile?> = coffeeDao.getSensoryProfile(coffeeId)

    suspend fun toggleFavorite(coffeeId: Int, isFavorite: Boolean) {
        if (isFavorite) {
            coffeeDao.deleteFavorite(LocalFavorite(coffeeId, 0))
        } else {
            coffeeDao.insertFavorite(LocalFavorite(coffeeId, System.currentTimeMillis()))
        }
    }

    suspend fun insertOrigin(origin: Origin): Long = originDao.insertOrigins(listOf(origin)).firstOrNull() ?: -1L
    suspend fun insertScoreSource(source: ScoreSource): Long = scoreSourceDao.insertSources(listOf(source)).firstOrNull() ?: -1L
    suspend fun insertCoffee(coffee: Coffee): Long = coffeeDao.insertCoffee(coffee)
    suspend fun insertSensoryProfile(profile: SensoryProfile) = coffeeDao.insertSensoryProfile(profile)
}
