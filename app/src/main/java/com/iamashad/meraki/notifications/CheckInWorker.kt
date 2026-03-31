package com.iamashad.meraki.notifications

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.iamashad.meraki.data.ChatDatabase
import com.iamashad.meraki.repository.UserPreferencesRepository
import com.iamashad.meraki.utils.MemoryManager
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Phase 5: Daily emotional check-in worker.
 *
 * Behaviour:
 *  - Runs once per day at the user's [UserPreferencesRepository.preferredCheckInTime].
 *  - Rotated message variants prevent notification fatigue.
 *  - Skipped silently when the app was opened in the last 12 hours (the user is
 *    already engaged, so the reminder adds no value).
 *  - Checks [UserPreferencesRepository.dailyCheckInEnabled] before posting.
 *  - If [UserPreferencesRepository.smartNudgesEnabled] is on AND 4+ sessions exist,
 *    a personalised contextual nudge is sent instead of the generic check-in text.
 *
 * Constraints:
 *  - [setRequiresBatteryNotLow] prevents draining low batteries for a non-critical notification.
 *
 * WorkManager survivability:
 *  - Enqueued as [ExistingPeriodicWorkPolicy.REPLACE] so rescheduling on time-change
 *    or reboot doesn't create duplicates.
 *  - [BootReceiver] re-enqueues this work after a device restart.
 */
class CheckInWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "CheckInWorker"

        /** Unique WorkManager work name — used for REPLACE scheduling and cancellation. */
        const val WORK_NAME = "meraki_checkin_worker"

        /** 12-hour gap threshold (milliseconds) — skip notification if app opened recently. */
        private const val TWELVE_HOURS_MS = 12L * 60 * 60 * 1000

        /**
         * Rotating check-in message variants.
         * Day-index is derived from the epoch day so the same variant always maps to the
         * same calendar day — deterministic but varied across the week.
         */
        private val CHECK_IN_VARIANTS = listOf(
            "How are you feeling today?",
            "Take a moment — how's your heart today?",
            "Hey, just checking in. How are you doing?",
            "A little check-in: how are you feeling right now?",
            "Your wellbeing matters. How are you today?",
            "It's a good time to check in with yourself. How are you?",
            "Meraki is here. How's your day going?"
        )

        // ── Scheduling ────────────────────────────────────────────────────────

        /**
         * Enqueues (or replaces) a daily [CheckInWorker] at the given [timeStr] (HH:mm, 24-h).
         *
         * Calculates the initial delay to the next occurrence of [timeStr] so the first
         * execution lands at the right clock time.  Subsequent runs repeat every 24 hours.
         */
        fun schedule(context: Context, timeStr: String) {
            val parts = timeStr.split(":").mapNotNull { it.toIntOrNull() }
            if (parts.size != 2) {
                Log.w(TAG, "Invalid time string '$timeStr' — skipping schedule")
                return
            }
            val (hour, minute) = parts

            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            // If the target time has already passed today, schedule for tomorrow.
            if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)

            val initialDelayMs = target.timeInMillis - now.timeInMillis

            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<CheckInWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )

            Log.d(TAG, "CheckInWorker scheduled at $timeStr (delay=${initialDelayMs}ms)")
        }

        /** Cancels any pending check-in work (called when user disables the toggle). */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "CheckInWorker cancelled")
        }
    }

    // ── Worker body ───────────────────────────────────────────────────────────

    override suspend fun doWork(): Result {
        val prefs = UserPreferencesRepository(context)

        // ── Gate 1: daily check-ins must be enabled ───────────────────────────
        if (!prefs.dailyCheckInEnabled.first()) {
            Log.d(TAG, "Daily check-ins disabled — skipping")
            return Result.success()
        }

        // ── Gate 2: skip if the app was opened in the last 12 hours ──────────
        val lastOpen = prefs.getLastAppOpenTime()
        if (System.currentTimeMillis() - lastOpen < TWELVE_HOURS_MS) {
            Log.d(TAG, "App opened recently (${System.currentTimeMillis() - lastOpen}ms ago) — skipping")
            return Result.success()
        }

        // ── Smart nudge path ─────────────────────────────────────────────────
        if (prefs.smartNudgesEnabled.first()) {
            val summaries = ChatDatabase.getInstance(context)
                .sessionSummaryDao()
                .getLastFourteenSummaries()

            if (summaries.size >= 4) {
                // Delegate to MemoryManager's companion to avoid duplicating the pattern logic.
                val nudge = MemoryManager.generateSmartNudge(summaries)
                if (nudge != null) {
                    NotificationHelper.send(
                        context  = context,
                        notifId  = NotificationHelper.NOTIF_ID_NUDGE,
                        title    = "Meraki",
                        body     = nudge
                    )
                    Log.d(TAG, "Smart nudge sent: $nudge")
                    return Result.success()
                }
            }
        }

        // ── Default check-in path ─────────────────────────────────────────────
        val epochDay = System.currentTimeMillis() / (24 * 60 * 60 * 1000L)
        val variant  = CHECK_IN_VARIANTS[(epochDay % CHECK_IN_VARIANTS.size).toInt()]

        NotificationHelper.send(
            context  = context,
            notifId  = NotificationHelper.NOTIF_ID_CHECKIN,
            title    = "Meraki",
            body     = variant
        )
        Log.d(TAG, "Check-in notification sent: $variant")
        return Result.success()
    }

}
