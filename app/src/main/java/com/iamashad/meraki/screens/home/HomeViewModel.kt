package com.iamashad.meraki.screens.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
    private val quotesRepository: QuotesRepository,
    firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore // Injected Firestore instance
) : ViewModel() {

    private val _quotes = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val quotes: StateFlow<List<Pair<String, String>>> = _quotes.asStateFlow()

    private val _author = MutableStateFlow("Anonymous")
    val author: StateFlow<String> = _author.asStateFlow()

    private val _user = MutableStateFlow(firebaseAuth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _photoUrl = MutableStateFlow(firebaseAuth.currentUser?.photoUrl)
    val photoUrl: StateFlow<Uri?> = _photoUrl.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())


    init {
        fetchInitialQuotes()
    }

    private fun fetchInitialQuotes() {
        viewModelScope.launch {
            try {
                // Fetch 5 initial quotes
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

    fun fetchSingleQuote() {
        viewModelScope.launch {
            try {
                // Fetch a single new quote
                val newQuote = quotesRepository.getRandomQuote()
                _quotes.emit(_quotes.value + (newQuote.quote to newQuote.author)) // Append the new quote
            } catch (e: Exception) {
                println("Failed to fetch new quote: ${e.localizedMessage}")
            }
        }
    }

    suspend fun logDailyUsage(userId: String) {
        try {
            val today = dateFormat.format(Date())
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("streakLogs")
                .whereEqualTo("date", today)
                .get()
                .await()

            if (snapshot.isEmpty) {
                val batch = firestore.batch()
                val logRef = firestore.collection("users").document(userId).collection("streakLogs")
                    .document()

                batch.set(logRef, mapOf("date" to today))
                batch.commit().await()
            }
        } catch (e: Exception) {
            println("Error logging daily usage: ${e.localizedMessage}")
        }
    }

    suspend fun calculateStreak(userId: String): Int {
        return try {
            val today = dateFormat.parse(dateFormat.format(Date())) ?: return 0
            var streak = 0

            val logs = firestore.collection("users")
                .document(userId)
                .collection("streakLogs")
                .get()
                .await()
                .documents
                .mapNotNull { dateFormat.parse(it.getString("date") ?: "") }
                .sortedDescending()

            var currentStreakDate = today
            for (logDate in logs) {
                if (dateFormat.format(logDate) == dateFormat.format(currentStreakDate)) {
                    streak++
                    currentStreakDate = Calendar.getInstance().apply {
                        time = currentStreakDate
                        add(Calendar.DAY_OF_YEAR, -1)
                    }.time
                } else {
                    break
                }
            }
            streak
        } catch (e: Exception) {
            println("Error calculating streak: ${e.localizedMessage}")
            0
        }
    }
}

