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
                }
            }
        }
    }

    fun triggerRefresh() {
        _refreshTrigger.tryEmit(Unit)
    }

    fun getDiaryEntries(): Flow<List<DiaryEntryEntity>> = _refreshTrigger.flatMapLatest {
        val user = userRepository.getActiveUserFlow().first() ?: return@flatMapLatest flowOf(emptyList())
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
    }.flowOn(Dispatchers.IO)

    fun getPantryItems(): Flow<List<PantryItemWithDetails>> = _refreshTrigger.flatMapLatest {
        val user = userRepository.getActiveUserFlow().first() ?: return@flatMapLatest flowOf(emptyList())
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
    }.flowOn(Dispatchers.IO)

    suspend fun syncPantryItems() {
        val user = userRepository.getActiveUser() ?: return
        try {
            val remoteItems = supabaseDataSource.getPantryItems(user.id)
            remoteItems.forEach { item ->
                val localCoffee = coffeeDao.getCoffeeById(item.coffeeId)
                if (localCoffee == null) {
                    val coffeeIds = listOf(item.coffeeId)
                    val remoteCoffee = supabaseDataSource.getCoffeesByIds(coffeeIds).firstOrNull()
                        ?: supabaseDataSource.getCustomCoffees(user.id).find { c -> c.id == item.coffeeId }?.toCoffee()
                    remoteCoffee?.let { c -> coffeeDao.insertCoffees(listOf(c)) }
                } else if (localCoffee.isCustom && localCoffee.imageUrl.isNotBlank()) {
                    // Si es custom y ya tiene imagen local, nos aseguramos de no perderla si el remoto viene vacío por delay de sync
                    val remoteCoffee = supabaseDataSource.getCustomCoffees(user.id).find { it.id == item.coffeeId }?.toCoffee()
                    if (remoteCoffee != null && remoteCoffee.imageUrl.isBlank()) {
                        coffeeDao.insertCoffees(listOf(remoteCoffee.copy(imageUrl = localCoffee.imageUrl)))
                    } else if (remoteCoffee != null) {
                        coffeeDao.insertCoffees(listOf(remoteCoffee))
                    }
                }
                diaryDao.upsertPantryItem(item)
            }
            Log.d("DiaryRepository", "Pantry items synced: ${remoteItems.size}")
        } catch (e: Exception) {
            Log.e("DiaryRepository", "Error syncing pantry items", e)
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
        preparationType: String = "Espresso",
        reduceFromPantry: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val user = userRepository.getActiveUser() ?: return@withContext
        val entry = DiaryEntryEntity(
            userId = user.id, coffeeId = coffeeId, coffeeName = coffeeName, 
            coffeeBrand = coffeeBrand, caffeineAmount = caffeineAmount, 
            amountMl = amountMl, coffeeGrams = coffeeGrams,
            preparationType = preparationType, timestamp = System.currentTimeMillis(), type = type
        )
        
        val rowId = diaryDao.insertDiaryEntry(entry)
        val entryWithId = entry.copy(id = rowId)
        
        if (coffeeId != null && type == "CUP" && reduceFromPantry) {
            val pantryItems = diaryDao.getPantryItems(user.id).first()
            val pantryItem = pantryItems.find { it.coffeeId == coffeeId }
            if (pantryItem != null) {
                val updatedItem = pantryItem.copy(
                    gramsRemaining = (pantryItem.gramsRemaining - coffeeGrams).coerceAtLeast(0),
                    lastUpdated = System.currentTimeMillis()
                )
                diaryDao.upsertPantryItem(updatedItem)
                if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
                    externalScope.launch { try { supabaseDataSource.upsertPantryItem(updatedItem) } catch (e: Exception) { } }
                }
            }
        }

        externalScope.launch {
            val isOnline = connectivityObserver.observe().first() == ConnectivityObserver.Status.Available
            if (isOnline) {
                try {
                    supabaseDataSource.insertDiaryEntry(entryWithId)
                    diaryDao.deletePendingDiarySync(entryWithId.id)
                } catch (e: Exception) {
                    Log.e("DiaryRepository", "Error syncing diary entry to Supabase", e)
                    enqueueDiaryEntryForSync(entryWithId.id, e)
                }
            } else {
                enqueueDiaryEntryForSync(entryWithId.id, IOException("No network available"))
            }
        }
    }


    private suspend fun enqueueDiaryEntryForSync(localEntryId: Long, error: Throwable?) {
        val existing = diaryDao.getPendingDiarySyncEntries().firstOrNull { it.localEntryId == localEntryId }
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

        val pendingEntries = diaryDao.getPendingDiarySyncEntries()
        pendingEntries.forEach { pending ->
            val localEntry = diaryDao.getDiaryEntryById(pending.localEntryId)
            if (localEntry == null) {
                diaryDao.deletePendingDiarySync(pending.localEntryId)
                return@forEach
            }

            try {
                supabaseDataSource.upsertDiaryEntry(localEntry)
                diaryDao.deletePendingDiarySync(pending.localEntryId)
            } catch (e: Exception) {
                enqueueDiaryEntryForSync(pending.localEntryId, e)
            }
        }
    }

    suspend fun createCustomCoffeeAndAddToPantry(
        name: String, brand: String, specialty: String, roast: String?, variety: String?, 
        country: String, hasCaffeine: Boolean, format: String, imageBytes: ByteArray?,
        totalGrams: Int = 250
    ) = withContext(Dispatchers.IO) {
        val user = userRepository.getActiveUser() ?: return@withContext
        val coffeeId = UUID.randomUUID().toString()
        
        var imageUrl = ""
        if (imageBytes != null) {
            try {
                imageUrl = supabaseDataSource.uploadImage("coffees", "custom/$coffeeId.jpg", imageBytes)
            } catch (e: Exception) {
                Log.e("DiaryRepository", "Error uploading custom coffee image", e)
            }
        }

        val customCoffee = CustomCoffeeEntity(
            id = coffeeId, userId = user.id, name = name, brand = brand,
            specialty = specialty, roast = roast, variety = variety,
            country = country, hasCaffeine = hasCaffeine, format = format,
            imageUrl = imageUrl, totalGrams = totalGrams
        )
        
        coffeeDao.insertCoffees(listOf(customCoffee.toCoffee()))
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            try { supabaseDataSource.upsertCustomCoffee(customCoffee) } catch (e: Exception) { }
        }

        addToPantry(coffeeId, totalGrams)
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

        val customCoffee = CustomCoffeeEntity(
            id = id, userId = user.id, name = name, brand = brand,
            specialty = specialty, roast = roast, variety = variety,
            country = country, hasCaffeine = hasCaffeine, format = format,
            imageUrl = imageUrl, totalGrams = totalGrams
        )

        coffeeDao.insertCoffees(listOf(customCoffee.toCoffee()))
        
        val pantryItem = diaryDao.getPantryItems(user.id).first().find { it.coffeeId == id }
        if (pantryItem != null) {
             val updatedItem = pantryItem.copy(totalGrams = totalGrams, lastUpdated = System.currentTimeMillis())
             diaryDao.upsertPantryItem(updatedItem)
             if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
                 externalScope.launch { try { supabaseDataSource.upsertPantryItem(updatedItem) } catch (e: Exception) { } }
             }
        }

        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch {
                try { 
                    supabaseDataSource.updateCustomCoffee(id, user.id, customCoffee) 
                } catch (e: Exception) { } 
            }
        }
    }

    suspend fun updatePantryStockFull(coffeeId: String, totalGrams: Int, gramsRemaining: Int) {
        val user = userRepository.getActiveUser() ?: return
        val item = PantryItemEntity(coffeeId, user.id, gramsRemaining, totalGrams)
        diaryDao.upsertPantryItem(item)
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch { try { supabaseDataSource.upsertPantryItem(item) } catch (e: Exception) { } }
        }
    }

    suspend fun addToPantry(coffeeId: String, totalGrams: Int) {
        updatePantryStockFull(coffeeId, totalGrams, totalGrams)
    }



    suspend fun updateDiaryEntry(entry: DiaryEntryEntity) = withContext(Dispatchers.IO) {
        diaryDao.insertDiaryEntry(entry)

        externalScope.launch {
            val isOnline = connectivityObserver.observe().first() == ConnectivityObserver.Status.Available
            if (isOnline) {
                try {
                    supabaseDataSource.upsertDiaryEntry(entry)
                    diaryDao.deletePendingDiarySync(entry.id)
                } catch (e: Exception) {
                    Log.e("DiaryRepository", "Error updating diary entry in Supabase", e)
                    enqueueDiaryEntryForSync(entry.id, e)
                }
            } else {
                enqueueDiaryEntryForSync(entry.id, IOException("No network available"))
            }
        }
    }

    suspend fun deleteDiaryEntry(entryId: Long) = withContext(Dispatchers.IO) {
        diaryDao.deletePendingDiarySync(entryId)
        diaryDao.deleteDiaryEntryById(entryId)
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch { try { supabaseDataSource.deleteDiaryEntry(entryId) } catch (e: Exception) { } }
        }
    }

    suspend fun deletePantryItem(coffeeId: String) = withContext(Dispatchers.IO) {
        val user = userRepository.getActiveUser() ?: return@withContext
        diaryDao.deletePantryItem(coffeeId, user.id)
        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch { try { supabaseDataSource.deletePantryItem(coffeeId, user.id) } catch (e: Exception) { } }
        }
    }
}
