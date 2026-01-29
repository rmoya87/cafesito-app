package com.cafesito.shared.data.remote

interface SupabaseRemoteDataSource {
    suspend fun getCustomCoffees(userId: Int): List<CustomCoffeeDto>
    suspend fun insertCustomCoffee(dto: CustomCoffeeDto)
    suspend fun updateCustomCoffee(dto: CustomCoffeeDto)
    suspend fun deleteCustomCoffee(id: String, userId: Int)

    suspend fun getDiaryEntries(userId: Int): List<DiaryEntryDto>
    suspend fun insertDiaryEntry(dto: DiaryEntryInsertDto): DiaryEntryDto
    suspend fun deleteDiaryEntry(entryId: Long)

    suspend fun getPantryItems(userId: Int): List<PantryItemDto>
    suspend fun upsertPantryItem(dto: PantryItemDto)
    suspend fun deletePantryItem(coffeeId: String, userId: Int)

    suspend fun getFavorites(userId: Int, isCustom: Boolean): List<FavoriteDto>
    suspend fun upsertFavorite(dto: FavoriteDto, isCustom: Boolean)
    suspend fun deleteFavorite(coffeeId: String, userId: Int, isCustom: Boolean)

    suspend fun getRatings(coffeeId: String): List<RatingDto>
    suspend fun upsertRating(dto: RatingUpsertDto)
    suspend fun deleteRating(coffeeId: String, userId: Int)
}
