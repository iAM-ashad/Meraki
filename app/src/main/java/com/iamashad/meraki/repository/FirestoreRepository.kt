package com.iamashad.meraki.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.iamashad.meraki.model.Journal
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository that handles Firestore operations for journal entries.
 * Provides functions for adding, deleting, querying, and listening to journal data.
 */
@Singleton
class FirestoreRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    // Reference to the "journals" collection in Firestore
    private val journalsCollection = db.collection("journals")

    /**
     * Adds a new journal or updates an existing one in Firestore.
     * Automatically generates a document ID if journalId is empty.
     */
    suspend fun addJournal(journal: Journal) {
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
    suspend fun deleteJournal(journalId: String) {
        journalsCollection.document(journalId).delete().await()
    }

    /**
     * Retrieves all journals for a specific user, ordered by date (newest first).
     */
    suspend fun getAllJournals(userId: String): List<Journal> {
        return journalsCollection
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
    }

    /**
     * Performs a simple search on journal title and content fields.
     * Firestore doesn't support full-text search, so we filter client-side.
     */
    suspend fun searchJournals(userId: String, query: String): List<Journal> {
        return journalsCollection
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val title = doc.getString("title").orEmpty()
                val content = doc.getString("content").orEmpty()
                if (title.contains(query, ignoreCase = true) || content.contains(query, ignoreCase = true)) {
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
