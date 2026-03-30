package com.iamashad.meraki.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [ChatDao] using an in-memory Room database.
 *
 * Robolectric provides the Android runtime so Room can open SQLite
 * in a JVM test without a physical device or emulator.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChatDaoTest {

    private lateinit var db: ChatDatabase
    private lateinit var dao: ChatDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChatDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        dao = db.chatDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── insertMessage ─────────────────────────────────────────────────────────

    @Test
    fun `insertMessage - single message is retrievable via getAllMessages`() = runTest {
        val msg = ChatMessage(message = "Hello", role = "user", userId = "u1")
        dao.insertMessage(msg)

        val results = dao.getAllMessages("u1")
        assertThat(results).hasSize(1)
        assertThat(results.first().message).isEqualTo("Hello")
        assertThat(results.first().role).isEqualTo("user")
    }

    @Test
    fun `insertMessage - multiple messages are ordered by id ASC`() = runTest {
        dao.insertMessage(ChatMessage(message = "First", role = "user", userId = "u1"))
        dao.insertMessage(ChatMessage(message = "Second", role = "assistant", userId = "u1"))
        dao.insertMessage(ChatMessage(message = "Third", role = "user", userId = "u1"))

        val results = dao.getAllMessages("u1")
        assertThat(results).hasSize(3)
        assertThat(results.map { it.message }).containsExactly("First", "Second", "Third").inOrder()
    }

    @Test
    fun `insertMessage - replace strategy replaces duplicate primary key`() = runTest {
        dao.insertMessage(ChatMessage(id = 1, message = "Original", role = "user", userId = "u1"))
        dao.insertMessage(ChatMessage(id = 1, message = "Replaced", role = "user", userId = "u1"))

        val results = dao.getAllMessages("u1")
        assertThat(results).hasSize(1)
        assertThat(results.first().message).isEqualTo("Replaced")
    }

    @Test
    fun `insertMessage - message with optional context is stored correctly`() = runTest {
        val msg = ChatMessage(message = "Ctx msg", role = "assistant", context = "anxiety", userId = "u1")
        dao.insertMessage(msg)

        val results = dao.getAllMessages("u1")
        assertThat(results.first().context).isEqualTo("anxiety")
    }

    @Test
    fun `insertMessage - null context is allowed`() = runTest {
        val msg = ChatMessage(message = "No ctx", role = "user", context = null, userId = "u1")
        dao.insertMessage(msg)

        val results = dao.getAllMessages("u1")
        assertThat(results.first().context).isNull()
    }

    // ── clearChatHistory ──────────────────────────────────────────────────────

    @Test
    fun `clearChatHistory - removes all messages for the specified user`() = runTest {
        dao.insertMessage(ChatMessage(message = "A", role = "user", userId = "u1"))
        dao.insertMessage(ChatMessage(message = "B", role = "assistant", userId = "u1"))

        dao.clearChatHistory("u1")

        val results = dao.getAllMessages("u1")
        assertThat(results).isEmpty()
    }

    @Test
    fun `clearChatHistory - does not affect messages of other users`() = runTest {
        dao.insertMessage(ChatMessage(message = "U1 msg", role = "user", userId = "u1"))
        dao.insertMessage(ChatMessage(message = "U2 msg", role = "user", userId = "u2"))

        dao.clearChatHistory("u1")

        assertThat(dao.getAllMessages("u1")).isEmpty()
        assertThat(dao.getAllMessages("u2")).hasSize(1)
    }

    @Test
    fun `clearChatHistory - on empty db is a no-op`() = runTest {
        dao.clearChatHistory("nobody")
        assertThat(dao.getAllMessages("nobody")).isEmpty()
    }

    // ── getAllMessages ─────────────────────────────────────────────────────────

    @Test
    fun `getAllMessages - returns empty list when no messages exist for user`() = runTest {
        assertThat(dao.getAllMessages("unknown")).isEmpty()
    }

    @Test
    fun `getAllMessages - scoped to userId, does not return other users data`() = runTest {
        dao.insertMessage(ChatMessage(message = "Mine", role = "user", userId = "u1"))
        dao.insertMessage(ChatMessage(message = "Theirs", role = "user", userId = "u2"))

        val results = dao.getAllMessages("u1")
        assertThat(results).hasSize(1)
        assertThat(results.first().message).isEqualTo("Mine")
    }

    // ── getAllMessagesFlow ─────────────────────────────────────────────────────

    @Test
    fun `getAllMessagesFlow - emits empty list on first collection when db is empty`() = runTest {
        dao.getAllMessagesFlow("u1").test {
            val first = awaitItem()
            assertThat(first).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllMessagesFlow - emits updated list after insertMessage`() = runTest {
        dao.getAllMessagesFlow("u1").test {
            // initial empty emission
            assertThat(awaitItem()).isEmpty()

            // insert and expect a new emission
            dao.insertMessage(ChatMessage(message = "Live", role = "user", userId = "u1"))
            val updated = awaitItem()
            assertThat(updated).hasSize(1)
            assertThat(updated.first().message).isEqualTo("Live")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllMessagesFlow - emits empty list after clearChatHistory`() = runTest {
        dao.insertMessage(ChatMessage(message = "Before clear", role = "user", userId = "u1"))

        dao.getAllMessagesFlow("u1").test {
            assertThat(awaitItem()).hasSize(1)

            dao.clearChatHistory("u1")
            assertThat(awaitItem()).isEmpty()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllMessagesFlow - emits correct count after multiple inserts`() = runTest {
        dao.getAllMessagesFlow("u1").test {
            assertThat(awaitItem()).isEmpty()

            dao.insertMessage(ChatMessage(message = "M1", role = "user", userId = "u1"))
            assertThat(awaitItem()).hasSize(1)

            dao.insertMessage(ChatMessage(message = "M2", role = "assistant", userId = "u1"))
            assertThat(awaitItem()).hasSize(2)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
