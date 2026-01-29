package com.cafesito.shared.presentation.search

import com.cafesito.shared.domain.search.SearchResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SearchReducerTest {
    private val reducer = SearchReducer()

    @Test
    fun `query updated clears error`() {
        val initial = SearchUiState(errorMessage = "Oops")

        val state = reducer.reduce(initial, SearchMutation.QueryUpdated("latte"))

        assertEquals("latte", state.query)
        assertNull(state.errorMessage)
    }

    @Test
    fun `search started sets loading`() {
        val initial = SearchUiState(query = "cafe")

        val state = reducer.reduce(initial, SearchMutation.SearchStarted)

        assertEquals("cafe", state.query)
        assertEquals(true, state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `search success populates results`() {
        val results = listOf(
            SearchResult("1", "Café", "Origen"),
            SearchResult("2", "Latte", "Con leche"),
        )

        val state = reducer.reduce(SearchUiState(isLoading = true), SearchMutation.SearchSucceeded(results))

        assertFalse(state.isLoading)
        assertEquals(2, state.results.size)
        assertEquals("Café", state.results.first().title)
        assertNull(state.errorMessage)
    }

    @Test
    fun `search failed clears results`() {
        val initial = SearchUiState(
            results = listOf(SearchUiItem("1", "Café", "Origen")),
            isLoading = true,
        )

        val state = reducer.reduce(initial, SearchMutation.SearchFailed("error"))

        assertFalse(state.isLoading)
        assertEquals(0, state.results.size)
        assertEquals("error", state.errorMessage)
    }
}
