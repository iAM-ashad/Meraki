package com.iamashad.meraki.screens.chatbot

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.data.ChatMessage
import com.iamashad.meraki.model.Message
import com.iamashad.meraki.repository.ChatRepository
import com.iamashad.meraki.rules.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [ChatViewModel] verifying UDF state transitions via Turbine.
 *
 * Key design decisions:
 * - [FirebaseAuth.getInstance] is a static call in the ViewModel constructor;
 *   [mockkStatic] is used to intercept it before the VM is created.
 * - A [CompletableDeferred] stands in for the Gemini API call so that
 *   intermediate states (`isTyping = true`) can be captured before the
 *   response resolves.
 * - [UnconfinedTestDispatcher] (via [MainDispatcherRule]) runs `viewModelScope`
 *   coroutines eagerly, so state mutations happen synchronously up to the
 *   first real suspension point (the deferred).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val chatRepository: ChatRepository = mockk(relaxed = true)
    private val generativeModel: GenerativeModel = mockk(relaxed = true)
    private val mockChat: Chat = mockk(relaxed = true)

    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        // Intercept the static FirebaseAuth.getInstance() call made in the
        // ViewModel's property initializer before any test can construct it.
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns mockk(relaxed = true) {
            every { currentUser } returns null
        }

        // Default: getAllMessages returns empty flow so loadPreviousConversation is safe
        every { chatRepository.getAllMessages(any()) } returns flowOf(emptyList())
        coJustRun { chatRepository.insertMessage(any()) }
        coJustRun { chatRepository.clearChatHistory(any()) }

        viewModel = ChatViewModel(chatRepository, generativeModel)
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

    // ── sendMessage — full UDF cycle with intermediate state capture ──────────

    @Test
    fun `sendMessage - appends user message then sets isTyping true then appends bot message`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Use CompletableDeferred so we can observe isTyping=true before
            // the API call completes.
            val apiDeferred = CompletableDeferred<GenerateContentResponse>()
            val mockResponse = mockk<GenerateContentResponse> {
                every { text } returns "I'm here to help."
            }

            every { generativeModel.startChat(any()) } returns mockChat
            coEvery { mockChat.sendMessage(any<String>()) } coAnswers { apiDeferred.await() }

            viewModel.uiState.test {
                // Consume the initial emission.
                assertThat(awaitItem().messages).isEmpty()

                viewModel.sendMessage("I feel anxious today", "user")

                // With UnconfinedTestDispatcher the two fast _uiState.update calls
                // (append user message, set isTyping=true) may be conflated by StateFlow
                // before Turbine's collector runs. Drain items until we see isTyping=true,
                // which confirms both updates have been processed.
                var typingItem = awaitItem()
                while (!typingItem.isTyping) typingItem = awaitItem()

                // By the time isTyping is true the user message must already be present.
                assertThat(typingItem.messages.any { it.message == "I feel anxious today" && it.role == "user" })
                    .isTrue()
                assertThat(typingItem.activeContext).isEqualTo("anxious")

                // Resolve the deferred — unblocks the suspended sendMessage coroutine.
                apiDeferred.complete(mockResponse)

                // Final update: bot message appended, isTyping=false.
                val afterBot = awaitItem()
                assertThat(afterBot.isTyping).isFalse()
                assertThat(afterBot.messages).hasSize(2)
                assertThat(afterBot.messages.last().message).isEqualTo("I'm here to help.")
                assertThat(afterBot.messages.last().role).isEqualTo("model")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `sendMessage - activeContext reflects emotion of message`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { generativeModel.startChat(any()) } returns mockChat
            coEvery { mockChat.sendMessage(any<String>()) } returns mockk(relaxed = true) {
                every { text } returns "Keep going!"
            }

            viewModel.sendMessage("I feel joyful and happy today", "user")

            // After the coroutine runs to completion (UnconfinedTestDispatcher)
            assertThat(viewModel.uiState.value.activeContext).isEqualTo("happy")
        }

    @Test
    fun `sendMessage - non-user role skips API call and context update`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.sendMessage("Bot greeting message", "model")

            val state = viewModel.uiState.value
            assertThat(state.messages).hasSize(1)
            assertThat(state.messages.first().role).isEqualTo("model")
            // isTyping should not have been set for model role
            assertThat(state.isTyping).isFalse()
        }

    @Test
    fun `sendMessage - accumulates messages across multiple calls`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { generativeModel.startChat(any()) } returns mockChat
            coEvery { mockChat.sendMessage(any<String>()) } returns mockk(relaxed = true) {
                every { text } returns "Response"
            }

            viewModel.sendMessage("Message 1", "user")
            viewModel.sendMessage("Message 2", "user")

            // 1 user + 1 bot from first call, 1 user + 1 bot from second
            assertThat(viewModel.uiState.value.messages).hasSize(4)
        }

    // ── clearChatHistory ──────────────────────────────────────────────────────

    @Test
    fun `clearChatHistory - empties messages list and resets activeContext`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Seed the state with messages by directly testing state after clear
            every { generativeModel.startChat(any()) } returns mockChat
            coEvery { mockChat.sendMessage(any<String>()) } returns mockk(relaxed = true) {
                every { text } returns "Hi"
            }

            viewModel.sendMessage("Hello", "user")
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
    fun `initializeContext - sets activeContext from last saved context`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { chatRepository.getLastContext(any()) } returns "stressed"
            every { chatRepository.getAllMessages(any()) } returns flowOf(emptyList())

            viewModel.initializeContext("user1")

            assertThat(viewModel.uiState.value.activeContext).isEqualTo("stressed")
        }

    @Test
    fun `initializeContext - defaults to neutral when no prior context`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { chatRepository.getLastContext(any()) } returns null
            every { chatRepository.getAllMessages(any()) } returns flowOf(emptyList())

            viewModel.initializeContext("user1")

            assertThat(viewModel.uiState.value.activeContext).isEqualTo("neutral")
        }

    // ── loadPreviousConversation ──────────────────────────────────────────────

    @Test
    fun `loadPreviousConversation - populates messages from repository`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val saved = listOf(
                ChatMessage(id = 1, message = "Hey", role = "user", userId = "u1"),
                ChatMessage(id = 2, message = "Hello!", role = "model", userId = "u1")
            )
            every { chatRepository.getAllMessages(any()) } returns flowOf(saved)
            coEvery { chatRepository.getLastContext(any()) } returns "calm"

            viewModel.loadPreviousConversation()

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
            coEvery { chatRepository.getLastContext(any()) } returns null

            viewModel.loadPreviousConversation()

            // distinct() on Message(message, role) collapses duplicates
            assertThat(viewModel.uiState.value.messages).hasSize(1)
        }

    // ── finishConversation ───────────────────────────────────────────────────

    @Test
    fun `finishConversation - updates activeContext to provided tag`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Add a message first so lastOrNull has something
            viewModel.sendMessage("I feel sad", "model") // model role, no API call

            viewModel.finishConversation("sad")

            assertThat(viewModel.uiState.value.activeContext).isEqualTo("sad")
        }

    // ── determineGradientColors ───────────────────────────────────────────────

    @Test
    fun `determineGradientColors - returns non-empty list for any context`() {
        val colors = viewModel.determineGradientColors()
        assertThat(colors).isNotEmpty()
    }
}
