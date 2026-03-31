package com.iamashad.meraki.screens.chatbot

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.data.ChatMessage
import com.iamashad.meraki.data.EmotionDao
import com.iamashad.meraki.data.EmotionLog
import com.iamashad.meraki.model.EmotionCategory
import com.iamashad.meraki.model.EmotionResult
import com.iamashad.meraki.model.Message
import com.iamashad.meraki.repository.ChatRepository
import com.iamashad.meraki.repository.GroqRepository
import com.iamashad.meraki.repository.GroqRepositoryImpl
import com.iamashad.meraki.repository.UserPreferencesRepository
import com.iamashad.meraki.utils.EmotionClassifier
import com.iamashad.meraki.utils.MemoryManager
import com.iamashad.meraki.utils.buildTokenAwareContext
import com.iamashad.meraki.utils.estimateTokens
import com.iamashad.meraki.utils.getEmotionGradient
import com.iamashad.meraki.utils.getSystemInstructions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.util.UUID
import javax.inject.Inject

// Phase 2: UDF — single immutable state class replacing dispersed mutableStateOf/mutableStateListOf.
// Phase 3: adds currentEmotion for intensity-aware gradient rendering.
data class ChatUiState(
    val activeContext: String = "neutral",
    val currentEmotion: EmotionResult? = null,   // Phase 3: last classified emotion result
    val messages: List<Message> = emptyList(),
    val isTyping: Boolean = false,
    val isLoading: Boolean = false
)

