package com.iamashad.meraki.screens.moodtracker

import androidx.lifecycle.ViewModel
import com.iamashad.meraki.model.Mood
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MoodTrackerViewModel @Inject constructor() : ViewModel() {

    val moods = listOf(
        Mood("Happy", "😊") {},
        Mood("Sad", "😢") {},
        Mood("Anxious", "😰") {},
        Mood("Calm", "😌") {},
        Mood("Excited", "🤩") {}
    )
}
