package com.example.cafesito.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoffeeRepository @Inject constructor(
    private val coffeeDao: CoffeeDao,
    private val originDao: OriginDao,
    private val scoreSourceDao: ScoreSourceDao
) {
    val allCoffees: Flow<List<CoffeeWithDetails>> = coffeeDao.getAllCoffees()
    val favorites: Flow<List<LocalFavorite>> = coffeeDao.getFavorites()
    val origins: Flow<List<Origin>> = originDao.getAllOrigins()

    fun getFilteredCoffees(query: String, minScore: Float, originId: Int?): Flow<List<CoffeeWithDetails>> {
        return coffeeDao.getFilteredCoffees(query, minScore, originId)
    }

    fun getCoffeeById(id: Int): Flow<CoffeeWithDetails?> = coffeeDao.getCoffeeById(id)

    suspend fun toggleFavorite(coffeeId: Int, isCurrentlyFavorite: Boolean) {
        if (isCurrentlyFavorite) {
            // Si ya es favorito, lo quitamos
            coffeeDao.removeFavorite(coffeeId)
        } else {
            // Si no es favorito, lo añadimos
            coffeeDao.addFavorite(LocalFavorite(coffeeId, System.currentTimeMillis()))
        }
    }
    
    suspend fun insertCoffee(coffee: Coffee) = coffeeDao.insertCoffee(coffee)
    suspend fun insertOrigin(origin: Origin) = originDao.insertOrigin(origin)
    suspend fun insertScoreSource(source: ScoreSource) = scoreSourceDao.insertScoreSource(source)
    suspend fun insertSensoryProfile(profile: SensoryProfile) = coffeeDao.insertSensoryProfile(profile)
}
