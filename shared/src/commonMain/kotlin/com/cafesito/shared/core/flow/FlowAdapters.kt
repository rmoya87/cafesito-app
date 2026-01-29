package com.cafesito.shared.core.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

fun interface Closeable {
    fun close()
}

class CStateFlow<T>(private val origin: StateFlow<T>) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val value: T
        get() = origin.value

    fun watch(block: (T) -> Unit): Closeable {
        val job = scope.launch {
            origin.collect { block(it) }
        }
        return Closeable { job.cancel() }
    }

    fun close() {
        scope.cancel()
    }
}

fun <T> StateFlow<T>.asCStateFlow(): CStateFlow<T> = CStateFlow(this)
