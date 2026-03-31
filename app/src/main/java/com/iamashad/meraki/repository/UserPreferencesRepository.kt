package com.iamashad.meraki.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

// File-level DataStore delegate — one instance per process, keyed by name.
private val Context.userPreferencesDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "user_preferences")

/**
 * Persists user-scoped preferences via Jetpack DataStore.
 *
 * Phase 2: Backs the daily message cap in ChatViewModel.
 * Phase 5: Adds notification-preference keys for the retention engine:
 *   - [dailyCheckInEnabled]  — whether the daily check-in notification is active.
 *   - [weeklyInsightsEnabled] — whether the weekly insight summary is active.
 *   - [smartNudgesEnabled]   — whether personalised contextual nudges are active.
 *   - [preferredCheckInTime] — HH:mm string (24-h) for the daily check-in alarm.
 *   - [getLastAppOpenTime] / [recordAppOpen] — used by CheckInWorker to enforce
 *     the 12-hour "recently-active" skip rule.
 *   - [hasShownNudgePrompt]  — one-shot flag so the auto-enable nudge dialog
 *     is shown only once after the 4-session threshold is crossed.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        // ── Phase 2: message-cap keys ─────────────────────────────────────────
        private val KEY_DAILY_MESSAGE_COUNT = intPreferencesKey("daily_message_count")
        private val KEY_LAST_RESET_DATE     = stringPreferencesKey("last_reset_date")

        /** Maximum number of user messages allowed per calendar day. */
        const val DAILY_MESSAGE_CAP = 50

        // ── Phase 5: notification-preference keys ─────────────────────────────

        private val KEY_DAILY_CHECKIN_ENABLED  = booleanPreferencesKey("daily_checkin_enabled")
        private val KEY_WEEKLY_INSIGHTS_ENABLED = booleanPreferencesKey("weekly_insights_enabled")
        private val KEY_SMART_NUDGES_ENABLED    = booleanPreferencesKey("smart_nudges_enabled")

        /** Preferred daily check-in time as "HH:mm" (24-hour). Default: 20:00 (8 PM). */
        private val KEY_PREFERRED_CHECKIN_TIME  = stringPreferencesKey("preferred_checkin_time")
        const val DEFAULT_CHECKIN_TIME          = "20:00"

        /**
         * Epoch-millisecond timestamp of the most recent app open.
         * Written by MainActivity.onCreate() and onResume().
         * Read by CheckInWorker to enforce the 12-hour skip rule.
         */
        private val KEY_LAST_APP_OPEN_TIME = longPreferencesKey("last_app_open_time")

        /**
         * Set to true once the "Enable Smart Nudges?" auto-prompt dialog has been shown.
         * Prevents the dialog from re-appearing every time the user opens Settings.
         */
        private val KEY_HAS_SHOWN_NUDGE_PROMPT = booleanPreferencesKey("has_shown_nudge_prompt")
    }

    // ── Phase 2: daily message cap ────────────────────────────────────────────

    /**
     * Returns the current daily message count.
     * Automatically resets the counter to 0 when the stored date differs from today
     * (i.e., on the first call of a new calendar day).
     */
    suspend fun getDailyMessageCount(): Int {
        val prefs = context.userPreferencesDataStore.data.first()
        val today      = LocalDate.now().toString()
        val storedDate = prefs[KEY_LAST_RESET_DATE] ?: ""

        return if (storedDate != today) {
            context.userPreferencesDataStore.edit { mutablePrefs ->
                mutablePrefs[KEY_LAST_RESET_DATE]     = today
                mutablePrefs[KEY_DAILY_MESSAGE_COUNT] = 0
            }
            0
        } else {
            prefs[KEY_DAILY_MESSAGE_COUNT] ?: 0
        }
    }

    /**
     * Atomically increments the daily message count.
     * Also handles the edge case where the date changes between the cap-check and
     * the increment (belt-and-suspenders reset logic mirrored from [getDailyMessageCount]).
     */
    suspend fun incrementDailyMessageCount() {
        val today = LocalDate.now().toString()
        context.userPreferencesDataStore.edit { prefs ->
            val storedDate = prefs[KEY_LAST_RESET_DATE] ?: ""
            if (storedDate != today) {
                prefs[KEY_LAST_RESET_DATE]     = today
                prefs[KEY_DAILY_MESSAGE_COUNT] = 1
            } else {
                val current = prefs[KEY_DAILY_MESSAGE_COUNT] ?: 0
                prefs[KEY_DAILY_MESSAGE_COUNT] = current + 1
            }
        }
    }

    // ── Phase 5: notification preference flows ────────────────────────────────

    /**
     * Observable flow for the daily check-in toggle.
     * Default: **ON** — notifications are enabled out of the box.
     */
    val dailyCheckInEnabled: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { prefs -> prefs[KEY_DAILY_CHECKIN_ENABLED] ?: true }

    /**
     * Observable flow for the weekly insight toggle.
     * Default: **ON**.
     */
    val weeklyInsightsEnabled: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { prefs -> prefs[KEY_WEEKLY_INSIGHTS_ENABLED] ?: true }

    /**
     * Observable flow for the smart nudges toggle.
     * Default: **OFF** — auto-enabled via a dialog after 4 sessions.
     */
    val smartNudgesEnabled: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { prefs -> prefs[KEY_SMART_NUDGES_ENABLED] ?: false }

    /**
     * Observable flow for the preferred check-in time (HH:mm 24-h).
     * Default: "20:00" (8:00 PM).
     */
    val preferredCheckInTime: Flow<String> = context.userPreferencesDataStore.data
        .map { prefs -> prefs[KEY_PREFERRED_CHECKIN_TIME] ?: DEFAULT_CHECKIN_TIME }

    // ── Phase 5: notification preference setters ──────────────────────────────

    suspend fun setDailyCheckInEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_DAILY_CHECKIN_ENABLED] = enabled
        }
    }

    suspend fun setWeeklyInsightsEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_WEEKLY_INSIGHTS_ENABLED] = enabled
        }
    }

    suspend fun setSmartNudgesEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_SMART_NUDGES_ENABLED] = enabled
        }
    }

    suspend fun setPreferredCheckInTime(time: String) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_PREFERRED_CHECKIN_TIME] = time
        }
    }

    // ── Phase 5: app-open tracking ────────────────────────────────────────────

    /**
     * Records the current timestamp as the last app-open time.
     * Called from [com.iamashad.meraki.MainActivity.onCreate] and onResume so the
     * 12-hour "recently-active" skip rule in [com.iamashad.meraki.notifications.CheckInWorker]
     * has fresh data.
     */
    suspend fun recordAppOpen() {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_LAST_APP_OPEN_TIME] = System.currentTimeMillis()
        }
    }

    /**
     * Returns the epoch-ms timestamp of the last recorded app open, or 0 if never set.
     */
    suspend fun getLastAppOpenTime(): Long {
        return context.userPreferencesDataStore.data.first()[KEY_LAST_APP_OPEN_TIME] ?: 0L
    }

    // ── Phase 5: nudge-prompt one-shot flag ───────────────────────────────────

    /** Returns true once the smart-nudge auto-prompt has been shown. */
    val hasShownNudgePrompt: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { prefs -> prefs[KEY_HAS_SHOWN_NUDGE_PROMPT] ?: false }

    /** Marks the smart-nudge auto-prompt as shown so it is never presented again. */
    suspend fun markNudgePromptShown() {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_HAS_SHOWN_NUDGE_PROMPT] = true
        }
    }
}
