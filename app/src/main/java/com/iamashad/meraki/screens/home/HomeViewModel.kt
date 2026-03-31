package com.iamashad.meraki.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.model.MindfulNudge
import com.iamashad.meraki.repository.NudgeRepository
import com.iamashad.meraki.utils.MemoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val nudgeRepository: NudgeRepository,
    private val firestore: FirebaseFirestore,
    private val memoryManager: MemoryManager
) : ViewModel() {

    // StateFlow to hold a list of mindful nudges
    private val _nudges = MutableStateFlow<List<MindfulNudge>>(emptyList())
    val nudges: StateFlow<List<MindfulNudge>> = _nudges.asStateFlow()

    /**
     * The dominant emotion from the user's most recent chat session.
     * Defaults to "neutral" on first launch (no session history).
     * Drives the mood-aware UI tint on the Home screen.
     */
    private val _dominantEmotion = MutableStateFlow("neutral")
    val dominantEmotion: StateFlow<String> = _dominantEmotion.asStateFlow()

    // Date formatter used to handle log dates
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        // Automatically fetch initial nudges when ViewModel is created
        fetchInitialNudges()
        loadDominantEmotion()
    }

    /**
     * Reads the last session summary to seed the mood-aware UI tint.
     * Falls back to "neutral" when no prior session exists.
     */
    private fun loadDominantEmotion() {
        viewModelScope.launch {
            val summaries = memoryManager.getRecentSummaries()
            val emotion = summaries.firstOrNull()?.dominantEmotion ?: "neutral"
            _dominantEmotion.emit(emotion)
        }
    }

    private fun fetchInitialNudges() {
        viewModelScope.launch {
            try {
                val initialNudges = nudgeRepository.getInitialNudges()
                _nudges.emit(initialNudges)
            } catch (e: Exception) {
                println("Failed to fetch initial nudges: ${e.localizedMessage}")
            }
        }
    }

    fun fetchNextNudge() {
        viewModelScope.launch {
            try {
                val nextNudge = nudgeRepository.getNextNudge()
                _nudges.emit(_nudges.value + nextNudge)
            } catch (e: Exception) {
                println("Failed to fetch new nudge: ${e.localizedMessage}")
            }
        }
    }

    // Log user's daily usage in Firestore to help track streaks
    suspend fun logDailyUsage(userId: String) {
        try {
            val today = dateFormat.format(Date())
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("streakLogs")
                .whereEqualTo("date", today)
                .get()
                .await()

            // Log only if there is no entry for today
            if (snapshot.isEmpty) {
                val batch = firestore.batch()
                val logRef = firestore.collection("users")
                    .document(userId)
                    .collection("streakLogs")
                    .document()

                batch.set(logRef, mapOf("date" to today))
                batch.commit().await()
            }
        } catch (e: Exception) {
            println("Error logging daily usage: ${e.localizedMessage}")
        }
    }

    // Calculates the user's current streak by checking continuous logs
    suspend fun calculateStreak(userId: String): Int {
        return try {
            val today = dateFormat.parse(dateFormat.format(Date())) ?: return 0
            var streak = 0

            // Get all streak logs, parse them to Date, and sort by most recent
            val logs = firestore.collection("users")
                .document(userId)
                .collection("streakLogs")
                .get()
                .await()
                .documents
                .mapNotNull { dateFormat.parse(it.getString("date") ?: "") }
                .sortedDescending()

            // Compare each log date to expected date in streak chain
            var currentStreakDate = today
            for (logDate in logs) {
                if (dateFormat.format(logDate) == dateFormat.format(currentStreakDate)) {
                    streak++
                    // Move to previous day in streak
                    currentStreakDate = Calendar.getInstance().apply {
                        time = currentStreakDate
                        add(Calendar.DAY_OF_YEAR, -1)
                    }.time
                } else {
                    break // streak breaks if a date is missing
                }
            }
            streak
        } catch (e: Exception) {
            println("Error calculating streak: ${e.localizedMessage}")
            0 // Return zero if error occurs
        }
    }
}

