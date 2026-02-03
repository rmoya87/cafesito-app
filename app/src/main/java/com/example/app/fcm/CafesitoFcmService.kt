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
        
        val type = message.data["type"]
        val targetId = message.data["targetId"]

        showNotification(title, body, type, targetId)
    }

    private fun showNotification(title: String, message: String, type: String?, targetId: String?) {
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

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("nav_type", type)
            putExtra("nav_id", targetId)
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
