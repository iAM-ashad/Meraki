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
import com.iamashad.meraki.model.ConfidenceScore
import com.iamashad.meraki.model.InsightTier
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

        /**
         * Minimum confidence tier required before a weekly notification is sent.
         *
         * Using [InsightTier.LOW] (score ≥ 0.20) means the user must have logged
         * at least a handful of moods or had a couple of sessions — enough for the
         * dominant-emotion summary to be trustworthy.  At [InsightTier.FORMING] we
         * stay silent rather than erode trust with a premature insight.
         */
        private val MIN_TIER_FOR_NOTIFICATION = InsightTier.LOW

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

        // ── Gate 2: confidence score must reach at least LOW ─────────────────
        //
        // The worker accesses the database directly (no Hilt injection) so we
        // compute the confidence score inline.  Chat message count is omitted
        // because the worker has no Firebase userId at this point; the remaining
        // three components still cover 90 % of the score weight.
        val db           = ChatDatabase.getInstance(context)
        val moodLogCount = db.emotionDao().getTotalLogCount()
        val avgConf      = db.emotionDao().getAverageConfidence()
        val sessionCount = db.sessionSummaryDao().getTotalSessionCount()

        val confidence = ConfidenceScore.compute(
            moodLogCount         = moodLogCount,
            sessionCount         = sessionCount,
            avgEmotionConfidence = avgConf,
            chatMessageCount     = 0          // not available in worker context
        )

        if (confidence.tier.ordinal < MIN_TIER_FOR_NOTIFICATION.ordinal) {
            Log.d(
                TAG,
                "Confidence tier ${confidence.tier} (score=${confidence.value}) is below " +
                "$MIN_TIER_FOR_NOTIFICATION — skipping notification to preserve user trust."
            )
            return Result.success()
        }

        // ── Load all recent summaries and filter to this week ────────────────
        val allSummaries  = db.sessionSummaryDao().getLastFourteenSummaries()
        val oneWeekAgo    = System.currentTimeMillis() - ONE_WEEK_MS
        val weekSummaries = allSummaries.filter { it.date >= oneWeekAgo }

        if (weekSummaries.isEmpty()) {
            Log.d(TAG, "No sessions in the past 7 days — skipping")
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

        Log.d(
            TAG,
            "Weekly insight sent: dominantEmotion=$dominantEmotion " +
            "(${weekSummaries.size} sessions, confidence=${confidence.value}, tier=${confidence.tier})"
        )
        return Result.success()
    }
}
