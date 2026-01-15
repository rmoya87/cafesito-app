package com.example.cafesito.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoffeeRepository @Inject constructor(
    private val coffeeDao: CoffeeDao,
    private val supabaseDataSource: SupabaseDataSource,
    private val userRepository: UserRepository
) {
    // CONSULTAS DIRECTAS A SUPABASE
    val allCoffees: Flow<List<CoffeeWithDetails>> = flow {
        try {
            val remoteCoffees = supabaseDataSource.getAllCoffees()
            val remoteFavorites = supabaseDataSource.getAllFavorites()
            val remoteReviews = supabaseDataSource.getAllReviews()
            val currentUser = userRepository.getActiveUser()

            val details = remoteCoffees.map { coffee ->
                CoffeeWithDetails(
                    coffee = coffee,
                    favorite = remoteFavorites.find { it.coffeeId == coffee.id && it.userId == currentUser?.id },
                    reviews = remoteReviews.filter { it.coffeeId == coffee.id }
                )
            }
            emit(details)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    val favorites: Flow<List<LocalFavorite>> = flow {
        val currentUser = userRepository.getActiveUser() ?: return@flow
        try {
            val remoteFavorites = supabaseDataSource.getAllFavorites()
            emit(remoteFavorites.filter { it.userId == currentUser.id })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    val allReviews: Flow<List<ReviewEntity>> = flow {
        try {
            emit(supabaseDataSource.getAllReviews())
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    fun getCoffeeWithDetailsById(id: String): Flow<CoffeeWithDetails?> = flow {
        try {
            val coffee = supabaseDataSource.getAllCoffees().find { it.id == id } ?: return@flow
            val remoteFavorites = supabaseDataSource.getAllFavorites()
            val remoteReviews = supabaseDataSource.getAllReviews()
            val currentUser = userRepository.getActiveUser()

            emit(CoffeeWithDetails(
                coffee = coffee,
                favorite = remoteFavorites.find { it.coffeeId == coffee.id && it.userId == currentUser?.id },
                reviews = remoteReviews.filter { it.coffeeId == coffee.id }
            ))
        } catch (e: Exception) {
            emit(null)
        }
    }

    fun getFilteredCoffees(
        query: String?,
        origin: String?,
        roast: String?,
        specialty: String?,
        variety: String?,
        format: String?,
        grind: String?
    ): Flow<List<CoffeeWithDetails>> = flow {
        // En remoto aplicamos filtros básicos por ahora para simplificar el cambio radical
        try {
            val remoteCoffees = supabaseDataSource.getAllCoffees().filter { coffee ->
                val matchQuery = query == null || coffee.nombre.contains(query, true) || coffee.marca.contains(query, true)
                val matchOrigin = origin == null || coffee.paisOrigen == origin
                val matchRoast = roast == null || coffee.tueste == roast
                matchQuery && matchOrigin && matchRoast
            }
            val remoteFavorites = supabaseDataSource.getAllFavorites()
            val remoteReviews = supabaseDataSource.getAllReviews()
            val currentUser = userRepository.getActiveUser()

            val details = remoteCoffees.map { coffee ->
                CoffeeWithDetails(
                    coffee = coffee,
                    favorite = remoteFavorites.find { it.coffeeId == coffee.id && it.userId == currentUser?.id },
                    reviews = remoteReviews.filter { it.coffeeId == coffee.id }
                )
            }
            emit(details)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun toggleFavorite(coffeeId: String, isFavorite: Boolean) {
        val currentUser = userRepository.getActiveUser() ?: return
        val favorite = LocalFavorite(coffeeId, currentUser.id, System.currentTimeMillis())

        try {
            if (isFavorite) {
                supabaseDataSource.insertFavorite(favorite)
            } else {
                supabaseDataSource.deleteFavorite(coffeeId, currentUser.id)
            }
        } catch (e: Exception) {
            Log.e("COFFEE_REPO", "Error toggling favorite in Supabase")
        }
    }

    suspend fun upsertReview(review: ReviewEntity) {
        try {
            supabaseDataSource.upsertReview(review)
        } catch (e: Exception) {
            Log.e("COFFEE_REPO", "Error upsert review in Supabase")
        }
    }

    suspend fun deleteReview(coffeeId: String, userId: Int) {
        try {
            supabaseDataSource.deleteReview(coffeeId, userId)
        } catch (e: Exception) {
            Log.e("COFFEE_REPO", "Error delete review in Supabase")
        }
    }
    
    suspend fun syncCoffees() {
        // Obsoleto pero mantenemos por compatibilidad de llamadas existentes
    }
}
