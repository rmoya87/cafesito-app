package com.cafesito.app.widget

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.cafesito.app.BuildConfig
import com.cafesito.app.brewlab.BrewLabTimerService
import java.util.Locale

/**
 * Gate mínimo de rollout por OEM/versión para entrada desde widget en lock/home.
 * Si no está habilitado, degradamos a quick tile a nivel analítico.
 */
object LockEntryFeatureFlags {

    fun widgetEntrySource(context: Context): String {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        val supportedManufacturer =
            manufacturer.contains("google") ||
                manufacturer.contains("xiaomi") ||
                manufacturer.contains("redmi") ||
                manufacturer.contains("poco")
        val supportedSdk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val rolloutEnabled = isInRollout(context, BuildConfig.LOCK_WIDGET_ROLLOUT_PERCENT)
        return if (supportedManufacturer && supportedSdk && rolloutEnabled) {
            BrewLabTimerService.ENTRY_SOURCE_WIDGET
        } else {
            BrewLabTimerService.ENTRY_SOURCE_QUICK_TILE
        }
    }

    private fun isInRollout(context: Context, percent: Int): Boolean {
        val clamped = percent.coerceIn(0, 100)
        if (clamped == 0) return false
        if (clamped == 100) return true

        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ).orEmpty()
        val bucket = (androidId.hashCode() and Int.MAX_VALUE) % 100
        return bucket < clamped
    }
}
