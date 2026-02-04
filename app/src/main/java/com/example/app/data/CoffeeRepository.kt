package com.cafesito.app.data

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.cafesito.app.ui.utils.ConnectivityObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Singleton
class CoffeeRepository @Inject constructor(
    private val coffeeDao: CoffeeDao,
    private val supabaseDataSource: SupabaseDataSource,
    private val userRepository: UserRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val externalScope: CoroutineScope
) {
    val favorites: Flow<List<LocalFavorite>> = coffeeDao.getLocalFavorites()
    val allReviews: Flow<List<ReviewEntity>> = coffeeDao.getAllReviews()

    fun getCoffeesPagingFlow(
        query: String? = null,
        origins: Set<String> = emptySet(),
        roasts: Set<String> = emptySet(),
        specialties: Set<String> = emptySet(),
        formats: Set<String> = emptySet(),
        minRating: Float = 0f
    ): Flow<PagingData<Coffee>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                CoffeePagingSource(supabaseDataSource, query, origins, roasts, specialties, formats, minRating)
            }
        ).flow
    }

    val allCoffees: Flow<List<CoffeeWithDetails>> = networkBoundResource(
        resourceKey = "all_coffees",
        query = { coffeeDao.getAllCoffeesWithDetails() },
        fetch = { 
            val user = userRepository.getActiveUser()
            val public = supabaseDataSource.getAllCoffees()
            val custom = user?.let { supabaseDataSource.getCustomCoffees(it.id).map { c -> c.toCoffee() } } ?: emptyList()
            val reviews = supabaseDataSource.getAllReviews()
            Triple(public, custom, reviews)
        },
        saveFetchResult = { (public, custom, reviews) ->
            withContext(Dispatchers.IO) {
                coffeeDao.insertCoffees(public + custom)
                reviews.forEach { coffeeDao.upsertReview(it) }
            }
        },
        scope = externalScope,
        connectivityObserver = connectivityObserver
    ).flowOn(Dispatchers.IO)

    fun getCoffeeWithDetailsById(id: String): Flow<CoffeeWithDetails?> = networkBoundResource(
        resourceKey = "coffee_detail_$id",
        query = { coffeeDao.getCoffeeWithDetailsById(id) },
        fetch = {
            val user = userRepository.getActiveUser()
            val coffee = supabaseDataSource.getCoffeesByIds(listOf(id)).firstOrNull()
                ?: user?.let { u -> supabaseDataSource.getCustomCoffees(u.id).find { it.id == id }?.toCoffee() }
            val reviews = supabaseDataSource.getReviewsByCoffeeId(id)
            Pair(coffee, reviews)
        },
        saveFetchResult = { (coffee, reviews) ->
            withContext(Dispatchers.IO) {
                coffee?.let { coffeeDao.insertCoffees(listOf(it)) }
                reviews.forEach { coffeeDao.upsertReview(it) }
            }
        },
        shouldFetch = { it == null },
        scope = externalScope,
        connectivityObserver = connectivityObserver
    ).flowOn(Dispatchers.IO)

    suspend fun toggleFavorite(coffeeId: String, shouldBeFavorite: Boolean) = withContext(Dispatchers.IO) {
        val currentUser = userRepository.getActiveUser() ?: return@withContext
        try {
            val isCustom = coffeeDao.getCoffeeById(coffeeId)?.isCustom ?: false
            val favorite = LocalFavorite(coffeeId, currentUser.id)
            val favoriteCustom = LocalFavoriteCustom(coffeeId, currentUser.id)

            if (shouldBeFavorite) {
                if (isCustom) coffeeDao.insertFavoriteCustom(favoriteCustom) else coffeeDao.insertFavorite(favorite)
            } else {
                if (isCustom) coffeeDao.deleteFavoriteCustom(favoriteCustom) else coffeeDao.deleteFavorite(favorite)
            }
            
            if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
                externalScope.launch {
                    try {
                         if (shouldBeFavorite) {
                            if (isCustom) supabaseDataSource.insertFavoriteCustom(favoriteCustom) else supabaseDataSource.insertFavorite(favorite)
                        } else {
                            if (isCustom) supabaseDataSource.deleteFavoriteCustom(coffeeId, currentUser.id) else supabaseDataSource.deleteFavorite(coffeeId, currentUser.id)
                        }
                    } catch (e: Exception) { }
                }
            }
        } catch (e: Exception) { }
    }
    
    suspend fun upsertReview(review: ReviewEntity) = withContext(Dispatchers.IO) {
        coffeeDao.upsertReview(review)
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch { try { supabaseDataSource.upsertReview(review) } catch (e: Exception) { } }
        }
    }

    suspend fun deleteReview(coffeeId: String, userId: Int) = withContext(Dispatchers.IO) {
        coffeeDao.deleteReviewByUser(coffeeId, userId)
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
             externalScope.launch { try { supabaseDataSource.deleteReview(coffeeId, userId) } catch (e: Exception) { } }
        }
    }

    suspend fun syncCoffees() {
        val public = supabaseDataSource.getAllCoffees()
        coffeeDao.insertCoffees(public)
    }

    fun triggerRefresh() {
        // Implementación opcional
    }
}
