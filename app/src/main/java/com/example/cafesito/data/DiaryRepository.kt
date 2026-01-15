package com.example.cafesito.data

import android.util.Log
import kotlinx.coroutines.flow.*
import java.util.Calendar
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
        Log.d("DIARY_REPO", "Refrescando datos del diario...")
        _refreshTrigger.tryEmit(Unit)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getDiaryEntries(): Flow<List<DiaryEntryEntity>> = _refreshTrigger.flatMapLatest {
        flow {
            val user = userRepository.getActiveUser() ?: return@flow emit(emptyList())
            try {
                val entries = supabaseDataSource.getDiaryEntries(user.id)
                Log.d("DIARY_REPO", "Entradas obtenidas: ${entries.size}")
                emit(entries)
            } catch (e: Exception) {
                Log.e("DIARY_REPO", "Error al obtener diario: ${e.message}")
                emit(emptyList())
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getPantryItems(): Flow<List<PantryItemWithDetails>> = _refreshTrigger.flatMapLatest {
        flow {
            val user = userRepository.getActiveUser() ?: return@flow emit(emptyList())
            try {
                val items = supabaseDataSource.getPantryItems(user.id)
                val coffees = supabaseDataSource.getAllCoffees()
                
                val details = items.mapNotNull { item ->
                    coffees.find { it.id == item.coffeeId }?.let { coffee ->
                        PantryItemWithDetails(item, coffee)
                    }
                }
                Log.d("DIARY_REPO", "Items en despensa: ${details.size}")
                emit(details)
            } catch (e: Exception) {
                Log.e("DIARY_REPO", "Error al obtener despensa: ${e.message}")
                emit(emptyList())
            }
        }
    }

    suspend fun addDiaryEntry(coffeeId: String?, coffeeName: String, caffeineAmount: Int, type: String = "CUP") {
        val user = userRepository.getActiveUser() ?: return
        try {
            val entry = DiaryEntryEntity(
                userId = user.id,
                coffeeId = coffeeId,
                coffeeName = coffeeName,
                caffeineAmount = caffeineAmount,
                timestamp = System.currentTimeMillis(),
                type = type
            )
            supabaseDataSource.insertDiaryEntry(entry)
            
            // Lógica de descuento de stock
            if (coffeeId != null && type == "CUP") {
                updatePantryStock(coffeeId, -15) 
            }
            
            triggerRefresh()
        } catch (e: Exception) {
            Log.e("DIARY_REPO", "Error al registrar consumo: ${e.message}")
        }
    }

    suspend fun deleteDiaryEntry(entryId: Int) {
        try {
            supabaseDataSource.deleteDiaryEntry(entryId)
            triggerRefresh()
        } catch (e: Exception) {
            Log.e("DIARY_REPO", "Error al eliminar entrada: ${e.message}")
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
        } catch (e: Exception) {
            Log.e("DIARY_REPO", "Error al añadir a despensa: ${e.message}")
        }
    }

    private suspend fun updatePantryStock(coffeeId: String, deltaGrams: Int) {
        val user = userRepository.getActiveUser() ?: return
        try {
            val items = supabaseDataSource.getPantryItems(user.id)
            val existing = items.find { it.coffeeId == coffeeId } ?: return
            
            val updated = existing.copy(
                gramsRemaining = (existing.gramsRemaining + deltaGrams).coerceAtLeast(0),
                lastUpdated = System.currentTimeMillis()
            )
            supabaseDataSource.upsertPantryItem(updated)
        } catch (e: Exception) { }
    }

    suspend fun deletePantryItem(coffeeId: String) {
        val user = userRepository.getActiveUser() ?: return
        try {
            supabaseDataSource.deletePantryItem(coffeeId, user.id)
            triggerRefresh()
        } catch (e: Exception) { }
    }
}
