package com.cafesito.app.data

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Ids de paso del tour por pestaña; mismos valores en WebApp (`appTabTour.ts`). */
object AppTabTour {
    const val STEP_HOME = "home"
    const val STEP_SEARCH = "search"
    const val STEP_BREWLAB = "brewlab"
    const val STEP_DIARY = "diary"
    const val STEP_PROFILE = "profile"

    val ALL_STEPS = listOf(STEP_HOME, STEP_SEARCH, STEP_BREWLAB, STEP_DIARY, STEP_PROFILE)

    private val json = Json { ignoreUnknownKeys = true }

    fun parseDismissedSteps(raw: String?): Set<String> {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return emptySet()
        return try {
            json.decodeFromString(ListSerializer(String.serializer()), s).toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun encodeDismissedSteps(steps: Set<String>): String =
        json.encodeToString(ListSerializer(String.serializer()), steps.sorted())

    /**
     * Devuelve el paso de tour para la ruta actual de NavHost (patrón con `{args}`) o null.
     * @param profileUserIdArg `userId` del destino `profile/{userId}`; en otras rutas ignorar.
     */
    fun resolveStepForNavRoute(routePattern: String, profileUserIdArg: Int?, ownUserId: Int?): String? {
        val base = routePattern.substringBefore("?").trim()
        val self = ownUserId ?: return null
        return when {
            base == "home" -> STEP_HOME
            base.startsWith("search") -> STEP_SEARCH
            base.startsWith("brewlab") || base == "brewlab_select_coffee" -> STEP_BREWLAB
            base.startsWith("diary") -> STEP_DIARY
            base.startsWith("profile/") || base == "profile/{userId}" -> {
                val uid = profileUserIdArg ?: return null
                if (uid == 0 || uid == self) STEP_PROFILE else null
            }
            else -> null
        }
    }
}

fun UserEntity.isAppTabTourGloballySkipped(): Boolean =
    (appTourSkippedAt ?: 0L) > 0L

fun UserEntity.shouldShowAppTabTourStep(stepId: String): Boolean {
    if (isAppTabTourGloballySkipped()) return false
    return stepId !in AppTabTour.parseDismissedSteps(appTourDismissedSteps)
}
