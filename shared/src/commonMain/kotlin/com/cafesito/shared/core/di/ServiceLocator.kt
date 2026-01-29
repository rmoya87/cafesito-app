package com.cafesito.shared.core.di

import com.cafesito.shared.data.InMemorySearchRepository
import com.cafesito.shared.domain.repository.SearchRepository
import com.cafesito.shared.domain.search.SearchViewModel

object ServiceLocator {
    private val searchRepository: SearchRepository by lazy { InMemorySearchRepository() }

    fun searchViewModel(): SearchViewModel = SearchViewModel(searchRepository)
}
