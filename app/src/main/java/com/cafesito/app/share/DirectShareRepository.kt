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
        val ranked = supabaseDataSource.getDirectShareTargets(me.id, limit)
            .mapNotNull { row ->
                val type = when (row.type.trim().lowercase()) {
                    "list" -> DirectShareTargetType.LIST
                    "contact" -> DirectShareTargetType.CONTACT
                    else -> null
                } ?: return@mapNotNull null
                val avatarUrl = if (type == DirectShareTargetType.CONTACT) {
                    resolveContactAvatarUrl(row.id, row.deepLink)
                } else {
                    null
                }
                DirectShareTarget(
                    id = row.id,
                    type = type,
                    label = row.label,
                    deepLink = row.deepLink,
                    rankScore = row.rankScore,
                    avatarUrl = avatarUrl
                )
            }
        if (ranked.isNotEmpty()) return ranked

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

    private suspend fun resolveContactAvatarUrl(rawId: String, deepLink: String): String? {
        val idCandidate = rawId.trim().toIntOrNull()
        if (idCandidate != null) {
            return userRepository.getUserById(idCandidate)?.avatarUrl?.takeIf { it.isNotBlank() }
        }

        val pathSegments = runCatching { java.net.URI(deepLink).path.orEmpty().trim('/').split("/") }
            .getOrDefault(emptyList())
        val profileIdx = pathSegments.indexOf("profile")
        val identifier = if (profileIdx >= 0 && profileIdx < pathSegments.lastIndex) {
            pathSegments[profileIdx + 1]
        } else {
            rawId
        }.trim()

        val userById = identifier.toIntOrNull()?.let { userRepository.getUserById(it) }
        if (userById?.avatarUrl?.isNotBlank() == true) return userById.avatarUrl

        return userRepository.getUserByUsername(identifier)?.avatarUrl?.takeIf { it.isNotBlank() }
    }
}
