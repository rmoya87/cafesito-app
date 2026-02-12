package com.cafesito.app.data

import android.util.Log
import com.cafesito.app.ui.utils.ConnectivityObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

val lastSyncMap = mutableMapOf<String, Long>()
const val CACHE_TIMEOUT = 5 * 60 * 1000L 

@OptIn(ExperimentalCoroutinesApi::class)
inline fun <ResultType, RequestType> networkBoundResource(
    resourceKey: String,
    crossinline query: () -> Flow<ResultType>,
    crossinline fetch: suspend () -> RequestType,
    crossinline saveFetchResult: suspend (RequestType) -> Unit,
    crossinline shouldFetch: (ResultType) -> Boolean = { true },
    scope: CoroutineScope,
    connectivityObserver: ConnectivityObserver
) = query().onStart {
    val now = System.currentTimeMillis()
    val lastSync = lastSyncMap[resourceKey] ?: 0L
    
    // Obtenemos el primer valor para decidir si descargar
    val currentData = query().firstOrNull()
    val needsFetch = currentData == null || shouldFetch(currentData) || (now - lastSync > CACHE_TIMEOUT)

    if (needsFetch && connectivityObserver.observe().first() == ConnectivityObserver.Status.Available) {
        lastSyncMap[resourceKey] = now
        scope.launch {
            try {
                val result = fetch()
                saveFetchResult(result)
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Log.e("NetworkBoundResource", "Fetch failed for $resourceKey", e)
                    lastSyncMap[resourceKey] = 0L 
                }
            }
        }
    }
}
