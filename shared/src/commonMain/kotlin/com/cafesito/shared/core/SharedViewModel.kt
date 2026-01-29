package com.cafesito.shared.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

open class SharedViewModel {
    private val job = SupervisorJob()
    protected val scope: CoroutineScope = CoroutineScope(job)

    open fun clear() {
        scope.cancel()
    }
}
