package com.cafesito.app.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cuando el usuario quita un café de una lista, el feed de actividad del perfil debe actualizarse
 * sin esperar a un refresh completo. Los ViewModels que realizan el borrado notifican aquí;
 * [ProfileViewModel] escucha y filtra la tarjeta correspondiente.
 */
@Singleton
class ListActivityRemovalBus @Inject constructor() {
    private val _removals = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 32)
    val removals: SharedFlow<Pair<String, String>> = _removals.asSharedFlow()

    fun notifyListItemRemoved(listId: String, coffeeId: String) {
        _removals.tryEmit(listId to coffeeId)
    }
}
