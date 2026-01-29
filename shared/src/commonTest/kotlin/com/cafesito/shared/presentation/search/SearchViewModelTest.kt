package com.cafesito.shared.presentation.search

import com.cafesito.shared.core.DispatcherProvider
import com.cafesito.shared.domain.search.SearchRepository
import com.cafesito.shared.domain.search.SearchResult
import com.cafesito.shared.domain.search.SearchUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private val dispatcherProvider = object : DispatcherProvider {
        override val main = testDispatcher
        override val io = testDispatcher
        override val default = testDispatcher
    }

    @Test
    fun `blank submit clears state`() {
        val viewModel = SearchViewModel(
            dispatcherProvider,
            SearchReducer(),
            SearchUseCase(FakeRepository()),
        )

        viewModel.onIntent(SearchIntent.Submit)

        assertEquals("", viewModel.state.value.query)
        assertTrue(viewModel.state.value.results.isEmpty())
    }

    @Test
    fun `search submit returns results`() {
        val viewModel = SearchViewModel(
            dispatcherProvider,
            SearchReducer(),
            SearchUseCase(FakeRepository()),
        )

        viewModel.onIntent(SearchIntent.QueryChanged("cafe"))
        viewModel.onIntent(SearchIntent.Submit)

        assertEquals(false, viewModel.state.value.isLoading)
        assertEquals(1, viewModel.state.value.results.size)
    }

    private class FakeRepository : SearchRepository {
        override suspend fun search(query: String): List<SearchResult> = listOf(
            SearchResult("1", "Cafe", "Test")
        )
    }
}
