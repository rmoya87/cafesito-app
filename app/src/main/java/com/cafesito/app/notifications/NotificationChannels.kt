package com.cafesito.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val CHANNEL_GENERAL = "cafesito_general"
    const val CHANNEL_SOCIAL = "cafesito_social"
    const val CHANNEL_MENTIONS = "cafesito_mentions"
    const val CHANNEL_BREW_TIMER = "cafesito_brew_timer"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = listOf(
            NotificationChannel(CHANNEL_GENERAL, "Cafesito general", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notificaciones generales de Cafesito"
            },
            NotificationChannel(CHANNEL_SOCIAL, "Actividad social", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Seguidores y actividad social"
            },
            NotificationChannel(CHANNEL_MENTIONS, "Menciones y comentarios", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Menciones directas y comentarios"
            },
            NotificationChannel(CHANNEL_BREW_TIMER, "Timer de elaboración", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Timer activo de Brew Lab (no descartable)"
                setShowBadge(false)
            }
        )
        manager.createNotificationChannels(channels)
    }

    fun resolveChannel(type: String): String = when (type.uppercase()) {
        "FOLLOW" -> CHANNEL_SOCIAL
        "MENTION", "COMMENT" -> CHANNEL_MENTIONS
        "FIRST_COFFEE", "FOLLOWED_FIRST_COFFEE" -> CHANNEL_SOCIAL
        else -> CHANNEL_GENERAL
    }
}
