package com.cafesito.shared.ios

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class FlowBridge<T : Any>(
    flow: Flow<T>,
    initial: T,
) {
    private val scope: CoroutineScope = MainScope()
    private val mutableState = MutableStateFlow(initial)
    val state: StateFlow<T> = mutableState

    init {
        flow
            .onEach { value -> mutableState.value = value }
            .launchIn(scope)
    }

    fun clear() {
        scope.cancel()
    }
}
