package com.cafesito.app.startup

import android.util.Log
import com.cafesito.app.analytics.AnalyticsHelper
import com.cafesito.app.data.SyncManager
import com.cafesito.app.data.UserRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSessionCoordinator @Inject constructor(
    private val syncManager: SyncManager,
    private val userRepository: UserRepository,
    private val analyticsHelper: AnalyticsHelper,
    private val lastAppOpenTracker: LastAppOpenTracker
) {
    @Volatile
    private var lastForegroundTokenRefreshAt: Long = 0L

    fun onAuthenticated(userId: Int, scope: CoroutineScope) {
        scope.launch {
            try {
                val lifecycleResult = userRepository.syncAccountLifecycleOnLogin(userId)
                if (lifecycleResult == UserRepository.AccountLifecycleSyncResult.Deleted) return@launch
                withContext(Dispatchers.IO) {
                    syncManager.syncEssentialForLaunch()
                }
                scope.launch {
                    delay(5_000)
                    withContext(Dispatchers.IO) {
                        syncManager.syncDeferred()
                    }
                }
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
        updateFcmToken(scope, reason = "authenticated")
        scope.launch { userRepository.touchUserInteraction() }
        lastAppOpenTracker.save()
    }

    fun onAppForeground(scope: CoroutineScope) {
        lastAppOpenTracker.save()
        val now = System.currentTimeMillis()
        if (now - lastForegroundTokenRefreshAt < 60_000L) return
        lastForegroundTokenRefreshAt = now
        updateFcmToken(scope, reason = "foreground")
        scope.launch {
            try {
                userRepository.syncPendingFcmTokenIfAny()
            } catch (e: Exception) {
                Log.e("FCM", "Error syncing pending FCM token on foreground", e)
            }
        }
    }

    fun onNotAuthenticated() {
        analyticsHelper.setUserId(null)
    }

    private fun updateFcmToken(scope: CoroutineScope, reason: String, attempt: Int = 1) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed (reason=$reason, attempt=$attempt)", task.exception)
                if (attempt < 3) {
                    scope.launch {
                        delay(1500L * attempt)
                        updateFcmToken(scope, reason, attempt + 1)
                    }
                }
                return@addOnCompleteListener
            }

            val token = task.result
            scope.launch {
                userRepository.updateFcmToken(token)
            }
        }
    }
}
