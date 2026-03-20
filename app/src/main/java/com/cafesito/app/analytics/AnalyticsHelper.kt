package com.cafesito.app.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import com.cafesito.app.BuildConfig
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val firebaseAnalytics: FirebaseAnalytics
) {
    private val gtmDataLayer: Any? = initGtmDataLayer()

    private fun initGtmDataLayer(): Any? {
        if (BuildConfig.GTM_CONTAINER_ID.isEmpty()) return null
        return try {
            val tagManagerClass = Class.forName("com.google.android.gms.tagmanager.TagManager")
            val getInstance = tagManagerClass.getMethod("getInstance", Context::class.java)
            val tagManager = getInstance.invoke(null, context)
            val getDataLayer = tagManagerClass.getMethod("getDataLayer")
            getDataLayer.invoke(tagManager)
        } catch (e: Exception) {
            Log.w("Analytics", "GTM DataLayer no disponible", e)
            null
        }
    }

    init {
        firebaseAnalytics.setUserProperty("platform", "android")
        gtmPush("platform", "android", "event", "gtm_platform_ready")
    }

    private fun gtmPush(vararg pairs: Any) {
        if (gtmDataLayer == null || pairs.size % 2 != 0) return
        try {
            val dataLayerClass = Class.forName("com.google.android.gms.tagmanager.DataLayer")
            val mapOf = dataLayerClass.getMethod("mapOf", Array<Any>::class.java)
            val map = mapOf.invoke(null, arrayOf(*pairs))
            val push = gtmDataLayer.javaClass.getMethod("push", Map::class.java)
            push.invoke(gtmDataLayer, map)
        } catch (e: Exception) {
            Log.w("Analytics", "GTM push falló", e)
        }
    }

    private fun gtmPushMap(map: Map<String, Any>) {
        if (gtmDataLayer == null || map.isEmpty()) return
        try {
            val push = gtmDataLayer.javaClass.getMethod("push", Map::class.java)
            push.invoke(gtmDataLayer, map)
        } catch (e: Exception) {
            Log.w("Analytics", "GTM push falló", e)
        }
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
        gtmPush("event", "screen_view", "screen_name", safeScreen, "screen_class", safeScreen)
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
        gtmPushMap(gtmParams)
    }

    /**
     * Establece el ID de usuario. Envía a Firebase y, si GTM está configurado, al dataLayer (evento set_user_id).
     */
    fun setUserId(userId: String?) {
        firebaseAnalytics.setUserId(userId)
        gtmPush("event", "set_user_id", "user_id", (userId ?: ""))
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
