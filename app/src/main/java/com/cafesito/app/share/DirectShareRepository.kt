package com.cafesito.app.share

import com.cafesito.app.data.SupabaseDataSource
import com.cafesito.app.data.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DirectShareRepository @Inject constructor(
    private val supabaseDataSource: SupabaseDataSource,
    private val userRepository: UserRepository
) {
    /**
     * Sprint 1: devuelve destinos base (listas del usuario) para arrancar Sharing Shortcuts.
     * En sprint 2 se sustituye por ranking backend real (listas + contactos frecuentes).
     */
    suspend fun getSuggestedTargets(limit: Int = 4): List<DirectShareTarget> {
        val me = userRepository.getActiveUser() ?: return emptyList()
        val lists = supabaseDataSource.getCachedUserListsMerged(me.id).take(limit)
        return lists.mapIndexed { index, list ->
            DirectShareTarget(
                id = list.id,
                type = DirectShareTargetType.LIST,
                label = list.name,
                deepLink = "https://cafesitoapp.com/profile/list/${list.id}",
                rankScore = (limit - index).toDouble()
            )
        }
    }
}
