package com.example.cafesito.data

import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoffeeRepository @Inject constructor(
    private val coffeeDao: CoffeeDao,
    private val supabaseDataSource: SupabaseDataSource,
    private val userRepository: UserRepository
) {
    // Gatillo para forzar la recarga de datos desde Supabase
    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    fun triggerRefresh() {
        _refreshTrigger.tryEmit(Unit)
    }

    // CONSULTAS PARA PERFIL Y DIARIO (PÚBLICAS + PROPIAS)
    @OptIn(ExperimentalCoroutinesApi::class)
    val allCoffees: Flow<List<CoffeeWithDetails>> = _refreshTrigger.flatMapLatest {
        flow {
            try {
                val user = userRepository.getActiveUser()
                val publicCoffees = try { supabaseDataSource.getAllCoffees() } catch (e: Exception) { emptyList() }
                val customCoffees = if (user != null) {
                    try { supabaseDataSource.getCustomCoffees(user.id).map { it.toCoffee() } } catch (e: Exception) { emptyList() }
                } else emptyList()
                
                val totalCoffees = publicCoffees + customCoffees
                // Combinamos favoritos remotos con locales para máxima fiabilidad
                val remoteFavorites = try { supabaseDataSource.getAllFavorites() } catch (e: Exception) { emptyList() }
                
                emit(totalCoffees.map { coffee ->
                    CoffeeWithDetails(
                        coffee = coffee,
                        favorite = remoteFavorites.find { it.coffeeId == coffee.id && it.userId == user?.id },
                        reviews = emptyList() // Se cargan por separado si es necesario
                    )
                })
            } catch (e: Exception) {
                Log.e("COFFEE_REPO", "Error allCoffees: ${e.message}")
                emit(emptyList())
            }
        }
    }

    // FAVORITOS: Ahora combinamos la base de datos local para reactividad inmediata
    @OptIn(ExperimentalCoroutinesApi::class)
    val favorites: Flow<List<LocalFavorite>> = combine(
        _refreshTrigger.flatMapLatest {
            flow {
                val user = userRepository.getActiveUser() ?: return@flow emit(emptyList())
                try {
                    val remote = supabaseDataSource.getAllFavorites()
                    emit(remote.filter { it.userId == user.id })
                } catch (e: Exception) { emit(emptyList()) }
            }
        },
        coffeeDao.getLocalFavorites()
    ) { remote, local ->
        // Unimos ambos para asegurar que si se acaba de insertar en local, aparezca
        (remote + local).distinctBy { it.coffeeId }
    }

    val allReviews: Flow<List<ReviewEntity>> = flow {
        try {
            emit(supabaseDataSource.getAllReviews())
        } catch (e: Exception) { emit(emptyList()) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCoffeeWithDetailsById(id: String): Flow<CoffeeWithDetails?> = _refreshTrigger.flatMapLatest {
        flow {
            try {
                val user = userRepository.getActiveUser()
                val publicCoffees = try { supabaseDataSource.getAllCoffees() } catch (e: Exception) { emptyList() }
                val customCoffees = if (user != null) {
                    try { supabaseDataSource.getCustomCoffees(user.id).map { it.toCoffee() } } catch (e: Exception) { emptyList() }
                } else emptyList()
                
                val coffee = (publicCoffees + customCoffees).find { it.id == id } ?: return@flow emit(null)
                
                // Consultamos favoritos tanto en remoto como en local
                val remoteFavorites = try { supabaseDataSource.getAllFavorites() } catch (e: Exception) { emptyList() }
                val localFavorites = coffeeDao.getLocalFavorites().first()
                val allFavs = (remoteFavorites + localFavorites).distinctBy { it.coffeeId }

                val remoteReviews = try { supabaseDataSource.getAllReviews() } catch (e: Exception) { emptyList() }

                emit(CoffeeWithDetails(
                    coffee = coffee,
                    favorite = allFavs.find { it.coffeeId == coffee.id && it.userId == user?.id },
                    reviews = remoteReviews.filter { it.coffeeId == coffee.id }
                ))
            } catch (e: Exception) { emit(null) }
        }
    }

    suspend fun toggleFavorite(coffeeId: String, shouldBeFavorite: Boolean) {
        val currentUser = userRepository.getActiveUser() ?: return
        val favorite = LocalFavorite(coffeeId, currentUser.id, System.currentTimeMillis())
        
        try {
            if (shouldBeFavorite) {
                // 1. Actualizar Room localmente para respuesta instantánea
                coffeeDao.insertFavorite(favorite)
                // 2. Intentar actualizar Supabase
                try {
                    supabaseDataSource.insertFavorite(favorite)
                } catch (e: Exception) {
                    Log.e("COFFEE_REPO", "Error al insertar favorito en Supabase: ${e.message}")
                }
            } else {
                // 1. Eliminar de Room
                coffeeDao.deleteFavorite(favorite)
                // 2. Intentar eliminar de Supabase
                try {
                    supabaseDataSource.deleteFavorite(coffeeId, currentUser.id)
                } catch (e: Exception) {
                    Log.e("COFFEE_REPO", "Error al eliminar favorito en Supabase: ${e.message}")
                }
            }
            // Forzamos el refresco de todos los flujos para que la UI se actualice
            triggerRefresh()
        } catch (e: Exception) {
            Log.e("COFFEE_REPO", "Error al gestionar favorito: ${e.message}")
        }
    }

    suspend fun upsertReview(review: ReviewEntity) {
        try { 
            supabaseDataSource.upsertReview(review) 
            triggerRefresh()
        } catch (e: Exception) { }
    }

    suspend fun deleteReview(coffeeId: String, userId: Int) {
        try { 
            supabaseDataSource.deleteReview(coffeeId, userId) 
            triggerRefresh()
        } catch (e: Exception) { }
    }

    suspend fun syncCoffees() {
        try {
            triggerRefresh()
        } catch (e: Exception) {
            Log.e("COFFEE_REPO", "Error al sincronizar catálogo", e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getFilteredCoffees(
        query: String? = null,
        origin: String? = null,
        roast: String? = null,
        specialty: String? = null,
        variety: String? = null,
        format: String? = null,
        grind: String? = null
    ): Flow<List<CoffeeWithDetails>> = _refreshTrigger.flatMapLatest {
        flow {
            try {
                val user = userRepository.getActiveUser()
                val publicCoffees = try { supabaseDataSource.getAllCoffees() } catch (e: Exception) { emptyList() }

                val filtered = publicCoffees.filter { coffee ->
                    val matchQuery = query.isNullOrBlank() || coffee.nombre.contains(query, true) || coffee.marca.contains(query, true)
                    val matchOrigin = origin == null || coffee.paisOrigen == origin
                    val matchRoast = roast == null || coffee.tueste == roast
                    matchQuery && matchOrigin && matchRoast
                }

                val remoteFavorites = try { supabaseDataSource.getAllFavorites() } catch (e: Exception) { emptyList() }
                val remoteReviews = try { supabaseDataSource.getAllReviews() } catch (e: Exception) { emptyList() }

                emit(filtered.map { coffee ->
                    CoffeeWithDetails(
                        coffee = coffee,
                        favorite = remoteFavorites.find { it.coffeeId == coffee.id && it.userId == user?.id },
                        reviews = remoteReviews.filter { it.coffeeId == coffee.id }
                    )
                })
            } catch (e: Exception) { emit(emptyList()) }
        }
    }
}
