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
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Phase 5: Weekly emotional insight worker.
 *
 * Behaviour:
 *  - Runs once per week (every 7 days), with initial delay calculated to the next
 *    Sunday at 09:00 so insights arrive on the natural week-boundary.
 *  - Checks [UserPreferencesRepository.weeklyInsightsEnabled] before posting.
 *  - **Minimum-session gate**: skips silently if fewer than 3 sessions occurred
 *    in the last 7 days (insufficient data — low-quality insights would damage trust).
 *  - Finds the dominant emotion for the week and sends:
 *    "This week you mostly felt [Emotion]. Tap to reflect."
 *
 * WorkManager survivability:
 *  - Enqueued as [ExistingPeriodicWorkPolicy.REPLACE].
 *  - [BootReceiver] re-enqueues this work after a device restart.
 */
class WeeklyInsightWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "WeeklyInsightWorker"

        /** Unique WorkManager work name. */
        const val WORK_NAME = "meraki_weekly_insight_worker"

        /** Minimum sessions required in the past week to generate an insight. */
        private const val MIN_SESSIONS_FOR_INSIGHT = 3

        /** Look-back window for "this week" sessions (7 days in milliseconds). */
        private const val ONE_WEEK_MS = 7L * 24 * 60 * 60 * 1000

        // ── Scheduling ────────────────────────────────────────────────────────

        /**
         * Enqueues (or replaces) a weekly [WeeklyInsightWorker] that fires on the next
         * Sunday at 09:00 local time, then every 7 days thereafter.
         */
        fun schedule(context: Context) {
            val now = Calendar.getInstance()

            // Calculate delay to the next Sunday at 09:00.
            val nextSunday = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // If this Sunday's 09:00 has already passed, move to the following Sunday.
                if (before(now)) add(Calendar.WEEK_OF_YEAR, 1)
            }

            val initialDelayMs = nextSunday.timeInMillis - now.timeInMillis

            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<WeeklyInsightWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )

            Log.d(TAG, "WeeklyInsightWorker scheduled for next Sunday 09:00 (delay=${initialDelayMs}ms)")
        }

        /** Cancels any pending weekly insight work. */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "WeeklyInsightWorker cancelled")
        }
    }

    // ── Worker body ───────────────────────────────────────────────────────────

    override suspend fun doWork(): Result {
        val prefs = UserPreferencesRepository(context)

        // ── Gate 1: weekly insights must be enabled ───────────────────────────
        if (!prefs.weeklyInsightsEnabled.first()) {
            Log.d(TAG, "Weekly insights disabled — skipping")
            return Result.success()
        }

        // ── Load all recent summaries from the database ───────────────────────
        val allSummaries = ChatDatabase.getInstance(context)
            .sessionSummaryDao()
            .getLastFourteenSummaries()

        // ── Gate 2: filter to sessions from the past 7 days ──────────────────
        val oneWeekAgo     = System.currentTimeMillis() - ONE_WEEK_MS
        val weekSummaries  = allSummaries.filter { it.date >= oneWeekAgo }

        if (weekSummaries.size < MIN_SESSIONS_FOR_INSIGHT) {
            Log.d(TAG, "Only ${weekSummaries.size} sessions this week — minimum is $MIN_SESSIONS_FOR_INSIGHT, skipping")
            return Result.success()
        }

        // ── Find the dominant emotion for the week ────────────────────────────
        val dominantEmotion = weekSummaries
            .groupingBy { it.dominantEmotion }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?.replaceFirstChar { it.uppercase() }

        if (dominantEmotion == null) {
            Log.w(TAG, "Could not determine dominant emotion — skipping")
            return Result.success()
        }

        // ── Send the weekly insight notification ──────────────────────────────
        NotificationHelper.send(
            context  = context,
            notifId  = NotificationHelper.NOTIF_ID_WEEKLY,
            title    = "Your Week with Meraki",
            body     = "This week you mostly felt $dominantEmotion. Tap to reflect."
        )

        Log.d(TAG, "Weekly insight sent: dominantEmotion=$dominantEmotion (${weekSummaries.size} sessions)")
        return Result.success()
    }
}
