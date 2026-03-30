package com.iamashad.meraki.repository

import app.cash.turbine.test
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.common.truth.Truth.assertThat
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.concurrent.Executor

/**
 * Unit tests for [FirestoreRepository] using MockK.
 *
 * Because [FirestoreRepository] uses `kotlinx-coroutines-play-services`
 * `Task<T>.await()`, tests stub [Task] with `isComplete = true` so the
 * coroutines extension returns immediately without requiring a real executor.
 *
 * [listenToJournals] uses `callbackFlow` + `addSnapshotListener`; tests
 * capture the [EventListener] via a MockK slot and invoke it directly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreRepositoryTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var journalsCollection: CollectionReference
    private lateinit var query: Query
    private lateinit var repository: FirestoreRepository

    private val testDispatcher = StandardTestDispatcher()

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds a completed [Task] whose [Task.getResult] returns [value].
     * `isComplete = true` makes the coroutines-play-services fast path return
     * immediately without dispatching to a real Android executor.
     */
    private fun <T> completedTask(value: T): Task<T> = mockk {
        every { isComplete } returns true
        every { isCanceled } returns false
        every { exception } returns null
        every { result } returns value
        every { addOnSuccessListener(any<OnSuccessListener<T>>()) } answers {
            firstArg<OnSuccessListener<T>>().onSuccess(value)
            this@mockk
        }
        every { addOnFailureListener(any<OnFailureListener>()) } returns this@mockk
        every { addOnSuccessListener(any<Executor>(), any<OnSuccessListener<T>>()) } answers {
            secondArg<OnSuccessListener<T>>().onSuccess(value)
            this@mockk
        }
        every { addOnCompleteListener(any()) } answers {
            // onCompleteListener receives the task itself
            firstArg<com.google.android.gms.tasks.OnCompleteListener<T>>().onComplete(this@mockk)
            this@mockk
        }
    }

    /**
     * Builds a completed [Task] that has failed with [exception].
     */
    private fun <T> failedTask(exception: Exception): Task<T> = mockk {
        every { isComplete } returns true
        every { isCanceled } returns false
        every { this@mockk.exception } returns exception
        every { result } throws exception
        every { addOnSuccessListener(any<OnSuccessListener<T>>()) } returns this@mockk
        every { addOnFailureListener(any<OnFailureListener>()) } answers {
            firstArg<OnFailureListener>().onFailure(exception)
            this@mockk
        }
        every { addOnCompleteListener(any()) } answers {
            firstArg<com.google.android.gms.tasks.OnCompleteListener<T>>().onComplete(this@mockk)
            this@mockk
        }
    }

    /**
     * Creates a [DocumentSnapshot] mock that simulates a valid journal document.
     */
    private fun mockJournalDoc(
        journalId: String = "j1",
        userId: String = "u1",
        title: String = "Happy",
        content: String = "A great day",
        moodScore: Long = 80L,
        reasons: List<String> = listOf("Work", "Friends"),
        timestamp: Timestamp = Timestamp(Date(1_700_000_000_000L)),
        imageUrl: String? = null
    ): DocumentSnapshot {
        // Build the mock outside the builder block so that List<String> and Timestamp
        // values are not misinterpreted as MockK matcher expressions.
        val doc = mockk<DocumentSnapshot>(relaxed = true)
        every { doc.getString("journalId") } returns journalId
        every { doc.getString("userId") } returns userId
        every { doc.getString("title") } returns title
        every { doc.getString("content") } returns content
        every { doc.getLong("moodScore") } returns moodScore
        every { doc.get("reasons") } returns reasons
        every { doc.get("date") } returns timestamp
        every { doc.getString("imageUrl") } returns imageUrl
        return doc
    }

    @Before
    fun setUp() {
        firestore = mockk(relaxed = true)
        journalsCollection = mockk(relaxed = true)
        query = mockk(relaxed = true)

        // Stub db.collection("journals") → journalsCollection
        every { firestore.collection("journals") } returns journalsCollection

        // Default chain: collection.whereEqualTo(...) → query
        every { journalsCollection.whereEqualTo(any<String>(), any()) } returns query

        // Default chain: query.orderBy(...) → query (same mock for chaining)
        every { query.orderBy(any<String>(), any()) } returns query

        repository = FirestoreRepository(firestore, testDispatcher)
    }

    // ── getAllJournals — success ───────────────────────────────────────────────

    @Test
    fun `getAllJournals - returns mapped journals on success`() = runTest(testDispatcher) {
        val doc = mockJournalDoc()
        val snapshot = mockk<QuerySnapshot> {
            every { documents } returns listOf(doc)
        }
        every { query.get() } returns completedTask(snapshot)

        val result = repository.getAllJournals("u1")

        assertThat(result).hasSize(1)
        assertThat(result.first().journalId).isEqualTo("j1")
        assertThat(result.first().userId).isEqualTo("u1")
        assertThat(result.first().title).isEqualTo("Happy")
        assertThat(result.first().moodScore).isEqualTo(80)
    }

    @Test
    fun `getAllJournals - returns empty list when snapshot has no documents`() =
        runTest(testDispatcher) {
            val snapshot = mockk<QuerySnapshot> {
                every { documents } returns emptyList()
            }
            every { query.get() } returns completedTask(snapshot)

            val result = repository.getAllJournals("u1")

            assertThat(result).isEmpty()
        }

    @Test
    fun `getAllJournals - skips documents where userId is empty`() = runTest(testDispatcher) {
        val validDoc = mockJournalDoc(journalId = "j1", userId = "u1")
        val invalidDoc = mockk<DocumentSnapshot>(relaxed = true) {
            every { getString("userId") } returns ""  // empty → mapToJournal returns null
            every { getString("journalId") } returns "j2"
        }
        val snapshot = mockk<QuerySnapshot> {
            every { documents } returns listOf(validDoc, invalidDoc)
        }
        every { query.get() } returns completedTask(snapshot)

        val result = repository.getAllJournals("u1")

        assertThat(result).hasSize(1)
        assertThat(result.first().journalId).isEqualTo("j1")
    }

    @Test
    fun `getAllJournals - maps moodScore field from Long to Int correctly`() =
        runTest(testDispatcher) {
            val doc = mockJournalDoc(moodScore = 65L)
            val snapshot = mockk<QuerySnapshot> {
                every { documents } returns listOf(doc)
            }
            every { query.get() } returns completedTask(snapshot)

            val result = repository.getAllJournals("u1")

            assertThat(result.first().moodScore).isEqualTo(65)
        }

    @Test
    fun `getAllJournals - maps reasons list correctly`() = runTest(testDispatcher) {
        val doc = mockJournalDoc(reasons = listOf("Sleep", "Health", "Exercise"))
        val snapshot = mockk<QuerySnapshot> {
            every { documents } returns listOf(doc)
        }
        every { query.get() } returns completedTask(snapshot)

        val result = repository.getAllJournals("u1")

        assertThat(result.first().reasons).containsExactly("Sleep", "Health", "Exercise").inOrder()
    }

    @Test
    fun `getAllJournals - imageUrl is null when document has no imageUrl`() =
        runTest(testDispatcher) {
            val doc = mockJournalDoc(imageUrl = null)
            val snapshot = mockk<QuerySnapshot> {
                every { documents } returns listOf(doc)
            }
            every { query.get() } returns completedTask(snapshot)

            val result = repository.getAllJournals("u1")

            assertThat(result.first().imageUrl).isNull()
        }

    @Test
    fun `getAllJournals - converts Timestamp to epoch millis`() = runTest(testDispatcher) {
        val epochMillis = 1_700_000_000_000L
        val doc = mockJournalDoc(timestamp = Timestamp(Date(epochMillis)))
        val snapshot = mockk<QuerySnapshot> {
            every { documents } returns listOf(doc)
        }
        every { query.get() } returns completedTask(snapshot)

        val result = repository.getAllJournals("u1")

        assertThat(result.first().date).isEqualTo(epochMillis)
    }

    // ── listenToJournals — success ────────────────────────────────────────────

    @Test
    fun `listenToJournals - emits mapped list when snapshot listener fires`() =
        runTest(testDispatcher) {
            val listenerSlot = slot<EventListener<QuerySnapshot>>()
            val listenerRegistration = mockk<ListenerRegistration>(relaxed = true)

            every {
                query.addSnapshotListener(capture(listenerSlot))
            } answers {
                // Immediately invoke the captured listener with a snapshot
                val doc = mockJournalDoc()
                val snapshot = mockk<QuerySnapshot> {
                    every { documents } returns listOf(doc)
                }
                listenerSlot.captured.onEvent(snapshot, null)
                listenerRegistration
            }

            repository.listenToJournals("u1").test {
                val journals = awaitItem()
                assertThat(journals).hasSize(1)
                assertThat(journals.first().title).isEqualTo("Happy")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `listenToJournals - emits empty list when snapshot has no documents`() =
        runTest(testDispatcher) {
            val listenerSlot = slot<EventListener<QuerySnapshot>>()
            val listenerRegistration = mockk<ListenerRegistration>(relaxed = true)

            every {
                query.addSnapshotListener(capture(listenerSlot))
            } answers {
                val snapshot = mockk<QuerySnapshot> {
                    every { documents } returns emptyList()
                }
                listenerSlot.captured.onEvent(snapshot, null)
                listenerRegistration
            }

            repository.listenToJournals("u1").test {
                assertThat(awaitItem()).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `listenToJournals - closes flow with exception on Firestore error`() =
        runTest(testDispatcher) {
            val listenerSlot = slot<EventListener<QuerySnapshot>>()
            val listenerRegistration = mockk<ListenerRegistration>(relaxed = true)
            val firestoreException = mockk<FirebaseFirestoreException>(relaxed = true)

            every {
                query.addSnapshotListener(capture(listenerSlot))
            } answers {
                // Simulate an error by passing null snapshot + exception
                listenerSlot.captured.onEvent(null, firestoreException)
                listenerRegistration
            }

            repository.listenToJournals("u1").test {
                val error = awaitError()
                assertThat(error).isEqualTo(firestoreException)
            }
        }

    @Test
    fun `listenToJournals - emits multiple times as snapshots update`() =
        runTest(testDispatcher) {
            val listenerSlot = slot<EventListener<QuerySnapshot>>()
            val listenerRegistration = mockk<ListenerRegistration>(relaxed = true)

            every {
                query.addSnapshotListener(capture(listenerSlot))
            } answers {
                // First emission: one journal
                val doc1 = mockJournalDoc(journalId = "j1")
                val snap1 = mockk<QuerySnapshot> { every { documents } returns listOf(doc1) }
                listenerSlot.captured.onEvent(snap1, null)

                // Second emission: two journals
                val doc2 = mockJournalDoc(journalId = "j2", title = "Calm")
                val snap2 = mockk<QuerySnapshot> { every { documents } returns listOf(doc1, doc2) }
                listenerSlot.captured.onEvent(snap2, null)

                listenerRegistration
            }

            repository.listenToJournals("u1").test {
                val first = awaitItem()
                assertThat(first).hasSize(1)

                val second = awaitItem()
                assertThat(second).hasSize(2)
                assertThat(second.map { it.journalId }).containsExactly("j1", "j2").inOrder()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `listenToJournals - removes listener registration when flow is cancelled`() =
        runTest(testDispatcher) {
            val listenerSlot = slot<EventListener<QuerySnapshot>>()
            val listenerRegistration = mockk<ListenerRegistration>(relaxed = true)

            every {
                query.addSnapshotListener(capture(listenerSlot))
            } answers {
                val snapshot = mockk<QuerySnapshot> { every { documents } returns emptyList() }
                listenerSlot.captured.onEvent(snapshot, null)
                listenerRegistration
            }

            repository.listenToJournals("u1").test {
                awaitItem() // consume the emission
                cancelAndIgnoreRemainingEvents()
            }

            // awaitClose { listenerRegistration.remove() } should have been called
            io.mockk.verify(atLeast = 1) { listenerRegistration.remove() }
        }
}
