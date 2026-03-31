package com.iamashad.meraki.utils

import android.util.Log
import com.iamashad.meraki.data.SessionSummary
import com.iamashad.meraki.data.SessionSummaryDao
import com.iamashad.meraki.di.IoDispatcher
import com.iamashad.meraki.model.Message
import com.iamashad.meraki.repository.GroqRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 4: Manages long-term session memory by generating compressed summaries
 * at the end of each conversation and persisting them to [SessionSummaryDao].
 *
 * Architecture role:
 *  - [summariseAndSave] is called from ChatViewModel's onCleared() so every session
 *    ending (foreground → background, navigation away, or process death recovery) is
 *    captured within ~30s.
 *  - [buildUserProfile] is called in startNewConversation() / loadPreviousConversation()
 *    to construct the 300-token profile string injected into the chat history.
 *  - [clearAllSummaries] is wired to the "Clear History" action for full privacy reset.
 *
 * Summarisation uses a dedicated low-temperature (0.3) Gemini instance so outputs are
 * deterministic and structured — independent of the empathetic (0.7) conversational model.
 */
@Singleton
class MemoryManager @Inject constructor(
    private val sessionSummaryDao: SessionSummaryDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val groqRepository: GroqRepository
) {

    companion object {
        private const val TAG = "MemoryManager"

        /** Minimum number of user messages before summarisation is attempted. */
        private const val MIN_MESSAGES_FOR_SUMMARY = 3

        /** Max messages included in the summarisation transcript to control prompt size. */
        private const val MAX_TRANSCRIPT_MESSAGES = 30

        /** Max characters per message in the transcript (prevents oversized prompts). */
        private const val MAX_CHARS_PER_MESSAGE = 200

        /**
         * Phase 5: Minimum session summaries required before smart-nudge pattern detection
         * is attempted.  Below this threshold the data is too sparse to draw reliable patterns,
         * and premature nudges would damage user trust.
         */
        private const val MIN_SESSIONS_FOR_NUDGE = 4

        // ── Phase 5: Smart Nudge public API ───────────────────────────────────

        /**
         * Analyses a list of session summaries for recurring patterns and returns a
         * contextual nudge string personalised to the user, or **null** when:
         *  - Fewer than [MIN_SESSIONS_FOR_NUDGE] summaries are available (4-session gate).
         *  - No strong pattern is detectable across the recent sessions.
         *
         * Pattern priority (first match wins):
         *  1. **Recurring theme** — a topic appearing in 2+ of the last 4 sessions
         *     (e.g., "You mentioned sleep last time. How are you sleeping?").
         *  2. **Day-of-week emotional pattern** — a negative emotion on today's named
         *     day of the week (e.g., "Sunday evenings can feel heavy sometimes.").
         *  3. **Dominant-emotion fallback** — most frequent emotion across last 4 sessions
         *     (e.g., "You've been feeling anxious lately. How are you holding up today?").
         *
         * This function is intentionally a [companion object] member so it can be called
         * from both [MemoryManager] instance methods and from [CheckInWorker] without
         * requiring a Hilt-injected instance inside the Worker.
         *
         * @param summaries Recent session summaries in descending date order.
         */
        fun generateSmartNudge(summaries: List<com.iamashad.meraki.data.SessionSummary>): String? {
            if (summaries.size < MIN_SESSIONS_FOR_NUDGE) return null

            val recent = summaries.take(MIN_SESSIONS_FOR_NUDGE)

            // ── Pattern 1: recurring theme across 2+ recent sessions ──────────
            val themes = recent
                .flatMap { it.keyThemes.split(",") }
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }

            val recurringTheme = themes
                .groupingBy { it }
                .eachCount()
                .filter { it.value >= 2 }
                .maxByOrNull { it.value }
                ?.key

            if (recurringTheme != null) {
                return "You mentioned $recurringTheme last time. How are things going with that?"
            }

            // ── Pattern 2: negative emotion on today's named day-of-week ─────
            val today    = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
            val dayName  = when (today) {
                java.util.Calendar.SUNDAY   -> "Sunday"
                java.util.Calendar.MONDAY   -> "Monday"
                java.util.Calendar.SATURDAY -> "Saturday"
                else                        -> null
            }
            val recentEmotion = recent.first().dominantEmotion
            if (dayName != null && recentEmotion in listOf("anxious", "sad", "stressed")) {
                return "$dayName evenings can feel heavy sometimes. Meraki is here if you need to talk."
            }

            // ── Pattern 3: dominant-emotion fallback ──────────────────────────
            val dominantEmotion = recent
                .groupingBy { it.dominantEmotion }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
                ?: return null

            return "You've been feeling $dominantEmotion lately. How are you holding up today?"
        }
    }

    // ── Public API ──────────────────────────────────────────────────────────────

    /**
     * Generates a 3-bullet structured summary for the session and persists it.
     *
     * Trigger points (called from ChatViewModel):
     *  - onCleared() — ViewModel destroyed (app background / navigation away)
     *  - finishConversation() — explicit session end tagged by user
     *
     * The summary is skipped if the session has fewer than [MIN_MESSAGES_FOR_SUMMARY]
     * user turns (too short to be meaningful) or if Gemini returns an empty response.
     *
     * @param sessionId       UUID identifying this conversation session.
     * @param messages        Full message list for the session (both roles).
     * @param dominantEmotion Primary emotion key as detected by EmotionClassifier during
     *                        this session (e.g. "anxious", "sad", "neutral").
     */
    suspend fun summariseAndSave(
        sessionId: String,
        messages: List<Message>,
        dominantEmotion: String
    ) = withContext(ioDispatcher) {
        val userMessages = messages.count { it.role == "user" }
        if (userMessages < MIN_MESSAGES_FOR_SUMMARY) {
            Log.d(TAG, "Session $sessionId too short ($userMessages user turns) — skipping summary")
            return@withContext
        }

        runCatching {
            val transcript = buildTranscript(messages)
            val prompt = buildSummarisationPrompt(transcript)
            val rawText = groqRepository.generateSimpleResponse(prompt, temperature = 0.3f)
                ?.trim()
                .orEmpty()

            if (rawText.isBlank()) {
                Log.w(TAG, "Empty summary response for session $sessionId")
                return@withContext
            }

            val parsed = parseSummaryResponse(rawText, dominantEmotion)
            val tokenCost = estimateTokens(rawText)

            val summary = SessionSummary(
                sessionId       = sessionId,
                dominantEmotion = dominantEmotion,
                keyThemes       = parsed.keyThemes,
                helperPattern   = parsed.helperPattern,
                summaryText     = rawText,
                tokenCount      = tokenCost
            )
            sessionSummaryDao.insertOrUpdate(summary)
            Log.d(TAG, "Summary saved for $sessionId — $tokenCost tokens, emotion=$dominantEmotion")

        }.onFailure { e ->
            Log.e(TAG, "Failed to summarise session $sessionId: ${e.message}", e)
        }
    }

    /**
     * Builds a concise user-profile string from the last 14 session summaries.
     *
     * Format matches what the model expects (injected via [SESSION_CONTEXT_TAG] in the
     * chat history):
     *
     *   "Recurring themes: work, sleep, family. Recent pattern: anxious.
     *    Last time: Reassurance worked well."
     *
     * Returns an empty string when no summaries exist (first-ever session).
     * The output is capped to stay within the 300-token USER_PROFILE_RESERVE budget.
     */
    fun buildUserProfile(summaries: List<SessionSummary>): String {
        if (summaries.isEmpty()) return ""

        // ── Extract top 3 recurring themes across all summaries ────────────────
        val allThemes = summaries
            .flatMap { it.keyThemes.split(",") }
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }

        val topThemes = allThemes
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }

        // ── Most recent emotional pattern ──────────────────────────────────────
        val recentEmotion = summaries.first().dominantEmotion

        // ── Most recent helper pattern ─────────────────────────────────────────
        val recentHelper = summaries.first().helperPattern.trim()

        // ── Assemble profile string ────────────────────────────────────────────
        val themesStr = if (topThemes.isNotEmpty()) topThemes.joinToString(", ") else "general wellbeing"

        val profile = buildString {
            append("Recurring themes: $themesStr. ")
            append("Recent pattern: $recentEmotion. ")
            if (recentHelper.isNotBlank()) {
                append("Last time: $recentHelper worked well.")
            }
        }.trim()

        // Guard the 300-token budget (~1200 chars at 4 chars/token)
        return profile.take(1200)
    }

    /**
     * Returns the last 14 session summaries for profile construction.
     * Runs on [ioDispatcher] — safe to call from any coroutine.
     */
    suspend fun getRecentSummaries(): List<SessionSummary> = withContext(ioDispatcher) {
        sessionSummaryDao.getLastFourteenSummaries()
    }

    /**
     * Deletes all session summaries.
     * Called from the "Clear History" action to honour the privacy contract.
     */
    suspend fun clearAllSummaries() = withContext(ioDispatcher) {
        sessionSummaryDao.clearAllSummaries()
        Log.d(TAG, "All session summaries cleared")
    }

    /**
     * Phase 5: Returns the count of stored session summaries.
     *
     * Used by [com.iamashad.meraki.screens.settings.SettingsViewModel] to determine
     * whether the 4-session threshold for the smart-nudge auto-prompt has been reached.
     */
    suspend fun getSessionCount(): Int = withContext(ioDispatcher) {
        sessionSummaryDao.getLastFourteenSummaries().size
    }

    /**
     * Phase 5: Convenience instance method wrapping [generateSmartNudge].
     *
     * Fetches the most recent summaries and delegates to the companion object implementation.
     * Returns null if the 4-session gate has not been met or no pattern is detected.
     */
    suspend fun buildSmartNudge(): String? = withContext(ioDispatcher) {
        val summaries = sessionSummaryDao.getLastFourteenSummaries()
        generateSmartNudge(summaries)
    }

    // ── Private helpers ─────────────────────────────────────────────────────────

    /** Builds a compact conversation transcript for the summarisation prompt. */
    private fun buildTranscript(messages: List<Message>): String =
        messages
            .takeLast(MAX_TRANSCRIPT_MESSAGES)
            .joinToString("\n") { msg ->
                val label = if (msg.role == "user") "User" else "Meraki"
                "$label: ${msg.message.take(MAX_CHARS_PER_MESSAGE)}"
            }

    /**
     * Summarisation prompt per Section 7.4 of the implementation plan.
     *
     * Temperature 0.3 on the model ensures structured, deterministic output.
     * The prompt enforces the exact 3-bullet format parsed by [parseSummaryResponse].
     */
    private fun buildSummarisationPrompt(transcript: String): String = """
        Analyse this emotional support conversation and return EXACTLY 3 bullet points.

        Use these exact labels and format:
        • Key concerns: [2-4 topics the user mentioned, comma-separated, e.g. "work stress, sleep, relationships"]
        • Emotional pattern: [dominant emotion and rough intensity, e.g. "anxious, high" or "sad, moderate"]
        • Helpful response type: [what AI response style seemed most effective, e.g. "Reassurance", "Active listening", "Gentle guidance", "Validation"]

        Conversation transcript:
        $transcript

        Return only the 3 bullet points. No preamble, no explanation, no extra text.
    """.trimIndent()

    /**
     * Parses the model's 3-bullet response into structured fields.
     * Falls back gracefully if the format deviates from expectations.
     */
    private fun parseSummaryResponse(rawText: String, fallbackEmotion: String): ParsedSummary {
        val lines = rawText.lines()

        val keyThemes = lines
            .firstOrNull { it.contains("Key concerns", ignoreCase = true) }
            ?.substringAfter(":")
            ?.trim()
            ?.take(200)
            ?: fallbackEmotion

        val helperPattern = lines
            .firstOrNull { it.contains("Helpful response", ignoreCase = true) }
            ?.substringAfter(":")
            ?.trim()
            ?.take(100)
            ?: "Active listening"

        return ParsedSummary(keyThemes = keyThemes, helperPattern = helperPattern)
    }

    private data class ParsedSummary(val keyThemes: String, val helperPattern: String)
}
