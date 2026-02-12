package com.cafesito.app.analytics

import android.os.Bundle
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
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
    }

    /**
     * Registra un evento personalizado.
     * @param name Nombre del evento.
     * @param params Parámetros adicionales.
     */
    fun trackEvent(name: String, params: Bundle = bundleOf()) {
        firebaseAnalytics.logEvent(name, params)
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
        firebaseAnalytics.setUserProperty(name, value)
    }
}
