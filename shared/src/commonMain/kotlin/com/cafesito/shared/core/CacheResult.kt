package com.cafesito.shared.core

data class CacheResult<T>(
    val data: T,
    val isFresh: Boolean
)
