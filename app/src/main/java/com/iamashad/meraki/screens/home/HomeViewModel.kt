package com.iamashad.meraki.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.model.ConfidenceScore
import com.iamashad.meraki.model.InsightTier
import com.iamashad.meraki.model.MindfulNudge
import com.iamashad.meraki.repository.ConfidenceScoreRepository
import com.iamashad.meraki.repository.GroqRepository
import com.iamashad.meraki.repository.NudgeRepository
import com.iamashad.meraki.utils.MemoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// ---------------------------------------------------------------------------
// Pattern alert model — surfaced in State 3 of the LivingMoodCard
// ---------------------------------------------------------------------------

enum class PatternActionType { BREATHING, CHATBOT }

data class PatternAlert(
    val message: String,
    val actionType: PatternActionType
)

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val nudgeRepository: NudgeRepository,
    private val firestore: FirebaseFirestore,
    private val memoryManager: MemoryManager,
    private val groqRepository: GroqRepository,
    private val confidenceScoreRepository: ConfidenceScoreRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    // StateFlow to hold a list of mindful nudges
    private val _nudges = MutableStateFlow<List<MindfulNudge>>(emptyList())
    val nudges: StateFlow<List<MindfulNudge>> = _nudges.asStateFlow()

    /**
     * The dominant emotion from the user's most recent chat session.
     * Defaults to "neutral" on first launch (no session history).
     * Drives the mood-aware UI tint on the Home screen.
     */
    private val _dominantEmotion = MutableStateFlow("neutral")
    val dominantEmotion: StateFlow<String> = _dominantEmotion.asStateFlow()

    // ---------------------------------------------------------------------------
    // Living Mood Card state
    // ---------------------------------------------------------------------------

    /** AI-generated one-sentence summary of the week's emotional pattern. */
    private val _weeklyInsight = MutableStateFlow<String?>(null)
    val weeklyInsight: StateFlow<String?> = _weeklyInsight.asStateFlow()

    /** True while the Groq call for the weekly insight is in-flight. */
    private val _insightLoading = MutableStateFlow(false)
    val insightLoading: StateFlow<Boolean> = _insightLoading.asStateFlow()

    /** Detected pattern alert — null when no meaningful pattern is found. */
    private val _patternAlert = MutableStateFlow<PatternAlert?>(null)
    val patternAlert: StateFlow<PatternAlert?> = _patternAlert.asStateFlow()

    /**
     * The current user confidence score.
     * Drives which tier of insight (or placeholder) is shown in the UI.
     */
    private val _confidenceScore = MutableStateFlow(ConfidenceScore.EMPTY)
    val confidenceScore: StateFlow<ConfidenceScore> = _confidenceScore.asStateFlow()

    /**
     * Convenience shortcut to the tier derived from [_confidenceScore].
     * The UI collects this to decide what content to render.
     */
    private val _insightTier = MutableStateFlow(InsightTier.FORMING)
    val insightTier: StateFlow<InsightTier> = _insightTier.asStateFlow()

    /**
     * Tracks the last moodLogs size for which insight was generated, so we
     * avoid regenerating on every recomposition and only refresh when a new
     * mood entry has been added.
     */
    private var lastInsightGeneratedForSize = -1

    // Date formatter used to handle log dates
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        fetchInitialNudges()
        loadDominantEmotion()
        refreshConfidenceScore()
    }

    /**
     * Reads the last session summary to seed the mood-aware UI tint.
     * Falls back to "neutral" when no prior session exists.
     */
    private fun loadDominantEmotion() {
        viewModelScope.launch {
            val summaries = memoryManager.getRecentSummaries()
            val emotion = summaries.firstOrNull()?.dominantEmotion ?: "neutral"
            _dominantEmotion.emit(emotion)
        }
    }

    private fun fetchInitialNudges() {
        viewModelScope.launch {
            try {
                val initialNudges = nudgeRepository.getInitialNudges()
                _nudges.emit(initialNudges)
            } catch (e: Exception) {
                println("Failed to fetch initial nudges: ${e.localizedMessage}")
            }
        }
    }

    fun fetchNextNudge() {
        viewModelScope.launch {
            try {
                val nextNudge = nudgeRepository.getNextNudge()
                _nudges.emit(_nudges.value + nextNudge)
            } catch (e: Exception) {
                println("Failed to fetch new nudge: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Recomputes the confidence score from the database and updates both
     * [_confidenceScore] and [_insightTier].
     *
     * Called on init and again whenever a new mood entry triggers
     * [generateWeeklyInsight] so the tier stays in sync with new data.
     */
    private fun refreshConfidenceScore() {
        viewModelScope.launch {
            val uid = firebaseAuth.currentUser?.uid ?: return@launch
            val score = confidenceScoreRepository.compute(uid)
            _confidenceScore.value = score
            _insightTier.value = score.tier
        }
    }

    // Log user's daily usage in Firestore to help track streaks
    suspend fun logDailyUsage(userId: String) {
        try {
            val today = dateFormat.format(Date())
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("streakLogs")
                .whereEqualTo("date", today)
                .get()
                .await()

            if (snapshot.isEmpty) {
                val batch = firestore.batch()
                val logRef = firestore.collection("users")
                    .document(userId)
                    .collection("streakLogs")
                    .document()

                batch.set(logRef, mapOf("date" to today))
                batch.commit().await()
            }
        } catch (e: Exception) {
            println("Error logging daily usage: ${e.localizedMessage}")
        }
    }

    // Calculates the user's current streak by checking continuous logs
    suspend fun calculateStreak(userId: String): Int {
        return try {
            val today = dateFormat.parse(dateFormat.format(Date())) ?: return 0
            var streak = 0

            val logs = firestore.collection("users")
                .document(userId)
                .collection("streakLogs")
                .get()
                .await()
                .documents
                .mapNotNull { dateFormat.parse(it.getString("date") ?: "") }
                .sortedDescending()

            var currentStreakDate = today
            for (logDate in logs) {
                if (dateFormat.format(logDate) == dateFormat.format(currentStreakDate)) {
                    streak++
                    currentStreakDate = Calendar.getInstance().apply {
                        time = currentStreakDate
                        add(Calendar.DAY_OF_YEAR, -1)
                    }.time
                } else {
                    break
                }
            }
            streak
        } catch (e: Exception) {
            println("Error calculating streak: ${e.localizedMessage}")
            0
        }
    }

    // ---------------------------------------------------------------------------
    // Living Mood Card — insight + pattern generation
    // ---------------------------------------------------------------------------

    /**
     * Generates the weekly AI insight and detects patterns from [moodLogs].
     *
     * The confidence tier gates what kind of insight is produced:
     *
     *   [InsightTier.FORMING]  — No Groq call. The UI will show a warm placeholder
     *                            explaining how many more check-ins are needed.
     *   [InsightTier.LOW]      — Basic mood-data-only prompt. One warm sentence
     *                            summarising the emotional arc with no personalization.
     *   [InsightTier.MODERATE] — Enriched prompt that names the session count so Groq
     *                            can acknowledge repeat engagement.
     *   [InsightTier.HIGH]     — Full prompt including recent session themes and dominant
     *                            emotions pulled from [MemoryManager], enabling the richest
     *                            personalization.
     *
     * Skips regeneration if the moodLogs list size hasn't changed (new log = size grows).
     */
    fun generateWeeklyInsight(moodLogs: List<Pair<String, Int>>) {
        if (moodLogs.isEmpty()) return
        if (moodLogs.size == lastInsightGeneratedForSize) return
        lastInsightGeneratedForSize = moodLogs.size

        viewModelScope.launch {
            // Refresh the confidence score before deciding what to generate
            val uid = firebaseAuth.currentUser?.uid
            if (uid != null) {
                val score = confidenceScoreRepository.compute(uid)
                _confidenceScore.value = score
                _insightTier.value = score.tier
            }

            val tier = _insightTier.value

            // Detect mood patterns — always run regardless of tier so the
            // pattern page is available as soon as there is enough mood data.
            _patternAlert.value = detectPatterns(moodLogs)

            // Gate the Groq call on the confidence tier
            if (tier == InsightTier.FORMING) {
                // Not enough data — let the UI show the placeholder
                _weeklyInsight.value = null
                return@launch
            }

            _insightLoading.value = true

            val prompt = when (tier) {
                InsightTier.LOW -> buildBasicInsightPrompt(moodLogs)
                InsightTier.MODERATE -> buildModerateInsightPrompt(moodLogs, _confidenceScore.value)
                InsightTier.HIGH -> {
                    val summaries = memoryManager.getRecentSummaries()
                    buildHighInsightPrompt(moodLogs, _confidenceScore.value, summaries)
                }
                else -> buildBasicInsightPrompt(moodLogs) // Unreachable — FORMING handled above
            }

            val result = groqRepository.generateSimpleResponse(
                prompt = prompt,
                temperature = 0.5f
            )
            _weeklyInsight.value = result
            _insightLoading.value = false
        }
    }

    // ---------------------------------------------------------------------------
    // Prompt builders — one per confidence tier
    // ---------------------------------------------------------------------------

    /**
     * Converts a 0–100 mood score into a short emotional phrase so that the
     * model never sees raw numbers — only human-readable feelings.
     *
     * This is the key step that prevents Groq from echoing back evaluative
     * language like "a mood score of 75" in the insight sentence.
     */
    private fun scoreToFeeling(score: Int): String = when {
        score >= 85 -> "joyful and vibrant"
        score >= 70 -> "light and fairly good"
        score >= 55 -> "calm and balanced"
        score >= 40 -> "a bit mixed or unsettled"
        score >= 25 -> "heavy-hearted and low"
        else        -> "really struggling"
    }

    /**
     * Describes the overall directional arc of [moodLogs] — used to give
     * the model a sense of trajectory without exposing any numbers.
     */
    private fun describeArc(moodLogs: List<Pair<String, Int>>): String {
        if (moodLogs.size < 2) return "steady"
        val first = moodLogs.first().second
        val last  = moodLogs.last().second
        val diff  = last - first
        return when {
            diff >  20 -> "gradually lifting as the week went on"
            diff < -20 -> "gradually getting heavier as the week went on"
            else -> {
                val scores = moodLogs.map { it.second }
                val range = (scores.maxOrNull() ?: 0) - (scores.minOrNull() ?: 0)
                if (range > 35) "moving up and down quite a bit" else "holding fairly steady"
            }
        }
    }

    /**
     * Converts the last 7 mood logs into a human-readable emotional narrative
     * with no numbers — e.g. "Mon: calm and balanced, Tue: light and fairly good".
     */
    private fun buildEmotionalSummary(moodLogs: List<Pair<String, Int>>): String =
        moodLogs.takeLast(7).joinToString("; ") { (date, score) ->
            "$date felt ${scoreToFeeling(score)}"
        }

    /**
     * Shared instruction block appended to every prompt tier.
     * Explicitly prohibits numbers, scores, and evaluative grading language.
     */
    private val noNumbersRule = """
        IMPORTANT RULES — you must follow all of these without exception:
        • Do NOT mention any numbers, scores, percentages, ratings, or grades.
        • Do NOT say things like "a score of X", "X out of 100", "X points", or similar.
        • Describe feelings only in emotional, human language — never in metrics.
        • Write exactly ONE sentence. Do not include quotation marks.
    """.trimIndent()

    /**
     * LOW tier — pure mood-feeling narrative, no personalization.
     * Produces a single warm sentence acknowledging the week's emotional arc.
     */
    private fun buildBasicInsightPrompt(moodLogs: List<Pair<String, Int>>): String {
        val emotionalSummary = buildEmotionalSummary(moodLogs)
        val arc = describeArc(moodLogs.takeLast(7))
        return """
            You are a warm, empathetic mental health companion. Based on how this person felt each
            day this week, write exactly ONE short sentence (under 25 words) that gently reflects
            their emotional arc. Be warm, human, and non-clinical. Do not give advice — just softly
            name what you notice in their week.

            $noNumbersRule

            How they felt each day: $emotionalSummary
            Overall direction: $arc

            Good examples of the tone and style to aim for:
            - "You had a gentle start to the week and seemed to find your footing as the days went on."
            - "There were some heavier moments mid-week, but you came through to a brighter place by the end."
            - "It felt like a steady, even-keeled week — a kind of quiet resilience showing through."

            Write only the one sentence.
        """.trimIndent()
    }

    /**
     * MODERATE tier — emotional narrative plus engagement context.
     * Acknowledges the user's ongoing journey without grading their feelings.
     */
    private fun buildModerateInsightPrompt(
        moodLogs: List<Pair<String, Int>>,
        score: ConfidenceScore
    ): String {
        val emotionalSummary = buildEmotionalSummary(moodLogs)
        val arc = describeArc(moodLogs.takeLast(7))
        return """
            You are a warm, empathetic mental health companion who has been walking alongside this
            person for a little while — they have checked in regularly and you're starting to know them.

            Based on how they felt each day this week, write exactly ONE short sentence (under 25 words)
            that gently reflects their emotional arc. You may subtly acknowledge their ongoing journey
            without being sycophantic. Be warm, human, and non-clinical. Do not give advice.

            $noNumbersRule

            How they felt each day: $emotionalSummary
            Overall direction: $arc

            Write only the one sentence.
        """.trimIndent()
    }

    /**
     * HIGH tier — emotional narrative plus session themes and dominant emotions from memory.
     * Groq has enough context to produce a genuinely personalized insight that
     * references the user's actual recurring concerns in human, feeling-based language.
     */
    private fun buildHighInsightPrompt(
        moodLogs: List<Pair<String, Int>>,
        score: ConfidenceScore,
        summaries: List<com.iamashad.meraki.data.SessionSummary>
    ): String {
        val emotionalSummary = buildEmotionalSummary(moodLogs)
        val arc = describeArc(moodLogs.takeLast(7))

        val recentThemes = summaries
            .take(4)
            .flatMap { it.keyThemes.split(",") }
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .joinToString(", ") { it.key }

        val recentDominantEmotion = summaries.firstOrNull()?.dominantEmotion ?: "neutral"

        return """
            You are a warm, empathetic mental health companion with real familiarity with this person.
            You know that they have been carrying feelings of $recentDominantEmotion lately, and that
            themes like $recentThemes have been showing up in their life.

            Based on how they felt each day this week, write exactly ONE short sentence (under 30 words)
            that reflects their emotional arc in a genuinely personal way. You may gently weave in their
            known themes or emotional patterns if it feels natural and relevant. Be warm, human, and
            non-clinical. Do not give advice.

            $noNumbersRule

            How they felt each day: $emotionalSummary
            Overall direction: $arc

            Write only the one sentence.
        """.trimIndent()
    }

    // ---------------------------------------------------------------------------
    // Pattern detection
    // ---------------------------------------------------------------------------

    /**
     * Scans [moodLogs] for three kinds of pattern:
     *  1. Three or more consecutive days of declining mood → breathing exercise
     *  2. Current mood significantly below the week's average → breathing exercise
     *  3. Volatile week (large swings) → chatbot conversation
     *
     * Returns the highest-priority [PatternAlert] found, or null if nothing notable.
     */
    private fun detectPatterns(moodLogs: List<Pair<String, Int>>): PatternAlert? {
        if (moodLogs.size < 3) return null

        // Pattern 1 — consecutive declining days
        var declineStreak = 1
        var maxDeclineStreak = 1
        for (i in 1 until moodLogs.size) {
            if (moodLogs[i].second < moodLogs[i - 1].second) {
                declineStreak++
                if (declineStreak > maxDeclineStreak) maxDeclineStreak = declineStreak
            } else {
                declineStreak = 1
            }
        }
        if (maxDeclineStreak >= 3) {
            return PatternAlert(
                message = "Noticed: your mood has been slipping for a few days. " +
                        "A short breathing exercise might help you reset.",
                actionType = PatternActionType.BREATHING
            )
        }

        // Pattern 2 — latest entry meaningfully below weekly average
        val scores = moodLogs.map { it.second }
        val avg = scores.average()
        val latest = scores.last()
        if (latest < avg * 0.75 && latest < 50) {
            return PatternAlert(
                message = "Noticed: today feels harder than usual. " +
                        "Want to try a short breathing exercise to find some calm?",
                actionType = PatternActionType.BREATHING
            )
        }

        // Pattern 3 — high volatility (range > 40 points over the week)
        val range = (scores.maxOrNull() ?: 0) - (scores.minOrNull() ?: 0)
        if (range > 40) {
            return PatternAlert(
                message = "Noticed: your week had some big swings. " +
                        "Talking it through with the AI companion might bring some clarity.",
                actionType = PatternActionType.CHATBOT
            )
        }

        return null
    }
}
