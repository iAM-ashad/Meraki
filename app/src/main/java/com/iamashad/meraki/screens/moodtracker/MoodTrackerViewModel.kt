package com.iamashad.meraki.screens.moodtracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.model.Mood
import com.iamashad.meraki.repository.MoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoodTrackerViewModel @Inject constructor(
    private val moodRepository: MoodRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _moodTrend = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val moodTrend: StateFlow<List<Pair<String, Int>>> = _moodTrend.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        fetchMoodTrend()
    }

    fun fetchMoodTrend() {
        viewModelScope.launch {
            _loading.emit(true)
            moodRepository.getAllMoods()
                .map { moods ->
                    moods.map { mood ->
                        android.text.format.DateFormat.format("MM-dd", mood.timestamp)
                            .toString() to mood.score
                    }.sortedBy { it.first }
                }
                .catch { throwable ->
                    // Handle errors gracefully
                    _moodTrend.emit(emptyList())
                    _loading.emit(false)
                }
                .collect { trend ->
                    _moodTrend.emit(trend)
                    _loading.emit(false)
                }
        }
    }

    fun logMood(score: Int) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            error("User not authenticated")
            return
        }

        viewModelScope.launch {
            val mood = Mood(
                score = score,
                timestamp = System.currentTimeMillis(),
                userId = currentUser.uid
            )
            try {
                moodRepository.addMood(mood)
            } catch (e: Exception) {
                error("Failed to log mood: ${e.message}")
            }
        }
    }
}
