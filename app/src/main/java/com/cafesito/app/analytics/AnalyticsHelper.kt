package com.cafesito.app.analytics

import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsHelper @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) {

    /**
     * Registra un evento de vista de pantalla.
     */
    fun trackScreenView(screenName: String) {
        val safeScreen = screenName.take(100)
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, safeScreen)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, safeScreen)
        }
    }

    /**
     * Registra un evento personalizado.
     * @param name Nombre del evento.
     * @param params Parámetros adicionales.
     */
    fun trackEvent(name: String, params: Bundle = bundleOf()) {
        val sanitized = sanitizeEventName(name)
        if (sanitized == null) {
            Log.w("Analytics", "Evento descartado por nombre inválido: $name")
            return
        }
        firebaseAnalytics.logEvent(sanitized, params)
    }

    /**
     * Establece el ID de usuario para Analytics.
     */
    fun setUserId(userId: String?) {
        firebaseAnalytics.setUserId(userId)
    }

    /**
     * Establece propiedades de usuario (ej: tipo de suscriptor).
     */
    fun setUserProperty(name: String, value: String?) {
        firebaseAnalytics.setUserProperty(name.take(24), value?.take(36))
    }

    private fun sanitizeEventName(rawName: String): String? {
        val cleaned = rawName
            .lowercase()
            .replace(Regex("[^a-z0-9_]"), "_")
            .trim('_')
            .take(40)

        if (cleaned.isBlank()) return null
        if (!cleaned.first().isLetter()) return "evt_$cleaned".take(40)
        return cleaned
    }
}
