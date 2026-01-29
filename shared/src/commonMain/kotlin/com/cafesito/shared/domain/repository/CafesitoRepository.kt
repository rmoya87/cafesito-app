package com.cafesito.shared.domain.repository

import com.cafesito.shared.core.DataResult
import com.cafesito.shared.domain.model.CustomCoffee
import com.cafesito.shared.domain.model.DiaryEntry
import com.cafesito.shared.domain.model.Favorite
import com.cafesito.shared.domain.model.PantryItem
import com.cafesito.shared.domain.model.Rating

interface CafesitoRepository {
    suspend fun getCustomCoffees(userId: Int): DataResult<List<CustomCoffee>>
    suspend fun addCustomCoffee(coffee: CustomCoffee): DataResult<Unit>
    suspend fun updateCustomCoffee(coffee: CustomCoffee): DataResult<Unit>
    suspend fun deleteCustomCoffee(id: String, userId: Int): DataResult<Unit>

    suspend fun getDiaryEntries(userId: Int): DataResult<List<DiaryEntry>>
    suspend fun addDiaryEntry(entry: DiaryEntry): DataResult<DiaryEntry>
    suspend fun deleteDiaryEntry(entryId: Long): DataResult<Unit>

    suspend fun getPantryItems(userId: Int): DataResult<List<PantryItem>>
    suspend fun upsertPantryItem(item: PantryItem): DataResult<Unit>
    suspend fun deletePantryItem(coffeeId: String, userId: Int): DataResult<Unit>

    suspend fun getFavorites(userId: Int, isCustom: Boolean): DataResult<List<Favorite>>
    suspend fun upsertFavorite(favorite: Favorite): DataResult<Unit>
    suspend fun deleteFavorite(coffeeId: String, userId: Int, isCustom: Boolean): DataResult<Unit>

    suspend fun getRatings(coffeeId: String): DataResult<List<Rating>>
    suspend fun upsertRating(rating: Rating): DataResult<Unit>
    suspend fun deleteRating(coffeeId: String, userId: Int): DataResult<Unit>
}
