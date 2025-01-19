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
    private val repository: FirestoreRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _moodInsights = MutableStateFlow<MoodInsightsAnalyzer.MoodInsights?>(null)
    val moodInsights: StateFlow<MoodInsightsAnalyzer.MoodInsights?> = _moodInsights.asStateFlow()

    init {
        fetchMoodInsights()
    }

    fun fetchMoodInsights() {
        val userId = firebaseAuth.currentUser?.uid.orEmpty()

        viewModelScope.launch {
            try {
                val journals = repository.getAllJournals(userId)
                val insights = MoodInsightsAnalyzer.calculateMoodTrends(journals)
                _moodInsights.emit(insights)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
