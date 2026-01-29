package com.cafesito.shared.bridge

import com.cafesito.shared.core.CachePolicy
import com.cafesito.shared.core.CacheResult
import com.cafesito.shared.domain.model.Coffee
import com.cafesito.shared.domain.repository.CoffeeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

fun interface Cancelable {
    fun cancel()
}

class CoffeeRepositoryBridge(
    private val repository: CoffeeRepository,
    private val scope: CoroutineScope
) {
    fun observeCoffees(
        policy: CachePolicy,
        onUpdate: (CacheResult<List<Coffee>>) -> Unit
    ): Cancelable {
        val job: Job = scope.launch {
            repository.observeCoffees(policy).collect { result ->
                onUpdate(result)
            }
        }
        return Cancelable { job.cancel() }
    }
}
