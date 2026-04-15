package com.iamashad.meraki.screens.onboarding

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.data.EmotionDao
import com.iamashad.meraki.data.EmotionLog
import com.iamashad.meraki.di.IoDispatcher
import com.iamashad.meraki.repository.GroqRepository
import com.iamashad.meraki.repository.UserPreferencesRepository
import com.iamashad.meraki.screens.onboarding.OnboardingViewModel.Companion.DEFAULT_CHECKIN_TIME
import com.iamashad.meraki.utils.MemoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel shared across the entire new onboarding arc (Phases 2, 3, 4).
 *
 * Responsibilities:
 *  - Hold the transient mood seed selected on [MoodSeedScreen] across the sign-up flow.
 *  - Save avatar selection to Firestore ([AvatarCelebrationScreen]).
 *  - Generate and stream the personalised Groq welcome + first journal prompt ([WelcomeAIScreen]).
 *  - Seed [MemoryManager] with a synthetic day-zero summary after welcome renders.
 *  - Parse natural-language check-in time via a non-streaming Groq call ([NotificationSetupScreen]).
 *  - Mark onboarding complete in [UserPreferencesRepository] once the user reaches Home.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val groqRepository: GroqRepository,
    private val memoryManager: MemoryManager,
    private val emotionDao: EmotionDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    companion object {
        private const val TAG = "OnboardingViewModel"
        private const val DEFAULT_CHECKIN_TIME = "20:00"

        // Regex that accepts HH:mm or H:mm 24-hour strings
        private val TIME_REGEX = Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")
    }

    // ── UI State ──────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // ── Phase 3: Mood Seed ─────────────────────────────────────────────────────

    /** Called from MoodSeedScreen when the user taps a mood chip. */
    fun selectMood(mood: String) {
        _uiState.update { it.copy(selectedMood = mood) }
    }

    /**
     * Persists the seeded mood to Room via [EmotionDao] after account creation.
     * Uses a synthetic sessionId ("onboarding-seed") and messageId (0) so the
     * entry is clearly identifiable as the pre-account seed.
     *
     * @param userId  Firebase UID of the newly created account.
     */
    fun persistMoodSeed(userId: String) {
        val mood = _uiState.value.selectedMood ?: return
        viewModelScope.launch(ioDispatcher) {
            try {
                val log = EmotionLog(
                    sessionId = "onboarding-seed-$userId",
                    messageId = 0L,
                    emotion = mood.lowercase(),
                    intensity = "moderate",
                    confidence = 1.0f,
                    timestamp = System.currentTimeMillis()
                )
                emotionDao.insertEmotionLog(log)
                Log.d(TAG, "Mood seed persisted: $mood for userId=$userId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist mood seed: ${e.message}", e)
            }
        }
    }

    // ── Phase 2: Avatar Selection ─────────────────────────────────────────────

    /** Updates the selected avatar in state (triggers confetti in the UI). */
    fun selectAvatar(avatarRes: Int) {
        _uiState.update { it.copy(selectedAvatar = avatarRes) }
    }

    /**
     * Saves the chosen avatar drawable resource ID to Firestore then sets
     * [OnboardingUiState.avatarSaved] to true to trigger navigation.
     */
    fun saveAvatar(userId: String, avatarRes: Int) {
        _uiState.update { it.copy(isSavingAvatar = true) }
        firestore.collection("users").document(userId)
            .update("profilePicRes", avatarRes)
            .addOnSuccessListener {
                _uiState.update { it.copy(isSavingAvatar = false, avatarSaved = true) }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save avatar: ${e.message}", e)
                // Navigate anyway — avatar defaults gracefully
                _uiState.update { it.copy(isSavingAvatar = false, avatarSaved = true) }
            }
    }

    // ── Phase 3: Welcome AI Screen ────────────────────────────────────────────

    /**
     * Calls Groq with a tightly scoped prompt to generate:
     *  (a) A 2-sentence warm welcome for the user.
     *  (b) One reflective journal prompt for their first night.
     *
     * Results are streamed into [OnboardingUiState.welcomeText] and
     * [OnboardingUiState.firstJournalPrompt] respectively.
     *
     * Also seeds [MemoryManager] with a synthetic day-zero summary once the
     * welcome has rendered, so the first chatbot session is contextually warm.
     */
    fun generateWelcome(userName: String, userId: String) {
        val mood = _uiState.value.selectedMood ?: "neutral"
        _uiState.update {
            it.copy(
                isGeneratingWelcome = true,
                welcomeText = "",
                firstJournalPrompt = ""
            )
        }

        viewModelScope.launch(ioDispatcher) {
            try {
                val systemPrompt = """
                    You are Meraki, a warm and emotionally intelligent companion.
                    The user's name is $userName and they just told us they're feeling: $mood.

                    Respond with EXACTLY this format (no extra text):
                    WELCOME: [2 short warm sentences greeting them by name and acknowledging their mood]
                    PROMPT: [1 gentle reflective journal question for tonight, starting with 'Tonight,']
                """.trimIndent()

                val fullResponse = groqRepository.generateSimpleResponse(
                    prompt = systemPrompt,
                    temperature = 0.75f
                ).orEmpty()

                val welcomePart = fullResponse
                    .lineSequence()
                    .firstOrNull { it.startsWith("WELCOME:") }
                    ?.removePrefix("WELCOME:")
                    ?.trim()
                    ?: "Hey $userName — welcome to Meraki. I'm really glad you're here."

                val journalPrompt = fullResponse
                    .lineSequence()
                    .firstOrNull { it.startsWith("PROMPT:") }
                    ?.removePrefix("PROMPT:")
                    ?.trim()
                    ?: "Tonight, what's one thing you're grateful for today?"

                _uiState.update {
                    it.copy(
                        welcomeText = welcomePart,
                        firstJournalPrompt = journalPrompt,
                        isGeneratingWelcome = false
                    )
                }

                // Seed MemoryManager with a synthetic day-zero summary
                seedMemoryManager(userName = userName, mood = mood, userId = userId)

            } catch (e: Exception) {
                Log.e(TAG, "Welcome generation failed: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        welcomeText = "Hey $userName — welcome to Meraki. I'm really glad you're here. 💙",
                        firstJournalPrompt = "Tonight, what's one thing you're grateful for today?",
                        isGeneratingWelcome = false
                    )
                }
            }
        }
    }

    /**
     * Builds and saves a synthetic day-zero [SessionSummary] via [MemoryManager].
     * This gives the AI a non-empty profile for the user's very first Chatbot session.
     */
    private suspend fun seedMemoryManager(userName: String, mood: String, userId: String) {
        try {
            val syntheticMessages = listOf(
                com.iamashad.meraki.model.Message(
                    role = "user",
                    message = "Hi, I'm $userName and I'm feeling $mood today."
                )
            )
            memoryManager.summariseAndSave(
                sessionId = "onboarding-seed-$userId",
                messages = syntheticMessages,
                dominantEmotion = mood.lowercase()
            )
            Log.d(TAG, "MemoryManager seeded for userId=$userId with mood=$mood")
        } catch (e: Exception) {
            Log.e(TAG, "MemoryManager seed failed (non-fatal): ${e.message}", e)
        }
    }

    // ── Phase 4: Notification Time Parsing ────────────────────────────────────

    /**
     * Parses a free-text wind-down time into a "HH:mm" string via a lightweight
     * non-streaming Groq call. Falls back to [DEFAULT_CHECKIN_TIME] (20:00) silently
     * if parsing fails or returns an invalid format.
     *
     * Example inputs: "9pm", "around 9 at night", "after dinner (8:30)".
     */
    fun parseAndSetCheckInTime(rawInput: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val prompt = """
                    Extract the time from this message and return ONLY a 24-hour HH:mm string (e.g. 21:00).
                    No explanation, no extra text — just the time.
                    Message: "$rawInput"
                """.trimIndent()

                val result = groqRepository.generateSimpleResponse(prompt, temperature = 0.1f)
                    ?.trim()
                    .orEmpty()

                val parsedTime = if (TIME_REGEX.matches(result)) result else DEFAULT_CHECKIN_TIME
                userPreferencesRepository.setPreferredCheckInTime(parsedTime)
                _uiState.update { it.copy(parsedCheckInTime = parsedTime) }
                Log.d(TAG, "Check-in time set to $parsedTime (raw: '$rawInput')")

            } catch (e: Exception) {
                Log.e(TAG, "Time parse failed, using default: ${e.message}", e)
                userPreferencesRepository.setPreferredCheckInTime(DEFAULT_CHECKIN_TIME)
                _uiState.update { it.copy(parsedCheckInTime = DEFAULT_CHECKIN_TIME) }
            }
        }
    }

    // ── Phase 4: Enable notifications ────────────────────────────────────────

    /**
     * Enables daily check-in notifications in [UserPreferencesRepository].
     * Called from [NotificationSetupScreen] when the user taps "Enable reminders".
     * The Android runtime permission is requested by the screen itself via
     * [ActivityResultLauncher] — this function only updates the DataStore flags.
     */
    fun enableNotifications() {
        viewModelScope.launch(ioDispatcher) {
            userPreferencesRepository.setDailyCheckInEnabled(true)
            Log.d(TAG, "Daily check-in notifications enabled")
        }
    }

    // ── Onboarding completion ─────────────────────────────────────────────────

    /**
     * Marks onboarding as permanently completed in DataStore.
     * Called from [WelcomeAIScreen] after the user taps "Start my journey"
     * (navigating to [NotificationSetupScreen]) or from [NotificationSetupScreen]
     * before navigating to [Home].
     */
    fun markOnboardingComplete() {
        viewModelScope.launch(ioDispatcher) {
            userPreferencesRepository.markOnboardingComplete()
            Log.d(TAG, "Onboarding marked complete")
        }
    }
}

/**
 * Consolidated UI state for the entire onboarding arc (Phases 2–4).
 */
data class OnboardingUiState(
    // Phase 3: mood seed
    val selectedMood: String? = null,

    // Phase 2: avatar selection
    val selectedAvatar: Int? = null,
    val isSavingAvatar: Boolean = false,
    val avatarSaved: Boolean = false,

    // Phase 3: welcome AI content
    val isGeneratingWelcome: Boolean = false,
    val welcomeText: String = "",
    val firstJournalPrompt: String = "",

    // Phase 4: notification time
    val parsedCheckInTime: String = "20:00"
)
