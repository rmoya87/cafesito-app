package com.cafesito.app.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cafesito.app.MainActivity
import com.cafesito.app.ui.timeline.TimelineNotificationSystem
import com.cafesito.app.notifications.NotificationActionReceiver
import com.cafesito.app.notifications.NotificationChannels
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CafesitoFcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Nuevo token: $token")
        // El token se sincroniza automáticamente en MainActivity al iniciar sesión
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        // Manejar tanto notificaciones automáticas de Firebase como 'data payload'
        val title = message.notification?.title ?: message.data["title"] ?: "Cafesito"
        val body = message.notification?.body ?: message.data["body"] ?: ""

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

        val navType = when (notificationType) {
            "FOLLOW" -> "FOLLOW"
            "MENTION" -> "MENTION"
            "COMMENT" -> "COMMENT"
            else -> "NOTIFICATIONS"
        }

        val intent = Intent(this, MainActivity::class.java).apply {
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
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )


        val notificationId = System.currentTimeMillis().toInt()
        val markReadIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_READ
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            this,
            notificationId,
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            // ✅ USAMOS EL ICONO DE PRIMER PLANO (FOREGROUND) QUE ES BLANCO Y TRANSPARENTE
            .setSmallIcon(com.cafesito.app.R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            // Color temático para la notificación
            .setColor(android.graphics.Color.parseColor("#6F4E37")) // Marrón café
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .addAction(0, "Marcar leída", markReadPendingIntent)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
