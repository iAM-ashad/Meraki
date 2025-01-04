package com.iamashad.meraki.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.iamashad.meraki.model.Journal
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()
    private val journalsCollection = db.collection("journals")

    suspend fun addJournal(journal: Journal) {
        val docId = if (journal.journalId.isNotEmpty()) journal.journalId else journalsCollection.document().id
        val dataToSave = hashMapOf(
            "journalId" to docId,
            "userId" to journal.userId,
            "title" to journal.title,
            "content" to journal.content,
            "moodScore" to journal.moodScore,
            "reasons" to journal.reasons, // Save reasons here
            "date" to com.google.firebase.Timestamp(journal.date / 1000, ((journal.date % 1000) * 1000000).toInt())
        )

        journalsCollection.document(docId).set(dataToSave).await()
    }

    suspend fun deleteJournal(journalId: String) {
        journalsCollection.document(journalId).delete().await()
    }

    fun listenToJournals(userId: String) = callbackFlow {
        val listenerRegistration = journalsCollection
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val journals = snapshot?.documents?.mapNotNull { mapToJournal(it) } ?: emptyList()
                trySend(journals)
            }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun searchJournals(userId: String, query: String): List<Journal> {
        return journalsCollection
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val title = doc.getString("title").orEmpty()
                val content = doc.getString("content").orEmpty()
                if (title.contains(query, true) || content.contains(query, true)) {
                    mapToJournal(doc)
                } else null
            }
    }

    private fun mapToJournal(doc: com.google.firebase.firestore.DocumentSnapshot): Journal? {
        val journalId = doc.getString("journalId").orEmpty()
        val title = doc.getString("title").orEmpty()
        val content = doc.getString("content").orEmpty()
        val moodScore = doc.getLong("moodScore")?.toInt() ?: 50 // Default to 50 if missing
        val reasons = doc.get("reasons") as? List<String> ?: emptyList() // Map the reasons field
        val dateValue = doc.get("date")
        val date = when (dateValue) {
            is com.google.firebase.Timestamp -> dateValue.toDate().time
            else -> System.currentTimeMillis()
        }
        val fetchedUserId = doc.getString("userId").orEmpty()

        return if (fetchedUserId.isNotEmpty()) {
            Journal(
                journalId = journalId,
                userId = fetchedUserId,
                title = title,
                content = content,
                date = date,
                moodScore = moodScore,
                reasons = reasons // Add the reasons to the Journal object
            )
        } else null
    }

}

