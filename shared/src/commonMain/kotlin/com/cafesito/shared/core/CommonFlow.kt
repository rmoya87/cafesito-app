package com.cafesito.shared.core

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

fun interface Closeable {
    fun close()
}

class CommonFlow<T>(private val flow: Flow<T>) {
    fun watch(block: (T) -> Unit): Closeable {
        val scope = MainScope()
        val job = scope.launch {
            flow.collect { block(it) }
        }
        return Closeable {
            job.cancel()
            scope.cancel()
        }
    }
}

fun <T> Flow<T>.asCommonFlow(): CommonFlow<T> = CommonFlow(this)
