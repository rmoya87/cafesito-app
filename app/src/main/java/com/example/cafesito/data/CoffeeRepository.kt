package com.example.cafesito.data

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.cafesito.ui.utils.ConnectivityObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    private val _refreshCount = MutableStateFlow(0L)

    // --- CACHÉ EN MEMORIA ---
    private var _cachedCoffees: List<CoffeeWithDetails>? = null

    private suspend fun ensureConnected() {
        if (connectivityObserver.observe().first() != ConnectivityObserver.Status.Available) {
            throw NoConnectivityException("No hay conexión a internet.")
        }
    }

    fun triggerRefresh() {
        _cachedCoffees = null
        _refreshCount.value++
        _refreshTrigger.tryEmit(Unit)
    }

    /**
     * Devuelve un flujo paginado de cafés directamente desde Supabase.
     */
    fun getCoffeesPagingFlow(
        query: String? = null,
        origins: Set<String> = emptySet(),
        roasts: Set<String> = emptySet(),
        specialties: Set<String> = emptySet(),
        formats: Set<String> = emptySet(),
        minRating: Float = 0f
    ): Flow<PagingData<CoffeeWithDetails>> {
        return _refreshCount.flatMapLatest {
            Pager(
                config = PagingConfig(
                    pageSize = 20,
                    enablePlaceholders = false,
                    initialLoadSize = 20
                ),
                pagingSourceFactory = {
                    CoffeePagingSource(
                        supabaseDataSource = supabaseDataSource,
                        query = query,
                        origins = origins,
                        roasts = roasts,
                        specialties = specialties,
                        formats = formats,
                        minRating = minRating
                    )
                }
            ).flow.map { pagingData ->
                val user = userRepository.getActiveUser()
                val remoteFavorites = try { supabaseDataSource.getAllFavorites() } catch (_: Exception) { emptyList() }
                val remoteFavoritesCustom = try { supabaseDataSource.getAllFavoritesCustom() } catch (_: Exception) { emptyList() }
                val localFavorites = coffeeDao.getLocalFavorites().first()
                val localFavoritesCustom = coffeeDao.getLocalFavoritesCustom().first()
                val allFavs = (remoteFavorites + remoteFavoritesCustom.map { it.toLocalFavorite() } + localFavorites + localFavoritesCustom.map { it.toLocalFavorite() }).distinctBy { it.coffeeId }
                val remoteReviews = try { supabaseDataSource.getAllReviews() } catch (_: Exception) { emptyList() }

                pagingData.map { coffee ->
                    CoffeeWithDetails(
                        coffee = coffee,
                        favorite = allFavs.find { it.coffeeId == coffee.id && it.userId == user?.id },
                        reviews = remoteReviews.filter { it.coffeeId == coffee.id }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allCoffees: Flow<List<CoffeeWithDetails>> = _refreshTrigger.flatMapLatest {
        flow {
            _cachedCoffees?.let { emit(it); return@flow }

            try {
                ensureConnected()
                val user = userRepository.getActiveUser()
                
                coroutineScope {
                    val publicDef = async { try { supabaseDataSource.getAllCoffees() } catch (_: Exception) { emptyList() } }
                    val customDef = async { 
                        if (user != null) {
                            try { supabaseDataSource.getCustomCoffees(user.id).map { it.toCoffee() } } catch (_: Exception) { emptyList() }
                        } else emptyList()
                    }
                    val favsDef = async { try { supabaseDataSource.getAllFavorites() } catch (_: Exception) { emptyList() } }
                    val favsCustomDef = async { try { supabaseDataSource.getAllFavoritesCustom() } catch (_: Exception) { emptyList() } }
                    val localFavsDef = async { coffeeDao.getLocalFavorites().first() }
                    val localFavsCustomDef = async { coffeeDao.getLocalFavoritesCustom().first() }
                    val reviewsDef = async { try { supabaseDataSource.getAllReviews() } catch (_: Exception) { emptyList() } }

                    val totalCoffees = publicDef.await() + customDef.await()
                    val allFavs = (favsDef.await() + favsCustomDef.await().map { it.toLocalFavorite() } + 
                                  localFavsDef.await() + localFavsCustomDef.await().map { it.toLocalFavorite() })
                                  .distinctBy { it.coffeeId }
                    val remoteReviews = reviewsDef.await()

                    val result = withContext(Dispatchers.Default) {
                        totalCoffees.map { coffee ->
                            CoffeeWithDetails(
                                coffee = coffee,
                                favorite = allFavs.find { it.coffeeId == coffee.id && it.userId == user?.id },
                                reviews = remoteReviews.filter { it.coffeeId == coffee.id }
                            )
                        }
                    }
                    _cachedCoffees = result
                    emit(result)
                }
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
                coroutineScope {
                    val stdDef = async { try { supabaseDataSource.getAllFavorites() } catch (_: Exception) { emptyList() } }
                    val customDef = async { try { supabaseDataSource.getAllFavoritesCustom() } catch (_: Exception) { emptyList() } }
                    val allRemote = stdDef.await() + customDef.await().map { it.toLocalFavorite() }
                    emit(allRemote.filter { it.userId == user.id })
                }
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
                
                coroutineScope {
                    val publicDef = async { try { supabaseDataSource.getAllCoffees() } catch (_: Exception) { emptyList() } }
                    val customDef = async { 
                        if (user != null) {
                            try { supabaseDataSource.getCustomCoffees(user.id).map { it.toCoffee() } } catch (_: Exception) { emptyList() }
                        } else emptyList()
                    }
                    val favsDef = async { try { supabaseDataSource.getAllFavorites() } catch (_: Exception) { emptyList() } }
                    val favsCustomDef = async { try { supabaseDataSource.getAllFavoritesCustom() } catch (_: Exception) { emptyList() } }
                    val localFavsDef = async { coffeeDao.getLocalFavorites().first() }
                    val localFavsCustomDef = async { coffeeDao.getLocalFavoritesCustom().first() }
                    val reviewsDef = async { try { supabaseDataSource.getAllReviews() } catch (_: Exception) { emptyList() } }

                    val public = publicDef.await()
                    val custom = customDef.await()
                    val coffee = (public + custom).find { it.id == id } ?: return@coroutineScope emit(null)
                    
                    val allFavs = (favsDef.await() + favsCustomDef.await().map { it.toLocalFavorite() } + 
                                  localFavsDef.await() + localFavsCustomDef.await().map { it.toLocalFavorite() })
                                  .distinctBy { it.coffeeId }
                    val remoteReviews = reviewsDef.await()

                    val result = withContext(Dispatchers.Default) {
                        CoffeeWithDetails(
                            coffee = coffee,
                            favorite = allFavs.find { it.coffeeId == coffee.id && it.userId == user?.id },
                            reviews = remoteReviews.filter { it.coffeeId == coffee.id }
                        )
                    }
                    emit(result)
                }
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
                
                coroutineScope {
                    val publicDef = async { try { supabaseDataSource.getAllCoffees() } catch (_: Exception) { emptyList() } }
                    val favsDef = async { try { supabaseDataSource.getAllFavorites() } catch (_: Exception) { emptyList() } }
                    val favsCustomDef = async { try { supabaseDataSource.getAllFavoritesCustom() } catch (_: Exception) { emptyList() } }
                    val reviewsDef = async { try { supabaseDataSource.getAllReviews() } catch (_: Exception) { emptyList() } }

                    val publicCoffees = publicDef.await()
                    val allRemoteFavs = favsDef.await() + favsCustomDef.await().map { it.toLocalFavorite() }
                    val remoteReviews = reviewsDef.await()

                    val result = withContext(Dispatchers.Default) {
                        publicCoffees.filter { coffee ->
                            val matchQuery = query.isNullOrBlank() || coffee.nombre.contains(query, true) || (coffee.marca ?: "").contains(query, true)
                            val matchOrigin = origin == null || coffee.paisOrigen == origin
                            val matchRoast = roast == null || coffee.tueste == roast
                            matchQuery && matchOrigin && matchRoast
                        }.map { coffee ->
                            CoffeeWithDetails(
                                coffee = coffee,
                                favorite = allRemoteFavs.find { it.coffeeId == coffee.id && it.userId == user?.id },
                                reviews = remoteReviews.filter { it.coffeeId == coffee.id }
                            )
                        }
                    }
                    emit(result)
                }
            } catch (_: Exception) { emit(emptyList()) }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Obtiene recomendaciones delegando el cálculo sensorial a Supabase (RPC).
     */
    fun getRecommendations(): Flow<List<CoffeeWithDetails>> = flow {
        try {
            ensureConnected()
            val currentUser = userRepository.getActiveUser() ?: return@flow emit(emptyList())
            
            coroutineScope {
                // 1. Llamada RPC para obtener los cafés recomendados ya procesados por el servidor
                val recommendedCoffeesDef = async { supabaseDataSource.getRecommendationsRpc(currentUser.id) }
                val favsDef = async { try { supabaseDataSource.getAllFavorites() } catch (_: Exception) { emptyList() } }
                val favsCustomDef = async { try { supabaseDataSource.getAllFavoritesCustom() } catch (_: Exception) { emptyList() } }
                val reviewsDef = async { try { supabaseDataSource.getAllReviews() } catch (_: Exception) { emptyList() } }

                val recommendedCoffees = recommendedCoffeesDef.await()
                val allFavs = favsDef.await() + favsCustomDef.await().map { it.toLocalFavorite() }
                val remoteReviews = reviewsDef.await()

                // 2. Mapear a CoffeeWithDetails
                val result = withContext(Dispatchers.Default) {
                    recommendedCoffees.map { coffee ->
                        CoffeeWithDetails(
                            coffee = coffee,
                            favorite = allFavs.find { it.coffeeId == coffee.id && it.userId == currentUser.id },
                            reviews = remoteReviews.filter { it.coffeeId == coffee.id }
                        )
                    }
                }
                emit(result)
            }
        } catch (e: Exception) {
            Log.e("COFFEE_REPO", "Error en getRecommendations RPC: ${e.message}")
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)
}