/**
 * ViewModel to manage chatbot state, messages, and interactions.
 * Handles message sending, context tagging, history loading and emotion-based feedback.
 *
 * Phase 2: all state is consolidated into a single MutableStateFlow<ChatUiState>.
 * Phase 3: keyword-based analyzeEmotion() replaced by on-device [EmotionClassifier];
 *          results are persisted in Room via [EmotionDao].
 * Phase 4: adds long-term session memory via [MemoryManager]:
 *   - User profile built from last 14 session summaries, passed to GroqRepository
 *     for injection into every API call.
 *   - Session summary generated (via Gemini temp=0.3) in onCleared() so every session
 *     ending is captured within ~30s.
 *   - clearChatHistory() now also wipes all [SessionSummary] entries.
 * Phase 6 (Groq migration): [GenerativeModel] and persistent [Chat] sessions replaced
 *   by [GroqRepository]. Every sendMessage call is now a fresh stateless streaming
 *   request that carries the full system prompt, history, user profile, and emotion
 *   context — no server-side session to manage or race against.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val groqRepository: GroqRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    // Phase 3: on-device emotion intelligence
    private val emotionClassifier: EmotionClassifier,
    private val emotionDao: EmotionDao,
    // Phase 4: long-term session memory
    private val memoryManager: MemoryManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Phase 1: exposes the token count of the history slice most recently sent to the
    // model, for debug overlays, logging, and future adaptive-trimming heuristics.
    private val _contextTokensUsed = MutableStateFlow(0)
    val contextTokensUsed: StateFlow<Int> = _contextTokensUsed.asStateFlow()

    private val userId: String = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    // Phase 3: UUID that identifies the current conversation session; used as the
    // foreign key in emotion_logs so all classifications are grouped by session.
    private var sessionId: String = UUID.randomUUID().toString()

    // Phase 4: dominant emotion observed during this session, updated as messages are
    // classified. Written into the session summary when the session ends.
    private var sessionDominantEmotion: String = "neutral"

    // Phase 4: pre-built user-profile string passed to GroqRepository on every API
    // call so the model receives long-term context without synthetic history entries.
    // Populated in startNewConversation() / loadPreviousConversation().
    private var cachedUserProfile: String = ""

    // Phase 2: serializes sendMessage() calls so rapid double-taps produce only one
    // API request.  tryLock() is used so a concurrent call is silently dropped rather
    // than queued — the UI also disables the send button while isSending is true.
    private val requestMutex = Mutex()

    // Phase 2: minimum gap (ms) between successive API calls.
    private var lastRequestMs: Long = 0L
    private val COOLDOWN_MS = 1500L

    companion object {
        /**
         * Phase 1: fixed message-count cap superseded by [buildTokenAwareContext], which
         * uses a dynamic token budget defined in [com.iamashad.meraki.utils.ContextConfig].
         * Retained here so any external call-sites that reference this constant continue
         * to compile without changes until they are individually migrated.
         *
         * @deprecated Use [buildTokenAwareContext] + ContextConfig.AVAILABLE_FOR_HISTORY instead.
         */
        @Deprecated("Replaced by token-aware context trimming (Phase 1)")
        const val MAX_HISTORY_MESSAGES = 20
    }

    // ── Lifecycle ───────────────────────────────────────────────────────────────

    /**
     * Phase 4: triggered when the ViewModel is destroyed (app goes to background,
     * user navigates away, or process is about to die). Generates and persists the
     * session summary within the viewModelScope, which is kept alive just long enough
     * for the coroutine to complete.
     *
     * The summary is only attempted when at least 3 user messages exist so trivial
     * sessions (e.g. just a greeting) are not summarised.
     */
    override fun onCleared() {
        super.onCleared()
        val messagesSnapshot = _uiState.value.messages
        val currentSession = sessionId
        val dominantEmotion = sessionDominantEmotion
        viewModelScope.launch {
            memoryManager.summariseAndSave(
                sessionId       = currentSession,
                messages        = messagesSnapshot,
                dominantEmotion = dominantEmotion
            )
        }
    }

    // ── Session initialisation ──────────────────────────────────────────────────

    // Checks if there are any previously saved messages for this user.
    suspend fun hasPreviousConversation(): Boolean {
        return chatRepository.getAllMessages(userId).first().isNotEmpty()
    }

    // Initializes the gradient context from the most recent session summary's dominant
    // emotion so the background colour is meaningful from the moment the screen opens.
    fun initializeContext(userId: String) {
        viewModelScope.launch {
            val summaries = memoryManager.getRecentSummaries()
            val lastEmotion = summaries.firstOrNull()?.dominantEmotion ?: "neutral"
            _uiState.update { it.copy(activeContext = lastEmotion) }
            Log.d("ChatViewModel", "Active context initialized: $lastEmotion")
        }
    }

    /**
     * Loads previous conversation from repository and populates messages.
     *
     * Phase 3: getAllMessages() now returns Flow — use .first() for a one-shot snapshot.
     * Phase 1: history sent for token-budget accounting is capped by buildTokenAwareContext(),
     *          though the full history is passed to GroqRepository on each API call.
     * Phase 4: fetches user profile from last 14 session summaries; stored in
     *          [cachedUserProfile] for injection into every GroqRepository call.
     * Phase 6: no chat session is initialized here — GroqRepository is stateless.
     */
    fun loadPreviousConversation() {
        viewModelScope.launch {
            val chatHistory = chatRepository.getAllMessages(userId).first()
            val messages = chatHistory.map { Message(it.message, it.role) }.distinct()

            // Phase 4: build user profile and derive active context from last summary.
            val summaries = memoryManager.getRecentSummaries()
            cachedUserProfile = memoryManager.buildUserProfile(summaries)
            val activeContext = summaries.firstOrNull()?.dominantEmotion ?: "neutral"

            _uiState.update { it.copy(messages = messages, activeContext = activeContext) }

            // Phase 1: token-aware trim for observability — the UI still shows the full
            // conversation, and the full history is passed to GroqRepository per call.
            val historyForMetrics = buildTokenAwareContext(messages)
            val tokensUsed = historyForMetrics.sumOf { estimateTokens(it.message) }
            val dropped = messages.size - historyForMetrics.size
            _contextTokensUsed.value = tokensUsed
            if (dropped > 0) {
                Log.d("ChatViewModel", "Context trimmed: $dropped messages dropped, $tokensUsed tokens used")
            }

            if (cachedUserProfile.isNotEmpty()) {
                Log.d("ChatViewModel", "User profile cached (${estimateTokens(cachedUserProfile)} tokens): $cachedUserProfile")
            }
        }
    }

    /**
     * Begins a new session and generates a friendly greeting.
     *
     * Phase 3: assigns a fresh session UUID for this conversation.
     * Phase 4: fetches the user profile so it is ready for injection into the
     *          first GroqRepository call of the new session.
     * Phase 6: no chat session is initialised — GroqRepository is stateless.
     */
    fun startNewConversation() {
        viewModelScope.launch {
            sessionId = UUID.randomUUID().toString()
            sessionDominantEmotion = "neutral"  // Phase 4: reset per session

            val userName = FirebaseAuth.getInstance().currentUser?.displayName
            val firstName = userName?.split(" ")?.firstOrNull()

            // Phase 4: build user profile from last 14 summaries for personalised greeting.
            // keyThemes from the most recent summary replaces the old manual context tag.
            val summaries = memoryManager.getRecentSummaries()
            cachedUserProfile = memoryManager.buildUserProfile(summaries)
            val keyThemes = summaries.firstOrNull()?.keyThemes
            val activeContext = summaries.firstOrNull()?.dominantEmotion ?: "neutral"

            val greetingMessage = buildGreeting(firstName, keyThemes, cachedUserProfile, activeContext)

            val botMessage = Message(greetingMessage, "model")
            _uiState.update { it.copy(messages = listOf(botMessage), activeContext = activeContext) }
            storeMessageInDatabase(botMessage)

            if (cachedUserProfile.isNotEmpty()) {
                Log.d("ChatViewModel", "New session — profile cached (${estimateTokens(cachedUserProfile)} tokens)")
            }
        }
    }

    // Phase 4 (updated): manual tag removed — summary is now generated automatically.
    // Triggers summariseAndSave() so the session memory is captured the moment the
    // user taps "End Session", without requiring any text input.
    fun finishConversation() {
        viewModelScope.launch {
            memoryManager.summariseAndSave(
                sessionId       = sessionId,
                messages        = _uiState.value.messages,
                dominantEmotion = sessionDominantEmotion
            )
        }
    }

    // Persists message in Room database.
    // Phase 3: returns the auto-generated row ID so it can be cross-referenced
    // with EmotionLog.messageId.
    private suspend fun storeMessageInDatabase(message: Message): Long {
        return chatRepository.insertMessage(
            ChatMessage(
                message = message.message,
                role = message.role,
                userId = userId
            )
        )
    }

    // ── Message sending ─────────────────────────────────────────────────────────

    /**
     * Sends a message to the Groq API and streams the response back incrementally.
     * Each call is a fully self-contained stateless request — [GroqRepository] assembles
     * the complete message list (system prompt, history, user turn) internally.
     * The [isTyping] indicator is shown while waiting for the first token and cleared
     * as soon as streaming begins; the bot message grows in-place as chunks arrive.
     *
     * Phase 2 additions:
     *  - [requestMutex]: only one call may run at a time; rapid double-taps are dropped.
     *  - Cooldown: enforces a [COOLDOWN_MS] gap between successive API calls.
     *  - Daily cap: blocks API calls once [UserPreferencesRepository.DAILY_MESSAGE_CAP]
     *    messages have been sent today, and surfaces a warm "come back tomorrow" message.
     *
     * Phase 3 additions:
     *  - Classifies emotion on-device via [EmotionClassifier] before contacting the API.
     *  - Persists [EmotionLog] linked to the stored user message row ID.
     *  - Passes a concise emotional-state annotation to [GroqRepository] so the AI
     *    can acknowledge the user's mood naturally.
     *
     * Phase 4 additions:
     *  - Updates [sessionDominantEmotion] so the end-of-session summary captures the
     *    overall emotional tone of the conversation.
     *  - The user profile ([cachedUserProfile]) is passed to [GroqRepository] on every
     *    call so the model has long-term context without synthetic history injections.
     *
     * Phase 6 (Groq migration):
     *  - Replaced Firebase GenerativeModel session streaming with [groqRepository.sendMessageStream].
     *  - Errors are emitted as strings into the same flow (no exceptions for API errors);
     *    rate-limit (429) surfaces a warm boundary message to the user.
     */
    fun sendMessage(messageText: String, role: String = "user") {
        viewModelScope.launch {
            // Phase 2: Drop the call immediately if another send is already in flight.
            if (!requestMutex.tryLock()) return@launch

            try {
                val userMessage = Message(messageText, role)
                _uiState.update { it.copy(messages = it.messages + userMessage) }

                // Phase 3: persist user message and capture its row ID for EmotionLog.
                val messageRowId = storeMessageInDatabase(userMessage)

                if (role == "user") {
                    // ── Phase 2: Daily message cap ────────────────────────────────────
                    val dailyCount = userPreferencesRepository.getDailyMessageCount()
                    if (dailyCount >= UserPreferencesRepository.DAILY_MESSAGE_CAP) {
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages + Message(
                                    "You have had a full session today. Come back tomorrow.",
                                    "model"
                                )
                            )
                        }
                        return@launch
                    }

                    // ── Phase 3: On-device emotion classification ─────────────────────
                    // classify() is a suspend fun running on Dispatchers.Default;
                    // it is guaranteed to complete in < 300 ms (TFLite or keyword fallback).
                    val emotionResult = emotionClassifier.classify(messageText)
                    val emotionCategory = emotionResult.primary

                    // Phase 4: track the most significant emotion seen this session.
                    // Simple heuristic: replace neutral with any classified emotion, or
                    // replace with a new emotion if the current one is only neutral.
                    if (emotionCategory != EmotionCategory.NEUTRAL ||
                        sessionDominantEmotion == "neutral"
                    ) {
                        sessionDominantEmotion = emotionCategory.key
                    }

                    // Update UI gradient immediately — target: within 300 ms of send.
                    _uiState.update {
                        it.copy(
                            activeContext = emotionCategory.key,
                            currentEmotion = emotionResult,
                            isTyping = true
                        )
                    }

                    // Persist the emotion log linked to the stored message row.
                    persistEmotionLog(emotionResult, messageRowId)

                    Log.d(
                        "ChatViewModel",
                        "Emotion: ${emotionCategory.displayName} " +
                            "(${emotionResult.intensity.displayName}) " +
                            "conf=${"%.2f".format(emotionResult.confidence)}"
                    )

                    // ── Phase 2: Cooldown enforcement ─────────────────────────────────
                    val now = System.currentTimeMillis()
                    val elapsed = now - lastRequestMs
                    if (elapsed < COOLDOWN_MS) {
                        delay(COOLDOWN_MS - elapsed)
                    }

                    // Phase 2: Record that the API call is starting, then count this message.
                    lastRequestMs = System.currentTimeMillis()
                    userPreferencesRepository.incrementDailyMessageCount()

                    // ── Phase 6: Stream via GroqRepository ────────────────────────────
                    // History excludes the very last message (the user turn we're about
                    // to send, already appended to uiState above).
                    val emotionAnnotation = buildEmotionAnnotation(emotionResult)

                    var streamedText = ""
                    var botMessageAdded = false
                    var errorEmitted = false

                    try {
                        groqRepository.sendMessageStream(
                            systemPrompt   = getSystemInstructions(),
                            history        = _uiState.value.messages.dropLast(1),
                            userMessage    = messageText,
                            userProfile    = cachedUserProfile,
                            emotionContext = emotionAnnotation
                        ).collect { token ->

                            // ── Error-sentinel detection ──────────────────────────────
                            // GroqRepositoryImpl emits human-readable error strings into
                            // the same flow rather than throwing exceptions, so we check
                            // for known constants before treating a token as content.
                            if (token == GroqRepositoryImpl.ERROR_RATE_LIMITED ||
                                token == GroqRepositoryImpl.ERROR_UNAUTHORIZED ||
                                token == GroqRepositoryImpl.ERROR_SERVER ||
                                token.startsWith(GroqRepositoryImpl.ERROR_API_FAILURE) ||
                                token == GroqRepositoryImpl.ERROR_NETWORK
                            ) {
                                Log.e("ChatViewModel", "Groq error received: $token")
                                errorEmitted = true
                                _uiState.update { state ->
                                    state.copy(
                                        isTyping = false,
                                        messages = state.messages + Message(token, "model")
                                    )
                                }
                                return@collect   // flow completes naturally after one error emission
                            }

                            // ── Normal token — grow the bot message in-place ──────────
                            streamedText += token
                            val displayText = sanitizeResponse(streamedText)
                            if (!botMessageAdded) {
                                // First real token — hide typing indicator and insert bot message
                                _uiState.update { state ->
                                    state.copy(
                                        isTyping = false,
                                        messages = state.messages + Message(displayText, "model")
                                    )
                                }
                                botMessageAdded = true
                            } else {
                                // Subsequent tokens — grow the last message in place
                                _uiState.update { state ->
                                    val updated = state.messages.toMutableList()
                                    updated[updated.lastIndex] = Message(displayText, "model")
                                    state.copy(messages = updated)
                                }
                            }
                        }

                        // Stream finished with no usable content (all chunks were empty/filtered)
                        // errorEmitted guard prevents a second message when an error sentinel was already shown.
                        if (!botMessageAdded && !errorEmitted) {
                            Log.w("ChatViewModel", "Stream completed but produced no text")
                            _uiState.update { state ->
                                state.copy(
                                    isTyping = false,
                                    messages = state.messages + Message(
                                        "I wasn't able to generate a response. Please try again.",
                                        "model"
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Streaming error: ${e.javaClass.simpleName}: ${e.message}", e)
                        _uiState.update { state ->
                            state.copy(
                                isTyping = false,
                                messages = state.messages + Message(
                                    "Error: ${e.message ?: "Something went wrong. Please try again."}",
                                    "model"
                                )
                            )
                        }
                    } finally {
                        _uiState.update { it.copy(isTyping = false) }
                        if (streamedText.isNotEmpty()) {
                            storeMessageInDatabase(Message(sanitizeResponse(streamedText), "model"))
                        }
                    }
                }
            } finally {
                requestMutex.unlock()
            }
        }
    }

    // ── Emotion helpers ─────────────────────────────────────────────────────────

    /**
     * Persists an [EmotionLog] entry in Room on the IO dispatcher.
     * Fire-and-forget; errors are logged but do not disrupt the send flow.
     */
    private fun persistEmotionLog(result: EmotionResult, messageRowId: Long) {
        viewModelScope.launch {
            runCatching {
                emotionDao.insertEmotionLog(
                    EmotionLog(
                        sessionId  = sessionId,
                        messageId  = messageRowId,
                        emotion    = result.primary.key,
                        intensity  = result.intensity.displayName,
                        confidence = result.confidence
                    )
                )
            }.onFailure {
                Log.e("ChatViewModel", "Failed to persist EmotionLog: ${it.message}", it)
            }
        }
    }

    /**
     * Phase 3.5: Builds the hidden emotional-context tag passed to [GroqRepository].
     *
     * Format matches the exact syntax documented in ## EMOTIONAL CONTEXT inside
     * [getSystemInstructions], so the model parses it consistently:
     *
     *   "\n[Context: Emotion=ANXIOUS, Intensity=HIGH]\n"
     *
     * Design decisions:
     * - Enum `.name` (uppercase) is used deliberately — the system prompt uses
     *   the same casing, so the model reliably maps ANXIOUS→anxious tone, etc.
     * - Leading/trailing newlines isolate the tag from the user's text so the
     *   model does not conflate it with the message content.
     * - Neutral messages with confidence below threshold are unannotated to avoid
     *   signalling false emotional state on short / ambiguous inputs.
     */
    private fun buildEmotionAnnotation(result: EmotionResult): String {
        if (result.primary == EmotionCategory.NEUTRAL &&
            result.confidence < EmotionClassifier.CONFIDENCE_THRESHOLD
        ) {
            return ""   // No annotation for genuinely ambiguous messages
        }
        return "\n[Context: Emotion=${result.primary.name}, Intensity=${result.intensity.name}]\n"
    }

    /**
     * Strips any leaked metadata tags from the model's response before it is
     * displayed or persisted. The system prompt instructs the model to keep
     * [Context: …] and [Detected emotional context: …] tags invisible, but
     * some LLM responses echo them back anyway. This is the safety net.
     */
    private fun sanitizeResponse(text: String): String =
        text
            .replace(Regex("""\[Context:\s*Emotion=[^\]]*]"""), "")
            .replace(Regex("""\[Detected emotional context:[^\]]*]"""), "")
            .trim()

    // ── History management ──────────────────────────────────────────────────────

    /**
     * Clears message list and history in the database, and resets the cached user
     * profile so the next conversation starts from a clean context.
     *
     * Phase 4: also wipes all [SessionSummary] entries to honour the full privacy
     * guarantee: after "Clear History" no personal memory logs or session summaries
     * remain on-device.
     */
    fun clearChatHistory() {
        viewModelScope.launch {
            chatRepository.clearChatHistory(userId)
            emotionDao.clearLogsForSession(sessionId)
            // Phase 4: wipe long-term memory summaries
            memoryManager.clearAllSummaries()
            cachedUserProfile = ""
            _uiState.update {
                it.copy(messages = emptyList(), activeContext = "", currentEmotion = null)
            }
            Log.d("ChatViewModel", "Full history and memory cleared for user $userId")
        }
    }

    // ── Gradient helpers ────────────────────────────────────────────────────────

    /**
     * Returns a background gradient list based on current mood/context.
     *
     * Phase 3: uses [getEmotionGradient] for intensity-aware colour selection when
     * a classified [EmotionResult] is available; falls back to the legacy string-keyed
     * [gradientMap] otherwise.
     */
    fun determineGradientColors(): List<Color> {
        val emotion = _uiState.value.currentEmotion
        return if (emotion != null) {
            getEmotionGradient(emotion.primary, emotion.intensity)
        } else {
            com.iamashad.meraki.utils.gradientMap[_uiState.value.activeContext]
                ?: com.iamashad.meraki.utils.gradientMap["neutral"]!!
        }
    }

    // ── Private helpers ─────────────────────────────────────────────────────────

    /**
     * Phase 4 (updated): builds a personalised greeting using auto-generated
     * [SessionSummary.keyThemes] instead of the old manual context tag, so the
     * opening line reflects the real topics from the previous session.
     *
     * Mood-Aware UI: the dominant emotion from the last session now steers the
     * greeting's emotional register. Negative emotions (anxious, sad, stressed,
     * angry) get a validation-led opening that meets the user where they are.
     * Positive emotions (calm, happy) stay light and curious. This means the
     * AI's very first line already feels attuned rather than generic.
     */
    private fun buildGreeting(
        firstName: String?,
        keyThemes: String?,
        userProfile: String,
        lastDominantEmotion: String = "neutral"
    ): String {
        val name = firstName ?: "there"

        val isNegative = lastDominantEmotion in setOf("sad", "anxious", "stressed", "angry")
        val isPositive = lastDominantEmotion in setOf("happy", "calm")

        return when {
            // Validation-led openings for users returning after a difficult session
            isNegative && userProfile.isNotEmpty() && !keyThemes.isNullOrBlank() ->
                "Welcome back, $name. I hope things have felt a little lighter since we talked about $keyThemes — how are you doing today?"
            isNegative && userProfile.isNotEmpty() ->
                "Welcome back, $name. I hope you've been able to take care of yourself — how are you holding up today?"
            isNegative ->
                "Hi $name, it's good to see you. You don't have to have it all figured out — how are you feeling right now?"

            // Energetic, curious openings for users in a positive state
            isPositive && !keyThemes.isNullOrBlank() ->
                "Welcome back, $name! Things seemed to be going well last time — how are you keeping up with $keyThemes?"
            isPositive ->
                "Welcome back, $name! Great to see you — what's on your mind today?"

            // Standard continuity-aware openings (neutral / no prior data)
            userProfile.isNotEmpty() && !keyThemes.isNullOrBlank() ->
                "Welcome back, $name. Last time we touched on $keyThemes — how are things feeling today?"
            userProfile.isNotEmpty() ->
                "Welcome back, $name. How are you feeling today?"
            !keyThemes.isNullOrBlank() ->
                "Hi $name! Last time we talked about $keyThemes. How are you doing now?"
            else ->
                "Hello $name! How can I help you today?"
        }
    }
}
