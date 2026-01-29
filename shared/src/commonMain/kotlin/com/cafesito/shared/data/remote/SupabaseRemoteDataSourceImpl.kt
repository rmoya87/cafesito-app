package com.cafesito.shared.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class SupabaseRemoteDataSourceImpl(
    private val client: SupabaseClient
) : SupabaseRemoteDataSource {
    override suspend fun getCustomCoffees(userId: Int): List<CustomCoffeeDto> {
        return client.postgrest["custom_coffees"].select {
            filter { eq("user_id", userId) }
        }.decodeList()
    }

    override suspend fun insertCustomCoffee(dto: CustomCoffeeDto) {
        client.postgrest["custom_coffees"].insert(dto)
    }

    override suspend fun updateCustomCoffee(dto: CustomCoffeeDto) {
        client.postgrest["custom_coffees"].update({
            set("name", dto.name)
            set("brand", dto.brand)
            set("specialty", dto.specialty)
            set("roast", dto.roast)
            set("variety", dto.variety)
            set("country", dto.country)
            set("has_caffeine", dto.hasCaffeine)
            set("format", dto.format)
            set("image_url", dto.imageUrl)
            set("total_grams", dto.totalGrams)
        }) {
            filter {
                eq("id", dto.id)
                eq("user_id", dto.userId)
            }
        }
    }

    override suspend fun deleteCustomCoffee(id: String, userId: Int) {
        client.postgrest["custom_coffees"].delete {
            filter {
                eq("id", id)
                eq("user_id", userId)
            }
        }
    }

    override suspend fun getDiaryEntries(userId: Int): List<DiaryEntryDto> {
        return client.postgrest["diary_entries"].select {
            filter { eq("user_id", userId) }
            order("timestamp", Order.DESCENDING)
        }.decodeList()
    }

    override suspend fun insertDiaryEntry(dto: DiaryEntryInsertDto): DiaryEntryDto {
        return client.postgrest["diary_entries"].insert(dto) { select() }.decodeSingle()
    }

    override suspend fun deleteDiaryEntry(entryId: Long) {
        client.postgrest["diary_entries"].delete {
            filter { eq("id", entryId) }
        }
    }

    override suspend fun getPantryItems(userId: Int): List<PantryItemDto> {
        return client.postgrest["pantry_items"].select {
            filter { eq("user_id", userId) }
        }.decodeList()
    }

    override suspend fun upsertPantryItem(dto: PantryItemDto) {
        client.postgrest["pantry_items"].upsert(dto)
    }

    override suspend fun deletePantryItem(coffeeId: String, userId: Int) {
        client.postgrest["pantry_items"].delete {
            filter {
                eq("coffee_id", coffeeId)
                eq("user_id", userId)
            }
        }
    }

    override suspend fun getFavorites(userId: Int, isCustom: Boolean): List<FavoriteDto> {
        return client.postgrest[favoriteTable(isCustom)].select {
            filter { eq("user_id", userId) }
        }.decodeList()
    }

    override suspend fun upsertFavorite(dto: FavoriteDto, isCustom: Boolean) {
        client.postgrest[favoriteTable(isCustom)].upsert(dto)
    }

    override suspend fun deleteFavorite(coffeeId: String, userId: Int, isCustom: Boolean) {
        client.postgrest[favoriteTable(isCustom)].delete {
            filter {
                eq("coffee_id", coffeeId)
                eq("user_id", userId)
            }
        }
    }

    override suspend fun getRatings(coffeeId: String): List<RatingDto> {
        return client.postgrest["reviews_db"].select {
            filter { eq("coffee_id", coffeeId) }
        }.decodeList()
    }

    override suspend fun upsertRating(dto: RatingUpsertDto) {
        client.postgrest["reviews_db"].upsert(dto, onConflict = "coffee_id,user_id")
    }

    override suspend fun deleteRating(coffeeId: String, userId: Int) {
        client.postgrest["reviews_db"].delete {
            filter {
                eq("coffee_id", coffeeId)
                eq("user_id", userId)
            }
        }
    }

    private fun favoriteTable(isCustom: Boolean): String =
        if (isCustom) "local_favorites_custom" else "local_favorites"
}
