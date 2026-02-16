package com.cafesito.app.ui.timeline

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.cafesito.app.MainActivity
import com.cafesito.app.R
import com.cafesito.app.notifications.NotificationActionReceiver
import com.cafesito.app.notifications.NotificationChannels

object TimelineNotificationSystem {
    const val CHANNEL_ID = "cafesito_notifications"
    const val EXTRA_TYPE = "timeline_notification_type"
    const val EXTRA_USER_ID = "timeline_notification_user_id"
    const val EXTRA_POST_ID = "timeline_notification_post_id"
    const val EXTRA_COMMENT_ID = "timeline_notification_comment_id"
    const val TYPE_FOLLOW = "follow"
    const val TYPE_MENTION = "mention"
    const val TYPE_COMMENT = "comment"
    private const val NAV_TYPE_KEY = "nav_type"

    fun ensureChannel(context: Context) {
        NotificationChannels.ensureCreated(context)
    }

    fun buildPendingIntent(context: Context, notification: TimelineNotification): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            when (notification) {
                is TimelineNotification.Follow -> {
                    putExtra(NAV_TYPE_KEY, "FOLLOW")
                    putExtra("nav_id", notification.user.id.toString())
                    putExtra(EXTRA_TYPE, TYPE_FOLLOW)
                    putExtra(EXTRA_USER_ID, notification.user.id)
                }
                is TimelineNotification.Mention -> {
                    putExtra(NAV_TYPE_KEY, "MENTION")
                    putExtra("nav_id", notification.postId)
                    putExtra("nav_comment_id", notification.commentId)
                    putExtra(EXTRA_TYPE, TYPE_MENTION)
                    putExtra(EXTRA_POST_ID, notification.postId)
                    putExtra(EXTRA_COMMENT_ID, notification.commentId)
                }
                is TimelineNotification.Comment -> {
                    putExtra(NAV_TYPE_KEY, "COMMENT")
                    putExtra("nav_id", notification.postId)
                    putExtra("nav_comment_id", notification.commentId)
                    putExtra(EXTRA_TYPE, TYPE_COMMENT)
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
            is TimelineNotification.Comment -> notification.user.username to notification.message
        }
        val channelId = NotificationChannels.resolveChannel(notification.type)
        val notificationId = notification.id.hashCode()

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(buildPendingIntent(context, notification))
            .setLargeIcon(largeIcon)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(
                0,
                "Marcar leída",
                buildReceiverPendingIntent(
                    context = context,
                    requestCode = notificationId,
                    action = NotificationActionReceiver.ACTION_MARK_READ,
                    notificationId = notificationId
                )
            )

        when (notification) {
            is TimelineNotification.Follow -> {
                builder.addAction(
                    0,
                    "Seguir",
                    buildReceiverPendingIntent(
                        context = context,
                        requestCode = notificationId + 1,
                        action = NotificationActionReceiver.ACTION_FOLLOW_BACK,
                        notificationId = notificationId,
                        targetUserId = notification.user.id
                    )
                )
            }

            is TimelineNotification.Mention,
            is TimelineNotification.Comment -> {
                val postId = when (notification) {
                    is TimelineNotification.Mention -> notification.postId
                    is TimelineNotification.Comment -> notification.postId
                    else -> null
                }
                val commentId = when (notification) {
                    is TimelineNotification.Mention -> notification.commentId
                    is TimelineNotification.Comment -> notification.commentId
                    else -> null
                }

                if (!postId.isNullOrBlank()) {
                    builder.addAction(
                        0,
                        "Responder",
                        PendingIntent.getActivity(
                            context,
                            notificationId + 2,
                            Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra(NAV_TYPE_KEY, if (notification is TimelineNotification.Mention) "MENTION" else "COMMENT")
                                putExtra("nav_id", postId)
                                commentId?.let { putExtra("nav_comment_id", it) }
                                putExtra("notification_action", "reply")
                            },
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                    builder.addAction(
                        0,
                        "Guardar",
                        buildReceiverPendingIntent(
                            context = context,
                            requestCode = notificationId + 3,
                            action = NotificationActionReceiver.ACTION_SAVE_POST,
                            notificationId = notificationId,
                            postId = postId,
                            commentId = commentId
                        )
                    )
                }
            }
        }

        return builder
    }

    private fun buildReceiverPendingIntent(
        context: Context,
        requestCode: Int,
        action: String,
        notificationId: Int,
        targetUserId: Int? = null,
        postId: String? = null,
        commentId: Int? = null
    ): PendingIntent {
        val receiverIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            targetUserId?.let { putExtra(NotificationActionReceiver.EXTRA_TARGET_USER_ID, it) }
            postId?.let { putExtra(NotificationActionReceiver.EXTRA_POST_ID, it) }
            commentId?.let { putExtra(NotificationActionReceiver.EXTRA_COMMENT_ID, it) }
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            receiverIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
