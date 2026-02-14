package com.cafesito.app.ui.utils

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.compose.ui.graphics.toArgb
import com.cafesito.app.ui.theme.EspressoDeep
import com.cafesito.app.ui.theme.NightEspresso
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// --- ONBOARDING DATA ---
data class OnboardingPage(val title: String, val description: String, val icon: String)

val onboardingPages = listOf(
    OnboardingPage("¡Bienvenido!", "La comunidad para los amantes del café de especialidad.", "☕"),
    OnboardingPage("Comparte tu Pasión", "Publica tus momentos cafeteros y descubre otros tipos de café.", "📸"),
    OnboardingPage("Tu Diario de Cata", "Valora cada café y crea tu propio perfil sensorial personalizado.", "📊")
)

// --- CAFFEINE LOGIC ---
object CaffeineCalculator {
    private val baseCaffeineMap = mapOf(
        "Espresso" to 63, "Marroqui" to 95, "Aguamiles" to 95, "Americano" to 95,
        "Capuchino" to 95, "Latte" to 95, "Macchiato" to 95, "Moca" to 105,
        "Vienés" to 80, "Irlandés" to 96, "Corretto" to 63, "Frappuccino" to 80,
        "Caramelo macchiato" to 90, "Freddo" to 80, "Latte macchiato" to 95,
        "Leche con chocolate" to 75, "Marroquí" to 95, "Romano" to 70,
        "Descafeinado" to 10
    )
    fun calculate(type: String, grams: Int?, isFromPantry: Boolean, isDecaf: Boolean = false): Int {
        val base = baseCaffeineMap[type] ?: 80
        val adjustedBase = if (isDecaf) (base * 0.1f).roundToInt() else base
        if (!isFromPantry || grams == null) return adjustedBase
        return (adjustedBase * (grams.toFloat() / 15f)).roundToInt()
    }
}

// --- FORMATTING ---
fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val weeks = days / 7

    return when {
        minutes < 1 -> "ahora"
        minutes < 60 -> "hace $minutes min"
        hours < 24 -> "hace $hours h"
        days < 7 -> "hace $days d"
        weeks < 4 -> "hace $weeks sem"
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

fun String.capitalizeWords(): String = if (this.isBlank()) "" else {
    this.lowercase(Locale.getDefault()).split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}

// --- NAVIGATION ---

fun openCustomTab(context: Context, url: String) {
    try {
        val lightParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(EspressoDeep.toArgb())
            .build()
            
        val darkParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(NightEspresso.toArgb())
            .build()
            
        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_LIGHT, lightParams)
            .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, darkParams)
            .build()
        intent.launchUrl(context, Uri.parse(url))
    } catch (e: Exception) {}
}
