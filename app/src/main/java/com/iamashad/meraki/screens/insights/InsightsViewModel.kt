package com.iamashad.meraki.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.repository.FirestoreRepository
import com.iamashad.meraki.utils.MoodInsightsAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val repository: FirestoreRepository,   // Repository to access Firestore journal data
    private val firebaseAuth: FirebaseAuth          // Firebase authentication for current user
) : ViewModel() {

    // Mutable state flow to hold calculated mood insights
    private val _moodInsights = MutableStateFlow<MoodInsightsAnalyzer.MoodInsights?>(null)

    // Publicly exposed immutable state flow
    val moodInsights: StateFlow<MoodInsightsAnalyzer.MoodInsights?> = _moodInsights.asStateFlow()

    init {
        // Automatically fetch insights on ViewModel initialization
        fetchMoodInsights()
    }

    /**
     * Fetches all mood journal entries for the logged-in user
     * and processes them through the MoodInsightsAnalyzer to
     * extract trends and meaningful statistics.
     */
    fun fetchMoodInsights() {
        // Get the current user's UID from Firebase
        val userId = firebaseAuth.currentUser?.uid.orEmpty()

        viewModelScope.launch {
            try {
                // Retrieve all mood journals from Firestore
                val journals = repository.getAllJournals(userId)

                // Analyze trends using the helper utility
                val insights = MoodInsightsAnalyzer.calculateMoodTrends(journals)

                // Emit the result to observers
                _moodInsights.emit(insights)
            } catch (e: Exception) {
                // Log or handle any errors during fetch or analysis
                e.printStackTrace()
            }
        }
    }
}

