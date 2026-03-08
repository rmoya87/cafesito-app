package com.cafesito.app.data

import android.util.Log
import com.cafesito.app.ui.utils.ConnectivityObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class DiaryRepository @Inject constructor(
    private val diaryDao: DiaryDao,
    private val coffeeDao: CoffeeDao,
    private val supabaseDataSource: SupabaseDataSource,
    private val userRepository: UserRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val externalScope: CoroutineScope
) {
    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    init {
        externalScope.launch {
            while (true) {
                try {
                    syncPendingDiaryEntries()
                } catch (e: Exception) {
                    Log.e("DiaryRepository", "Error in periodic diary sync", e)
                }
                delay(10 * 60 * 1000L)
            }
        }

        externalScope.launch {
            connectivityObserver.observe().collect { status ->
                if (status == ConnectivityObserver.Status.Available) {
                    syncPendingDiaryEntries()
                    syncLocalCustomCoffees()
                }
            }
        }
    }

    fun triggerRefresh() {
        _refreshTrigger.tryEmit(Unit)
    }

    fun getDiaryEntries(): Flow<List<DiaryEntryEntity>> = combine(
        _refreshTrigger.onStart { emit(Unit) },
        userRepository.getActiveUserFlow()
    ) { _, activeUser -> activeUser }
        .flatMapLatest { user ->
            if (user == null) return@flatMapLatest flowOf(emptyList())
            diaryDao.getDiaryEntries(user.id).onStart {
                if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
                    externalScope.launch {
                        try {
                            val entries = supabaseDataSource.getDiaryEntries(user.id)
                            entries.forEach { diaryDao.insertDiaryEntry(it) }
                        } catch (e: Exception) {
                            Log.e("DiaryRepository", "Error fetching diary entries", e)
                        }
                    }
                }
            }
        }
        .flowOn(Dispatchers.IO)

    fun getPantryItems(): Flow<List<PantryItemWithDetails>> = combine(
        _refreshTrigger.onStart { emit(Unit) },
        userRepository.getActiveUserFlow()
    ) { _, activeUser -> activeUser }
        .flatMapLatest { user ->
            if (user == null) return@flatMapLatest flowOf(emptyList())
            combine(
                diaryDao.getPantryItems(user.id),
                coffeeDao.getAllCoffeesWithDetails()
            ) { items, coffeesWithDetails ->
                items.mapNotNull { item ->
                    coffeesWithDetails.find { it.coffee.id == item.coffeeId }?.let { details ->
                        PantryItemWithDetails(item, details.coffee, details.coffee.isCustom)
                    }
                }
            }.onStart {
                if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
                    externalScope.launch { syncPantryItems() }
                }
            }
        }
        .flowOn(Dispatchers.IO)

    suspend fun syncPantryItems() {
        val user = userRepository.getActiveUser() ?: return
        try {
            val remoteItems = supabaseDataSource.getPantryItems(user.id)
            val keepCoffeeIds = remoteItems.map { it.coffeeId }
            if (keepCoffeeIds.isEmpty()) {
                diaryDao.deleteAllPantryItemsForUser(user.id)
            } else {
                diaryDao.deletePantryItemsForUserNotIn(user.id, keepCoffeeIds)
            }
            remoteItems.forEach { item ->
                val localCoffee = coffeeDao.getCoffeeById(item.coffeeId)
                if (localCoffee == null) {
                    val coffeeIds = listOf(item.coffeeId)
                    val remoteCoffee = supabaseDataSource.getCoffeesByIds(coffeeIds).firstOrNull()
                        ?: supabaseDataSource.getCustomCoffees(user.id).find { c -> c.id == item.coffeeId }
                    remoteCoffee?.let { c -> coffeeDao.insertCoffees(listOf(c)) }
                }
                diaryDao.upsertPantryItem(item)
            }
        } catch (e: Exception) {
            Log.e("DiaryRepository", "Error syncing pantry items", e)
        }
    }

    /** Sincroniza entradas del diario: borra en local las que ya no vienen del servidor (p. ej. borradas en web). */
    suspend fun syncDiaryEntriesFromRemote() {
        val user = userRepository.getActiveUser() ?: return
        try {
            val remoteEntries = supabaseDataSource.getDiaryEntries(user.id)
            val keepIds = remoteEntries.map { it.id }.filter { it > 0L }
            if (keepIds.isEmpty()) {
                diaryDao.deleteAllDiaryEntriesForUser(user.id)
            } else {
                diaryDao.deleteDiaryEntriesForUserNotIn(user.id, keepIds)
            }
            remoteEntries.forEach { diaryDao.insertDiaryEntry(it) }
        } catch (e: Exception) {
            Log.e("DiaryRepository", "Error syncing diary entries", e)
        }
    }

    companion object {
        /** Tipo de entrada: taza de café (por defecto). */
        const val TYPE_CUP = "CUP"
    }

    suspend fun addDiaryEntry(
        coffeeId: String?,
        coffeeName: String,
        coffeeBrand: String,
        caffeineAmount: Int,
        type: String = TYPE_CUP,
        amountMl: Int = 250,
        coffeeGrams: Int = 15,
        preparationType: String = "Espresso",
        sizeLabel: String? = null,
        reduceFromPantry: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val user = userRepository.getActiveUser() ?: return@withContext
        val entry = DiaryEntryEntity(
            userId = user.id, coffeeId = coffeeId, coffeeName = coffeeName, 
            coffeeBrand = coffeeBrand, caffeineAmount = caffeineAmount, 
            amountMl = amountMl, coffeeGrams = coffeeGrams,
            preparationType = preparationType, sizeLabel = sizeLabel, timestamp = System.currentTimeMillis(), type = type
        )
        
        val rowId = diaryDao.insertDiaryEntry(entry)
        val entryWithId = entry.copy(id = rowId)
        
        if (coffeeId != null && type == TYPE_CUP && reduceFromPantry) {
            val pantryItems = diaryDao.getPantryItems(user.id).first()
            val pantryItem = pantryItems.find { it.coffeeId == coffeeId }
            if (pantryItem != null) {
                val updatedItem = pantryItem.copy(
                    gramsRemaining = (pantryItem.gramsRemaining - coffeeGrams).coerceAtLeast(0),
                    lastUpdated = System.currentTimeMillis()
                )
                diaryDao.upsertPantryItem(updatedItem)
                externalScope.launch { try { supabaseDataSource.upsertPantryItem(updatedItem) } catch (e: Exception) { } }
            }
        }

        externalScope.launch {
            try {
                // Forzamos el user_id de la sesión activa para evitar violación de RLS
                supabaseDataSource.insertDiaryEntry(entryWithId.copy(userId = user.id))
                diaryDao.deletePendingDiarySync(entryWithId.id)
            } catch (e: Exception) {
                Log.e("DiaryRepository", "RLS Error or Sync Failure: ${e.message}")
                enqueueDiaryEntryForSync(entryWithId.id, e)
            }
        }
    }

    private suspend fun enqueueDiaryEntryForSync(localEntryId: Long, error: Throwable?) {
        val existing = diaryDao.getPendingDiarySyncEntries().find { it.localEntryId == localEntryId }
        diaryDao.upsertPendingDiarySync(
            PendingDiarySyncEntity(
                localEntryId = localEntryId,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                lastAttemptAt = System.currentTimeMillis(),
                retryCount = (existing?.retryCount ?: 0) + 1,
                lastError = error?.message
            )
        )
    }

    suspend fun syncPendingDiaryEntries() = withContext(Dispatchers.IO) {
        if (connectivityObserver.observe().first() != ConnectivityObserver.Status.Available) return@withContext
        val user = userRepository.getActiveUser() ?: return@withContext

        val pendingEntries = diaryDao.getPendingDiarySyncEntries()
        pendingEntries.forEach { pending ->
            val localEntry = diaryDao.getDiaryEntryById(pending.localEntryId)
            if (localEntry == null) {
                diaryDao.deletePendingDiarySync(pending.localEntryId)
                return@forEach
            }

            try {
                supabaseDataSource.upsertDiaryEntry(localEntry.copy(userId = user.id))
                diaryDao.deletePendingDiarySync(pending.localEntryId)
            } catch (e: Exception) {
                Log.e("DiaryRepository", "Retry failed for ${pending.localEntryId}: ${e.message}")
            }
        }
    }

    suspend fun createCustomCoffee(
        name: String, brand: String, specialty: String, roast: String?, variety: String?, 
        country: String, hasCaffeine: Boolean, format: String, imageBytes: ByteArray?,
        totalGrams: Int = 250
    ): String? = withContext(Dispatchers.IO) {
        val user = userRepository.getActiveUser() ?: return@withContext null
        val coffeeId = UUID.randomUUID().toString()
        
        var imageUrl = ""
        if (imageBytes != null) {
            try {
                imageUrl = supabaseDataSource.uploadImage("coffees", "custom/$coffeeId.jpg", imageBytes)
            } catch (e: Exception) { }
        }

        val customCoffee = Coffee(
            id = coffeeId,
            nombre = name,
            marca = brand,
            especialidad = specialty,
            tueste = roast ?: "",
            variedadTipo = variety,
            paisOrigen = country,
            cafeina = if (hasCaffeine) "Sí" else "No",
            formato = format,
            imageUrl = imageUrl,
            isCustom = true,
            userId = user.id
        )

        coffeeDao.insertCoffees(listOf(customCoffee))
        try {
            supabaseDataSource.upsertCustomCoffee(customCoffee)
        } catch (e: Exception) { }

        coffeeId
    }

    suspend fun createCustomCoffeeAndAddToPantry(
        name: String, brand: String, specialty: String, roast: String?, variety: String?, 
        country: String, hasCaffeine: Boolean, format: String, imageBytes: ByteArray?,
        totalGrams: Int = 250
    ): String? = withContext(Dispatchers.IO) {
        val coffeeId = createCustomCoffee(name, brand, specialty, roast, variety, country, hasCaffeine, format, imageBytes, totalGrams)
            ?: return@withContext null

        addToPantry(coffeeId, totalGrams)
        coffeeId
    }

    suspend fun updateCustomCoffee(
        id: String, name: String, brand: String, specialty: String, roast: String?, 
        variety: String?, country: String, hasCaffeine: Boolean, format: String, 
        imageBytes: ByteArray?, totalGrams: Int
    ) = withContext(Dispatchers.IO) {
        val user = userRepository.getActiveUser() ?: return@withContext
        
        var imageUrl = coffeeDao.getCoffeeById(id)?.imageUrl ?: ""
        if (imageBytes != null) {
            try {
                imageUrl = supabaseDataSource.uploadImage("coffees", "custom/$id.jpg", imageBytes)
            } catch (e: Exception) { }
        }

        val customCoffee = Coffee(
            id = id,
            nombre = name,
            marca = brand,
            especialidad = specialty,
            tueste = roast ?: "",
            variedadTipo = variety,
            paisOrigen = country,
            cafeina = if (hasCaffeine) "Sí" else "No",
            formato = format,
            imageUrl = imageUrl,
            isCustom = true,
            userId = user.id
        )

        coffeeDao.insertCoffees(listOf(customCoffee))
        
        val pantryItem = diaryDao.getPantryItems(user.id).first().find { it.coffeeId == id }
        if (pantryItem != null) {
             val updatedItem = pantryItem.copy(totalGrams = totalGrams, lastUpdated = System.currentTimeMillis())
             diaryDao.upsertPantryItem(updatedItem)
             externalScope.launch { try { supabaseDataSource.upsertPantryItem(updatedItem) } catch (e: Exception) { } }
        }

        externalScope.launch {
            try {
                supabaseDataSource.updateCustomCoffee(id, user.id, customCoffee)
            } catch (e: Exception) {
                Log.e("DiaryRepository", "Error updating custom coffee in remote", e)
            }
        }
    }

    suspend fun updatePantryStockFull(coffeeId: String, totalGrams: Int, gramsRemaining: Int) {
        val user = userRepository.getActiveUser() ?: return
        val item = PantryItemEntity(coffeeId, user.id, gramsRemaining, totalGrams)
        diaryDao.upsertPantryItem(item)
        externalScope.launch { try { supabaseDataSource.upsertPantryItem(item) } catch (e: Exception) { } }
    }

    suspend fun addToPantry(coffeeId: String, totalGrams: Int) {
        val user = userRepository.getActiveUser() ?: return
        val existing = diaryDao.getPantryItems(user.id).first().find { it.coffeeId == coffeeId }
        if (existing != null) {
            updatePantryStockFull(coffeeId, existing.totalGrams + totalGrams, existing.gramsRemaining + totalGrams)
        } else {
            updatePantryStockFull(coffeeId, totalGrams, totalGrams)
        }
    }

    suspend fun updateDiaryEntry(entry: DiaryEntryEntity) = withContext(Dispatchers.IO) {
        val user = userRepository.getActiveUser() ?: return@withContext
        diaryDao.insertDiaryEntry(entry)
        externalScope.launch {
            try {
                supabaseDataSource.upsertDiaryEntry(entry.copy(userId = user.id))
                diaryDao.deletePendingDiarySync(entry.id)
            } catch (e: Exception) {
                enqueueDiaryEntryForSync(entry.id, e)
            }
        }
    }

    suspend fun deleteDiaryEntry(entryId: Long) = withContext(Dispatchers.IO) {
        diaryDao.deletePendingDiarySync(entryId)
        diaryDao.deleteDiaryEntryById(entryId)
        externalScope.launch { try { supabaseDataSource.deleteDiaryEntry(entryId) } catch (e: Exception) { } }
    }

    suspend fun deletePantryItem(coffeeId: String) = withContext(Dispatchers.IO) {
        val user = userRepository.getActiveUser() ?: return@withContext
        diaryDao.deletePantryItem(coffeeId, user.id)
        externalScope.launch { try { supabaseDataSource.deletePantryItem(coffeeId, user.id) } catch (e: Exception) { } }
    }

    private suspend fun syncLocalCustomCoffees() = withContext(Dispatchers.IO) {
        val user = userRepository.getActiveUser() ?: return@withContext
        val localCustomCoffees = coffeeDao.getCustomCoffeesByUserId(user.id)
        localCustomCoffees.forEach { customCoffee ->
            try {
                supabaseDataSource.upsertCustomCoffee(customCoffee)
            } catch (e: Exception) { }
        }
    }
}
