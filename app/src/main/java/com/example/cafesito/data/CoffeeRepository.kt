package com.example.cafesito.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoffeeRepository @Inject constructor(
    private val coffeeDao: CoffeeDao
) {
    val allCoffees: Flow<List<CoffeeWithDetails>> = coffeeDao.getAllCoffeesWithDetails()
    val favorites: Flow<List<LocalFavorite>> = coffeeDao.getLocalFavorites()
    val allReviews: Flow<List<ReviewEntity>> = coffeeDao.getAllReviews()

    suspend fun getCoffeeById(id: String): Coffee? = coffeeDao.getCoffeeById(id)
    
    fun getCoffeeWithDetailsById(id: String): Flow<CoffeeWithDetails?> = coffeeDao.getCoffeeWithDetailsById(id)

    fun getFilteredCoffees(
        query: String?, 
        origin: String?,
        roast: String?,
        specialty: String?,
        variety: String?,
        format: String?,
        grind: String?
    ): Flow<List<CoffeeWithDetails>> = 
        coffeeDao.getFilteredCoffees(query, origin, roast, specialty, variety, format, grind)

    suspend fun toggleFavorite(coffeeId: String, isFavorite: Boolean) {
        if (isFavorite) {
            coffeeDao.deleteFavorite(LocalFavorite(coffeeId, 0))
        } else {
            coffeeDao.insertFavorite(LocalFavorite(coffeeId, System.currentTimeMillis()))
        }
    }

    suspend fun upsertReview(review: ReviewEntity) = coffeeDao.upsertReview(review)

    suspend fun insertCoffee(coffee: Coffee) = coffeeDao.insertCoffee(coffee)
    
    suspend fun deleteAllCoffees() = coffeeDao.deleteAllCoffees()
}
