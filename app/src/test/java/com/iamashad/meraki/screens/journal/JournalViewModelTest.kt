package com.iamashad.meraki.screens.journal

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.model.Journal
import com.iamashad.meraki.repository.FirestoreRepository
import com.iamashad.meraki.rules.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [JournalViewModel] verifying:
 * - [journals] StateFlow populates from [FirestoreRepository.listenToJournals]
 * - [searchQuery] / [isSearching] toggle correctly on [updateSearchQuery]
 * - [searchResults] emits after the 300ms debounce elapses
 * - [clearSearchResults] resets search state
 * - [errorState] is set when repository throws
 *
 * Uses [StandardTestDispatcher] so virtual time can be advanced to trigger
 * the 300ms debounce in [JournalViewModel.observeSearchQuery].
 *
 * IMPORTANT: all repository mocks must be configured BEFORE constructing the
 * ViewModel because [JournalViewModel.init] immediately starts listening to
 * the journals flow and the search query collector.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModelTest {

    // StandardTestDispatcher gives us control over virtual time (required for debounce)
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val repository: FirestoreRepository = mockk(relaxed = true)
    private val firebaseAuth: FirebaseAuth = mockk(relaxed = true) {
        every { currentUser } returns null   // userId will be empty string
    }

    private lateinit var viewModel: JournalViewModel

    // ── sample data ───────────────────────────────────────────────────────────

    private fun journal(id: String, title: String) = Journal(
        journalId = id,
        userId = "u1",
        title = title,
        content = "content of $id",
        moodScore = 70,
        reasons = emptyList(),
        date = System.currentTimeMillis()
    )

    @Before
    fun setUp() {
        // Default stubs — set BEFORE ViewModel construction so init block succeeds.
        every { repository.listenToJournals(any()) } returns flowOf(emptyList())
        coEvery { repository.searchJournals(any(), any()) } returns emptyList()
        coJustRun { repository.addJournal(any()) }
        coJustRun { repository.deleteJournal(any()) }

        viewModel = JournalViewModel(repository, firebaseAuth)
    }

    // ── journals loading ───────────────────────────────────────────────────────

    @Test
    fun `journals - emits list from listenToJournals on init`() =
        runTest(testDispatcher) {
            val jList = listOf(journal("j1", "Happy"), journal("j2", "Calm"))
            every { repository.listenToJournals(any()) } returns flowOf(jList)

            // Reconstruct the VM so init picks up the new stub
            viewModel = JournalViewModel(repository, firebaseAuth)
            advanceUntilIdle()

            assertThat(viewModel.journals.value).hasSize(2)
            assertThat(viewModel.journals.value.map { it.journalId })
                .containsExactly("j1", "j2").inOrder()
        }

    @Test
    fun `journals - starts empty when repository emits empty list`() =
        runTest(testDispatcher) {
            advanceUntilIdle()
            assertThat(viewModel.journals.value).isEmpty()
        }

    @Test
    fun `journals - not updated while isSearching is true`() =
        runTest(testDispatcher) {
            val twoJournals = listOf(journal("j1", "First"), journal("j2", "Second"))
            every { repository.listenToJournals(any()) } returns flowOf(twoJournals)
            viewModel = JournalViewModel(repository, firebaseAuth)

            // Start a search — isSearching becomes true
            viewModel.updateSearchQuery("First")
            advanceUntilIdle()

            // Even though listenToJournals emitted 2 items, journals should be empty
            // because the collectLatest guard skips updates while isSearching=true
            assertThat(viewModel.journals.value).isEmpty()
        }

    // ── updateSearchQuery / isSearching ───────────────────────────────────────

    @Test
    fun `updateSearchQuery - sets searchQuery and isSearching when non-empty`() =
        runTest(testDispatcher) {
            viewModel.updateSearchQuery("test query")

            assertThat(viewModel.searchQuery.value).isEqualTo("test query")
            assertThat(viewModel.isSearching.value).isTrue()
        }

    @Test
    fun `updateSearchQuery with empty string - sets isSearching false`() =
        runTest(testDispatcher) {
            viewModel.updateSearchQuery("something")
            viewModel.updateSearchQuery("")

            assertThat(viewModel.isSearching.value).isFalse()
        }

    @Test
    fun `updateSearchQuery - searchQuery StateFlow reflects latest value`() =
        runTest(testDispatcher) {
            viewModel.searchQuery.test {
                awaitItem() // initial ""

                viewModel.updateSearchQuery("apple")
                assertThat(awaitItem()).isEqualTo("apple")

                viewModel.updateSearchQuery("banana")
                assertThat(awaitItem()).isEqualTo("banana")

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── searchResults / debounce ───────────────────────────────────────────────

    @Test
    fun `searchResults - emits results from repository after 300ms debounce`() =
        runTest(testDispatcher) {
            val results = listOf(journal("j3", "Anxious day"))
            coEvery { repository.searchJournals(any(), eq("anxious")) } returns results

            viewModel.updateSearchQuery("anxious")
            // Advance past the 300ms debounce window
            advanceTimeBy(301)
            advanceUntilIdle()

            assertThat(viewModel.searchResults.value).hasSize(1)
            assertThat(viewModel.searchResults.value.first().title).isEqualTo("Anxious day")
        }

    @Test
    fun `searchResults - empty before debounce window elapses`() =
        runTest(testDispatcher) {
            coEvery { repository.searchJournals(any(), any()) } returns listOf(journal("j4", "Happy"))

            viewModel.updateSearchQuery("happy")
            // Only advance 100ms — debounce hasn't fired yet
            advanceTimeBy(100)

            assertThat(viewModel.searchResults.value).isEmpty()
        }

    @Test
    fun `searchResults - empty when searchQuery is cleared`() =
        runTest(testDispatcher) {
            viewModel.updateSearchQuery("test")
            advanceTimeBy(301)
            advanceUntilIdle()

            viewModel.updateSearchQuery("")
            advanceTimeBy(301)
            advanceUntilIdle()

            // clearSearchResults() called when query is empty
            assertThat(viewModel.searchResults.value).isEmpty()
        }

    @Test
    fun `searchResults - calls searchJournals with correct query`() =
        runTest(testDispatcher) {
            viewModel.updateSearchQuery("gratitude")
            advanceTimeBy(301)
            advanceUntilIdle()

            coVerify { repository.searchJournals(any(), "gratitude") }
        }

    // ── clearSearchResults ────────────────────────────────────────────────────

    @Test
    fun `clearSearchResults - resets searchResults and isSearching`() =
        runTest(testDispatcher) {
            val results = listOf(journal("j5", "Calm"))
            coEvery { repository.searchJournals(any(), any()) } returns results

            viewModel.updateSearchQuery("calm")
            advanceTimeBy(301)
            advanceUntilIdle()
            assertThat(viewModel.searchResults.value).isNotEmpty()

            viewModel.clearSearchResults()

            assertThat(viewModel.searchResults.value).isEmpty()
            assertThat(viewModel.isSearching.value).isFalse()
        }

    // ── addJournal ────────────────────────────────────────────────────────────

    @Test
    fun `addJournal - delegates to repository`() = runTest(testDispatcher) {
        val j = journal("j6", "New entry")
        viewModel.addJournal(j)
        advanceUntilIdle()

        coVerify { repository.addJournal(j) }
    }

    @Test
    fun `addJournal - sets errorState when repository throws`() = runTest(testDispatcher) {
        coEvery { repository.addJournal(any()) } throws Exception("Firestore error")

        viewModel.addJournal(journal("j7", "Bad"))
        advanceUntilIdle()

        assertThat(viewModel.errorState.value).isNotNull()
    }

    // ── deleteJournal ─────────────────────────────────────────────────────────

    @Test
    fun `deleteJournal - delegates to repository with correct id`() = runTest(testDispatcher) {
        viewModel.deleteJournal("j-del")
        advanceUntilIdle()

        coVerify { repository.deleteJournal("j-del") }
    }

    @Test
    fun `deleteJournal - sets errorState when repository throws`() = runTest(testDispatcher) {
        coEvery { repository.deleteJournal(any()) } throws Exception("Delete failed")

        viewModel.deleteJournal("j-bad")
        advanceUntilIdle()

        assertThat(viewModel.errorState.value).isNotNull()
    }

    // ── errorState ────────────────────────────────────────────────────────────

    @Test
    fun `errorState - is null in initial state`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertThat(viewModel.errorState.value).isNull()
    }

    @Test
    fun `errorState - is set when listenToJournals throws`() = runTest(testDispatcher) {
        every { repository.listenToJournals(any()) } returns
            kotlinx.coroutines.flow.flow { throw Exception("Listener failed") }

        viewModel = JournalViewModel(repository, firebaseAuth)
        advanceUntilIdle()

        assertThat(viewModel.errorState.value).isNotNull()
    }
}
