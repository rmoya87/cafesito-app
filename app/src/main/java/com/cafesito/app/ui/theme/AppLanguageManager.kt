package com.cafesito.app.ui.theme

import android.content.Context
import android.content.ContextWrapper
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object AppLanguageManager {
    private const val PREFS_NAME = "cafesito_prefs"
    private const val KEY_LANGUAGE = "app_language"

    const val SYSTEM = "system"
    private val SUPPORTED = setOf("es", "en", "fr", "pt", "de")

    fun getLanguagePreference(context: Context): String {
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, SYSTEM)
            ?.lowercase(Locale.ROOT)
            ?: SYSTEM
    }

    fun getSupportedLanguages(): List<String> = listOf(SYSTEM, "es", "en", "fr", "pt", "de")

    fun resolveEffectiveLanguage(context: Context, preference: String = getLanguagePreference(context)): String {
        val normalized = preference.lowercase(Locale.ROOT)
        if (normalized != SYSTEM && normalized in SUPPORTED) return normalized
        val systemLanguage = Locale.getDefault().language.lowercase(Locale.ROOT)
        return if (systemLanguage in SUPPORTED) systemLanguage else "en"
    }

    fun applySavedLanguage(context: Context) {
        val preference = getLanguagePreference(context)
        applyLocale(preference, context)
    }

    fun setLanguagePreference(context: Context, preference: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, preference.lowercase(Locale.ROOT))
            .apply()
        applyLocale(preference, context)
        // Fuerza refresco visual inmediato en pantallas Compose activas.
        findActivity(context)?.recreate()
    }

    private fun applyLocale(preference: String, context: Context) {
        val normalized = preference.lowercase(Locale.ROOT)
        if (normalized == SYSTEM) {
            // Seguir siempre el idioma real del sistema.
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            return
        }
        val effective = resolveEffectiveLanguage(context, preference)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(effective))
    }

    private tailrec fun findActivity(context: Context): android.app.Activity? {
        return when (context) {
            is android.app.Activity -> context
            is ContextWrapper -> findActivity(context.baseContext)
            else -> null
        }
    }
}

