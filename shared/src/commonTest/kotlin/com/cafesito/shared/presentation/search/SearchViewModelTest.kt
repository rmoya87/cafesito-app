package com.cafesito.shared.presentation.search

import com.cafesito.shared.domain.search.CoffeeSummary
import com.cafesito.shared.domain.search.SearchCoffeesUseCase
import com.cafesito.shared.domain.search.SearchFilters
import com.cafesito.shared.domain.search.SearchRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class SearchViewModelTest {
    @Test
    fun updatesFiltersOnIntent() = runTest {
        val viewModel = SearchViewModel(SearchCoffeesUseCase(FakeRepository()), TestScope(testScheduler))

        viewModel.handle(SearchIntent.UpdateQuery("latte"))
        viewModel.handle(SearchIntent.UpdateMinRating(4f))

        val state = viewModel.state.value
        assertEquals("latte", state.filters.query)
        assertEquals(4f, state.filters.minRating)
    }

    @Test
    fun submitSearchUpdatesResults() = runTest {
        val viewModel = SearchViewModel(SearchCoffeesUseCase(FakeRepository()), TestScope(testScheduler))

        viewModel.handle(SearchIntent.UpdateQuery("cafe"))
        viewModel.handle(SearchIntent.SubmitSearch)

        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(1, state.results.size)
        assertEquals("coffee-1", state.results.first().id)
    }

    private class FakeRepository : SearchRepository {
        override suspend fun search(filters: SearchFilters): Result<List<CoffeeSummary>> {
            return Result.success(
                listOf(
                    CoffeeSummary(
                        id = "coffee-1",
                        name = "Cafesito",
                        brand = "Casa",
                        origin = "Colombia",
                        roast = "Medium",
                        rating = 4.5f,
                        imageUrl = null
                    )
                )
            )
        }
    }
}
