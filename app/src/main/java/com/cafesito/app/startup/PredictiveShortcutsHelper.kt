package com.cafesito.app.startup

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.cafesito.app.MainActivity
import com.cafesito.app.R

/**
 * Gestiona los atajos dinámicos para que el sistema pueda mostrarlos como
 * "Predictive app actions" en la pantalla de aplicaciones recientes
 * (p. ej. "Elaborar café", "Añadir a diario").
 *
 * - [updatePredictiveShortcuts]: publica/actualiza los atajos dinámicos cuando el usuario está autenticado.
 * - [reportShortcutUsed]: notifica al sistema que se usó un atajo (mejora el ranking en sugerencias).
 */
object PredictiveShortcutsHelper {

    private const val SHORTCUT_SEARCH = "search"
    private const val SHORTCUT_BREWLAB = "brewlab"
    private const val SHORTCUT_DIARY = "diary"

    private val PREDICTIVE_IDS = listOf(SHORTCUT_BREWLAB, SHORTCUT_DIARY, SHORTCUT_SEARCH)

    /**
     * Actualiza los atajos dinámicos para que aparezcan como acciones sugeridas
     * en la tarjeta de la app en recientes. Debe llamarse cuando el usuario está autenticado.
     */
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun updatePredictiveShortcuts(context: Context) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
        val maxShortcuts = shortcutManager.maxShortcutCountPerActivity
        if (maxShortcuts < PREDICTIVE_IDS.size) return

        val shortcuts = listOf(
            buildShortcut(context, SHORTCUT_BREWLAB, "cafesito://shortcut/brewlab", context.getString(R.string.shortcut_brewlab_short), context.getString(R.string.shortcut_brewlab_long)),
            buildShortcut(context, SHORTCUT_DIARY, "cafesito://shortcut/diary", context.getString(R.string.shortcut_diary_short), context.getString(R.string.shortcut_diary_long)),
            buildShortcut(context, SHORTCUT_SEARCH, "cafesito://shortcut/search", context.getString(R.string.shortcut_search_short), context.getString(R.string.shortcut_search_long))
        )
        try {
            shortcutManager.dynamicShortcuts = shortcuts
        } catch (e: Exception) {
            // Ignorar si el sistema rechaza (límites, etc.)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun buildShortcut(
        context: Context,
        id: String,
        dataUri: String,
        shortLabel: String,
        longLabel: String
    ): ShortcutInfo {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(dataUri)
            setPackage(context.packageName)
            setClass(context, MainActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return ShortcutInfo.Builder(context, id)
            .setShortLabel(shortLabel)
            .setLongLabel(longLabel)
            .setIntent(intent)
            .build()
    }

    /**
     * Notifica al sistema que el usuario usó un atajo (p. ej. abrió Brew Lab o Diario).
     * Ayuda a que las sugerencias en recientes prioricen las acciones más usadas.
     */
    fun reportShortcutUsed(context: Context, shortcutId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
        if (shortcutId !in PREDICTIVE_IDS) return
        try {
            shortcutManager.reportShortcutUsed(shortcutId)
        } catch (e: Exception) {
            // Ignorar
        }
    }

    /**
     * Devuelve el shortcutId asociado a la ruta actual para reportar uso.
     */
    fun shortcutIdForRoute(route: String): String? = when {
        route == "brewlab" || route.startsWith("brewlab") -> SHORTCUT_BREWLAB
        route == "diary" || route.startsWith("diary") -> SHORTCUT_DIARY
        route == "search" || route.startsWith("search") -> SHORTCUT_SEARCH
        else -> null
    }
}
