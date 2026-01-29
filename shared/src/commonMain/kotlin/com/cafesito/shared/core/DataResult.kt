package com.cafesito.shared.core

import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

sealed class DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>()
    data class Failure(val error: DataError, val cause: Throwable? = null) : DataResult<Nothing>()
}

sealed class DataError {
    data object Network : DataError()
    data object Unauthorized : DataError()
    data object NotFound : DataError()
    data object Timeout : DataError()
    data class Unknown(val message: String? = null) : DataError()
}

suspend fun <T> safeCall(timeoutMs: Long, block: suspend () -> T): DataResult<T> {
    return try {
        val result = withTimeout(timeoutMs) { block() }
        DataResult.Success(result)
    } catch (throwable: Throwable) {
        DataResult.Failure(mapError(throwable), throwable)
    }
}

private fun mapError(throwable: Throwable): DataError {
    return when (throwable) {
        is TimeoutCancellationException -> DataError.Timeout
        is IOException -> DataError.Network
        is HttpRequestException -> DataError.Network
        is ClientRequestException -> if (throwable.response.status.value == 401) {
            DataError.Unauthorized
        } else {
            DataError.Network
        }
        is ServerResponseException -> if (throwable.response.status.value == 404) {
            DataError.NotFound
        } else {
            DataError.Network
        }
        else -> DataError.Unknown(throwable.message)
    }
}
