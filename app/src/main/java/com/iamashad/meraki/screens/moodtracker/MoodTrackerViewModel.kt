package com.iamashad.meraki.screens.moodtracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.model.Mood
import com.iamashad.meraki.repository.MoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoodTrackerViewModel @Inject constructor(
    private val moodRepository: MoodRepository
) : ViewModel() {

    private val _moodTrend = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val moodTrend: StateFlow<List<Pair<String, Int>>> get() = _moodTrend

    private val _loading = MutableStateFlow(true) // New loading state
    val loading: StateFlow<Boolean> = _loading

    init {
        fetchMoodTrend()
    }

    fun fetchMoodTrend() {
        viewModelScope.launch {
            _loading.value = true // Set loading to true before fetching data
            moodRepository.getAllMoods().collect { moods ->
                _moodTrend.value = moods.map {
                    val formattedDate = android.text.format.DateFormat.format("MM-dd", it.timestamp).toString()
                    formattedDate to it.score
                }.sortedBy { it.first }
                _loading.value = false // Set loading to false after data is fetched
            }
        }
    }

    fun logMood(score: Int) {
        viewModelScope.launch {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val currentTime = System.currentTimeMillis()
                val mood = Mood(
                    score = score,
                    timestamp = currentTime,
                    userId = currentUser.uid // Bind the entry to the current user's UID
                )
                moodRepository.addMood(mood)
            } else {
                error("User not authenticated").stackTrace
            }
        }
    }

}
