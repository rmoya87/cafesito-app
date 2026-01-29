package com.cafesito.app.data

import android.util.Log
import com.cafesito.app.data.shared.toDomain
import com.cafesito.app.data.shared.toEntity
import com.cafesito.app.ui.utils.ConnectivityObserver
import com.cafesito.app.health.HealthConnectRepository
import com.cafesito.shared.core.DataResult
import com.cafesito.shared.domain.repository.CafesitoRepository
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
    private val cafesitoRepository: CafesitoRepository,
    private val userRepository: UserRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val healthConnectRepository: HealthConnectRepository
) {
    private val _refreshCount = MutableStateFlow(0L)

    private suspend fun ensureConnected() {
        if (connectivityObserver.observe().first() != ConnectivityObserver.Status.Available) {
            throw NoConnectivityException("No hay conexión a internet.")
        }
    }

    fun triggerRefresh() {
        _refreshCount.value++
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getDiaryEntries(): Flow<List<DiaryEntryEntity>> = _refreshCount.flatMapLatest {
        flow {
            val user = userRepository.getActiveUser() ?: return@flow emit(emptyList())
            try {
                ensureConnected()
                when (val result = cafesitoRepository.getDiaryEntries(user.id)) {
                    is DataResult.Success -> emit(result.data.map { it.toEntity() })
                    is DataResult.Failure -> emit(emptyList())
                }
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
                ensureConnected()
                val pantryEntities = when (val result = cafesitoRepository.getPantryItems(user.id)) {
                    is DataResult.Success -> result.data.map { it.toEntity() }
                    is DataResult.Failure -> emptyList()
                }
                
                // ✅ OPTIMIZACIÓN: Solo descargar los cafés necesarios (by IDs)
                val coffeeIds = pantryEntities.map { it.coffeeId }.distinct()
                val publicCoffees = try { 
                    if (coffeeIds.isNotEmpty()) supabaseDataSource.getCoffeesByIds(coffeeIds) else emptyList()
                } catch (e: Exception) { emptyList() }
                
                val customEntities = when (val result = cafesitoRepository.getCustomCoffees(user.id)) {
                    is DataResult.Success -> result.data.map { it.toEntity() }
                    is DataResult.Failure -> emptyList()
                }
                
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
        ensureConnected()
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
                val insertedEntry = when (val result = cafesitoRepository.addDiaryEntry(entry.toDomain())) {
                    is DataResult.Success -> result.data.toEntity()
                    is DataResult.Failure -> entry
                }
                
                // Sync with Health Connect if enabled
                healthConnectRepository.syncDiaryEntry(
                    mg = if (type == "WATER") 0 else caffeineAmount,
                    ml = if (type == "WATER") amountMl else 0,
                    timestamp = entry.timestamp,
                    entryId = insertedEntry.id.toString()
                )
                
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
        ensureConnected()
        val user = userRepository.getActiveUser() ?: return
        try {
            val items = when (val result = cafesitoRepository.getPantryItems(user.id)) {
                is DataResult.Success -> result.data.map { it.toEntity() }
                is DataResult.Failure -> emptyList()
            }
            val existing = items.find { it.coffeeId == coffeeId }
            
            if (existing != null) {
                val updated = existing.copy(
                    gramsRemaining = (existing.gramsRemaining + deltaGrams).coerceAtLeast(0), 
                    lastUpdated = System.currentTimeMillis()
                )
                cafesitoRepository.upsertPantryItem(updated.toDomain())
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
        ensureConnected()
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
                
                cafesitoRepository.addCustomCoffee(customCoffee.toDomain())
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
        ensureConnected()
        val user = userRepository.getActiveUser() ?: return
        withContext(NonCancellable) {
            try {
                val currentCoffees = when (val result = cafesitoRepository.getCustomCoffees(user.id)) {
                    is DataResult.Success -> result.data.map { it.toEntity() }
                    is DataResult.Failure -> emptyList()
                }
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
                
                cafesitoRepository.updateCustomCoffee(updatedCoffee.toDomain())
                
                val currentPantryItem = when (val result = cafesitoRepository.getPantryItems(user.id)) {
                    is DataResult.Success -> result.data.map { it.toEntity() }
                    is DataResult.Failure -> emptyList()
                }.find { it.coffeeId == id }
                if (currentPantryItem != null) {
                    cafesitoRepository.upsertPantryItem(currentPantryItem.copy(totalGrams = totalGrams).toDomain())
                }
                
                triggerRefresh()
            } catch (e: Exception) {
                Log.e("DIARY_REPO", "Error al actualizar café", e)
                throw e
            }
        }
    }

    suspend fun updatePantryStockFull(coffeeId: String, totalGrams: Int, gramsRemaining: Int) {
        ensureConnected()
        val user = userRepository.getActiveUser() ?: return
        try {
            val item = PantryItemEntity(
                coffeeId = coffeeId, 
                userId = user.id, 
                gramsRemaining = gramsRemaining, 
                totalGrams = totalGrams,
                lastUpdated = System.currentTimeMillis()
            )
            cafesitoRepository.upsertPantryItem(item.toDomain())
            triggerRefresh()
        } catch (e: Exception) { throw e }
    }

    suspend fun addToPantry(coffeeId: String, totalGrams: Int) {
        ensureConnected()
        updatePantryStockFull(coffeeId, totalGrams, totalGrams)
    }

    suspend fun deletePantryItem(coffeeId: String) {
        ensureConnected()
        val user = userRepository.getActiveUser() ?: return
        try {
            cafesitoRepository.deletePantryItem(coffeeId, user.id)
            triggerRefresh()
        } catch (e: Exception) { }
    }

    suspend fun deleteDiaryEntry(entryId: Long) {
        try {
            ensureConnected()
            cafesitoRepository.deleteDiaryEntry(entryId)
            triggerRefresh()
        } catch (e: Exception) { }
    }

    suspend fun syncExternalHealthData() {
        if (!healthConnectRepository.isEnabled()) return
        
        try {
            val user = userRepository.getActiveUser() ?: return
            val externalRecords = healthConnectRepository.readAndSyncExternalData()
            val currentEntries = when (val result = cafesitoRepository.getDiaryEntries(user.id)) {
                is DataResult.Success -> result.data.map { it.toEntity() }
                is DataResult.Failure -> emptyList()
            }
            val existingExternalIds = currentEntries.mapNotNull { it.externalId }.toSet()
            
            externalRecords.forEach { (mg, ts) ->
                val externalId = "hc_$ts" // Unique for HC records based on timestamp
                if (!existingExternalIds.contains(externalId)) {
                    val entry = DiaryEntryEntity(
                        userId = user.id,
                        coffeeName = "Registro Externo",
                        coffeeBrand = "Salud",
                        caffeineAmount = mg,
                        timestamp = ts,
                        type = "CUP",
                        externalId = externalId
                    )
                    cafesitoRepository.addDiaryEntry(entry.toDomain())
                }
            }
            triggerRefresh()
        } catch (e: Exception) {
            Log.e("DIARY_REPO", "Error syncing HC", e)
        }
    }
}
