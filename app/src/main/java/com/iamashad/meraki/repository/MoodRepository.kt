package com.iamashad.meraki.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.model.Mood
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MoodRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val moodsCollection = firestore.collection("moods")

    fun getAllMoods() = callbackFlow {
        val listenerRegistration = moodsCollection
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val moods = snapshot?.documents?.mapNotNull { it.toObject(Mood::class.java) }
                trySend(moods ?: emptyList())
            }
        awaitClose { listenerRegistration.remove() }
    }

    // Add a new mood
    suspend fun addMood(mood: Mood) {
        try {
            moodsCollection.add(mood).await()
        } catch (e: Exception) {
            throw e
        }
    }
}
