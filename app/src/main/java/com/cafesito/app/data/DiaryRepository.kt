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
    private val finishedCoffeeDao: FinishedCoffeeDao,
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
                items.map { item ->
                    val details = coffeesWithDetails.find { it.coffee.id == item.coffeeId }
                    if (details != null) {
                        PantryItemWithDetails(item, details.coffee, details.coffee.isCustom)
                    } else {
                        // Café aún no en la lista (p. ej. recién sincronizado): mostrar ítem con stub para que la despensa no lo oculte
                        PantryItemWithDetails(item, stubCoffeeForId(item.coffeeId), false)
                    }
                }
            }.onStart {
                if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
                    externalScope.launch { syncPantryItems() }
                }
            }
        }
        .flowOn(Dispatchers.IO)

    /** Café mínimo para mostrar un ítem de despensa cuando el detalle aún no está en caché (evita ocultar filas por desfase de emisión). */
    private fun stubCoffeeForId(coffeeId: String): Coffee =
        Coffee(id = coffeeId, nombre = "Café")

    fun getFinishedCoffees(): Flow<List<FinishedCoffeeWithDetails>> = combine(
        _refreshTrigger.onStart { emit(Unit) },
        userRepository.getActiveUserFlow()
    ) { _, activeUser -> activeUser }
        .flatMapLatest { user ->
            if (user == null) return@flatMapLatest flowOf(emptyList())
            combine(
                finishedCoffeeDao.getFinishedCoffeesByUser(user.id),
                coffeeDao.getAllCoffeesWithDetails()
            ) { finishedList, coffeesWithDetails ->
                finishedList.mapNotNull { e ->
                    coffeesWithDetails.find { it.coffee.id == e.coffeeId }?.let { details ->
                        FinishedCoffeeWithDetails(e.finishedAtMs, details.coffee)
                    }
                }
            }.onStart {
                if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
                    externalScope.launch { syncFinishedCoffees() }
                }
            }
        }
        .flowOn(Dispatchers.IO)

    /** Sincroniza historial desde Supabase (fuente de verdad): reemplaza el contenido local. */
    suspend fun syncFinishedCoffees() {
        val user = userRepository.getActiveUser() ?: return
        try {
            val remote = supabaseDataSource.getFinishedCoffees(user.id)
            finishedCoffeeDao.deleteAllForUser(user.id)
            remote.forEach { entity ->
                finishedCoffeeDao.insertFinishedCoffee(
                    FinishedCoffeeEntity(id = entity.id, userId = entity.userId, coffeeId = entity.coffeeId, finishedAtMs = entity.finishedAtMs)
                )
            }
        } catch (e: Exception) {
            Log.e("DiaryRepository", "Error syncing finished coffees", e)
        }
    }

    /** Marca como terminado el registro de despensa con este id (se borra de despensa y se añade a historial). */
    suspend fun markCoffeeAsFinished(pantryItemId: String) = withContext(Dispatchers.IO) {
        val user = userRepository.getActiveUser() ?: return@withContext
        val item = diaryDao.getPantryItemById(pantryItemId) ?: return@withContext
        val now = System.currentTimeMillis()
        supabaseDataSource.insertFinishedCoffee(FinishedCoffeeInsert(userId = user.id, coffeeId = item.coffeeId, finishedAtMs = now))
        diaryDao.deletePantryItemById(pantryItemId)
        supabaseDataSource.deletePantryItemById(pantryItemId)
        syncFinishedCoffees()
    }

    suspend fun syncPantryItems() {
        val user = userRepository.getActiveUser() ?: return
        try {
            val localItems = diaryDao.getPantryItems(user.id).first()
            var remoteItems = supabaseDataSource.getPantryItems(user.id)
            val remoteIds = remoteItems.map { it.id }.toSet()
            // Subir a Supabase los ítems que solo están en local (p. ej. añadidos sin conexión)
            localItems.filter { it.id !in remoteIds }.forEach { localItem ->
                try {
                    supabaseDataSource.upsertPantryItem(localItem)
                } catch (e: Exception) {
                    Log.w("DiaryRepository", "No se pudo subir ítem despensa ${localItem.id}: ${e.message}")
                }
            }
            remoteItems = supabaseDataSource.getPantryItems(user.id)
            val keepIds = remoteItems.map { it.id }
            if (keepIds.isEmpty()) {
                diaryDao.deleteAllPantryItemsForUser(user.id)
            } else {
                diaryDao.deletePantryItemsForUserNotIn(user.id, keepIds)
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
        /** Tipo de entrada: registro de agua (método Agua en elaboración). */
        const val TYPE_WATER = "WATER"
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
        reduceFromPantry: Boolean = true,
        reduceFromPantryItemId: String? = null
    ) = withContext(Dispatchers.IO) {
        val user = userRepository.getActiveUser() ?: return@withContext
        var pantryItemIdUsed: String? = null
        val entry = DiaryEntryEntity(
            userId = user.id, coffeeId = coffeeId, coffeeName = coffeeName,
            coffeeBrand = coffeeBrand, caffeineAmount = caffeineAmount,
            amountMl = amountMl, coffeeGrams = coffeeGrams,
            preparationType = preparationType, sizeLabel = sizeLabel, timestamp = System.currentTimeMillis(), type = type,
            pantryItemId = null
        )

        if (coffeeId != null && type == TYPE_CUP && reduceFromPantry) {
            val pantryItem = reduceFromPantryItemId?.let { diaryDao.getPantryItemById(it) }
                ?: run {
                    val pantryItems = diaryDao.getPantryItems(user.id).first()
                    pantryItems.firstOrNull { it.coffeeId == coffeeId && it.gramsRemaining >= coffeeGrams }
                        ?: pantryItems.firstOrNull { it.coffeeId == coffeeId }
                }
            if (pantryItem != null) {
                pantryItemIdUsed = pantryItem.id
                val newRemaining = (pantryItem.gramsRemaining - coffeeGrams).coerceAtLeast(0)
                val updatedItem = pantryItem.copy(gramsRemaining = newRemaining, lastUpdated = System.currentTimeMillis())
                diaryDao.upsertPantryItem(updatedItem)
                externalScope.launch { try { supabaseDataSource.upsertPantryItem(updatedItem) } catch (e: Exception) { } }
            }
        }

        val entryWithPantryId = entry.copy(pantryItemId = pantryItemIdUsed)
        val rowId = diaryDao.insertDiaryEntry(entryWithPantryId)
        val entryWithId = entryWithPantryId.copy(id = rowId)

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
        
        val pantryItem = diaryDao.getPantryItems(user.id).first().firstOrNull { it.coffeeId == id }
        if (pantryItem != null) {
            val updatedItem = pantryItem.copy(totalGrams = totalGrams, gramsRemaining = totalGrams.coerceAtMost(pantryItem.gramsRemaining), lastUpdated = System.currentTimeMillis())
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

    /** Actualiza un registro de despensa por id (editar total/restante). */
    suspend fun updatePantryStockById(id: String, totalGrams: Int, gramsRemaining: Int) {
        val item = diaryDao.getPantryItemById(id) ?: return
        val updated = item.copy(totalGrams = totalGrams, gramsRemaining = gramsRemaining.coerceIn(0, totalGrams), lastUpdated = System.currentTimeMillis())
        diaryDao.upsertPantryItem(updated)
        externalScope.launch { try { supabaseDataSource.upsertPantryItem(updated) } catch (e: Exception) { } }
    }

    /** Añade un nuevo registro a la despensa (siempre inserta; el mismo café puede estar varias veces). */
    suspend fun addToPantry(coffeeId: String, totalGrams: Int) {
        val user = userRepository.getActiveUser() ?: return
        val id = UUID.randomUUID().toString()
        val item = PantryItemEntity(id = id, coffeeId = coffeeId, userId = user.id, gramsRemaining = totalGrams, totalGrams = totalGrams)
        diaryDao.upsertPantryItem(item)
        externalScope.launch { try { supabaseDataSource.upsertPantryItem(item) } catch (e: Exception) { } }
    }

    /** Elimina un registro de despensa por su id. */
    suspend fun deletePantryItemById(id: String) {
        diaryDao.deletePantryItemById(id)
        externalScope.launch { try { supabaseDataSource.deletePantryItemById(id) } catch (e: Exception) { } }
    }

    suspend fun updateDiaryEntry(entry: DiaryEntryEntity) = withContext(Dispatchers.IO) {
        try {
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
        } catch (e: Exception) {
            Log.e("DiaryRepository", "updateDiaryEntry failed", e)
        }
    }

    /** Elimina una entrada del diario (actividad). Si la entrada tenía pantry_item_id, restaura el stock en ese ítem. */
    suspend fun deleteDiaryEntry(entryId: Long) = withContext(Dispatchers.IO) {
        val entry = diaryDao.getDiaryEntryById(entryId)
        if (entry != null && entry.pantryItemId != null && entry.coffeeGrams > 0 && entry.type == TYPE_CUP) {
            val pantryItem = diaryDao.getPantryItemById(entry.pantryItemId)
            if (pantryItem != null) {
                val newRemaining = (pantryItem.gramsRemaining + entry.coffeeGrams).coerceAtMost(pantryItem.totalGrams)
                val updatedItem = pantryItem.copy(gramsRemaining = newRemaining, lastUpdated = System.currentTimeMillis())
                diaryDao.upsertPantryItem(updatedItem)
                externalScope.launch { try { supabaseDataSource.upsertPantryItem(updatedItem) } catch (e: Exception) { } }
            }
        }
        diaryDao.deletePendingDiarySync(entryId)
        diaryDao.deleteDiaryEntryById(entryId)
        externalScope.launch { try { supabaseDataSource.deleteDiaryEntry(entryId) } catch (e: Exception) { } }
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
