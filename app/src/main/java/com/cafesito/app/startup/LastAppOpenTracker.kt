package com.cafesito.app.startup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "cafesito_prefs"
private const val KEY_LAST_APP_OPEN = "last_app_open_timestamp"

/**
 * Guarda y consulta la última vez que la app pasó a primer plano.
 * Se usa para la notificación social "alguien que sigues ha probado un café nuevo":
 * esa notificación solo se muestra si (1) llevas más de 2 días sin abrir la app
 * y (2) a partir de ese momento el backend envía un FCM porque alguien a quien sigues
 * ha probado un café por primera vez; si nadie ha probado nada, el backend no envía
 * mensaje y no salta ninguna notificación.
 */
@Singleton
class LastAppOpenTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun save() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_APP_OPEN, System.currentTimeMillis())
            .apply()
    }

    fun getLastOpenMillis(): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_APP_OPEN, 0L)
    }

    /** True si han pasado más de [days] días desde la última apertura. */
    fun hasBeenInactiveMoreThanDays(days: Int): Boolean {
        val last = getLastOpenMillis()
        if (last == 0L) return true
        return System.currentTimeMillis() - last > days * 24L * 60L * 60L * 1000L
    }
}
