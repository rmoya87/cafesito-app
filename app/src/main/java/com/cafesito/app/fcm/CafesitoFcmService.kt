package com.cafesito.app.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cafesito.app.MainActivity
import com.cafesito.app.data.UserRepository
import com.cafesito.app.notifications.NotificationActionReceiver
import com.cafesito.app.notifications.NotificationChannels
import com.cafesito.app.ui.timeline.TimelineNotificationSystem
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale
import java.net.URL

class CafesitoFcmService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FcmServiceEntryPoint {
        fun userRepository(): UserRepository
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Nuevo token: $token")
        serviceScope.launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    applicationContext,
                    FcmServiceEntryPoint::class.java
                )
                entryPoint.userRepository().updateFcmToken(token)
            } catch (e: Exception) {
                Log.e("FCM", "Error sincronizando token FCM", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Manejar tanto notificaciones automáticas de Firebase como 'data payload'
        val rawTitle = message.notification?.title ?: message.data["title"] ?: "Cafesito"
        val rawBody = message.notification?.body ?: message.data["body"] ?: ""

        val title = rawTitle.capitalizedFirst()
        val body = rawBody.capitalizedFirst()

        showNotification(title, body, message)
    }

    private fun showNotification(title: String, message: String, remoteMessage: RemoteMessage) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        NotificationChannels.ensureCreated(this)

        val notificationType = (remoteMessage.data["type"] ?: remoteMessage.data["notification_type"] ?: "").uppercase()
        val channelId = NotificationChannels.resolveChannel(notificationType)
        val relatedId = remoteMessage.data["related_id"] ?: remoteMessage.data["targetId"]
        val postId = remoteMessage.data["post_id"] ?: remoteMessage.data["target_post_id"] ?: relatedId?.split(":")?.firstOrNull()
        val commentId = remoteMessage.data["comment_id"]?.toIntOrNull()
            ?: relatedId?.split(":")?.getOrNull(1)?.toIntOrNull()
        val targetUserId = remoteMessage.data["target_user_id"]?.toIntOrNull()
            ?: remoteMessage.data["from_user_id"]?.toIntOrNull()
            ?: relatedId?.toIntOrNull()

        val navType = when (notificationType) {
            "FOLLOW" -> "FOLLOW"
            "MENTION" -> "MENTION"
            "COMMENT" -> "COMMENT"
            else -> "NOTIFICATIONS"
        }

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("nav_type", navType)
            if (!postId.isNullOrBlank()) {
                putExtra("nav_id", postId)
                putExtra(TimelineNotificationSystem.EXTRA_POST_ID, postId)
            }
            commentId?.let {
                putExtra("nav_comment_id", it)
                putExtra(TimelineNotificationSystem.EXTRA_COMMENT_ID, it)
            }
            if (notificationType.isNotBlank()) {
                putExtra(TimelineNotificationSystem.EXTRA_TYPE, notificationType.lowercase())
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationType.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationId = System.currentTimeMillis().toInt()
        val markReadPendingIntent = buildActionPendingIntent(
            requestCode = notificationId,
            action = NotificationActionReceiver.ACTION_MARK_READ,
            notificationId = notificationId
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(com.cafesito.app.R.drawable.ic_notification_small)
            .setLargeIcon(resolveLargeIcon(remoteMessage.data["avatar_url"]))
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(android.graphics.Color.parseColor("#6F4E37"))
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .addAction(0, "Marcar leída", markReadPendingIntent)

        if (notificationType == "FOLLOW" && targetUserId != null) {
            val followBackPendingIntent = buildActionPendingIntent(
                requestCode = notificationId + 1,
                action = NotificationActionReceiver.ACTION_FOLLOW_BACK,
                notificationId = notificationId,
                targetUserId = targetUserId
            )
            notificationBuilder.addAction(0, "Seguir", followBackPendingIntent)
        }

        if ((notificationType == "COMMENT" || notificationType == "MENTION") && !postId.isNullOrBlank()) {
            val viewIntent = PendingIntent.getActivity(
                this,
                notificationId + 2,
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("nav_type", navType)
                    putExtra("nav_id", postId)
                    commentId?.let { putExtra("nav_comment_id", it) }
                    putExtra("notification_action", "view")
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.addAction(0, "Ver", viewIntent)

            if (notificationType == "COMMENT") {
                val savePostPendingIntent = buildActionPendingIntent(
                    requestCode = notificationId + 3,
                    action = NotificationActionReceiver.ACTION_SAVE_POST,
                    notificationId = notificationId,
                    postId = postId,
                    commentId = commentId
                )
                notificationBuilder.addAction(0, "Guardar", savePostPendingIntent)
            }
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun String.capitalizedFirst(): String {
        val trimmed = trim()
        if (trimmed.isEmpty()) return trimmed
        return trimmed.replaceFirstChar { first ->
            if (first.isLowerCase()) first.titlecase(Locale.getDefault()) else first.toString()
        }
    }


    private fun buildActionPendingIntent(
        requestCode: Int,
        action: String,
        notificationId: Int,
        targetUserId: Int? = null,
        postId: String? = null,
        commentId: Int? = null
    ): PendingIntent {
        val actionIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            targetUserId?.let { putExtra(NotificationActionReceiver.EXTRA_TARGET_USER_ID, it) }
            postId?.let { putExtra(NotificationActionReceiver.EXTRA_POST_ID, it) }
            commentId?.let { putExtra(NotificationActionReceiver.EXTRA_COMMENT_ID, it) }
        }
        return PendingIntent.getBroadcast(
            this,
            requestCode,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
