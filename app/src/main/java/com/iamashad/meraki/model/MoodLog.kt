package com.iamashad.meraki.model

data class MoodLog(
    val userId: String = "",
    val moodScore: Int = 0,
    val moodLabel: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

