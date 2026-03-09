package com.cafesito.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.cafesito.app.analytics.AnalyticsHelper
import com.cafesito.app.data.UserRepository
import com.cafesito.app.navigation.AppNavigation
import com.cafesito.app.navigation.NotificationNavigation
import com.cafesito.app.startup.AppSessionCoordinator
import com.cafesito.app.startup.AppUiInitializer
import com.cafesito.app.startup.ShortcutActionResolver
import com.cafesito.app.ui.access.SessionState
import com.cafesito.app.ui.access.SessionViewModel
import com.cafesito.app.ui.theme.CafesitoTheme
import com.cafesito.app.ui.theme.ThemeMode
import com.cafesito.app.ui.theme.resolveDarkTheme
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
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("cafesito_prefs", Context.MODE_PRIVATE) }
            val themeMode = remember { prefs.getString(ThemeMode.KEY, ThemeMode.AUTO) ?: ThemeMode.AUTO }
            val darkTheme = resolveDarkTheme(themeMode = themeMode, isSystemInDarkTheme = isSystemInDarkTheme())
            CafesitoTheme(darkTheme = darkTheme) {
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

    override fun onStart() {
        super.onStart()
        sessionCoordinator.onAppForeground(lifecycleScope)
    }
}
