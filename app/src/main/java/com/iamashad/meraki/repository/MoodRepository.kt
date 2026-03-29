package com.iamashad.meraki.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.di.IoDispatcher
import com.iamashad.meraki.model.Mood
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repository for managing mood data in Firestore.
 * Provides methods to add and observe mood entries for the current user.
 *
 * Phase 3: All operations are main-safe.
 * - addMood() is wrapped in withContext(ioDispatcher).
 * - getAllMoods() uses flowOn(ioDispatcher) so downstream collection
 *   and Firestore-to-Mood mapping run on the IO thread.
 * - ioDispatcher is injected via Hilt for testability.
 *
 * Note: @Inject constructor removed — this class is provided as a singleton
 * by NetworkModule.provideMoodRepository to avoid a duplicate binding.
 */
class MoodRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    // Reference to the "moods" collection in Firestore
    private val moodsCollection = firestore.collection("moods")

    /**
     * Observes all mood entries for the currently logged-in user in real time.
     * Emits updates whenever the data changes.
     * Phase 3: flowOn(ioDispatcher) ensures the producer runs on the IO thread.
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
    }.flowOn(ioDispatcher)

    /**
     * Adds a new mood entry to the "moods" collection in Firestore.
     * Phase 3: wrapped in withContext(ioDispatcher) for main-safety.
     */
    suspend fun addMood(mood: Mood) = withContext(ioDispatcher) {
        try {
            moodsCollection.add(mood).await()
        } catch (e: Exception) {
            throw e // Re-throw exception to handle it in ViewModel or use-case
        }
    }
}
