package com.iamashad.meraki.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.model.Mood
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Repository for managing mood data in Firestore.
 * Provides methods to add and observe mood entries for the current user.
 */
class MoodRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    // Reference to the "moods" collection in Firestore
    private val moodsCollection = firestore.collection("moods")

    /**
     * Observes all mood entries for the currently logged-in user in real time.
     * Emits updates whenever the data changes.
     */
    fun getAllMoods() = callbackFlow {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // Listen to mood entries where userId matches current user's UID
            val listenerRegistration = moodsCollection
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("timestamp")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error) // Close the flow if an error occurs
                        return@addSnapshotListener
                    }

                    // Convert snapshot to a list of Mood objects
                    val moods = snapshot?.documents?.mapNotNull { it.toObject(Mood::class.java) }
                    trySend(moods ?: emptyList()) // Emit the list or empty if null
                }

            // Remove listener when the flow is closed
            awaitClose { listenerRegistration.remove() }
        } else {
            // If user is not authenticated, close the flow with an error
            close(IllegalStateException("User not logged in."))
        }
    }

    /**
     * Adds a new mood entry to the "moods" collection in Firestore.
     *
     * @param mood The Mood object to add.
     */
    suspend fun addMood(mood: Mood) {
        try {
            moodsCollection.add(mood).await()
        } catch (e: Exception) {
            throw e // Re-throw exception to handle it in ViewModel or use-case
        }
    }
}
