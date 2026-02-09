package com.cafesito.app.ui.timeline

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import com.cafesito.app.MainActivity
import com.cafesito.app.R

object TimelineNotificationSystem {
    const val CHANNEL_ID = "cafesito_notifications"
    const val EXTRA_TYPE = "timeline_notification_type"
    const val EXTRA_USER_ID = "timeline_notification_user_id"
    const val EXTRA_POST_ID = "timeline_notification_post_id"
    const val EXTRA_COMMENT_ID = "timeline_notification_comment_id"
    const val TYPE_FOLLOW = "follow"
    const val TYPE_MENTION = "mention"
    private const val NAV_TYPE_KEY = "nav_type"
    private const val NAV_TYPE_NOTIFICATIONS = "NOTIFICATIONS"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notificaciones Cafesito",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun buildPendingIntent(context: Context, notification: TimelineNotification): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NAV_TYPE_KEY, NAV_TYPE_NOTIFICATIONS)
            when (notification) {
                is TimelineNotification.Follow -> {
                    putExtra(EXTRA_TYPE, TYPE_FOLLOW)
                    putExtra(EXTRA_USER_ID, notification.user.id)
                }
                is TimelineNotification.Mention -> {
                    putExtra(EXTRA_TYPE, TYPE_MENTION)
                    putExtra(EXTRA_POST_ID, notification.postId)
                    putExtra(EXTRA_COMMENT_ID, notification.commentId)
                }
            }
        }
        return PendingIntent.getActivity(
            context,
            notification.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildSystemNotification(
        context: Context,
        notification: TimelineNotification,
        largeIcon: Bitmap? = null
    ): NotificationCompat.Builder {
        val (title, message) = when (notification) {
            is TimelineNotification.Follow -> notification.user.username to "ha empezado a seguirte"
            is TimelineNotification.Mention -> notification.user.username to "te ha mencionado en un comentario"
        }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(buildPendingIntent(context, notification))
            .setLargeIcon(largeIcon)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    }
}
