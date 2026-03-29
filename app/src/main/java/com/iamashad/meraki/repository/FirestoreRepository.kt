package com.iamashad.meraki.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.iamashad.meraki.di.IoDispatcher
import com.iamashad.meraki.model.Journal
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repository that handles Firestore operations for journal entries.
 * Provides functions for adding, deleting, querying, and listening to journal data.
 *
 * Phase 3: All operations are main-safe.
 * - suspend functions are wrapped in withContext(ioDispatcher).
 * - listenToJournals() uses flowOn(ioDispatcher) so downstream collection
 *   and mapping execute on the IO thread.
 * - ioDispatcher is injected via Hilt for testability.
 *
 * Note: @Inject constructor removed — this class is provided as a singleton
 * by NetworkModule.provideFirestoreRepository to avoid a duplicate binding.
 */
class FirestoreRepository(
    private val db: FirebaseFirestore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    // Reference to the "journals" collection in Firestore
    private val journalsCollection = db.collection("journals")

    /**
     * Adds a new journal or updates an existing one in Firestore.
     * Automatically generates a document ID if journalId is empty.
     */
    suspend fun addJournal(journal: Journal) = withContext(ioDispatcher) {
        val docId = if (journal.journalId.isNotEmpty())
            journal.journalId
        else
            journalsCollection.document().id

        val dataToSave = hashMapOf(
            "journalId" to docId,
            "userId" to journal.userId,
            "title" to journal.title,
            "content" to journal.content,
            "moodScore" to journal.moodScore,
            "reasons" to journal.reasons,
            "date" to FieldValue.serverTimestamp(), // Server timestamp ensures consistency
            "imageUrl" to journal.imageUrl
        )

        journalsCollection.document(docId).set(dataToSave).await()
    }

    /**
     * Deletes a journal document by its ID.
     */
    suspend fun deleteJournal(journalId: String) = withContext(ioDispatcher) {
        journalsCollection.document(journalId).delete().await()
    }

    /**
     * Retrieves all journals for a specific user, ordered by date (newest first).
     * Phase 3: document mapping is explicitly on ioDispatcher via withContext.
     */
    suspend fun getAllJournals(userId: String): List<Journal> = withContext(ioDispatcher) {
        journalsCollection
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { mapToJournal(it) }
    }

    /**
     * Sets up a real-time listener for journal changes in Firestore.
     * Emits updated lists whenever data changes for the specified user.
     * Phase 3: flowOn(ioDispatcher) ensures the producer runs on the IO thread.
     */
    fun listenToJournals(userId: String) = callbackFlow {
        val listenerRegistration = journalsCollection
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error) // Close the flow if error occurs
                    return@addSnapshotListener
                }
                val journals = snapshot?.documents?.mapNotNull { mapToJournal(it) } ?: emptyList()
                trySend(journals) // Emit the list
            }

        awaitClose { listenerRegistration.remove() } // Remove listener on cancellation
    }.flowOn(ioDispatcher)

    /**
     * Performs a simple search on journal title and content fields.
     * Firestore doesn't support full-text search, so we filter client-side.
     * Phase 3: client-side filtering runs on ioDispatcher via withContext.
     */
    suspend fun searchJournals(userId: String, query: String): List<Journal> =
        withContext(ioDispatcher) {
            journalsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val title = doc.getString("title").orEmpty()
                    val content = doc.getString("content").orEmpty()
                    if (title.contains(query, ignoreCase = true) ||
                        content.contains(query, ignoreCase = true)
                    ) {
                        mapToJournal(doc)
                    } else null
                }
        }

    /**
     * Converts a Firestore document into a Journal data class.
     * Handles null-safe conversions and default fallbacks.
     */
    private fun mapToJournal(doc: com.google.firebase.firestore.DocumentSnapshot): Journal? {
        val journalId = doc.getString("journalId").orEmpty()
        val title = doc.getString("title").orEmpty()
        val content = doc.getString("content").orEmpty()
        val moodScore = doc.getLong("moodScore")?.toInt() ?: 50
        val reasons = doc.get("reasons") as? List<String> ?: emptyList()

        // Convert Firestore timestamp to epoch milliseconds
        val dateValue = doc.get("date")
        val date = when (dateValue) {
            is com.google.firebase.Timestamp -> dateValue.toDate().time
            else -> System.currentTimeMillis()
        }

        val imageUrl = doc.getString("imageUrl")
        val fetchedUserId = doc.getString("userId").orEmpty()

        return if (fetchedUserId.isNotEmpty()) {
            Journal(
                journalId = journalId,
                userId = fetchedUserId,
                title = title,
                content = content,
                date = date,
                moodScore = moodScore,
                reasons = reasons,
                imageUrl = imageUrl
            )
        } else null
    }
}
