package com.cafesito.app.data

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.cafesito.app.ui.utils.ConnectivityObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Singleton
class CoffeeRepository @Inject constructor(
    private val coffeeDao: CoffeeDao,
    private val supabaseDataSource: SupabaseDataSource,
    private val userRepository: UserRepository,
    private val connectivityObserver: ConnectivityObserver
) {
    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    private val _refreshCount = MutableStateFlow(0L)

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

    fun getCoffeesPagingFlow(
        query: String? = null,
        origins: Set<String> = emptySet(),
        roasts: Set<String> = emptySet(),
        specialties: Set<String> = emptySet(),
        formats: Set<String> = emptySet(),
        minRating: Float = 0f
    ): Flow<PagingData<Coffee>> {
        return _refreshCount.flatMapLatest {
            Pager(
                config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                pagingSourceFactory = {
                    CoffeePagingSource(supabaseDataSource, query, origins, roasts, specialties, formats, minRating)
                }
            ).flow
        }
    }

    val allCoffees: Flow<List<CoffeeWithDetails>> = _refreshTrigger
        .debounce(300)
        .flatMapLatest {
            flow {
                _cachedCoffees?.let { emit(it); return@flow }
                try {
                    ensureConnected()
                    val user = userRepository.getActiveUser()
                    supervisorScope {
                        val publicDef = async<List<Coffee>> { try { supabaseDataSource.getAllCoffees() } catch (_: Exception) { emptyList() } }
                        val customDef = async<List<Coffee>> { 
                            if (user != null) try { supabaseDataSource.getCustomCoffees(user.id).map { it.toCoffee() } } catch (_: Exception) { emptyList() }
                            else emptyList()
                        }
                        val favsDef = async<List<LocalFavorite>> { try { supabaseDataSource.getAllFavorites() } catch (_: Exception) { emptyList() } }
                        val favsCustomDef = async<List<LocalFavoriteCustom>> { try { supabaseDataSource.getAllFavoritesCustom() } catch (_: Exception) { emptyList() } }
                        val localFavsDef = async<List<LocalFavorite>> { coffeeDao.getLocalFavorites().first() }
                        val localFavsCustomDef = async<List<LocalFavoriteCustom>> { coffeeDao.getLocalFavoritesCustom().first() }
                        val reviewsDef = async<List<ReviewEntity>> { try { supabaseDataSource.getAllReviews() } catch (_: Exception) { emptyList() } }

                        val coffees = publicDef.await() + customDef.await()
                        val allFavs = (favsDef.await() + favsCustomDef.await().map { it.toLocalFavorite() } + 
                                      localFavsDef.await() + localFavsCustomDef.await().map { it.toLocalFavorite() })
                                      .distinctBy { it.coffeeId }
                        val remoteReviews = reviewsDef.await()

                        val result = coffees.map { coffee ->
                            CoffeeWithDetails(
                                coffee = coffee,
                                favorite = allFavs.find { it.coffeeId == coffee.id && it.userId == user?.id },
                                reviews = remoteReviews.filter { it.coffeeId == coffee.id }
                            )
                        }
                        _cachedCoffees = result
                        emit(result)
                    }
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { emit(_cachedCoffees ?: emptyList()) }
            }.flowOn(Dispatchers.IO)
        }

    val favorites: Flow<List<LocalFavorite>> = combine(
        _refreshTrigger.debounce(300).flatMapLatest {
            flow {
                try {
                    ensureConnected()
                    val user = userRepository.getActiveUser() ?: return@flow emit(emptyList())
                    supervisorScope {
                        val stdDef = async<List<LocalFavorite>> { try { supabaseDataSource.getAllFavorites() } catch (_: Exception) { emptyList() } }
                        val customDef = async<List<LocalFavoriteCustom>> { try { supabaseDataSource.getAllFavoritesCustom() } catch (_: Exception) { emptyList() } }
                        val allRemote = stdDef.await() + customDef.await().map { it.toLocalFavorite() }
                        emit(allRemote.filter { it.userId == user.id })
                    }
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { emit(emptyList()) }
            }.flowOn(Dispatchers.IO)
        },
        coffeeDao.getLocalFavorites(),
        coffeeDao.getLocalFavoritesCustom()
    ) { remote, local, localCustom ->
        (remote + local + localCustom.map { it.toLocalFavorite() }).distinctBy { it.coffeeId }
    }

    val allReviews: Flow<List<ReviewEntity>> = _refreshTrigger.debounce(300).flatMapLatest {
        flow {
            try {
                ensureConnected()
                emit(supabaseDataSource.getAllReviews())
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { emit(emptyList()) }
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
                    val favCustom = LocalFavoriteCustom(coffeeId, currentUser.id)
                    coffeeDao.deleteFavoriteCustom(favCustom)
                    try { supabaseDataSource.deleteFavoriteCustom(coffeeId, currentUser.id) } catch (_: Exception) { }
                } else {
                    val favorite = LocalFavorite(coffeeId, currentUser.id)
                    coffeeDao.deleteFavorite(favorite)
                    try { supabaseDataSource.deleteFavorite(coffeeId, currentUser.id) } catch (_: Exception) { }
                }
            }
            triggerRefresh()
        } catch (e: Exception) {
            Log.e("COFFEE_REPO", "Error toggleFavorite", e)
        }
    }

    fun getCoffeeWithDetailsById(id: String): Flow<CoffeeWithDetails?> = _refreshTrigger.debounce(100).flatMapLatest {
        flow {
            try {
                ensureConnected()
                val user = userRepository.getActiveUser()
                supervisorScope {
                    val publicDef = async<Coffee?> { try { supabaseDataSource.getCoffeesByIds(listOf(id)).firstOrNull() } catch (_: Exception) { null } }
                    val customDef = async<Coffee?> {
                        if (user != null) try { supabaseDataSource.getCustomCoffees(user.id).find { it.id == id }?.toCoffee() } catch (_: Exception) { null }
                        else null
                    }
                    val favsDef = async<List<LocalFavorite>> { 
                        if (user != null) try { supabaseDataSource.getFavoritesByUserId(user.id) } catch (_: Exception) { emptyList() } else emptyList()
                    }
                    val favsCustomDef = async<List<LocalFavoriteCustom>> { 
                        if (user != null) try { supabaseDataSource.getFavoritesCustomByUserId(user.id) } catch (_: Exception) { emptyList() } else emptyList()
                    }
                    val localFavsDef = async<List<LocalFavorite>> { coffeeDao.getLocalFavorites().first() }
                    val localFavsCustomDef = async<List<LocalFavoriteCustom>> { coffeeDao.getLocalFavoritesCustom().first() }
                    val reviewsDef = async<List<ReviewEntity>> { try { supabaseDataSource.getReviewsByCoffeeId(id) } catch (_: Exception) { emptyList() } }

                    val coffee = publicDef.await() ?: customDef.await() ?: return@supervisorScope emit(null)
                    val allFavs = (favsDef.await() + favsCustomDef.await().map { it.toLocalFavorite() } +
                            localFavsDef.await() + localFavsCustomDef.await().map { it.toLocalFavorite() })
                        .distinctBy { it.coffeeId }
                    val remoteReviews = reviewsDef.await()

                    emit(CoffeeWithDetails(coffee, allFavs.find { it.coffeeId == coffee.id && it.userId == user?.id }, remoteReviews))
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { emit(null) }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun upsertReview(review: ReviewEntity) = withContext(Dispatchers.IO) {
        try { ensureConnected(); supabaseDataSource.upsertReview(review); triggerRefresh() }
        catch (e: Exception) { Log.e("COFFEE_REPO", "Error upsertReview: ${e.message}") }
    }

    suspend fun deleteReview(coffeeId: String, userId: Int) = withContext(Dispatchers.IO) {
        try { ensureConnected(); supabaseDataSource.deleteReview(coffeeId, userId); triggerRefresh() }
        catch (e: Exception) { Log.e("COFFEE_REPO", "Error deleteReview: ${e.message}") }
    }

    suspend fun syncCoffees() = withContext(Dispatchers.IO) {
        try { ensureConnected(); triggerRefresh() }
        catch (e: Exception) { Log.e("COFFEE_REPO", "Error syncCoffees", e) }
    }

    fun getFilteredCoffees(query: String? = null, origin: String? = null, roast: String? = null): Flow<List<CoffeeWithDetails>> = 
        _refreshTrigger.debounce(300).flatMapLatest {
            flow {
                try {
                    ensureConnected()
                    val user = userRepository.getActiveUser()
                    supervisorScope {
                        val publicDef = async<List<Coffee>> { try { supabaseDataSource.getAllCoffees(query, origin, roast) } catch (_: Exception) { emptyList() } }
                        val favsDef = async<List<LocalFavorite>> { try { supabaseDataSource.getAllFavorites() } catch (_: Exception) { emptyList() } }
                        val favsCustomDef = async<List<LocalFavoriteCustom>> { try { supabaseDataSource.getAllFavoritesCustom() } catch (_: Exception) { emptyList() } }
                        val reviewsDef = async<List<ReviewEntity>> { try { supabaseDataSource.getAllReviews() } catch (_: Exception) { emptyList() } }

                        val coffees = publicDef.await()
                        val allFavs = favsDef.await() + favsCustomDef.await().map { it.toLocalFavorite() }
                        val remoteReviews = reviewsDef.await()

                        emit(coffees.map { coffee ->
                            CoffeeWithDetails(coffee, allFavs.find { it.coffeeId == coffee.id && it.userId == user?.id }, remoteReviews.filter { it.coffeeId == coffee.id })
                        })
                    }
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { emit(emptyList()) }
            }.flowOn(Dispatchers.IO)
        }
}
