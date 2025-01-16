package com.iamashad.meraki.screens.insights

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.repository.FirestoreRepository
import com.iamashad.meraki.utils.MoodInsightsAnalyzer
import kotlinx.coroutines.launch

class InsightsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FirestoreRepository()
    private val userId: String = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    private val _moodInsights = MutableLiveData<MoodInsightsAnalyzer.MoodInsights>()
    val moodInsights: LiveData<MoodInsightsAnalyzer.MoodInsights> = _moodInsights

    fun fetchMoodInsights() {
        viewModelScope.launch {
            val journals = repository.getAllJournals(userId)
            val insights = MoodInsightsAnalyzer.calculateMoodTrends(journals)
            _moodInsights.postValue(insights)
        }
    }
}
