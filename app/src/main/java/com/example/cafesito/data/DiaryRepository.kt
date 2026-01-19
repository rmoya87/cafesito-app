package com.example.cafesito.data

import android.util.Log
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryRepository @Inject constructor(
    private val diaryDao: DiaryDao,
    private val supabaseDataSource: SupabaseDataSource,
    private val userRepository: UserRepository
) {
    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    fun triggerRefresh() {
        Log.d("DIARY_REPO", "Refrescando datos...")
        _refreshTrigger.tryEmit(Unit)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getDiaryEntries(): Flow<List<DiaryEntryEntity>> = _refreshTrigger.flatMapLatest {
        flow {
            val user = userRepository.getActiveUser() ?: return@flow emit(emptyList())
            try {
                emit(supabaseDataSource.getDiaryEntries(user.id))
            } catch (e: Exception) { 
                Log.e("DIARY_REPO", "Error cargando entradas", e)
                emit(emptyList()) 
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getPantryItems(): Flow<List<PantryItemWithDetails>> = _refreshTrigger.flatMapLatest {
        flow {
            val user = userRepository.getActiveUser() ?: return@flow emit(emptyList())
            try {
                val pantryEntities = supabaseDataSource.getPantryItems(user.id)
                val publicCoffees = try { supabaseDataSource.getAllCoffees() } catch (e: Exception) { emptyList() }
                val customEntities = try { supabaseDataSource.getCustomCoffees(user.id) } catch (e: Exception) { emptyList() }
                
                val allAvailable = publicCoffees + customEntities.map { it.toCoffee() }
                
                val itemsWithStock = pantryEntities.mapNotNull { item ->
                    allAvailable.find { it.id == item.coffeeId }?.let { coffee ->
                        PantryItemWithDetails(item, coffee)
                    }
                }

                val onlyCustom = customEntities.filter { custom ->
                    pantryEntities.none { it.coffeeId == custom.id }
                }.map { custom ->
                    PantryItemWithDetails(
                        PantryItemEntity(custom.id, user.id, custom.totalGrams, custom.totalGrams, System.currentTimeMillis()),
                        custom.toCoffee()
                    )
                }
                
                emit((itemsWithStock + onlyCustom).sortedByDescending { it.pantryItem.lastUpdated })
            } catch (e: Exception) {
                Log.e("DIARY_REPO", "Error al cargar despensa", e)
                emit(emptyList())
            }
        }
    }

    suspend fun addDiaryEntry(
        coffeeId: String?, 
        coffeeName: String, 
        coffeeBrand: String,
        caffeineAmount: Int, 
        type: String = "CUP", 
        amountMl: Int = 250,
        coffeeGrams: Int = 15,
        preparationType: String = "Espresso"
    ) {
        val user = userRepository.getActiveUser() ?: return
        withContext(NonCancellable) {
            try {
                val entry = DiaryEntryEntity(
                    userId = user.id, 
                    coffeeId = coffeeId, 
                    coffeeName = coffeeName, 
                    coffeeBrand = coffeeBrand,
                    caffeineAmount = caffeineAmount, 
                    amountMl = amountMl, 
                    coffeeGrams = coffeeGrams,
                    preparationType = preparationType,
                    timestamp = System.currentTimeMillis(), 
                    type = type
                )
                supabaseDataSource.insertDiaryEntry(entry)
                
                if (coffeeId != null && type == "CUP") {
                    updatePantryStock(coffeeId, -coffeeGrams)
                }
                
                triggerRefresh()
            } catch (e: Exception) { 
                Log.e("DIARY_REPO", "Error al añadir entrada", e)
            }
        }
    }

    private suspend fun updatePantryStock(coffeeId: String, deltaGrams: Int) {
        val user = userRepository.getActiveUser() ?: return
        try {
            val items = supabaseDataSource.getPantryItems(user.id)
            val existing = items.find { it.coffeeId == coffeeId }
            
            if (existing != null) {
                val updated = existing.copy(
                    gramsRemaining = (existing.gramsRemaining + deltaGrams).coerceAtLeast(0), 
                    lastUpdated = System.currentTimeMillis()
                )
                supabaseDataSource.upsertPantryItem(updated)
            } else {
                val customEntities = supabaseDataSource.getCustomCoffees(user.id)
                val totalG = customEntities.find { it.id == coffeeId }?.totalGrams ?: 250
                val newItem = PantryItemEntity(
                    coffeeId = coffeeId,
                    userId = user.id,
                    gramsRemaining = (totalG + deltaGrams).coerceAtLeast(0),
                    totalGrams = totalG,
                    lastUpdated = System.currentTimeMillis()
                )
                supabaseDataSource.upsertPantryItem(newItem)
            }
        } catch (e: Exception) { 
            Log.e("DIARY_REPO", "Error actualizando stock", e)
        }
    }

    suspend fun createCustomCoffeeAndAddToPantry(
        name: String, brand: String, specialty: String, roast: String?, variety: String?, 
        country: String, hasCaffeine: Boolean, format: String, imageBytes: ByteArray?,
        totalGrams: Int = 250
    ) {
        val user = userRepository.getActiveUser() ?: return
        withContext(NonCancellable) {
            try {
                val coffeeId = UUID.randomUUID().toString()
                var imageUrl = ""
                if (imageBytes != null) {
                    try {
                        imageUrl = supabaseDataSource.uploadImage("coffees", "custom_$coffeeId.jpg", imageBytes)
                    } catch (e: Exception) { Log.e("DIARY_REPO", "Error imagen") }
                }

                val customCoffee = CustomCoffeeEntity(
                    id = coffeeId, userId = user.id, name = name, brand = brand,
                    specialty = specialty, roast = roast, variety = variety,
                    country = country, hasCaffeine = hasCaffeine, format = format, 
                    imageUrl = imageUrl, totalGrams = totalGrams
                )
                
                supabaseDataSource.insertCustomCoffee(customCoffee)
                addToPantry(coffeeId, totalGrams) 
                triggerRefresh()
            } catch (e: Exception) {
                Log.e("DIARY_REPO", "Error al crear café", e)
                throw e
            }
        }
    }

    suspend fun addToPantry(coffeeId: String, totalGrams: Int) {
        val user = userRepository.getActiveUser() ?: return
        try {
            val item = PantryItemEntity(
                coffeeId = coffeeId, 
                userId = user.id, 
                gramsRemaining = totalGrams, 
                totalGrams = totalGrams,
                lastUpdated = System.currentTimeMillis()
            )
            supabaseDataSource.upsertPantryItem(item)
            triggerRefresh()
        } catch (e: Exception) { throw e }
    }

    suspend fun deletePantryItem(coffeeId: String) {
        val user = userRepository.getActiveUser() ?: return
        try {
            supabaseDataSource.deletePantryItem(coffeeId, user.id)
            triggerRefresh()
        } catch (e: Exception) { }
    }

    suspend fun deleteDiaryEntry(entryId: Long) {
        try {
            supabaseDataSource.deleteDiaryEntry(entryId)
            triggerRefresh()
        } catch (e: Exception) { }
    }
}
