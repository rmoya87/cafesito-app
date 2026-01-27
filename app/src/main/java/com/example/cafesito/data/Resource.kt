package com.example.cafesito.data

/**
 * Wrapper para manejar estados de carga, éxito y error.
 * Evita mostrar listas vacías cuando hay un error de red.
 */
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}
