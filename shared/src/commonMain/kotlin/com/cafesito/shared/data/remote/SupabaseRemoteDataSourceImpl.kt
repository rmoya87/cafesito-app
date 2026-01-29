package com.cafesito.shared.data.remote

import com.cafesito.shared.data.model.CustomCoffeeDto
import com.cafesito.shared.data.model.DiaryEntryDto
import com.cafesito.shared.data.model.FavoriteDto
import com.cafesito.shared.data.model.PantryItemDto
import com.cafesito.shared.data.model.ReviewDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.postgrest

class SupabaseRemoteDataSourceImpl(
    private val client: SupabaseClient
) : SupabaseRemoteDataSource {
    override suspend fun signInWithGoogleIdToken(idToken: String): String? {
        client.auth.signInWith(IDToken) {
            this.idToken = idToken
            provider = Google
        }
        return client.auth.currentUserOrNull()?.id
    }

    override suspend fun signOut() {
        client.auth.signOut()
    }

    override suspend fun getCustomCoffees(userId: Int): List<CustomCoffeeDto> =
        client.postgrest["custom_coffees"].select {
            filter { eq("user_id", userId) }
        }.decodeList()

    override suspend fun upsertCustomCoffee(coffee: CustomCoffeeDto) {
        client.postgrest["custom_coffees"].upsert(coffee)
    }

    override suspend fun insertDiaryEntry(entry: DiaryEntryDto) {
        client.postgrest["diary_entries"].insert(entry)
    }

    override suspend fun getDiaryEntries(userId: Int): List<DiaryEntryDto> =
        client.postgrest["diary_entries"].select {
            filter { eq("user_id", userId) }
        }.decodeList()

    override suspend fun upsertPantryItem(item: PantryItemDto) {
        client.postgrest["pantry_items"].upsert(item)
    }

    override suspend fun getPantryItems(userId: Int): List<PantryItemDto> =
        client.postgrest["pantry_items"].select {
            filter { eq("user_id", userId) }
        }.decodeList()

    override suspend fun getFavorites(userId: Int): List<FavoriteDto> =
        client.postgrest["local_favorites"].select {
            filter { eq("user_id", userId) }
        }.decodeList()

    override suspend fun upsertFavorite(favorite: FavoriteDto) {
        client.postgrest["local_favorites"].upsert(favorite)
    }

    override suspend fun deleteFavorite(coffeeId: String, userId: Int) {
        client.postgrest["local_favorites"].delete {
            filter {
                eq("coffee_id", coffeeId)
                eq("user_id", userId)
            }
        }
    }

    override suspend fun upsertReview(review: ReviewDto) {
        client.postgrest["reviews_db"].upsert(review)
    }
}
