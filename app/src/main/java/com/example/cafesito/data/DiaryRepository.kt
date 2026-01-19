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
    private val _refreshCount = MutableStateFlow(0L)

    fun triggerRefresh() {
        _refreshCount.value++
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getDiaryEntries(): Flow<List<DiaryEntryEntity>> = _refreshCount.flatMapLatest {
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
    fun getPantryItems(): Flow<List<PantryItemWithDetails>> = _refreshCount.flatMapLatest {
        flow {
            val user = userRepository.getActiveUser() ?: return@flow emit(emptyList())
            try {
                val pantryEntities = supabaseDataSource.getPantryItems(user.id)
                val publicCoffees = try { supabaseDataSource.getAllCoffees() } catch (e: Exception) { emptyList() }
                val customEntities = try { supabaseDataSource.getCustomCoffees(user.id) } catch (e: Exception) { emptyList() }
                
                val allAvailable = publicCoffees + customEntities.map { it.toCoffee() }
                
                val itemsWithStock = pantryEntities.mapNotNull { item ->
                    allAvailable.find { it.id == item.coffeeId }?.let { coffee ->
                        val isCustom = customEntities.any { it.id == item.coffeeId }
                        PantryItemWithDetails(item, coffee, isCustom)
                    }
                }
                
                emit(itemsWithStock.sortedByDescending { it.pantryItem.lastUpdated })
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
                    updatePantryStockDelta(coffeeId, -coffeeGrams)
                }
                
                triggerRefresh()
            } catch (e: Exception) { 
                Log.e("DIARY_REPO", "Error al añadir entrada", e)
            }
        }
    }

    private suspend fun updatePantryStockDelta(coffeeId: String, deltaGrams: Int) {
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
                updatePantryStockFull(coffeeId, totalGrams, totalGrams) 
                triggerRefresh()
            } catch (e: Exception) {
                Log.e("DIARY_REPO", "Error al crear café", e)
                throw e
            }
        }
    }

    suspend fun updateCustomCoffee(
        id: String, name: String, brand: String, specialty: String, roast: String?, 
        variety: String?, country: String, hasCaffeine: Boolean, format: String, 
        imageBytes: ByteArray?, totalGrams: Int
    ) {
        val user = userRepository.getActiveUser() ?: return
        withContext(NonCancellable) {
            try {
                val currentCoffees = supabaseDataSource.getCustomCoffees(user.id)
                val existing = currentCoffees.find { it.id == id }
                
                var imageUrl = existing?.imageUrl ?: ""
                if (imageBytes != null) {
                    imageUrl = supabaseDataSource.uploadImage("coffees", "custom_$id.jpg", imageBytes)
                }

                val updatedCoffee = CustomCoffeeEntity(
                    id = id, userId = user.id, name = name, brand = brand,
                    specialty = specialty, roast = roast, variety = variety,
                    country = country, hasCaffeine = hasCaffeine, format = format, 
                    imageUrl = imageUrl, totalGrams = totalGrams
                )
                
                supabaseDataSource.updateCustomCoffee(id, user.id, updatedCoffee) 
                
                val currentPantryItem = supabaseDataSource.getPantryItems(user.id).find { it.coffeeId == id }
                if (currentPantryItem != null) {
                    supabaseDataSource.upsertPantryItem(currentPantryItem.copy(totalGrams = totalGrams))
                }
                
                triggerRefresh()
            } catch (e: Exception) {
                Log.e("DIARY_REPO", "Error al actualizar café", e)
                throw e
            }
        }
    }

    suspend fun updatePantryStockFull(coffeeId: String, totalGrams: Int, gramsRemaining: Int) {
        val user = userRepository.getActiveUser() ?: return
        try {
            val item = PantryItemEntity(
                coffeeId = coffeeId, 
                userId = user.id, 
                gramsRemaining = gramsRemaining, 
                totalGrams = totalGrams,
                lastUpdated = System.currentTimeMillis()
            )
            supabaseDataSource.upsertPantryItem(item)
            triggerRefresh()
        } catch (e: Exception) { throw e }
    }

    suspend fun addToPantry(coffeeId: String, totalGrams: Int) {
        updatePantryStockFull(coffeeId, totalGrams, totalGrams)
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
