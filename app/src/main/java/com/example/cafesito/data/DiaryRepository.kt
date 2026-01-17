package com.example.cafesito.data

import android.util.Log
import kotlinx.coroutines.flow.*
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
        Log.d("DIARY_REPO", "Triggering refresh...")
        _refreshTrigger.tryEmit(Unit)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getDiaryEntries(): Flow<List<DiaryEntryEntity>> = _refreshTrigger.flatMapLatest {
        flow {
            val user = userRepository.getActiveUser() ?: return@flow emit(emptyList())
            try {
                emit(supabaseDataSource.getDiaryEntries(user.id))
            } catch (e: Exception) { emit(emptyList()) }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getPantryItems(): Flow<List<PantryItemWithDetails>> = _refreshTrigger.flatMapLatest {
        flow {
            val user = userRepository.getActiveUser() ?: return@flow emit(emptyList())
            try {
                val items = supabaseDataSource.getPantryItems(user.id)
                val publicCoffees = try { supabaseDataSource.getAllCoffees() } catch (e: Exception) { emptyList() }
                val customCoffees = try { supabaseDataSource.getCustomCoffees(user.id).map { it.toCoffee() } } catch (e: Exception) { emptyList() }
                val allAvailable = publicCoffees + customCoffees
                
                val details = items.mapNotNull { item ->
                    allAvailable.find { it.id == item.coffeeId }?.let { PantryItemWithDetails(item, it) }
                }
                emit(details)
            } catch (e: Exception) { emit(emptyList()) }
        }
    }

    suspend fun addDiaryEntry(coffeeId: String?, coffeeName: String, caffeineAmount: Int, type: String = "CUP", amountMl: Int = 250) {
        val user = userRepository.getActiveUser() ?: return
        try {
            val entry = DiaryEntryEntity(userId = user.id, coffeeId = coffeeId, coffeeName = coffeeName, caffeineAmount = caffeineAmount, amountMl = amountMl, type = type)
            supabaseDataSource.insertDiaryEntry(entry)
            if (coffeeId != null && type == "CUP") updatePantryStock(coffeeId, -15) 
            triggerRefresh()
        } catch (e: Exception) { }
    }

    suspend fun deleteDiaryEntry(entryId: Long) {
        try {
            supabaseDataSource.deleteDiaryEntry(entryId)
            triggerRefresh()
        } catch (e: Exception) { }
    }

    suspend fun createCustomCoffeeAndAddToPantry(
        name: String, brand: String, specialty: String, roast: String?, variety: String?, 
        country: String, hasCaffeine: Boolean, format: String, imageBytes: ByteArray?,
        totalGrams: Int = 250
    ) {
        val user = userRepository.getActiveUser() ?: return
        try {
            val coffeeId = UUID.randomUUID().toString()
            var imageUrl = ""
            if (imageBytes != null) {
                try { imageUrl = supabaseDataSource.uploadImage("coffees", "custom_$coffeeId.jpg", imageBytes) } catch (e: Exception) { }
            }

            val customCoffee = CustomCoffeeEntity(id = coffeeId, userId = user.id, name = name, brand = brand, specialty = specialty, roast = roast, variety = variety, country = country, hasCaffeine = hasCaffeine, format = format, imageUrl = imageUrl)
            supabaseDataSource.insertCustomCoffee(customCoffee)
            
            // Añadir al stock
            addToPantry(coffeeId, totalGrams, isCustom = true)
            
            // FORZAR REFRESCO PARA QUE APAREZCA EN EL SLIDER
            triggerRefresh()
        } catch (e: Exception) { }
    }

    suspend fun addToPantry(coffeeId: String, totalGrams: Int, isCustom: Boolean = false) {
        val user = userRepository.getActiveUser() ?: return
        try {
            val item = PantryItemEntity(coffeeId = coffeeId, userId = user.id, gramsRemaining = totalGrams, totalGrams = totalGrams, isCustom = isCustom, lastUpdated = System.currentTimeMillis())
            supabaseDataSource.upsertPantryItem(item)
            triggerRefresh()
        } catch (e: Exception) { }
    }

    private suspend fun updatePantryStock(coffeeId: String, deltaGrams: Int) {
        val user = userRepository.getActiveUser() ?: return
        try {
            val items = supabaseDataSource.getPantryItems(user.id)
            items.find { it.coffeeId == coffeeId }?.let { existing ->
                val updated = existing.copy(gramsRemaining = (existing.gramsRemaining + deltaGrams).coerceAtLeast(0), lastUpdated = System.currentTimeMillis())
                supabaseDataSource.upsertPantryItem(updated)
            }
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
