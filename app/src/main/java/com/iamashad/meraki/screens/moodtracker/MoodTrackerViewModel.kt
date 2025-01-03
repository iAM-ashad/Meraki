package com.iamashad.meraki.screens.moodtracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    init {
        fetchMoodTrend()
    }

    fun fetchMoodTrend() {
        viewModelScope.launch {
            moodRepository.getAllMoods().collect { moods: List<Mood> ->
                _moodTrend.value = moods.map {
                    val formattedDate =
                        android.text.format.DateFormat.format("MM-dd", it.timestamp).toString()
                    formattedDate to it.score
                }.sortedBy { it.first }
            }
        }
    }

    fun logMood(score: Int) {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            val mood = Mood(
                score = score,
                timestamp = currentTime
            )
            moodRepository.addMood(mood)
        }
    }
}
