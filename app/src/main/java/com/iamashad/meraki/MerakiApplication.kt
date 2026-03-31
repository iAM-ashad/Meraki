package com.iamashad.meraki

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize
import com.iamashad.meraki.notifications.CheckInWorker
import com.iamashad.meraki.notifications.NotificationHelper
import com.iamashad.meraki.notifications.WeeklyInsightWorker
import com.iamashad.meraki.repository.UserPreferencesRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class MerakiApplication : Application() {

    /**
     * Application-scoped coroutine scope (SupervisorJob so one child failure doesn't
     * cancel the others).  Used for lightweight startup work that must survive beyond
     * any single Activity or ViewModel lifecycle.
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // ── Firebase initialisation ───────────────────────────────────────────
        Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) {
                // Debug builds: use the token-based debug provider so sideloaded / ADB-installed
                // builds pass App Check without requiring a Play Store installation.
                // On first run, the debug token is printed to logcat — register it in:
                //   Firebase Console → App Check → Apps → Meraki → Manage debug tokens
                com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
            } else {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }
        )

        // ── Phase 5: Retention engine bootstrap ──────────────────────────────
        // Ensure the companion_checkin notification channel exists before any
        // worker or foreground code tries to post on it.
        NotificationHelper.ensureChannelExists(this)

        // Schedule (or no-op if already scheduled) the daily and weekly workers.
        // Workers are gated by their own preference checks, so scheduling them
        // here is always safe — disabled toggles simply cause doWork() to return
        // Result.success() without posting a notification.
        scheduleRetentionWorkers()
    }

    /**
     * Reads the stored notification preferences and enqueues [CheckInWorker] and
     * [WeeklyInsightWorker] if their respective toggles are on.
     *
     * Both workers use [ExistingPeriodicWorkPolicy.REPLACE], so calling this on
     * every cold-start is idempotent — it merely updates the schedule if nothing
     * has changed, and corrects it if the stored time drifted (e.g. after a DST
     * change or device clock adjustment).
     */
    private fun scheduleRetentionWorkers() {
        appScope.launch {
            try {
                val prefs = UserPreferencesRepository(applicationContext)

                val dailyEnabled  = prefs.dailyCheckInEnabled.first()
                val weeklyEnabled = prefs.weeklyInsightsEnabled.first()
                val checkInTime   = prefs.preferredCheckInTime.first()

                if (dailyEnabled) {
                    CheckInWorker.schedule(applicationContext, checkInTime)
                }
                if (weeklyEnabled) {
                    WeeklyInsightWorker.schedule(applicationContext)
                }
            } catch (e: Exception) {
                // Non-fatal: workers will be rescheduled by BootReceiver on next restart.
                android.util.Log.e("MerakiApplication", "Failed to schedule retention workers", e)
            }
        }
    }
}
