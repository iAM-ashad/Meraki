package com.iamashad.meraki.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.iamashad.meraki.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Phase 5: Re-schedules WorkManager jobs after a device restart.
 *
 * WorkManager's own [androidx.work.impl.background.systemalarm.SystemAlarmScheduler] will
 * restore work enqueued with [androidx.work.PeriodicWorkRequest] on reboot.  However,
 * re-registering here provides belt-and-suspenders safety — especially for the initial
 * delay recalculation (next day/week occurrence) that must be recomputed after a restart.
 *
 * Registration in AndroidManifest:
 * ```xml
 * <receiver android:name=".notifications.BootReceiver" android:exported="true">
 *   <intent-filter>
 *     <action android:name="android.intent.action.BOOT_COMPLETED"/>
 *     <action android:name="android.intent.action.QUICKBOOT_POWERON"/>  <!-- HTC devices -->
 *   </intent-filter>
 * </receiver>
 * ```
 * Requires: `<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>`
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON") return

        Log.d(TAG, "Boot completed — rescheduling Meraki workers")

        // goAsync() lets us run a short coroutine without the BroadcastReceiver timing out.
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = UserPreferencesRepository(context)

                val dailyEnabled   = prefs.dailyCheckInEnabled.first()
                val weeklyEnabled  = prefs.weeklyInsightsEnabled.first()
                val preferredTime  = prefs.preferredCheckInTime.first()

                if (dailyEnabled) {
                    CheckInWorker.schedule(context, preferredTime)
                    Log.d(TAG, "CheckInWorker rescheduled at $preferredTime")
                }
                if (weeklyEnabled) {
                    WeeklyInsightWorker.schedule(context)
                    Log.d(TAG, "WeeklyInsightWorker rescheduled")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule workers on boot: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
