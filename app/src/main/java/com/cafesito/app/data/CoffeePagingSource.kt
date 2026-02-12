package com.cafesito.app.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.flow.first

class CoffeePagingSource(
    private val supabaseDataSource: SupabaseDataSource,
    private val query: String? = null,
    private val origins: Set<String> = emptySet(),
    private val roasts: Set<String> = emptySet(),
    private val specialties: Set<String> = emptySet(),
    private val formats: Set<String> = emptySet(),
    private val minRating: Float = 0f
) : PagingSource<Int, Coffee>() {

    override fun getRefreshKey(state: PagingState<Int, Coffee>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Coffee> {
        val position = params.key ?: 0
        val pageSize = params.loadSize
        
        return try {
            val from = position.toLong()
            val to = (from + pageSize - 1)
            
            val coffees = supabaseDataSource.getCoffeesPaginated(
                from = from,
                to = to,
                query = query,
                origins = origins,
                roasts = roasts,
                specialties = specialties,
                formats = formats,
                minRating = minRating
            )
            
            LoadResult.Page(
                data = coffees,
                prevKey = if (position == 0) null else position - pageSize,
                nextKey = if (coffees.isEmpty()) null else position + pageSize
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
