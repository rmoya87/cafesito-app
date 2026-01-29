package com.cafesito.shared.data.repository

import com.cafesito.shared.core.DataResult
import com.cafesito.shared.core.safeCall
import com.cafesito.shared.data.mappers.toDomain
import com.cafesito.shared.data.mappers.toDto
import com.cafesito.shared.data.mappers.toInsertDto
import com.cafesito.shared.data.mappers.toUpsertDto
import com.cafesito.shared.data.remote.SupabaseRemoteDataSource
import com.cafesito.shared.data.remote.SupabaseRemoteDataSourceImpl
import com.cafesito.shared.domain.model.CustomCoffee
import com.cafesito.shared.domain.model.DiaryEntry
import com.cafesito.shared.domain.model.Favorite
import com.cafesito.shared.domain.model.PantryItem
import com.cafesito.shared.domain.model.Rating
import com.cafesito.shared.domain.repository.CafesitoRepository
class SupabaseCafesitoRepository(
    private val remoteDataSource: SupabaseRemoteDataSource,
    private val timeoutMs: Long = 10_000L
) : CafesitoRepository {
    companion object {
        fun fromClient(client: io.github.jan.supabase.SupabaseClient, timeoutMs: Long = 10_000L): SupabaseCafesitoRepository {
            return SupabaseCafesitoRepository(SupabaseRemoteDataSourceImpl(client), timeoutMs)
        }
    }

    override suspend fun getCustomCoffees(userId: Int): DataResult<List<CustomCoffee>> {
        return safeCall(timeoutMs) {
            remoteDataSource.getCustomCoffees(userId).map { it.toDomain() }
        }
    }

    override suspend fun addCustomCoffee(coffee: CustomCoffee): DataResult<Unit> {
        return safeCall(timeoutMs) {
            remoteDataSource.insertCustomCoffee(coffee.toDto())
        }
    }

    override suspend fun updateCustomCoffee(coffee: CustomCoffee): DataResult<Unit> {
        return safeCall(timeoutMs) {
            remoteDataSource.updateCustomCoffee(coffee.toDto())
        }
    }

    override suspend fun deleteCustomCoffee(id: String, userId: Int): DataResult<Unit> {
        return safeCall(timeoutMs) {
            remoteDataSource.deleteCustomCoffee(id, userId)
        }
    }

    override suspend fun getDiaryEntries(userId: Int): DataResult<List<DiaryEntry>> {
        return safeCall(timeoutMs) {
            remoteDataSource.getDiaryEntries(userId).map { it.toDomain() }
        }
    }

    override suspend fun addDiaryEntry(entry: DiaryEntry): DataResult<DiaryEntry> {
        return safeCall(timeoutMs) {
            remoteDataSource.insertDiaryEntry(entry.toInsertDto()).toDomain()
        }
    }

    override suspend fun deleteDiaryEntry(entryId: Long): DataResult<Unit> {
        return safeCall(timeoutMs) {
            remoteDataSource.deleteDiaryEntry(entryId)
        }
    }

    override suspend fun getPantryItems(userId: Int): DataResult<List<PantryItem>> {
        return safeCall(timeoutMs) {
            remoteDataSource.getPantryItems(userId).map { it.toDomain() }
        }
    }

    override suspend fun upsertPantryItem(item: PantryItem): DataResult<Unit> {
        return safeCall(timeoutMs) {
            remoteDataSource.upsertPantryItem(item.toDto())
        }
    }

    override suspend fun deletePantryItem(coffeeId: String, userId: Int): DataResult<Unit> {
        return safeCall(timeoutMs) {
            remoteDataSource.deletePantryItem(coffeeId, userId)
        }
    }

    override suspend fun getFavorites(userId: Int, isCustom: Boolean): DataResult<List<Favorite>> {
        return safeCall(timeoutMs) {
            remoteDataSource.getFavorites(userId, isCustom).map { it.toDomain(isCustom) }
        }
    }

    override suspend fun upsertFavorite(favorite: Favorite): DataResult<Unit> {
        return safeCall(timeoutMs) {
            remoteDataSource.upsertFavorite(favorite.toDto(), favorite.isCustom)
        }
    }

    override suspend fun deleteFavorite(coffeeId: String, userId: Int, isCustom: Boolean): DataResult<Unit> {
        return safeCall(timeoutMs) {
            remoteDataSource.deleteFavorite(coffeeId, userId, isCustom)
        }
    }

    override suspend fun getRatings(coffeeId: String): DataResult<List<Rating>> {
        return safeCall(timeoutMs) {
            remoteDataSource.getRatings(coffeeId).map { it.toDomain() }
        }
    }

    override suspend fun upsertRating(rating: Rating): DataResult<Unit> {
        return safeCall(timeoutMs) {
            remoteDataSource.upsertRating(rating.toUpsertDto())
        }
    }

    override suspend fun deleteRating(coffeeId: String, userId: Int): DataResult<Unit> {
        return safeCall(timeoutMs) {
            remoteDataSource.deleteRating(coffeeId, userId)
        }
    }
}
