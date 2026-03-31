package com.iamashad.meraki.screens.moodtracker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.model.Mood
import com.iamashad.meraki.repository.MoodRepository
import com.iamashad.meraki.utils.MemoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.Locale.getDefault
import javax.inject.Inject

@HiltViewModel
class MoodTrackerViewModel @Inject constructor(
    private val moodRepository: MoodRepository,
    private val firebaseAuth: FirebaseAuth,
    private val memoryManager: MemoryManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _moodTrend = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val moodTrend: StateFlow<List<Pair<String, Int>>> = _moodTrend.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /**
     * The pre-filled emotions passed from the journal screen (if any).
     * Used to drive the Priority 1 (post-journal) prompt and initialize the mood score.
     */
    private val preFilledEmotions: String? = savedStateHandle["preFilledEmotions"]

    /**
     * Suggested initial mood score based on pre-filled journal emotions.
     * Defaults to 50f (neutral) if no pre-filled data is present.
     */
    private val _suggestedMoodScore = MutableStateFlow(calculateInitialScore(preFilledEmotions))
    val suggestedMoodScore: StateFlow<Float> = _suggestedMoodScore.asStateFlow()

    /**
     * Context-aware opening prompt for the mood log screen.
     *
     * Built from three data sources (priority order):
     *  1. Last session's dominant emotion (MemoryManager) — empathetic follow-up when
     *     the user was anxious / sad / stressed in their previous chat.
     *  2. Mood history patterns — detects if today (e.g. Sunday) is historically a low-mood day.
     *  3. Day-of-week greeting — soft acknowledgment for generic days.
     *  4. Time-of-day greeting — morning check-in vs. evening wind-down fallback.
     */
    private val _moodPrompt = MutableStateFlow("How are you feeling right now?")
    val moodPrompt: StateFlow<String> = _moodPrompt.asStateFlow()

    /**
     * One-time snackbar prompt shown only when arriving from the journal screen.
     */
    private val _postJournalSnackbarPrompt = MutableStateFlow<String?>(null)
    val postJournalSnackbarPrompt: StateFlow<String?> = _postJournalSnackbarPrompt.asStateFlow()

    init {
        fetchMoodTrend()
        buildMoodPrompt()
        checkPostJournalContext()
    }

    /**
     * If the user just arrived from the journal screen, prepares a one-time
     * "beautified" snackbar prompt.
     */
    private fun checkPostJournalContext() {
        if (preFilledEmotions != null) {
            _postJournalSnackbarPrompt.value = "You just journaled about being ${
                preFilledEmotions.lowercase(
                    getDefault()
                )
            }. Let us know how you feel right now?"
        }
    }

    /**
     * Reads recent session summaries, mood history, and the current time to produce
     * an adaptive opening question for the mood log screen.
     */
    private fun buildMoodPrompt() {
        viewModelScope.launch {
            val summaries = memoryManager.getRecentSummaries()
            val lastEmotion = summaries.firstOrNull()?.dominantEmotion?.lowercase()

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

            // Pattern Detection: Is today (Sunday) historically a low-mood day for this user?
            val isSunday = dayOfWeek == Calendar.SUNDAY
            var sundayPatternDetected = false
            if (isSunday) {
                moodRepository.getAllMoods().firstOrNull()?.let { moods ->
                    val sundayMoods = moods.filter {
                        val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                    }
                    if (sundayMoods.size >= 2) {
                        val avgSunday = sundayMoods.map { it.score }.average()
                        val overallAvg = moods.map { it.score }.average()
                        // If Sundays are 10%+ lower than their overall average mood
                        if (avgSunday < overallAvg * 0.9) {
                            sundayPatternDetected = true
                        }
                    }
                }
            }

            val prompt = when {
                // Priority 1 – follow up on a difficult previous session
                lastEmotion == "anxious" ->
                    "Yesterday felt a bit heavy — how are you doing today?"
                lastEmotion == "sad" ->
                    "You seemed down last time. How are things feeling now?"
                lastEmotion == "stressed" ->
                    "Things felt stressful before — any lighter today?"

                // Priority 2 – mood history patterns (Sunday specific)
                sundayPatternDetected ->
                    "Sundays can feel a bit tougher for you historically. How are you holding up today?"

                // Priority 3 – day-of-week pattern (Generic)
                dayOfWeek == Calendar.SUNDAY ->
                    "Sundays can feel heavy sometimes — how are you holding up?"
                dayOfWeek == Calendar.MONDAY ->
                    "New week starting — how are you feeling as it begins?"
                dayOfWeek == Calendar.SATURDAY ->
                    "Happy weekend! How's your energy feeling today?"

                // Priority 4 – time-of-day greeting fallback
                hour in 5..11 ->
                    "Good morning — how are you starting the day?"
                hour in 12..17 ->
                    "How's your afternoon going so far?"
                hour in 18..21 ->
                    "How did today end up feeling?"
                else ->
                    "How are you feeling right now?"
            }

            _moodPrompt.emit(prompt)
        }
    }

    private fun calculateInitialScore(emotions: String?): Float {
        if (emotions == null) return 50f
        val e = emotions.lowercase()
        return when {
            e.contains("happy") || e.contains("excited") || e.contains("peaceful") || e.contains("grateful") -> 85f
            e.contains("calm") || e.contains("hopeful") -> 70f
            e.contains("sad") || e.contains("lonely") || e.contains("tired") -> 25f
            e.contains("stressed") || e.contains("angry") || e.contains("confused") -> 35f
            else -> 50f
        }
    }

    fun fetchMoodTrend() {
        viewModelScope.launch {
            _loading.emit(true)
            moodRepository.getAllMoods()
                .map { moods ->
                    moods.sortedBy { it.timestamp }
                        .map { mood ->
                            android.text.format.DateFormat.format("MM-dd", mood.timestamp)
                                .toString() to mood.score
                        }
                }
                .catch { throwable ->
                    // Handle errors gracefully
                    _moodTrend.emit(emptyList())
                    _loading.emit(false)
                }
                .collect { trend ->
                    _moodTrend.emit(trend)
                    _loading.emit(false)
                }
        }
    }

    fun logMood(score: Int) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            error("User not authenticated")
            return
        }

        viewModelScope.launch {
            val mood = Mood(
                score = score,
                timestamp = System.currentTimeMillis(),
                userId = currentUser.uid
            )
            try {
                moodRepository.addMood(mood)
            } catch (e: Exception) {
                error("Failed to log mood: ${e.message}")
            }
        }
    }
}
