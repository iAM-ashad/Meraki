package com.iamashad.meraki.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.iamashad.meraki.data.ChatDao
import com.iamashad.meraki.data.ChatMessage
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ChatRepository] with a mocked [ChatDao].
 *
 * Uses [StandardTestDispatcher] so that coroutine execution is
 * deterministic and controlled by [runTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryTest {

    private lateinit var chatDao: ChatDao
    private lateinit var repository: ChatRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        chatDao = mockk(relaxed = true)
        repository = ChatRepository(chatDao, testDispatcher)
    }

    // ── insertMessage ─────────────────────────────────────────────────────────

    @Test
    fun `insertMessage - delegates to chatDao insertMessage`() = runTest(testDispatcher) {
        val msg = ChatMessage(message = "Hi", role = "user", userId = "u1")
        coJustRun { chatDao.insertMessage(msg) }

        repository.insertMessage(msg)

        coVerify(exactly = 1) { chatDao.insertMessage(msg) }
    }

    @Test
    fun `insertMessage - passes message with context to dao`() = runTest(testDispatcher) {
        val msg = ChatMessage(message = "Tell me more", role = "user", context = "anxiety", userId = "u1")
        coJustRun { chatDao.insertMessage(msg) }

        repository.insertMessage(msg)

        coVerify { chatDao.insertMessage(match { it.context == "anxiety" }) }
    }

    // ── clearChatHistory ──────────────────────────────────────────────────────

    @Test
    fun `clearChatHistory - delegates to chatDao clearChatHistory with correct userId`() =
        runTest(testDispatcher) {
            coJustRun { chatDao.clearChatHistory("u1") }

            repository.clearChatHistory("u1")

            coVerify(exactly = 1) { chatDao.clearChatHistory("u1") }
        }

    @Test
    fun `clearChatHistory - uses the exact userId provided`() = runTest(testDispatcher) {
        coJustRun { chatDao.clearChatHistory(any()) }

        repository.clearChatHistory("specificUser123")

        coVerify { chatDao.clearChatHistory("specificUser123") }
    }

    // ── getAllMessages ─────────────────────────────────────────────────────────

    @Test
    fun `getAllMessages - emits list from dao flow`() = runTest(testDispatcher) {
        val messages = listOf(
            ChatMessage(id = 1, message = "Hello", role = "user", userId = "u1"),
            ChatMessage(id = 2, message = "Hi there", role = "assistant", userId = "u1")
        )
        every { chatDao.getAllMessagesFlow("u1") } returns flowOf(messages)

        repository.getAllMessages("u1").test {
            val result = awaitItem()
            assertThat(result).hasSize(2)
            assertThat(result.first().message).isEqualTo("Hello")
            assertThat(result.last().message).isEqualTo("Hi there")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllMessages - emits empty list when dao returns empty flow`() = runTest(testDispatcher) {
        every { chatDao.getAllMessagesFlow("u1") } returns flowOf(emptyList())

        repository.getAllMessages("u1").test {
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllMessages - deduplicates messages by id`() = runTest(testDispatcher) {
        // DAO returns two entries with the same id (duplicates can arise from DISTINCT + index edge cases)
        val duplicates = listOf(
            ChatMessage(id = 1, message = "Dup", role = "user", userId = "u1"),
            ChatMessage(id = 1, message = "Dup", role = "user", userId = "u1")
        )
        every { chatDao.getAllMessagesFlow("u1") } returns flowOf(duplicates)

        repository.getAllMessages("u1").test {
            val result = awaitItem()
            // distinctBy { it.id } in the repository should collapse duplicates
            assertThat(result).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllMessages - passes correct userId to dao`() = runTest(testDispatcher) {
        every { chatDao.getAllMessagesFlow("user42") } returns flowOf(emptyList())

        repository.getAllMessages("user42").test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        // Verify the DAO was called with the exact userId
        io.mockk.verify { chatDao.getAllMessagesFlow("user42") }
    }

    // ── getLastContext ────────────────────────────────────────────────────────

    @Test
    fun `getLastContext - returns context of last message that has non-blank context`() =
        runTest(testDispatcher) {
            val messages = listOf(
                ChatMessage(id = 1, message = "A", role = "user", context = "stress", userId = "u1"),
                ChatMessage(id = 2, message = "B", role = "assistant", context = null, userId = "u1"),
                ChatMessage(id = 3, message = "C", role = "user", context = "anxiety", userId = "u1")
            )
            every { chatDao.getAllMessagesFlow("u1") } returns flowOf(messages)

            val result = repository.getLastContext("u1")
            assertThat(result).isEqualTo("anxiety")
        }

    @Test
    fun `getLastContext - returns null when no message has a context`() =
        runTest(testDispatcher) {
            val messages = listOf(
                ChatMessage(id = 1, message = "A", role = "user", context = null, userId = "u1"),
                ChatMessage(id = 2, message = "B", role = "assistant", context = "", userId = "u1")
            )
            every { chatDao.getAllMessagesFlow("u1") } returns flowOf(messages)

            val result = repository.getLastContext("u1")
            assertThat(result).isNull()
        }

    @Test
    fun `getLastContext - returns null when message list is empty`() =
        runTest(testDispatcher) {
            every { chatDao.getAllMessagesFlow("u1") } returns flowOf(emptyList())

            val result = repository.getLastContext("u1")
            assertThat(result).isNull()
        }
}
