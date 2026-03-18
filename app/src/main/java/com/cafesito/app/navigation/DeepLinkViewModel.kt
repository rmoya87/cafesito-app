package com.cafesito.app.navigation

import com.cafesito.app.data.SupabaseDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Resuelve datos para deep links (p. ej. listId → ownerId para abrir lista compartida desde enlace).
 */
@HiltViewModel
class DeepLinkViewModel @Inject constructor(
    private val supabaseDataSource: SupabaseDataSource
) : androidx.lifecycle.ViewModel() {

    /**
     * Obtiene el user_id del dueño de la lista para navegar a profile/{ownerId}/list/{listId}.
     * Devuelve null si la lista no existe o no hay acceso.
     */
    suspend fun getListOwnerId(listId: String): Int? =
        supabaseDataSource.getUserListById(listId)?.userId?.toInt()

    /**
     * Info mínima de lista para pantalla "Unirse por enlace" (solo listas públicas o por invitación).
     * Devuelve null si la lista no existe o no permite unirse por enlace.
     */
    suspend fun getListInfoForJoin(listId: String): SupabaseDataSource.ListInfoForJoin? =
        supabaseDataSource.getListInfoForJoin(listId)
}
