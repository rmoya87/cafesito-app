package com.cafesito.shared.data.remote

import com.cafesito.shared.data.model.CustomCoffeeDto
import com.cafesito.shared.data.model.DiaryEntryDto
import com.cafesito.shared.data.model.FavoriteDto
import com.cafesito.shared.data.model.PantryItemDto
import com.cafesito.shared.data.model.ReviewDto

interface SupabaseRemoteDataSource {
    suspend fun signInWithGoogleIdToken(idToken: String): String?
    suspend fun signOut()

    suspend fun getCustomCoffees(userId: Int): List<CustomCoffeeDto>
    suspend fun upsertCustomCoffee(coffee: CustomCoffeeDto)

    suspend fun insertDiaryEntry(entry: DiaryEntryDto)
    suspend fun getDiaryEntries(userId: Int): List<DiaryEntryDto>

    suspend fun upsertPantryItem(item: PantryItemDto)
    suspend fun getPantryItems(userId: Int): List<PantryItemDto>

    suspend fun getFavorites(userId: Int): List<FavoriteDto>
    suspend fun upsertFavorite(favorite: FavoriteDto)
    suspend fun deleteFavorite(coffeeId: String, userId: Int)

    suspend fun upsertReview(review: ReviewDto)
}
