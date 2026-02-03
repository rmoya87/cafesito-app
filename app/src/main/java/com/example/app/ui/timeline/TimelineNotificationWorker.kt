package com.cafesito.app.ui.timeline

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.cafesito.app.data.UserRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class TimelineNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val userRepository: UserRepository,
    private val notificationStore: TimelineNotificationStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val activeUser = userRepository.getActiveUser() ?: return Result.success()
        return try {
            userRepository.syncUsers()

            val allUsers = userRepository.getAllUsersList()
            val notifications = userRepository.getNotificationsForUser(activeUser.id).first()
                .mapNotNull { it.toTimelineNotification(allUsers) }
                .filter { !it.isRead }
            val notifiedIds = notificationStore.getNotifiedIds()
            val newNotifications = notifications.filter { it.id !in notifiedIds }

            if (newNotifications.isNotEmpty()) {
                TimelineNotificationSystem.ensureChannel(context)
                val manager = NotificationManagerCompat.from(context)
                val imageLoader = ImageLoader(context)

                newNotifications.forEach { notification ->
                    val avatarUrl = when (notification) {
                        is TimelineNotification.Follow -> notification.user.avatarUrl
                        is TimelineNotification.Mention -> notification.user.avatarUrl
                    }

                    val bitmap = if (avatarUrl.isNotEmpty()) {
                        fetchBitmap(imageLoader, avatarUrl)
                    } else {
                        null
                    }

                    val systemNotification = TimelineNotificationSystem.buildSystemNotification(
                        context, 
                        notification,
                        bitmap
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

    private suspend fun fetchBitmap(imageLoader: ImageLoader, url: String): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false) // Necesario para notificaciones
            .build()
        val result = imageLoader.execute(request)
        return if (result is SuccessResult) {
            (result.drawable as? BitmapDrawable)?.bitmap
        } else {
            null
        }
    }
}
