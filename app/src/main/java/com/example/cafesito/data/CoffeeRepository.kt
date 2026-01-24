package com.example.cafesito.data

import android.util.Log
import com.example.cafesito.ui.utils.ConnectivityObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoffeeRepository @Inject constructor(
    private val coffeeDao: CoffeeDao,
    private val supabaseDataSource: SupabaseDataSource,
    private val userRepository: UserRepository,
    private val connectivityObserver: ConnectivityObserver
) {
    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    private suspend fun ensureConnected() {
        if (connectivityObserver.observe().first() != ConnectivityObserver.Status.Available) {
            throw NoConnectivityException("No hay conexión a internet.")
        }
    }

    fun triggerRefresh() {
        _refreshTrigger.tryEmit(Unit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allCoffees: Flow<List<CoffeeWithDetails>> = _refreshTrigger.flatMapLatest {
        flow {
            try {
                ensureConnected()
                val user = userRepository.getActiveUser()
                val publicCoffees = try { supabaseDataSource.getAllCoffees() } catch (_: Exception) { emptyList() }
                val customCoffees = if (user != null) {
                    try { supabaseDataSource.getCustomCoffees(user.id).map { it.toCoffee() } } catch (_: Exception) { emptyList() }
                } else emptyList()
                
                val totalCoffees = publicCoffees + customCoffees
                val remoteFavorites = try { supabaseDataSource.getAllFavorites() } catch (_: Exception) { emptyList() }
                val remoteFavoritesCustom = try { supabaseDataSource.getAllFavoritesCustom() } catch (_: Exception) { emptyList() }
                val localFavorites = coffeeDao.getLocalFavorites().first()
                val localFavoritesCustom = coffeeDao.getLocalFavoritesCustom().first()
                val allFavs = (remoteFavorites + remoteFavoritesCustom.map { it.toLocalFavorite() } + localFavorites + localFavoritesCustom.map { it.toLocalFavorite() }).distinctBy { it.coffeeId }

                // ✅ Cargar reseñas para que el promedio no sea 0
                val remoteReviews = try { supabaseDataSource.getAllReviews() } catch (_: Exception) { emptyList() }

                emit(totalCoffees.map { coffee ->
                    CoffeeWithDetails(
                        coffee = coffee,
                        favorite = allFavs.find { it.coffeeId == coffee.id && it.userId == user?.id },
                        reviews = remoteReviews.filter { it.coffeeId == coffee.id }
                    )
                })
            } catch (e: Exception) {
                Log.e("COFFEE_REPO", "Error allCoffees: ${e.message}")
                emit(emptyList())
            }
        }.flowOn(Dispatchers.IO)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val favorites: Flow<List<LocalFavorite>> = combine(
        _refreshTrigger.flatMapLatest {
            flow {
                ensureConnected()
                val user = userRepository.getActiveUser() ?: return@flow emit(emptyList())
                try {
                    val remoteStd = supabaseDataSource.getAllFavorites()
                    val remoteCustom = try { supabaseDataSource.getAllFavoritesCustom() } catch (_: Exception) { emptyList() }
                    val allRemote = remoteStd + remoteCustom.map { it.toLocalFavorite() }
                    emit(allRemote.filter { it.userId == user.id })
                } catch (_: Exception) { emit(emptyList()) }
            }.flowOn(Dispatchers.IO)
        },
        coffeeDao.getLocalFavorites(),
        coffeeDao.getLocalFavoritesCustom()
    ) { remote, local, localCustom ->
        (remote + local + localCustom.map { it.toLocalFavorite() }).distinctBy { it.coffeeId }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allReviews: Flow<List<ReviewEntity>> = _refreshTrigger.flatMapLatest {
        flow {
            try {
                ensureConnected()
                emit(supabaseDataSource.getAllReviews())
            } catch (e: Exception) {
                Log.e("COFFEE_REPO", "Error cargando reseñas: ${e.message}")
                emit(emptyList())
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun toggleFavorite(coffeeId: String, shouldBeFavorite: Boolean) = withContext(Dispatchers.IO) {
        ensureConnected()
        val currentUser = userRepository.getActiveUser() ?: return@withContext
        try {
            val isCustom = coffeeDao.getCoffeeById(coffeeId)?.isCustom 
                ?: (supabaseDataSource.getCustomCoffees(currentUser.id).any { it.id == coffeeId })

            if (shouldBeFavorite) {
                if (isCustom) {
                    val favCustom = LocalFavoriteCustom(coffeeId, currentUser.id)
                    coffeeDao.insertFavoriteCustom(favCustom)
                    try { supabaseDataSource.insertFavoriteCustom(favCustom) } catch (_: Exception) { }
                } else {
                    val favorite = LocalFavorite(coffeeId, currentUser.id)
                    coffeeDao.insertFavorite(favorite)
                    try { supabaseDataSource.insertFavorite(favorite) } catch (_: Exception) { }
                }
            } else {
                if (isCustom) {
                    coffeeDao.deleteFavoriteCustom(LocalFavoriteCustom(coffeeId, currentUser.id))
                    try { supabaseDataSource.deleteFavoriteCustom(coffeeId, currentUser.id) } catch (_: Exception) { }
                } else {
                    coffeeDao.deleteFavorite(LocalFavorite(coffeeId, currentUser.id))
                    try { supabaseDataSource.deleteFavorite(coffeeId, currentUser.id) } catch (_: Exception) { }
                }
            }
            triggerRefresh()
        } catch (e: Exception) {
            Log.e("COFFEE_REPO", "Error general en toggleFavorite", e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCoffeeWithDetailsById(id: String): Flow<CoffeeWithDetails?> = _refreshTrigger.flatMapLatest {
        flow {
            try {
                ensureConnected()
                val user = userRepository.getActiveUser()
                val publicCoffees = try { supabaseDataSource.getAllCoffees() } catch (_: Exception) { emptyList() }
                val customCoffees = if (user != null) {
                    try { supabaseDataSource.getCustomCoffees(user.id).map { it.toCoffee() } } catch (_: Exception) { emptyList() }
                } else emptyList()
                
                val coffee = (publicCoffees + customCoffees).find { it.id == id } ?: return@flow emit(null)
                
                val remoteFavorites = try { supabaseDataSource.getAllFavorites() } catch (_: Exception) { emptyList() }
                val remoteFavoritesCustom = try { supabaseDataSource.getAllFavoritesCustom() } catch (_: Exception) { emptyList() }
                val localFavorites = coffeeDao.getLocalFavorites().first()
                val localFavoritesCustom = coffeeDao.getLocalFavoritesCustom().first()
                val allFavs = (remoteFavorites + remoteFavoritesCustom.map { it.toLocalFavorite() } + localFavorites + localFavoritesCustom.map { it.toLocalFavorite() }).distinctBy { it.coffeeId }

                val remoteReviews = try { supabaseDataSource.getAllReviews() } catch (_: Exception) { emptyList() }

                emit(CoffeeWithDetails(
                    coffee = coffee,
                    favorite = allFavs.find { it.coffeeId == coffee.id && it.userId == user?.id },
                    reviews = remoteReviews.filter { it.coffeeId == coffee.id }
                ))
            } catch (_: Exception) { emit(null) }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun upsertReview(review: ReviewEntity) = withContext(Dispatchers.IO) {
        try { 
            ensureConnected()
            supabaseDataSource.upsertReview(review) 
            triggerRefresh()
        } catch (e: Exception) { 
            Log.e("COFFEE_REPO", "ERROR AL GUARDAR RESEÑA EN SUPABASE: ${e.message}", e)
        }
    }

    suspend fun deleteReview(coffeeId: String, userId: Int) = withContext(Dispatchers.IO) {
        try { 
            ensureConnected()
            supabaseDataSource.deleteReview(coffeeId, userId) 
            triggerRefresh()
        } catch (e: Exception) { 
            Log.e("COFFEE_REPO", "Error al borrar reseña: ${e.message}")
        }
    }

    suspend fun syncCoffees() = withContext(Dispatchers.IO) {
        try {
            ensureConnected()
            triggerRefresh()
        } catch (e: Exception) {
            Log.e("COFFEE_REPO", "Error al sincronizar catálogo", e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getFilteredCoffees(
        query: String? = null,
        origin: String? = null,
        roast: String? = null
    ): Flow<List<CoffeeWithDetails>> = _refreshTrigger.flatMapLatest {
        flow {
            try {
                ensureConnected()
                val user = userRepository.getActiveUser()
                val publicCoffees = try { supabaseDataSource.getAllCoffees() } catch (_: Exception) { emptyList() }
                val filtered = publicCoffees.filter { coffee ->
                    val matchQuery = query.isNullOrBlank() || coffee.nombre.contains(query, true) || coffee.marca.contains(query, true)
                    val matchOrigin = origin == null || coffee.paisOrigen == origin
                    val matchRoast = roast == null || coffee.tueste == roast
                    matchQuery && matchOrigin && matchRoast
                }
                val remoteFavorites = try { supabaseDataSource.getAllFavorites() } catch (_: Exception) { emptyList() }
                val remoteFavoritesCustom = try { supabaseDataSource.getAllFavoritesCustom() } catch (_: Exception) { emptyList() }
                val allRemoteFavs = (remoteFavorites + remoteFavoritesCustom.map { it.toLocalFavorite() })
                val remoteReviews = try { supabaseDataSource.getAllReviews() } catch (_: Exception) { emptyList() }
                emit(filtered.map { coffee ->
                    CoffeeWithDetails(
                        coffee = coffee,
                        favorite = allRemoteFavs.find { it.coffeeId == coffee.id && it.userId == user?.id },
                        reviews = remoteReviews.filter { it.coffeeId == coffee.id }
                    )
                })
            } catch (_: Exception) { emit(emptyList()) }
        }.flowOn(Dispatchers.IO)
    }

    fun getRecommendations(): Flow<List<CoffeeWithDetails>> = flow {
        try {
            ensureConnected()
            val remoteCoffees = supabaseDataSource.getAllCoffees()
            val remoteFavorites = supabaseDataSource.getAllFavorites()
            val remoteFavoritesCustom = try { supabaseDataSource.getAllFavoritesCustom() } catch (_: Exception) { emptyList() }
            val remoteReviews = supabaseDataSource.getAllReviews()
            val currentUser = userRepository.getActiveUser() ?: return@flow emit(emptyList())

            val favIds = (remoteFavorites + remoteFavoritesCustom.map { it.toLocalFavorite() })
                .filter { it.userId == currentUser.id }
                .map { it.coffeeId }.toSet()
            
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
        } catch (_: Exception) { emit(emptyList()) }
    }.flowOn(Dispatchers.IO)
}
