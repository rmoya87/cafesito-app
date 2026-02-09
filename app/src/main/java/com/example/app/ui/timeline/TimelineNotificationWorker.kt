package com.cafesito.app.ui.timeline

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cafesito.app.data.UserRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

@HiltWorker
class TimelineNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val userRepository: UserRepository,
    private val notificationStore: TimelineNotificationStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            val activeUser = userRepository.getActiveUser() ?: return@withContext Result.success()
            try {
                userRepository.syncUsers()

                val allUsers = userRepository.getAllUsersList()
                val notifications = userRepository.getNotificationsForUser(activeUser.id).first()
                    .mapNotNull { it.toTimelineNotification(allUsers) }
                    .filter { !it.isRead }
                val notifiedIds = notificationStore.getNotifiedIds()
                val newNotifications = notifications.filter { it.id !in notifiedIds }.take(5)

                if (newNotifications.isNotEmpty()) {
                    TimelineNotificationSystem.ensureChannel(context)
                    val manager = NotificationManagerCompat.from(context)

                    newNotifications.forEach { notification ->
                        val systemNotification = TimelineNotificationSystem.buildSystemNotification(
                            context,
                            notification,
                            null
                        ).build()
                        manager.notify(notification.id.hashCode(), systemNotification)
                    }
                    notificationStore.addNotifiedIds(newNotifications.map { it.id }.toSet())
                }

                Result.success()
            } catch (_: Exception) {
                Result.retry()
            }
        }
    }
}
