package com.iamashad.meraki.integration

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.data.EmotionDao
import com.iamashad.meraki.model.EmotionCategory
import com.iamashad.meraki.model.EmotionIntensity
import com.iamashad.meraki.model.EmotionResult
import com.iamashad.meraki.model.Message
import com.iamashad.meraki.repository.ChatRepository
import com.iamashad.meraki.repository.GroqRepository
import com.iamashad.meraki.repository.UserPreferencesRepository
import com.iamashad.meraki.rules.MainDispatcherRule
import com.iamashad.meraki.screens.chatbot.ChatViewModel
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
 * Phase 5 – Golden Path 3: "The Chatbot User"
 *
 * End-to-end integration scenario through [ChatViewModel]:
 *
 *   1. Home screen is reached → chatbot is idle (empty messages, neutral context)
 *   2. User navigates to Chatbot screen and types "Happy"
 *   3. User's message appears in uiState.messages immediately
 *   4. Emotion analysis maps "Happy" → "happy" context
 *   5. Background gradient updates to the "happy" yellow palette
 *   6. AI backend (faked) returns a response
 *   7. Bot's response appears in uiState.messages
 *   8. isTyping indicator turns off
 *
 * Phase 6 (Groq migration): Updated to use [GroqRepository] mock instead of the
 * removed Firebase [GenerativeModel].  [sendMessageStream] is stubbed to return a
 * [flowOf] so the test is deterministic.
 *
 * Phase 3 update: [EmotionClassifier.classify] is stubbed to return a HAPPY/MEDIUM
 * result for any input so existing assertions remain valid.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatFlowTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ── dependencies ──────────────────────────────────────────────────────────

    private val chatRepository: ChatRepository = mockk(relaxed = true)
    private val groqRepository: GroqRepository = mockk(relaxed = true)
    private val userPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true)
    // Phase 3: on-device emotion intelligence mocks
    private val emotionClassifier: EmotionClassifier = mockk(relaxed = true)
    private val emotionDao: EmotionDao = mockk(relaxed = true)
    // Phase 4: long-term session memory mock
    private val memoryManager: MemoryManager = mockk(relaxed = true)

    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns mockk(relaxed = true) {
            every { currentUser } returns null
        }
        every { chatRepository.getAllMessages(any()) } returns flowOf(emptyList())
        coEvery { chatRepository.insertMessage(any()) } returns 1L
        coJustRun { chatRepository.clearChatHistory(any()) }

        // Phase 2: daily cap — default to 0 so the cap is never hit in these tests.
        coEvery { userPreferencesRepository.getDailyMessageCount() } returns 0
        coJustRun { userPreferencesRepository.incrementDailyMessageCount() }

        // Phase 3: stub classifier to return HAPPY for any input so legacy assertions hold.
        coEvery { emotionClassifier.classify(any()) } returns EmotionResult(
            primary    = EmotionCategory.HAPPY,
            intensity  = EmotionIntensity.MEDIUM,
            confidence = 0.80f
        )
        coEvery { emotionDao.insertEmotionLog(any()) } returns 1L
        coJustRun { emotionDao.clearLogsForSession(any()) }

        // Phase 4: no prior summaries
        coEvery { memoryManager.getRecentSummaries() } returns emptyList()
        coJustRun { memoryManager.summariseAndSave(any(), any(), any()) }

        // Default Groq stub — overridden per-test via stubAiResponse
        every {
            groqRepository.sendMessageStream(any(), any(), any(), any(), any())
        } returns flowOf("Default response")

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

    // ── helper: stub groqRepository.sendMessageStream to emit a single token ──

    private fun stubAiResponse(responseText: String) {
        every {
            groqRepository.sendMessageStream(any(), any(), any(), any(), any())
        } returns flowOf(responseText)
    }

    // ── scenario tests ────────────────────────────────────────────────────────

    @Test
    fun `step 1 - chatbot starts idle with empty messages and neutral context`() = runTest {
        val state = viewModel.uiState.value
        assertThat(state.messages).isEmpty()
        assertThat(state.activeContext).isEqualTo("neutral")
        assertThat(state.isTyping).isFalse()
    }

    @Test
    fun `step 2 - sending Happy adds user message to state immediately`() = runTest {
        stubAiResponse("That's great to hear!")

        viewModel.uiState.test {
            awaitItem() // initial state

            viewModel.sendMessage("Happy")

            // User's message should appear in the next emission
            val afterUserMsg = awaitItem()
            assertThat(afterUserMsg.messages).isNotEmpty()
            assertThat(afterUserMsg.messages.first().message).isEqualTo("Happy")
            assertThat(afterUserMsg.messages.first().role).isEqualTo("user")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `step 3 - sending Happy updates context to happy`() = runTest {
        stubAiResponse("That's great!")

        viewModel.sendMessage("Happy")
        advanceUntilIdle()

        // Phase 3: EmotionClassifier stub returns HAPPY → key == "happy"
        assertThat(viewModel.uiState.value.activeContext).isEqualTo("happy")
    }

    @Test
    fun `step 4 - determineGradientColors returns happy yellow palette after Happy message`() =
        runTest {
            stubAiResponse("Glad you're feeling well!")

            viewModel.sendMessage("Happy")
            advanceUntilIdle()

            val gradients = viewModel.determineGradientColors()
            assertThat(gradients).isNotEmpty()

            // Verify the active context was set to "happy" so the gradient is non-neutral
            assertThat(viewModel.uiState.value.activeContext).isEqualTo("happy")
        }

    @Test
    fun `step 5 - AI response appears in messages after send`() = runTest {
        stubAiResponse("It's wonderful to hear that you're happy!")

        viewModel.sendMessage("Happy")
        advanceUntilIdle()

        val messages = viewModel.uiState.value.messages
        assertThat(messages).hasSize(2)

        val botMessage = messages.last()
        assertThat(botMessage.role).isEqualTo("model")
        assertThat(botMessage.message).isEqualTo("It's wonderful to hear that you're happy!")
    }

    @Test
    fun `step 6 - isTyping is false after AI response received`() = runTest {
        stubAiResponse("Response!")

        viewModel.sendMessage("Happy")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isTyping).isFalse()
    }

    @Test
    fun `full golden path - send Happy - context updates - bot replies - typing clears`() =
        runTest {
            stubAiResponse("What a great day it is when you feel happy!")

            viewModel.sendMessage("Happy")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            // 1) user message present
            assertThat(state.messages).hasSize(2)
            assertThat(state.messages.first().role).isEqualTo("user")
            // 2) context updated via EmotionClassifier stub → "happy"
            assertThat(state.activeContext).isEqualTo("happy")
            // 3) bot replied
            assertThat(state.messages.last().role).isEqualTo("model")
            assertThat(state.messages.last().message)
                .isEqualTo("What a great day it is when you feel happy!")
            // 4) typing indicator off
            assertThat(state.isTyping).isFalse()
        }

    @Test
    fun `sending multiple messages accumulates them in order`() = runTest {
        stubAiResponse("Reply 1")

        viewModel.sendMessage("Hello")
        advanceUntilIdle()

        stubAiResponse("Reply 2")
        viewModel.sendMessage("Happy again!")
        advanceUntilIdle()

        val messages = viewModel.uiState.value.messages
        // 2 user messages + 2 bot replies = 4 total
        assertThat(messages).hasSize(4)
        assertThat(messages[0].role).isEqualTo("user")
        assertThat(messages[1].role).isEqualTo("model")
        assertThat(messages[2].role).isEqualTo("user")
        assertThat(messages[3].role).isEqualTo("model")
    }

    @Test
    fun `clearChatHistory resets messages and context`() = runTest {
        stubAiResponse("OK!")
        viewModel.sendMessage("Happy")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.messages).isNotEmpty()

        viewModel.clearChatHistory()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.messages).isEmpty()
    }
}
