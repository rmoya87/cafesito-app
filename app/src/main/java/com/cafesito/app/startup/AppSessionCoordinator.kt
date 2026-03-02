package com.cafesito.app.startup

import android.util.Log
import com.cafesito.app.analytics.AnalyticsHelper
import com.cafesito.app.data.SyncManager
import com.cafesito.app.data.UserRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSessionCoordinator @Inject constructor(
    private val syncManager: SyncManager,
    private val userRepository: UserRepository,
    private val analyticsHelper: AnalyticsHelper
) {
    fun onAuthenticated(userId: Int, scope: CoroutineScope) {
        scope.launch {
            try {
                val lifecycleResult = userRepository.syncAccountLifecycleOnLogin(userId)
                if (lifecycleResult == UserRepository.AccountLifecycleSyncResult.Deleted) return@launch
                syncManager.syncAll()
            } catch (e: Exception) {
                Log.e("Sync", "Error during initial sync", e)
            }
        }
        scope.launch {
            try {
                userRepository.syncPendingFcmTokenIfAny()
            } catch (e: Exception) {
                Log.e("FCM", "Error syncing pending FCM token", e)
            }
        }
        analyticsHelper.setUserId(userId.toString())
        updateFcmToken(scope)
        scope.launch { userRepository.touchUserInteraction() }
    }

    fun onNotAuthenticated() {
        analyticsHelper.setUserId(null)
    }

    private fun updateFcmToken(scope: CoroutineScope) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            scope.launch {
                userRepository.updateFcmToken(token)
            }
        }
    }
}
