package com.cafesito.app.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import com.cafesito.app.BuildConfig
import com.google.android.gms.tagmanager.DataLayer
import com.google.android.gms.tagmanager.TagManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAnalytics: FirebaseAnalytics
) {
    private val gtmDataLayer: DataLayer? =
        if (BuildConfig.GTM_CONTAINER_ID.isNotEmpty()) {
            TagManager.getInstance(context).getDataLayer()
        } else null

    init {
        firebaseAnalytics.setUserProperty("platform", "android")
        gtmDataLayer?.push(DataLayer.mapOf("platform", "android", "event", "gtm_platform_ready"))
    }

    /**
     * Registra un evento de vista de pantalla. Envía a Firebase y, si GTM está configurado, al dataLayer.
     */
    fun trackScreenView(screenName: String) {
        val safeScreen = screenName.take(100)
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, safeScreen)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, safeScreen)
        }
        gtmDataLayer?.push(
            DataLayer.mapOf(
                "event", "screen_view",
                "screen_name", safeScreen,
                "screen_class", safeScreen
            )
        )
    }

    /**
     * Registra un evento personalizado. Envía a Firebase y, si GTM está configurado, al dataLayer.
     */
    fun trackEvent(name: String, params: Bundle = bundleOf()) {
        val sanitized = sanitizeEventName(name)
        if (sanitized == null) {
            Log.w("Analytics", "Evento descartado por nombre inválido: $name")
            return
        }
        firebaseAnalytics.logEvent(sanitized, params)
        val gtmParams = mutableMapOf<String, Any>("event" to sanitized)
        params.keySet()?.forEach { key ->
            @Suppress("UNCHECKED_CAST")
            (params.get(key) as? String)?.let { gtmParams[key] = it }
            (params.get(key) as? Boolean)?.let { gtmParams[key] = it }
        }
        gtmDataLayer?.push(gtmParams)
    }

    /**
     * Establece el ID de usuario. Envía a Firebase y, si GTM está configurado, al dataLayer (evento set_user_id).
     */
    fun setUserId(userId: String?) {
        firebaseAnalytics.setUserId(userId)
        gtmDataLayer?.push(
            DataLayer.mapOf(
                "event", "set_user_id",
                "user_id", (userId ?: "")
            )
        )
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
