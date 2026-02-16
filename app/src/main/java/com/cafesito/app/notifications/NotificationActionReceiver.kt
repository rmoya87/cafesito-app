package com.cafesito.app.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationId != -1) {
            manager.cancel(notificationId)
        }

        if (action == ACTION_MARK_READ) {
            Toast.makeText(context, "Notificación marcada como leída", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val ACTION_MARK_READ = "com.cafesito.app.ACTION_MARK_READ"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
