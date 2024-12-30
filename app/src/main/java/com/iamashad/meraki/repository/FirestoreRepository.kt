package com.iamashad.meraki.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.iamashad.meraki.model.Journal
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()
    private val journalsCollection = db.collection("journals")

    // Add Journal
    suspend fun addJournal(journal: Journal) {
        val docId = if (journal.journalId.isNotEmpty()) journal.journalId else journalsCollection.document().id
        val dataToSave = hashMapOf(
            "journalId" to docId,
            "userId" to journal.userId,
            "title" to journal.title,
            "content" to journal.content,
            "date" to com.google.firebase.Timestamp(journal.date / 1000, ((journal.date % 1000) * 1000000).toInt())
        )
        journalsCollection.document(docId).set(dataToSave).await()
    }

    // Delete Journal
    suspend fun deleteJournal(journalId: String) {
        journalsCollection.document(journalId).delete().await()
    }

    // Search Journals
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

    // Firestore PagingSource for pagination
    fun getJournalPagingSource(userId: String): PagingSource<Query, Journal> {
        return object : PagingSource<Query, Journal>() {
            override suspend fun load(params: LoadParams<Query>): LoadResult<Query, Journal> {
                return try {
                    val query = if (params.key == null) {
                        journalsCollection
                            .whereEqualTo("userId", userId)
                            .orderBy("date", Query.Direction.DESCENDING)
                            .limit(params.loadSize.toLong())
                    } else {
                        params.key!!.startAfter(params.key!!)
                    }

                    val result = query.get().await()
                    val data = result.documents.mapNotNull { mapToJournal(it) }

                    LoadResult.Page(
                        data = data,
                        prevKey = null,
                        nextKey = if (result.documents.isEmpty()) null else query
                    )
                } catch (e: Exception) {
                    LoadResult.Error(e)
                }
            }

            override fun getRefreshKey(state: PagingState<Query, Journal>): Query? = null
        }
    }

    private fun mapToJournal(doc: com.google.firebase.firestore.DocumentSnapshot): Journal? {
        val journalId = doc.getString("journalId").orEmpty()
        val title = doc.getString("title").orEmpty()
        val content = doc.getString("content").orEmpty()
        val dateValue = doc.get("date")
        val date = when (dateValue) {
            is com.google.firebase.Timestamp -> dateValue.toDate().time
            else -> System.currentTimeMillis()
        }
        val fetchedUserId = doc.getString("userId").orEmpty()

        return if (fetchedUserId.isNotEmpty()) {
            Journal(journalId, fetchedUserId, title, content, date)
        } else null
    }
}
