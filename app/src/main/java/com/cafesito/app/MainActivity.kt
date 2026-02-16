package com.cafesito.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.lifecycleScope
import com.cafesito.app.analytics.AnalyticsHelper
import com.cafesito.app.data.UserRepository
import com.cafesito.app.navigation.AppNavigation
import com.cafesito.app.startup.AppSessionCoordinator
import com.cafesito.app.startup.AppUiInitializer
import com.cafesito.app.startup.ShortcutActionResolver
import com.cafesito.app.ui.access.SessionState
import com.cafesito.app.ui.access.SessionViewModel
import com.cafesito.app.ui.theme.CafesitoTheme
import com.cafesito.app.ui.timeline.NotificationNavigation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val sessionViewModel: SessionViewModel by viewModels()

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    @Inject
    lateinit var sessionCoordinator: AppSessionCoordinator

    private val notificationNavigation = mutableStateOf<NotificationNavigation?>(null)
    private val shortcutNavigation = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppUiInitializer.configure(this)
        notificationNavigation.value = NotificationNavigation.fromIntent(intent)
        shortcutNavigation.value = ShortcutActionResolver.resolve(intent)

        setContent {
            CafesitoTheme {
                val sessionState by sessionViewModel.sessionState.collectAsState()

                LaunchedEffect(sessionState) {
                    when (val state = sessionState) {
                        is SessionState.Authenticated -> sessionCoordinator.onAuthenticated(state.userId, lifecycleScope)
                        is SessionState.NotAuthenticated -> sessionCoordinator.onNotAuthenticated()
                        else -> Unit
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation(
                        sessionState = sessionState,
                        userRepository = userRepository,
                        notificationNavigation = notificationNavigation.value,
                        onNotificationConsumed = { notificationNavigation.value = null },
                        shortcutAction = shortcutNavigation.value,
                        onShortcutConsumed = { shortcutNavigation.value = null },
                        analyticsHelper = analyticsHelper
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationNavigation.value = NotificationNavigation.fromIntent(intent)
        shortcutNavigation.value = ShortcutActionResolver.resolve(intent)
    }
}
