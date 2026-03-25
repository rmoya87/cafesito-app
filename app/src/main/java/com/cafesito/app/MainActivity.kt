package com.cafesito.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.cafesito.app.analytics.AnalyticsHelper
import com.cafesito.app.data.UserRepository
import com.cafesito.app.navigation.AppNavigation
import com.cafesito.app.navigation.NotificationNavigation
import com.cafesito.app.share.DirectShareRepository
import com.cafesito.app.share.DirectShareShortcutPublisher
import com.cafesito.app.startup.AppSessionCoordinator
import com.cafesito.app.startup.AppUiInitializer
import com.cafesito.app.startup.PredictiveShortcutsHelper
import com.cafesito.app.startup.ShortcutActionResolver
import com.cafesito.app.ui.access.SessionState
import com.cafesito.app.ui.access.SessionViewModel
import com.cafesito.app.ui.theme.CafesitoTheme
import com.cafesito.app.ui.theme.DynamicColorMode
import com.cafesito.app.ui.theme.ThemeMode
import com.cafesito.app.ui.theme.resolveDarkTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val sessionViewModel: SessionViewModel by viewModels()

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    @Inject
    lateinit var sessionCoordinator: AppSessionCoordinator

    @Inject
    lateinit var directShareRepository: DirectShareRepository

    @Inject
    lateinit var directShareShortcutPublisher: DirectShareShortcutPublisher

    private val notificationNavigation = mutableStateOf<NotificationNavigation?>(null)
    private val shortcutNavigation = mutableStateOf<String?>(null)
    private var deepLinkListId by mutableStateOf<String?>(null)
    private var deepLinkProfileId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppUiInitializer.configure(this)
        notificationNavigation.value = NotificationNavigation.fromIntent(intent)
        shortcutNavigation.value = ShortcutActionResolver.resolve(intent)
        deepLinkListId = parseListIdFromIntent(intent)
        deepLinkProfileId = parseProfileIdFromIntent(intent)

        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("cafesito_prefs", Context.MODE_PRIVATE) }
            var themeMode by remember { mutableStateOf(prefs.getString(ThemeMode.KEY, ThemeMode.AUTO) ?: ThemeMode.AUTO) }
            var dynamicColorEnabled by remember { mutableStateOf(prefs.getBoolean(DynamicColorMode.KEY, DynamicColorMode.DEFAULT)) }
            DisposableEffect(prefs) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
                    when (key) {
                        ThemeMode.KEY -> {
                            themeMode = sharedPrefs.getString(ThemeMode.KEY, ThemeMode.AUTO) ?: ThemeMode.AUTO
                        }
                        DynamicColorMode.KEY -> {
                            dynamicColorEnabled = sharedPrefs.getBoolean(DynamicColorMode.KEY, DynamicColorMode.DEFAULT)
                        }
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            val darkTheme = resolveDarkTheme(themeMode = themeMode, isSystemInDarkTheme = isSystemInDarkTheme())
            CafesitoTheme(darkTheme = darkTheme, dynamicColorEnabled = dynamicColorEnabled) {
                val sessionState by sessionViewModel.sessionState.collectAsState()

                LaunchedEffect(sessionState) {
                    when (val state = sessionState) {
                        is SessionState.Authenticated -> {
                            sessionCoordinator.onAuthenticated(state.userId, lifecycleScope)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                                PredictiveShortcutsHelper.updatePredictiveShortcuts(context)
                            }
                            lifecycleScope.launch(Dispatchers.IO) {
                                val targets = directShareRepository.getSuggestedTargets(limit = 4)
                                directShareShortcutPublisher.publishSuggestedTargets(context, targets)
                            }
                        }
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
                        deepLinkListId = deepLinkListId,
                        onDeepLinkListConsumed = { deepLinkListId = null },
                        deepLinkProfileUserId = deepLinkProfileId,
                        onDeepLinkProfileConsumed = { deepLinkProfileId = null },
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
        deepLinkListId = parseListIdFromIntent(intent)
        deepLinkProfileId = parseProfileIdFromIntent(intent)
    }

    /** Extrae listId de un intent con data https://cafesitoapp.com/profile/list/{listId} o /lists/join/{listId}. */
    private fun parseListIdFromIntent(intent: Intent?): String? {
        val uri: Uri = intent?.data ?: return null
        val host = uri.host ?: return null
        if (host != "cafesitoapp.com" && !host.endsWith(".cafesitoapp.com")) return null
        val path = uri.path ?: return null
        val segments = path.trim('/').split("/")
        // /lists/join/UUID
        if (segments.getOrNull(0) == "lists" && segments.getOrNull(1) == "join" && segments.getOrNull(2)?.isNotBlank() == true) {
            return segments[2]
        }
        // /profile/list/UUID
        val listIdx = segments.indexOf("list")
        if (listIdx >= 0 && listIdx < segments.lastIndex) return segments[listIdx + 1].takeIf { it.isNotBlank() }
        return null
    }

    /** Extrae identificador de perfil de https://cafesitoapp.com/profile/{userId|username}. */
    private fun parseProfileIdFromIntent(intent: Intent?): String? {
        val extraProfileId = intent?.getStringExtra(DirectShareShortcutPublisher.EXTRA_DIRECT_SHARE_TARGET_ID)
            ?.takeIf { it.isNotBlank() }
        val extraTargetType = intent?.getStringExtra(DirectShareShortcutPublisher.EXTRA_DIRECT_SHARE_TARGET_TYPE)
            ?.trim()
            ?.lowercase()
        if (extraTargetType == "contact" && !extraProfileId.isNullOrBlank()) {
            return extraProfileId
        }

        val uri: Uri = intent?.data ?: return null
        val host = uri.host ?: return null
        if (host != "cafesitoapp.com" && !host.endsWith(".cafesitoapp.com")) return null
        val segments = uri.path.orEmpty().trim('/').split("/")
        // /profile/{userId|username}
        if (segments.getOrNull(0) != "profile") return null
        // Ignorar /profile/list/{listId} que se resuelve por parseListIdFromIntent
        if (segments.getOrNull(1) == "list") return null
        return segments.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    override fun onStart() {
        super.onStart()
        sessionCoordinator.onAppForeground(lifecycleScope)
    }
}
