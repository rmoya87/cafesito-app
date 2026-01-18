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
    // CONSULTAS UNIFICADAS (PÚBLICAS + PRIVADAS)
    val allCoffees: Flow<List<CoffeeWithDetails>> = flow {
        try {
            val user = userRepository.getActiveUser()
            val publicCoffees = try { supabaseDataSource.getAllCoffees() } catch (e: Exception) { emptyList() }
            val customCoffees = if (user != null) {
                try { supabaseDataSource.getCustomCoffees(user.id).map { it.toCoffee() } } catch (e: Exception) { emptyList() }
            } else emptyList()

            val allAvailable = publicCoffees + customCoffees
            val remoteFavorites = try { supabaseDataSource.getAllFavorites() } catch (e: Exception) { emptyList() }
            val remoteReviews = try { supabaseDataSource.getAllReviews() } catch (e: Exception) { emptyList() }

            val details = allAvailable.map { coffee ->
                CoffeeWithDetails(
                    coffee = coffee,
                    favorite = remoteFavorites.find { it.coffeeId == coffee.id && it.userId == user?.id },
                    reviews = remoteReviews.filter { it.coffeeId == coffee.id }
                )
            }
            emit(details)
        } catch (e: Exception) {
            Log.e("COFFEE_REPO", "Error allCoffees: ${e.message}")
            emit(emptyList())
        }
    }

    val favorites: Flow<List<LocalFavorite>> = flow {
        val currentUser = userRepository.getActiveUser() ?: return@flow
        try {
            val remoteFavorites = supabaseDataSource.getAllFavorites()
            emit(remoteFavorites.filter { it.userId == currentUser.id })
        } catch (e: Exception) { emit(emptyList()) }
    }

    val allReviews: Flow<List<ReviewEntity>> = flow {
        try {
            emit(supabaseDataSource.getAllReviews())
        } catch (e: Exception) { emit(emptyList()) }
    }

    fun getFilteredCoffees(
        query: String? = null,
        origin: String? = null,
        roast: String? = null,
        specialty: String? = null,
        variety: String? = null,
        format: String? = null,
        grind: String? = null
    ): Flow<List<CoffeeWithDetails>> = flow {
        try {
            val user = userRepository.getActiveUser()
            val publicCoffees = try { supabaseDataSource.getAllCoffees() } catch (e: Exception) { emptyList() }
            val customCoffees = if (user != null) {
                try { supabaseDataSource.getCustomCoffees(user.id).map { it.toCoffee() } } catch (e: Exception) { emptyList() }
            } else emptyList()

            val filtered = (publicCoffees + customCoffees).filter { coffee ->
                val matchQuery = query.isNullOrBlank() || coffee.nombre.contains(query, true) || coffee.marca.contains(query, true)
                val matchOrigin = origin == null || coffee.paisOrigen == origin
                val matchRoast = roast == null || coffee.tueste == roast
                matchQuery && matchOrigin && matchRoast
            }

            val remoteFavorites = try { supabaseDataSource.getAllFavorites() } catch (e: Exception) { emptyList() }
            val remoteReviews = try { supabaseDataSource.getAllReviews() } catch (e: Exception) { emptyList() }

            val details = filtered.map { coffee ->
                CoffeeWithDetails(
                    coffee = coffee,
                    favorite = remoteFavorites.find { it.coffeeId == coffee.id && it.userId == user?.id },
                    reviews = remoteReviews.filter { it.coffeeId == coffee.id }
                )
            }
            emit(details)
        } catch (e: Exception) { emit(emptyList()) }
    }

    fun getCoffeeWithDetailsById(id: String): Flow<CoffeeWithDetails?> = flow {
        try {
            val user = userRepository.getActiveUser()
            val publicCoffees = try { supabaseDataSource.getAllCoffees() } catch (e: Exception) { emptyList() }
            val customCoffees = if (user != null) {
                try { supabaseDataSource.getCustomCoffees(user.id).map { it.toCoffee() } } catch (e: Exception) { emptyList() }
            } else emptyList()
            
            val coffee = (publicCoffees + customCoffees).find { it.id == id } ?: return@flow
            val remoteFavorites = supabaseDataSource.getAllFavorites()
            val remoteReviews = supabaseDataSource.getAllReviews()

            emit(CoffeeWithDetails(
                coffee = coffee,
                favorite = remoteFavorites.find { it.coffeeId == coffee.id && it.userId == user?.id },
                reviews = remoteReviews.filter { it.coffeeId == coffee.id }
            ))
        } catch (e: Exception) { emit(null) }
    }

    fun getRecommendations(): Flow<List<CoffeeWithDetails>> = flow {
        try {
            val remoteCoffees = supabaseDataSource.getAllCoffees()
            val remoteFavorites = supabaseDataSource.getAllFavorites()
            val remoteReviews = supabaseDataSource.getAllReviews()
            val currentUser = userRepository.getActiveUser() ?: return@flow emit(emptyList())

            val favIds = remoteFavorites.filter { it.userId == currentUser.id }.map { it.coffeeId }.toSet()
            val myFavs = remoteCoffees.filter { favIds.contains(it.id) }

            if (myFavs.isEmpty()) {
                emit(emptyList())
                return@flow
            }

            val avgAroma = myFavs.map { it.aroma }.average()
            val avgSabor = myFavs.map { it.sabor }.average()
            val recommendations = remoteCoffees
                .filter { !favIds.contains(it.id) }
                .map { coffee ->
                    val distance = kotlin.math.sqrt(Math.pow((coffee.aroma - avgAroma), 2.0) + Math.pow((coffee.sabor - avgSabor), 2.0))
                    coffee to distance
                }
                .sortedBy { it.second }.take(5).map { (coffee, _) ->
                    CoffeeWithDetails(coffee = coffee, favorite = null, reviews = remoteReviews.filter { it.coffeeId == coffee.id })
                }
            emit(recommendations)
        } catch (e: Exception) { emit(emptyList()) }
    }

    suspend fun toggleFavorite(coffeeId: String, isFavorite: Boolean) {
        val currentUser = userRepository.getActiveUser() ?: return
        val favorite = LocalFavorite(coffeeId, currentUser.id, System.currentTimeMillis())
        try {
            if (isFavorite) supabaseDataSource.insertFavorite(favorite)
            else supabaseDataSource.deleteFavorite(coffeeId, currentUser.id)
        } catch (e: Exception) { Log.e("COFFEE_REPO", "Error favorite") }
    }

    suspend fun upsertReview(review: ReviewEntity) {
        try { supabaseDataSource.upsertReview(review) } catch (e: Exception) { }
    }

    suspend fun deleteReview(coffeeId: String, userId: Int) {
        try { supabaseDataSource.deleteReview(coffeeId, userId) } catch (e: Exception) { }
    }

    suspend fun syncCoffees() {
        try {
            val remoteCoffees = supabaseDataSource.getAllCoffees()
            coffeeDao.insertCoffees(remoteCoffees)
            Log.d("COFFEE_REPO", "Catálogo sincronizado: ${remoteCoffees.size} cafés.")
        } catch (e: Exception) {
            Log.e("COFFEE_REPO", "Error al sincronizar catálogo", e)
        }
    }
}
