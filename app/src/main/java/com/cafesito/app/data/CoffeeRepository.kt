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
    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    fun triggerRefresh() {
        _refreshTrigger.tryEmit(Unit)
    }

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

    val allCoffees: Flow<List<CoffeeWithDetails>> = _refreshTrigger
        .flatMapLatest {
            networkBoundResource(
                resourceKey = "all_coffees",
                query = { coffeeDao.getAllCoffeesWithDetails() },
                fetch = {
                    val user = userRepository.getActiveUser()
                    val public = supabaseDataSource.getAllCoffees()
                    val custom = user?.let { supabaseDataSource.getCustomCoffees(it.id) } ?: emptyList()
                    val reviews = supabaseDataSource.getAllReviews()
                    Triple(public, custom, reviews)
                },
                saveFetchResult = { (public, custom, reviews) ->
                    withContext(Dispatchers.IO) {
                        coffeeDao.insertCoffees(public + custom)
                        coffeeDao.upsertReviews(reviews)
                    }
                },
                scope = externalScope,
                connectivityObserver = connectivityObserver
            )
        }.flowOn(Dispatchers.IO)

    fun getCoffeeWithDetailsById(id: String): Flow<CoffeeWithDetails?> = _refreshTrigger
        .flatMapLatest {
            networkBoundResource(
                resourceKey = "coffee_detail_$id",
                query = { coffeeDao.getCoffeeWithDetailsById(id) },
                fetch = {
                    val user = userRepository.getActiveUser()
                    val coffee = supabaseDataSource.getCoffeesByIds(listOf(id)).firstOrNull()
                        ?: user?.let { u -> supabaseDataSource.getCustomCoffees(u.id).find { it.id == id } }
                    val reviews = supabaseDataSource.getReviewsByCoffeeId(id)
                    Pair(coffee, reviews)
                },
                saveFetchResult = { (coffee, reviews) ->
                    withContext(Dispatchers.IO) {
                        coffee?.let { coffeeDao.insertCoffees(listOf(it)) }
                        // Sincronizamos las reseñas locales tras la carga
                        coffeeDao.upsertReviews(reviews)
                    }
                },
                shouldFetch = { true },
                scope = externalScope,
                connectivityObserver = connectivityObserver
            )
        }.flowOn(Dispatchers.IO)

    suspend fun toggleFavorite(coffeeId: String, shouldBeFavorite: Boolean) = withContext(Dispatchers.IO) {
        val currentUser = userRepository.getActiveUser() ?: return@withContext
        try {
            val favorite = LocalFavorite(coffeeId, currentUser.id)

            if (shouldBeFavorite) {
                coffeeDao.insertFavorite(favorite)
            } else {
                coffeeDao.deleteFavorite(favorite)
            }

            if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
                externalScope.launch {
                    try {
                        if (shouldBeFavorite) {
                            supabaseDataSource.insertFavorite(favorite)
                        } else {
                            supabaseDataSource.deleteFavorite(coffeeId, currentUser.id)
                        }
                    } catch (e: Exception) { }
                }
            }
        } catch (e: Exception) { }
    }

    suspend fun syncFavoritesFromRemote() = withContext(Dispatchers.IO) {
        val currentUser = userRepository.getActiveUser() ?: return@withContext
        if (connectivityObserver.observe().first() != ConnectivityObserver.Status.Available) return@withContext
        try {
            val remoteFavorites = supabaseDataSource.getFavoritesByUserId(currentUser.id)
            coffeeDao.deleteFavoritesByUserId(currentUser.id)
            if (remoteFavorites.isNotEmpty()) coffeeDao.insertFavorites(remoteFavorites)
        } catch (e: Exception) {
            Log.w("CoffeeRepository", "syncFavoritesFromRemote failed", e)
        }
    }
    
    suspend fun upsertReview(review: ReviewEntity) = withContext(Dispatchers.IO) {
        coffeeDao.upsertReview(review) // Actualizamos local primero para UI fluida
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch { try { supabaseDataSource.upsertReview(review) } catch (e: Exception) { } }
        }
    }

    suspend fun deleteReview(coffeeId: String, userId: Int) = withContext(Dispatchers.IO) {
        coffeeDao.deleteReviewByUser(coffeeId, userId) // ✅ IMPORTANTE: Borramos de Room para que desaparezca del Timeline
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
             externalScope.launch { try { supabaseDataSource.deleteReview(coffeeId, userId) } catch (e: Exception) { } }
        }
    }

    suspend fun syncCoffees() {
        val user = userRepository.getActiveUser()
        val public = supabaseDataSource.getAllCoffees()
        val custom = user?.let { supabaseDataSource.getCustomCoffees(it.id) } ?: emptyList()
        coffeeDao.insertCoffees(public + custom)
        triggerRefresh()
    }

    suspend fun findCoffeeIdByBarcode(rawBarcode: String): String? = withContext(Dispatchers.IO) {
        val normalized = rawBarcode.filter { it.isDigit() }
        if (normalized.isBlank()) return@withContext null

        val remoteId = try {
            if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
                supabaseDataSource.getCoffeeByBarcode(normalized)?.id
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }

        if (remoteId != null) return@withContext remoteId

        coffeeDao.getAllCoffeesWithDetails().first()
            .firstOrNull { it.coffee.codigoBarras?.filter { ch -> ch.isDigit() } == normalized }
            ?.coffee
            ?.id
    }
}
