package com.iamashad.meraki

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.iamashad.meraki.components.ConnectivityObserver
import com.iamashad.meraki.navigation.MerakiNavigation
import com.iamashad.meraki.notifications.NotificationHelper
import com.iamashad.meraki.repository.UserPreferencesRepository
import com.iamashad.meraki.ui.theme.MerakiTheme
import com.iamashad.meraki.ui.theme.ThemePreference
import com.iamashad.meraki.utils.ConnectivityStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    /**
     * Phase 5: Holds the pending deep-link destination emitted by a notification tap.
     *
     * Value is set in [onCreate] (fresh launch) and [onNewIntent] (app already running).
     * [MerakiNavigation] collects this flow and routes to the appropriate screen.
     *
     * Using [MutableStateFlow] means any collector (NavHost / SplashScreen) that is
     * already composed will immediately react when [onNewIntent] updates the value.
     */
    private val _navigateToChatbot = MutableStateFlow(false)
    val navigateToChatbot = _navigateToChatbot.asStateFlow()

    companion object {
        /**
         * Intent extra key written by [NotificationHelper.buildChatbotPendingIntent].
         * Value is [NotificationHelper.VALUE_CHATBOT] when the Chatbot screen is the target.
         */
        const val EXTRA_NAVIGATE_TO = NotificationHelper.EXTRA_NAVIGATE_TO
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called BEFORE super.onCreate() so the SplashScreen API can attach
        // to the window before the activity inflates its view hierarchy.
        // setKeepOnScreenCondition { false } dismisses the OS splash immediately,
        // handing off to the MerakiVideoLoader (ExoPlayer) without any delay.
        installSplashScreen().setKeepOnScreenCondition { false }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Phase 5: Record this open so CheckInWorker can enforce the 12-hour skip rule.
        recordAppOpen()

        // Phase 5: Check for a notification deep-link on fresh launch.
        _navigateToChatbot.value =
            intent?.getStringExtra(EXTRA_NAVIGATE_TO) == NotificationHelper.VALUE_CHATBOT

        setContent {
            val connectivityStatus = remember { ConnectivityStatus(applicationContext) }
            val context = LocalContext.current
            val isDynamicColorEnabled by ThemePreference.isDynamicColorEnabled(context)
                .collectAsState(initial = false)

            // Collect the deep-link flag so MerakiNavigation reacts to onNewIntent changes.
            val shouldGoToChatbot by navigateToChatbot.collectAsState()

            MerakiTheme(dynamicColor = isDynamicColorEnabled) {
                ConnectivityObserver(connectivityStatus) {
                    MerakiNavigation(navigateToChatbot = shouldGoToChatbot)
                }
            }
        }
    }

    /**
     * Called when the activity is already running (singleTop) and a new Intent arrives —
     * e.g. the user taps a check-in notification while the app is open.
     *
     * Updates [_navigateToChatbot] so the already-composed NavHost navigates directly
     * to the Chatbot screen without going through the Splash route again.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getStringExtra(EXTRA_NAVIGATE_TO) == NotificationHelper.VALUE_CHATBOT) {
            _navigateToChatbot.value = true
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun recordAppOpen() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                userPreferencesRepository.recordAppOpen()
            } catch (e: Exception) {
                // Non-fatal — worst case the 12-hour check reads a stale timestamp.
                android.util.Log.w("MainActivity", "Failed to record app open time", e)
            }
        }
    }
}
