package com.cafesito.app.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.cafesito.app.analytics.AnalyticsHelper
import com.cafesito.app.data.SocialRepository
import com.cafesito.app.data.UserRepository
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    NotificationActionsEntryPoint::class.java
                )
                val userRepository = entryPoint.userRepository()
                val socialRepository = entryPoint.socialRepository()
                val analytics = entryPoint.analyticsHelper()

                when (action) {
                    ACTION_FOLLOW_BACK -> handleFollowBack(intent, userRepository, analytics)
                    ACTION_SAVE_POST -> handleSavePost(intent, userRepository, socialRepository, analytics)
                    ACTION_REPLY -> {
                        analytics.trackEvent("notification_action_reply")
                    }
                }
            }
            pendingResult.finish()
        }
    }

    private suspend fun handleFollowBack(
        intent: Intent,
        userRepository: UserRepository,
        analytics: AnalyticsHelper
    ) {
        val targetUserId = intent.getIntExtra(EXTRA_TARGET_USER_ID, -1)
        val me = userRepository.getActiveUser() ?: return
        if (targetUserId <= 0 || targetUserId == me.id) return
        userRepository.toggleFollow(me.id, targetUserId)
        analytics.trackEvent("notification_action_follow_back")
    }

    private suspend fun handleSavePost(
        intent: Intent,
        userRepository: UserRepository,
        socialRepository: SocialRepository,
        analytics: AnalyticsHelper
    ) {
        val postId = intent.getStringExtra(EXTRA_POST_ID) ?: return
        val me = userRepository.getActiveUser() ?: return
        socialRepository.savePost(postId, me.id)
        analytics.trackEvent("notification_action_save_post")
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationActionsEntryPoint {
        fun userRepository(): UserRepository
        fun socialRepository(): SocialRepository
        fun analyticsHelper(): AnalyticsHelper
    }

    companion object {
        const val ACTION_MARK_READ = "com.cafesito.app.ACTION_MARK_READ"
        const val ACTION_FOLLOW_BACK = "com.cafesito.app.ACTION_FOLLOW_BACK"
        const val ACTION_REPLY = "com.cafesito.app.ACTION_REPLY"
        const val ACTION_SAVE_POST = "com.cafesito.app.ACTION_SAVE_POST"

        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_TARGET_USER_ID = "extra_target_user_id"
        const val EXTRA_POST_ID = "extra_post_id"
        const val EXTRA_COMMENT_ID = "extra_comment_id"
    }
}
