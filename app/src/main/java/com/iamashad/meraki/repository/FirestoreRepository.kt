package com.iamashad.meraki.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.model.Journal
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()
    private val journalsCollection = db.collection("journals")

    suspend fun addJournal(journal: Journal) {
        val docId =
            if (journal.journalId.isNotEmpty()) journal.journalId else journalsCollection.document().id

        // Convert Long to Timestamp for Firestore
        val dataToSave = hashMapOf(
            "journalId" to docId,
            "userId" to journal.userId,
            "title" to journal.title,
            "content" to journal.content,
            "date" to com.google.firebase.Timestamp(
                journal.date / 1000,
                ((journal.date % 1000) * 1000000).toInt()
            ) // Convert Long to Timestamp
        )

        journalsCollection.document(docId)
            .set(dataToSave)
            .await()
    }

    suspend fun getJournals(userId: String): List<Journal> {
        return journalsCollection
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val journalId = doc.getString("journalId").orEmpty()
                val title = doc.getString("title").orEmpty()
                val content = doc.getString("content").orEmpty()
                val dateValue = doc.get("date")
                val date = when (dateValue) {
                    is com.google.firebase.Timestamp -> dateValue.toDate().time
                    is Long -> dateValue
                    else -> System.currentTimeMillis()
                }
                val fetchedUserId = doc.getString("userId").orEmpty()

                if (fetchedUserId == userId) {
                    Journal(journalId, fetchedUserId, title, content, date)
                } else {
                    null
                }
            }
    }

    fun listenToJournals(userId: String, onJournalsUpdated: (List<Journal>) -> Unit) {
        journalsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    e.printStackTrace() // Handle errors
                    return@addSnapshotListener
                }

                val updatedJournals = snapshot?.documents?.mapNotNull { doc ->
                    val journalId = doc.getString("journalId").orEmpty()
                    val title = doc.getString("title").orEmpty()
                    val content = doc.getString("content").orEmpty()
                    val dateValue = doc.get("date")
                    val date = when (dateValue) {
                        is com.google.firebase.Timestamp -> dateValue.toDate().time
                        is Long -> dateValue
                        else -> System.currentTimeMillis()
                    }
                    val fetchedUserId = doc.getString("userId").orEmpty()

                    if (fetchedUserId == userId) {
                        Journal(journalId, fetchedUserId, title, content, date)
                    } else {
                        null
                    }
                }.orEmpty()

                onJournalsUpdated(updatedJournals)
            }
    }


    suspend fun deleteJournal(journalId: String) {
        journalsCollection.document(journalId).delete().await()
    }
}

