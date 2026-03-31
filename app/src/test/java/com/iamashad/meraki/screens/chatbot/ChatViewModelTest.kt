package com.iamashad.meraki.screens.chatbot

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.data.ChatMessage
import com.iamashad.meraki.data.EmotionDao
import com.iamashad.meraki.data.SessionSummary
import com.iamashad.meraki.model.EmotionCategory
import com.iamashad.meraki.model.EmotionIntensity
import com.iamashad.meraki.model.EmotionResult
import com.iamashad.meraki.model.Message
import com.iamashad.meraki.repository.ChatRepository
import com.iamashad.meraki.repository.GroqRepository
import com.iamashad.meraki.repository.UserPreferencesRepository
import com.iamashad.meraki.rules.MainDispatcherRule
import com.iamashad.meraki.utils.EmotionClassifier
import com.iamashad.meraki.utils.MemoryManager
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [ChatViewModel] verifying UDF state transitions via Turbine.
 *
 * Phase 6 (Groq migration): Updated to use [GroqRepository] mock instead of the
 * removed Firebase [GenerativeModel].  [sendMessageStream] is stubbed to return a
 * simple [flowOf] so state transitions can be observed synchronously under
 * [UnconfinedTestDispatcher].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val chatRepository: ChatRepository = mockk(relaxed = true)
    private val groqRepository: GroqRepository = mockk(relaxed = true)
    private val userPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true)
    private val emotionClassifier: EmotionClassifier = mockk(relaxed = true)
    private val emotionDao: EmotionDao = mockk(relaxed = true)
    private val memoryManager: MemoryManager = mockk(relaxed = true)

    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        // Intercept the static FirebaseAuth.getInstance() call made in the
        // ViewModel's property initializer before any test can construct it.
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns mockk(relaxed = true) {
            every { currentUser } returns null
        }

        every { chatRepository.getAllMessages(any()) } returns flowOf(emptyList())
        coJustRun { chatRepository.clearChatHistory(any()) }
        coEvery { chatRepository.insertMessage(any()) } returns 1L

        // Default: daily cap not reached
        coEvery { userPreferencesRepository.getDailyMessageCount() } returns 0
        coJustRun { userPreferencesRepository.incrementDailyMessageCount() }

        // Default: emotion classifier returns NEUTRAL
        coEvery { emotionClassifier.classify(any()) } returns EmotionResult(
            primary    = EmotionCategory.NEUTRAL,
            intensity  = EmotionIntensity.LOW,
            confidence = 0.80f
        )
        coEvery { emotionDao.insertEmotionLog(any()) } returns 1L
        coJustRun { emotionDao.clearLogsForSession(any()) }

        // Default: no prior summaries
        coEvery { memoryManager.getRecentSummaries() } returns emptyList()
        coJustRun { memoryManager.summariseAndSave(any(), any(), any()) }

        // Default: Groq streaming returns a single token
        every {
            groqRepository.sendMessageStream(any(), any(), any(), any(), any())
        } returns flowOf("I'm here to help.")

        viewModel = ChatViewModel(
            chatRepository,
            groqRepository,
            userPreferencesRepository,
            emotionClassifier,
            emotionDao,
            memoryManager
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ── initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial uiState has empty messages and isTyping false`() = runTest {
        val state = viewModel.uiState.value
        assertThat(state.messages).isEmpty()
        assertThat(state.isTyping).isFalse()
        assertThat(state.isLoading).isFalse()
        assertThat(state.activeContext).isEqualTo("neutral")
    }

    // ── sendMessage — full UDF cycle ──────────────────────────────────────────

    @Test
    fun `sendMessage - appends user message then appends bot message after stream`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.uiState.test {
                assertThat(awaitItem().messages).isEmpty()

                viewModel.sendMessage("I feel anxious today", "user")
                advanceUntilIdle()

                // Drain until we find a state that has both the user and bot messages
                var latest = awaitItem()
                while (latest.messages.none { it.role == "model" }) {
                    latest = awaitItem()
                }

                assertThat(latest.messages.any {
                    it.message == "I feel anxious today" && it.role == "user"
                }).isTrue()
                assertThat(latest.messages.any { it.role == "model" }).isTrue()
                assertThat(latest.isTyping).isFalse()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `sendMessage - activeContext reflects emotion of message`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { emotionClassifier.classify(any()) } returns EmotionResult(
                primary    = EmotionCategory.HAPPY,
                intensity  = EmotionIntensity.MEDIUM,
                confidence = 0.80f
            )

            viewModel.sendMessage("I feel joyful and happy today", "user")
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.activeContext).isEqualTo("happy")
        }

    @Test
    fun `sendMessage - non-user role skips API call and context update`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.sendMessage("Bot greeting message", "model")

            val state = viewModel.uiState.value
            assertThat(state.messages).hasSize(1)
            assertThat(state.messages.first().role).isEqualTo("model")
            assertThat(state.isTyping).isFalse()
        }

    @Test
    fun `sendMessage - accumulates messages across multiple calls`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.sendMessage("Message 1", "user")
            advanceUntilIdle()

            viewModel.sendMessage("Message 2", "user")
            advanceUntilIdle()

            // 1 user + 1 bot from first call, 1 user + 1 bot from second
            assertThat(viewModel.uiState.value.messages).hasSize(4)
        }

    // ── clearChatHistory ──────────────────────────────────────────────────────

    @Test
    fun `clearChatHistory - empties messages list and resets activeContext`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.sendMessage("Hello", "user")
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.messages).isNotEmpty()

            viewModel.clearChatHistory()

            val state = viewModel.uiState.value
            assertThat(state.messages).isEmpty()
            assertThat(state.activeContext).isEmpty()
        }

    @Test
    fun `clearChatHistory - delegates to repository`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.clearChatHistory()
            io.mockk.coVerify { chatRepository.clearChatHistory(any()) }
        }

    // ── initializeContext ─────────────────────────────────────────────────────

    @Test
    fun `initializeContext - sets activeContext from last summary dominantEmotion`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val summary = SessionSummary(
                sessionId = "s1", dominantEmotion = "stressed",
                keyThemes = "work", helperPattern = "Reassurance",
                summaryText = "", tokenCount = 10
            )
            coEvery { memoryManager.getRecentSummaries() } returns listOf(summary)

            viewModel.initializeContext("user1")
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.activeContext).isEqualTo("stressed")
        }

    @Test
    fun `initializeContext - defaults to neutral when no summaries exist`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { memoryManager.getRecentSummaries() } returns emptyList()

            viewModel.initializeContext("user1")
            advanceUntilIdle()

            assertThat(viewModel.uiState.value.activeContext).isEqualTo("neutral")
        }

    // ── loadPreviousConversation ──────────────────────────────────────────────

    @Test
    fun `loadPreviousConversation - populates messages and activeContext from last summary`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val saved = listOf(
                ChatMessage(id = 1, message = "Hey", role = "user", userId = "u1"),
                ChatMessage(id = 2, message = "Hello!", role = "model", userId = "u1")
            )
            every { chatRepository.getAllMessages(any()) } returns flowOf(saved)
            val summary = SessionSummary(
                sessionId = "s1", dominantEmotion = "calm",
                keyThemes = "work", helperPattern = "Reassurance",
                summaryText = "", tokenCount = 10
            )
            coEvery { memoryManager.getRecentSummaries() } returns listOf(summary)

            viewModel.loadPreviousConversation()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state.messages).hasSize(2)
            assertThat(state.messages.first().message).isEqualTo("Hey")
            assertThat(state.activeContext).isEqualTo("calm")
        }

    @Test
    fun `loadPreviousConversation - deduplicates messages with same content`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val saved = listOf(
                ChatMessage(id = 1, message = "Dup", role = "user", userId = "u1"),
                ChatMessage(id = 1, message = "Dup", role = "user", userId = "u1")
            )
            every { chatRepository.getAllMessages(any()) } returns flowOf(saved)

            viewModel.loadPreviousConversation()
            advanceUntilIdle()

            // distinct() on Message(message, role) collapses duplicates
            assertThat(viewModel.uiState.value.messages).hasSize(1)
        }

    // ── finishConversation ───────────────────────────────────────────────────

    @Test
    fun `finishConversation - triggers summariseAndSave with current session data`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.sendMessage("I feel sad", "model") // model role, no API call

            viewModel.finishConversation()
            advanceUntilIdle()

            io.mockk.coVerify { memoryManager.summariseAndSave(any(), any(), any()) }
        }

    // ── determineGradientColors ───────────────────────────────────────────────

    @Test
    fun `determineGradientColors - returns non-empty list for any context`() {
        val colors = viewModel.determineGradientColors()
        assertThat(colors).isNotEmpty()
    }
}
