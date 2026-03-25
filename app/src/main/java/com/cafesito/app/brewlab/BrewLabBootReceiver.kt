package com.cafesito.app.brewlab

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Reconcilia el timer de Brew Lab tras reinicio del dispositivo y muestra
 * una notificación para continuar/cancelar si había una elaboración en curso.
 */
class BrewLabBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences(BrewLabTimerService.PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastBootHandledAt = prefs.getLong(BrewLabTimerService.KEY_LAST_BOOT_HANDLED_AT, 0L)
        // Algunos OEM disparan BOOT_COMPLETED y LOCKED_BOOT_COMPLETED con poca diferencia.
        if (now - lastBootHandledAt < BOOT_DEDUP_WINDOW_MS) return

        val wasRunning = prefs.getBoolean(BrewLabTimerService.KEY_RUNNING, false)
        if (!wasRunning) return

        val total = prefs.getInt(BrewLabTimerService.KEY_TOTAL, 0)
        val elapsed = prefs.getInt(BrewLabTimerService.KEY_ELAPSED, 0)
        val method = prefs.getString(BrewLabTimerService.KEY_METHOD, "").orEmpty()
        val isPaused = prefs.getBoolean(BrewLabTimerService.KEY_PAUSED, false)
        val lastUpdatedAt = prefs.getLong(BrewLabTimerService.KEY_LAST_UPDATED_AT, 0L)
        if (total <= 0 || method.isBlank()) return

        val extraElapsed = if (!isPaused && lastUpdatedAt > 0L) {
            ((System.currentTimeMillis() - lastUpdatedAt) / 1000L).toInt().coerceAtLeast(0)
        } else {
            0
        }
        val adjustedElapsed = (elapsed + extraElapsed).coerceAtMost(total)
        val remaining = (total - adjustedElapsed).coerceAtLeast(0)

        prefs.edit()
            .putLong(BrewLabTimerService.KEY_LAST_BOOT_HANDLED_AT, now)
            .putInt(BrewLabTimerService.KEY_ELAPSED, adjustedElapsed)
            .putLong(BrewLabTimerService.KEY_LAST_UPDATED_AT, now)
            .apply()

        if (remaining <= 0) {
            prefs.edit()
                .putBoolean(BrewLabTimerService.KEY_RUNNING, false)
                .putBoolean(BrewLabTimerService.KEY_JUST_ENDED, true)
                .apply()
            BrewLabTimerService.showRegisterAfterRebootNotification(context)
            return
        }

        BrewLabTimerService.showResumeAfterRebootNotification(
            context = context,
            methodName = method,
            remainingSeconds = remaining
        )
    }

    companion object {
        private const val BOOT_DEDUP_WINDOW_MS = 15_000L
    }
}
