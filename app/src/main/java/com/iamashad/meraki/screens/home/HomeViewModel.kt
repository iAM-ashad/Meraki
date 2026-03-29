package com.iamashad.meraki.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.repository.QuotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
    private val quotesRepository: QuotesRepository, // Repository to fetch quotes
    private val firestore: FirebaseFirestore // Firestore instance for streak logging
) : ViewModel() {

    // StateFlow to hold a list of quotes (quote -> author)
    private val _quotes = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val quotes: StateFlow<List<Pair<String, String>>> = _quotes.asStateFlow()

    // Optional author field (not used currently)
    private val _author = MutableStateFlow("Anonymous")
    val author: StateFlow<String> = _author.asStateFlow()

    // Date formatter used to handle log dates
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        // Automatically fetch quotes when ViewModel is created
        fetchInitialQuotes()
    }

    // Fetches 5 quotes concurrently and stores them in state
    private fun fetchInitialQuotes() {
        viewModelScope.launch {
            try {
                val response = (1..5).map {
                    async { quotesRepository.getRandomQuote() }
                }.awaitAll()

                val initialQuotes = response.map { it.quote to it.author }
                _quotes.emit(initialQuotes)
            } catch (e: Exception) {
                println("Failed to fetch initial quotes: ${e.localizedMessage}")
            }
        }
    }

    // Fetch a single quote and append it to the current list
    fun fetchSingleQuote() {
        viewModelScope.launch {
            try {
                val newQuote = quotesRepository.getRandomQuote()
                _quotes.emit(_quotes.value + (newQuote.quote to newQuote.author))
            } catch (e: Exception) {
                println("Failed to fetch new quote: ${e.localizedMessage}")
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

