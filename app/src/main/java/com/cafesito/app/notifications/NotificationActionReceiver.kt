package com.cafesito.app.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.util.Log
import com.cafesito.app.analytics.AnalyticsHelper
import com.cafesito.app.data.SocialRepository
import com.cafesito.app.data.SupabaseDataSource
import com.cafesito.app.data.UserRepository
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                    ACTION_ACCEPT_LIST_INVITE -> handleAcceptListInvite(context, intent, entryPoint.supabaseDataSource(), analytics)
                    ACTION_DECLINE_LIST_INVITE -> handleDeclineListInvite(context, intent, entryPoint.supabaseDataSource(), analytics)
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

    private suspend fun handleAcceptListInvite(
        context: Context,
        intent: Intent,
        supabaseDataSource: SupabaseDataSource,
        analytics: AnalyticsHelper
    ) {
        val invitationId = intent.getStringExtra(EXTRA_INVITATION_ID) ?: return
        runCatching {
            supabaseDataSource.acceptListInvitation(invitationId)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Lista añadida", Toast.LENGTH_SHORT).show()
            }
            analytics.trackEvent("notification_action_accept_list_invite")
        }.onFailure {
            Log.e("NotificationActionReceiver", "acceptListInvitation failed", it)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "No se pudo añadir la lista", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun handleDeclineListInvite(
        context: Context,
        intent: Intent,
        supabaseDataSource: SupabaseDataSource,
        analytics: AnalyticsHelper
    ) {
        val invitationId = intent.getStringExtra(EXTRA_INVITATION_ID) ?: return
        runCatching {
            supabaseDataSource.declineListInvitation(invitationId)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Invitación rechazada", Toast.LENGTH_SHORT).show()
            }
            analytics.trackEvent("notification_action_decline_list_invite")
        }.onFailure {
            Log.e("NotificationActionReceiver", "declineListInvitation failed", it)
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationActionsEntryPoint {
        fun userRepository(): UserRepository
        fun socialRepository(): SocialRepository
        fun supabaseDataSource(): SupabaseDataSource
        fun analyticsHelper(): AnalyticsHelper
    }

    companion object {
        const val ACTION_MARK_READ = "com.cafesito.app.ACTION_MARK_READ"
        const val ACTION_FOLLOW_BACK = "com.cafesito.app.ACTION_FOLLOW_BACK"
        const val ACTION_REPLY = "com.cafesito.app.ACTION_REPLY"
        const val ACTION_SAVE_POST = "com.cafesito.app.ACTION_SAVE_POST"
        const val ACTION_ACCEPT_LIST_INVITE = "com.cafesito.app.ACTION_ACCEPT_LIST_INVITE"
        const val ACTION_DECLINE_LIST_INVITE = "com.cafesito.app.ACTION_DECLINE_LIST_INVITE"

        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_TARGET_USER_ID = "extra_target_user_id"
        const val EXTRA_POST_ID = "extra_post_id"
        const val EXTRA_COMMENT_ID = "extra_comment_id"
        const val EXTRA_INVITATION_ID = "extra_invitation_id"
    }
}
