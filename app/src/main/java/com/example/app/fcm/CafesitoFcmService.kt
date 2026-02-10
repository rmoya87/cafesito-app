package com.cafesito.app.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cafesito.app.MainActivity
import com.cafesito.app.ui.timeline.TimelineNotificationSystem
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
        val channelId = "fcm_default_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notificaciones de Cafesito",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones generales, mensajes y seguidores"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationType = (remoteMessage.data["type"] ?: remoteMessage.data["notification_type"] ?: "").uppercase()
        val relatedId = remoteMessage.data["related_id"]
        val postId = remoteMessage.data["post_id"] ?: relatedId?.split(":")?.firstOrNull()
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

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
