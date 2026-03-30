package com.iamashad.meraki.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.data.ChatDao
import com.iamashad.meraki.data.ChatDatabase
import com.iamashad.meraki.data.ChatMessage
import com.iamashad.meraki.model.Journal
import com.iamashad.meraki.repository.ChatRepository
import com.iamashad.meraki.repository.FirestoreRepository
import com.iamashad.meraki.rules.MainDispatcherRule
import com.iamashad.meraki.screens.journal.JournalViewModel
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 5 – Golden Path 2: "The Journaler"
 *
 * End-to-end integration scenario tested through a real Room in-memory DB
 * (for the chat history side) and a mocked FirestoreRepository (for journals):
 *
 *   1. JournalScreen opens — journal list is empty
 *   2. User taps Add Journal; fills mood, reason, note and taps Save
 *   3. New entry flows from FirestoreRepository → JournalViewModel.journals StateFlow
 *   4. The journal list is now non-empty and contains the saved entry
 *   5. User searches for the entry by title
 *   6. Search results return the correct entry
 *
 * The in-memory Room database is used to verify the ChatRepository integration
 * (chat history persistence) as a complementary sub-scenario.
 *
 * FirestoreRepository is stubbed with a [MutableSharedFlow] so we can push
 * updated journal lists and observe them through JournalViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JournalFlowTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    // ── Room in-memory DB for chat history sub-scenario ──────────────────────
    private lateinit var db: ChatDatabase
    private lateinit var dao: ChatDao
    private lateinit var chatRepository: ChatRepository

    // ── Firestore/Journal mocks ───────────────────────────────────────────────
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var viewModel: JournalViewModel

    // Shared flow lets the test push live journal updates into the ViewModel.
    private val liveJournals = MutableSharedFlow<List<Journal>>(replay = 1)

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun journal(
        id: String,
        title: String,
        userId: String = "test-user",
        content: String = "Some thoughts for $title"
    ) = Journal(
        journalId = id,
        userId = userId,
        title = title,
        content = content,
        moodScore = 80,
        reasons = listOf("Work", "Exercise"),
        date = System.currentTimeMillis()
    )

    @Before
    fun setUp() {
        // Room in-memory database (Robolectric provides Android runtime)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChatDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.chatDao()
        chatRepository = ChatRepository(dao, testDispatcher)

        // Firestore: listen via the shared flow, other ops are no-ops
        firestoreRepository = mockk(relaxed = true)
        every { firestoreRepository.listenToJournals(any()) } returns liveJournals
        coJustRun { firestoreRepository.addJournal(any()) }
        coJustRun { firestoreRepository.deleteJournal(any()) }

        firebaseAuth = mockk(relaxed = true) {
            every { currentUser?.uid } returns "test-user"
        }

        viewModel = JournalViewModel(firestoreRepository, firebaseAuth)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── scenario tests ────────────────────────────────────────────────────────

    @Test
    fun `step 1 - journal screen opens with empty list`() = runTest(testDispatcher) {
        liveJournals.emit(emptyList())
        advanceUntilIdle()

        assertThat(viewModel.journals.value).isEmpty()
        assertThat(viewModel.isSearching.value).isFalse()
    }

    @Test
    fun `step 2 - addJournal delegates save to FirestoreRepository`() =
        runTest(testDispatcher) {
            val entry = journal("j-new", "Happy", content = "Had a great workout today!")

            viewModel.addJournal(entry)
            advanceUntilIdle()

            coVerify { firestoreRepository.addJournal(entry) }
        }

    @Test
    fun `step 3 - journal list updates when Firestore emits new entry`() =
        runTest(testDispatcher) {
            val entry = journal("j-new", "Happy", content = "Had a great workout today!")

            // Simulate Firestore snapshot arriving after the save
            liveJournals.emit(listOf(entry))
            advanceUntilIdle()

            assertThat(viewModel.journals.value).hasSize(1)
            assertThat(viewModel.journals.value.first().journalId).isEqualTo("j-new")
        }

    @Test
    fun `step 4 - multiple journal entries are all visible in the list`() =
        runTest(testDispatcher) {
            val entries = listOf(
                journal("j1", "Happy"),
                journal("j2", "Calm"),
                journal("j3", "Anxious")
            )
            liveJournals.emit(entries)
            advanceUntilIdle()

            assertThat(viewModel.journals.value).hasSize(3)
            assertThat(viewModel.journals.value.map { it.title })
                .containsExactly("Happy", "Calm", "Anxious").inOrder()
        }

    @Test
    fun `step 5 - search returns matching entries after debounce`() =
        runTest(testDispatcher) {
            val results = listOf(journal("j-happy", "Happy", content = "Great mood"))
            every { firestoreRepository.listenToJournals(any()) } returns flowOf(results)
            io.mockk.coEvery {
                firestoreRepository.searchJournals(any(), eq("Happy"))
            } returns results

            // Reconstruct VM to pick up the new stub
            viewModel = JournalViewModel(firestoreRepository, firebaseAuth)

            viewModel.updateSearchQuery("Happy")
            advanceTimeBy(301)
            advanceUntilIdle()

            assertThat(viewModel.isSearching.value).isTrue()
            assertThat(viewModel.searchResults.value).hasSize(1)
            assertThat(viewModel.searchResults.value.first().title).isEqualTo("Happy")
        }

    @Test
    fun `step 6 - clearing search restores non-search state`() =
        runTest(testDispatcher) {
            viewModel.updateSearchQuery("Happy")
            advanceTimeBy(301)
            advanceUntilIdle()

            viewModel.clearSearchResults()

            assertThat(viewModel.isSearching.value).isFalse()
            assertThat(viewModel.searchResults.value).isEmpty()
        }

    @Test
    fun `full golden path - add then delete journal delegated correctly`() =
        runTest(testDispatcher) {
            val entry = journal("j-del", "To Delete")

            viewModel.addJournal(entry)
            advanceUntilIdle()

            viewModel.deleteJournal("j-del")
            advanceUntilIdle()

            coVerify { firestoreRepository.addJournal(entry) }
            coVerify { firestoreRepository.deleteJournal("j-del") }
        }

    // ── Room chat-history sub-scenario ────────────────────────────────────────

    @Test
    fun `chat history - messages saved to Room persist across queries`() = runTest(testDispatcher) {
        val msg1 = ChatMessage(message = "I feel happy today!", role = "user", userId = "test-user")
        val msg2 = ChatMessage(message = "That's wonderful!", role = "model", userId = "test-user")

        dao.insertMessage(msg1)
        dao.insertMessage(msg2)

        val history = dao.getAllMessages("test-user")
        assertThat(history).hasSize(2)
        assertThat(history.first().message).isEqualTo("I feel happy today!")
        assertThat(history.last().role).isEqualTo("model")
    }
}
