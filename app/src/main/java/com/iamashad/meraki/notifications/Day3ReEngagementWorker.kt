package com.iamashad.meraki.notifications

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.iamashad.meraki.data.ChatDatabase
import com.iamashad.meraki.repository.UserPreferencesRepository
import com.iamashad.meraki.utils.MemoryManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Phase 5 (Onboarding Overhaul): Day 3 AI Re-Engagement Worker.
 *
 * Fires a single [OneTimeWorkRequest] 72 hours after account creation.
 * Scheduled from [WelcomeAIScreen] via [Day3ReEngagementWorker.schedule].
 *
 * Runtime behaviour:
 *  1. **Opt-out guard** — reads [UserPreferencesRepository.dailyCheckInEnabled].
 *     If false (user skipped during [NotificationSetupScreen]), aborts silently.
 *     The user's onboarding choice is a hard contract and is never overridden.
 *  2. **Pattern detection** — loads session summaries via [ChatDatabase].
 *     Delegates to [MemoryManager.generateSmartNudge] to detect a personalised
 *     insight (requires ≥ 4 sessions — matches the existing smart-nudge gate).
 *  3. **Fallback** — if fewer than 4 sessions exist (user hasn't chatted much yet),
 *     selects a warm generic affirmation from [FALLBACK_MESSAGES].
 *  4. **Delivery** — sends via [NotificationHelper.send].
 *
 * Architecture notes:
 *  - Uses the same direct-construction approach as [CheckInWorker] — no Hilt injection
 *    in Workers (Hilt Workers require HiltWorkerFactory wired to WorkManager which is
 *    already set up; however, this simpler pattern keeps it consistent with the existing
 *    codebase).
 *  - [ExistingWorkPolicy.KEEP] prevents double-scheduling on re-installs.
 *  - Best-effort: always returns [Result.success] so WorkManager doesn't retry a
 *    cosmetic notification.
 *
 * Deliberately ONE message — not a drip sequence. See plan §5 for rationale.
 */
class Day3ReEngagementWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "Day3Worker"

        /** Unique WorkManager work name for [ExistingWorkPolicy.KEEP] deduplication. */
        const val WORK_NAME = "meraki_day3_reengagement"

        /** 72 hours after account creation. */
        private const val INITIAL_DELAY_HOURS = 72L

        /**
         * Notification slot for the day-3 re-engagement message.
         * Does not overlap [NotificationHelper.NOTIF_ID_CHECKIN] (200),
         * [NotificationHelper.NOTIF_ID_WEEKLY] (201), or [NotificationHelper.NOTIF_ID_NUDGE] (202).
         */
        private const val NOTIF_ID_DAY3 = 203

        /**
         * Warm fallback messages used when the user hasn't had enough chat sessions
         * for the pattern-detection gate (< 4 sessions).
         *
         * Indexed by epoch-day modulo so the same day always gets the same message
         * (deterministic, never repetitive within the same week).
         */
        private val FALLBACK_MESSAGES = listOf(
            "Hey — you've been on Meraki's mind. How are you doing?",
            "Sometimes the first step is just showing up. You did. How's today?",
            "Three days in — you're doing great. Want to check in for a moment?",
            "Meraki is here whenever you're ready to talk. 💙"
        )

        /**
         * Enqueues the one-time re-engagement work request.
         * Called from [WelcomeAIScreen] after the AI welcome message finishes rendering.
         *
         * Uses [ExistingWorkPolicy.KEEP] so re-running onboarding (edge case) doesn't
         * schedule duplicate notifications.
         *
         * @param context Application context (use applicationContext at the call site).
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request: OneTimeWorkRequest =
                OneTimeWorkRequestBuilder<Day3ReEngagementWorker>()
                    .setInitialDelay(INITIAL_DELAY_HOURS, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)

            Log.d(TAG, "Day3ReEngagementWorker scheduled — fires in ${INITIAL_DELAY_HOURS}h")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Day3ReEngagementWorker fired")

        return try {
            // ── Step 1: Opt-out guard ─────────────────────────────────────────
            val prefs = UserPreferencesRepository(context)
            val notificationsEnabled = prefs.dailyCheckInEnabled.first()
            if (!notificationsEnabled) {
                Log.d(TAG, "Notifications disabled by user during onboarding — aborting Day 3 message")
                return Result.success()
            }

            // ── Step 2: Load session summaries ────────────────────────────────
            val summaries = ChatDatabase
                .getInstance(context)
                .sessionSummaryDao()
                .getLastFourteenSummaries()

            // ── Step 3: Personalise via companion pattern-detection OR fall back
            // [MemoryManager.generateSmartNudge] requires ≥ 4 sessions — same gate
            // as the daily smart-nudge path in CheckInWorker, keeping behaviour consistent.
            val notificationBody = MemoryManager.generateSmartNudge(summaries)
                ?: run {
                    val epochDay = System.currentTimeMillis() / (24 * 60 * 60 * 1000L)
                    FALLBACK_MESSAGES[(epochDay % FALLBACK_MESSAGES.size).toInt()]
                }

            // ── Step 4: Deliver ───────────────────────────────────────────────
            NotificationHelper.ensureChannelExists(context)
            NotificationHelper.send(
                context = context,
                notifId = NOTIF_ID_DAY3,
                title   = "Meraki is thinking of you 💙",
                body    = notificationBody
            )

            Log.d(TAG, "Day 3 re-engagement sent: $notificationBody")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Day3ReEngagementWorker failed: ${e.message}", e)
            // Best-effort — always succeed so WorkManager doesn't retry a cosmetic notification.
            Result.success()
        }
    }
}
