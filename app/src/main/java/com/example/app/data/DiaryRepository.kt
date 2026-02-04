package com.cafesito.app.data

import android.util.Log
import com.cafesito.app.ui.utils.ConnectivityObserver
import com.cafesito.app.health.HealthConnectRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryRepository @Inject constructor(
    private val diaryDao: DiaryDao,
    private val supabaseDataSource: SupabaseDataSource,
    private val userRepository: UserRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val healthConnectRepository: HealthConnectRepository,
    private val externalScope: CoroutineScope
) {
    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

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
                    } catch (e: Exception) { }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    fun getPantryItems(): Flow<List<PantryItemWithDetails>> = _refreshTrigger.flatMapLatest {
        val user = userRepository.getActiveUserFlow().first() ?: return@flatMapLatest flowOf(emptyList())
        combine(
            diaryDao.getPantryItems(user.id),
            userDaoQueryForCoffees() // Necesitaríamos coffeeDao aquí o a través de coffeeRepository
        ) { items, coffees ->
            items.mapNotNull { item ->
                coffees.find { it.coffee.id == item.coffeeId }?.let { details ->
                    PantryItemWithDetails(item, details.coffee, details.coffee.isCustom)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    // Helper ficticio para simplificar, idealmente usaría CoffeeRepository
    private fun userDaoQueryForCoffees(): Flow<List<CoffeeWithDetails>> = flowOf(emptyList())

    suspend fun addDiaryEntry(
        coffeeId: String?, 
        coffeeName: String, 
        coffeeBrand: String,
        caffeineAmount: Int, 
        type: String = "CUP", 
        amountMl: Int = 250,
        coffeeGrams: Int = 15,
        preparationType: String = "Espresso"
    ) = withContext(Dispatchers.IO) {
        val user = userRepository.getActiveUser() ?: return@withContext
        val entry = DiaryEntryEntity(
            userId = user.id, coffeeId = coffeeId, coffeeName = coffeeName, 
            coffeeBrand = coffeeBrand, caffeineAmount = caffeineAmount, 
            amountMl = amountMl, coffeeGrams = coffeeGrams,
            preparationType = preparationType, timestamp = System.currentTimeMillis(), type = type
        )
        
        diaryDao.insertDiaryEntry(entry)
        
        if (coffeeId != null && type == "CUP") {
            val pantryItem = diaryDao.getPantryItems(user.id).first().find { it.coffeeId == coffeeId }
            if (pantryItem != null) {
                diaryDao.upsertPantryItem(pantryItem.copy(
                    gramsRemaining = (pantryItem.gramsRemaining - coffeeGrams).coerceAtLeast(0),
                    lastUpdated = System.currentTimeMillis()
                ))
            }
        }

        if (connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
            externalScope.launch {
                try {
                    val inserted = supabaseDataSource.insertDiaryEntry(entry)
                    healthConnectRepository.syncDiaryEntry(
                        mg = if (type == "WATER") 0 else caffeineAmount,
                        ml = if (type == "WATER") amountMl else 0,
                        timestamp = entry.timestamp,
                        entryId = inserted.id.toString()
                    )
                } catch (e: Exception) { }
            }
        }
    }

    suspend fun createCustomCoffeeAndAddToPantry(
        name: String, brand: String, specialty: String, roast: String?, variety: String?, 
        country: String, hasCaffeine: Boolean, format: String, imageBytes: ByteArray?,
        totalGrams: Int = 250
    ) {
        val user = userRepository.getActiveUser() ?: return
        val coffeeId = UUID.randomUUID().toString()
        // Lógica de inserción local y remota aquí...
    }

    suspend fun updateCustomCoffee(
        id: String, name: String, brand: String, specialty: String, roast: String?, 
        variety: String?, country: String, hasCaffeine: Boolean, format: String, 
        imageBytes: ByteArray?, totalGrams: Int
    ) {
        // Lógica de actualización
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

    suspend fun deleteDiaryEntry(entryId: Long) = withContext(Dispatchers.IO) {
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

    suspend fun syncExternalHealthData() {
        // Implementar sincronización
    }
}
