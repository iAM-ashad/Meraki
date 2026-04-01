package com.iamashad.meraki.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.model.MindfulNudge
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
    private val groqRepository: GroqRepository
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
     * Tracks the last moodLogs size for which insight was generated, so we
     * avoid regenerating on every recomposition and only refresh when a new
     * mood entry has been added.
     */
    private var lastInsightGeneratedForSize = -1

    // Date formatter used to handle log dates
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        // Automatically fetch initial nudges when ViewModel is created
        fetchInitialNudges()
        loadDominantEmotion()
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

            // Log only if there is no entry for today
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

            // Get all streak logs, parse them to Date, and sort by most recent
            val logs = firestore.collection("users")
                .document(userId)
                .collection("streakLogs")
                .get()
                .await()
                .documents
                .mapNotNull { dateFormat.parse(it.getString("date") ?: "") }
                .sortedDescending()

            // Compare each log date to expected date in streak chain
            var currentStreakDate = today
            for (logDate in logs) {
                if (dateFormat.format(logDate) == dateFormat.format(currentStreakDate)) {
                    streak++
                    // Move to previous day in streak
                    currentStreakDate = Calendar.getInstance().apply {
                        time = currentStreakDate
                        add(Calendar.DAY_OF_YEAR, -1)
                    }.time
                } else {
                    break // streak breaks if a date is missing
                }
            }
            streak
        } catch (e: Exception) {
            println("Error calculating streak: ${e.localizedMessage}")
            0 // Return zero if error occurs
        }
    }

    // ---------------------------------------------------------------------------
    // Living Mood Card — insight + pattern generation
    // ---------------------------------------------------------------------------

    /**
     * Generates the weekly AI insight and detects patterns from [moodLogs].
     * Skips regeneration if the list size hasn't changed since the last call
     * (new mood logged = size increases).
     */
    fun generateWeeklyInsight(moodLogs: List<Pair<String, Int>>) {
        if (moodLogs.isEmpty()) return
        if (moodLogs.size == lastInsightGeneratedForSize) return
        lastInsightGeneratedForSize = moodLogs.size

        viewModelScope.launch {
            _insightLoading.value = true
            // Detect patterns first — synchronous, no network needed
            _patternAlert.value = detectPatterns(moodLogs)

            // Ask Groq for a one-sentence emotional summary
            val result = groqRepository.generateSimpleResponse(
                prompt = buildInsightPrompt(moodLogs),
                temperature = 0.5f
            )
            _weeklyInsight.value = result
            _insightLoading.value = false
        }
    }

    /**
     * Builds a compact, focused prompt so Groq returns exactly one warm sentence
     * summarising the week's emotional arc.
     */
    private fun buildInsightPrompt(moodLogs: List<Pair<String, Int>>): String {
        val moodSummary = moodLogs.takeLast(7)
            .joinToString(", ") { (date, score) -> "$date: $score/100" }
        return """
            You are a warm, empathetic mental health companion. Based on the following mood scores
            from the past week, write exactly ONE short sentence (under 25 words) that reflects the
            emotional pattern of the week. Be warm, specific, and non-clinical. Do not give advice —
            just gently name what you notice. Do not include quotation marks.

            Mood data: $moodSummary

            Example output: You had a strong start this week but energy dipped mid-week — journaling on Thursday seemed to help.

            Write only the one sentence.
        """.trimIndent()
    }

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