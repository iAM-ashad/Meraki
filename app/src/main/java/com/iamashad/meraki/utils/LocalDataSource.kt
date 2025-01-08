package com.iamashad.meraki.utils

import androidx.compose.ui.graphics.Color

val emotionKeywords = mapOf(
    "calm" to listOf(
        "calm", "relaxed", "peaceful", "content", "serene", "chill", "at ease", "soothing"
    ),
    "stressed" to listOf(
        "stress",
        "stressed",
        "overwhelmed",
        "pressure",
        "burnout",
        "exhausted",
        "tense",
        "worried sick"
    ),
    "anxious" to listOf(
        "anxiety",
        "anxious",
        "nervous",
        "worried",
        "uneasy",
        "panicked",
        "restless",
        "on edge",
        "freaking out"
    ),
    "neutral" to listOf(
        "okay",
        "fine",
        "neutral",
        "indifferent",
        "alright",
        "meh",
        "so-so",
        "not much",
        "nothing special"
    ),
    "happy" to listOf(
        "happy",
        "joyful",
        "excited",
        "pleased",
        "grateful",
        "content",
        "blessed",
        "cheerful",
        "good",
        "thrilled",
        "ecstatic",
        "glad",
        "elated"
    ),
    "sad" to listOf(
        "sad",
        "depressed",
        "upset",
        "down",
        "blue",
        "heartbroken",
        "low",
        "teary",
        "miserable",
        "hurt",
        "lonely",
        "crying",
        "bummed"
    ),
    "angry" to listOf(
        "angry",
        "mad",
        "frustrated",
        "annoyed",
        "irritated",
        "furious",
        "pissed",
        "outraged",
        "resentful",
        "upset",
        "hostile"
    )
)
val gradientMap = mapOf(
    "calm" to listOf(Color(0xFFB3E5FC), Color(0xFF0288D1)), // Calm blues
    "stressed" to listOf(Color(0xFFFFCCBC), Color(0xFFFF5722)), // Warm oranges
    "anxious" to listOf(Color(0xFFE1BEE7), Color(0xFF8E24AA)), // Gentle purples
    "neutral" to listOf(Color(0xFFE1F5FE), Color(0xFF0277BD)), // Default cool tones
    "happy" to listOf(Color(0xFFFFF176), Color(0xFFFFC107)), // Bright yellows
    "sad" to listOf(Color(0xFF90CAF9), Color(0xFF1E88E5)), // Soft blues
    "angry" to listOf(Color(0xFFFF8A80), Color(0xFFD32F2F)) // Intense reds
)

val allEmotions = listOf(
    "Happy" to "😊",
    "Sad" to "😢",
    "Excited" to "🤩",
    "Calm" to "😌",
    "Confused" to "😕",
    "Surprised" to "😲",
    "Amazed" to "😮",
    "Peaceful" to "🕊️",
    "Cool" to "😎",
    "Stressed" to "😣",
    "Angry" to "😡",
    "Lonely" to "🥺",
    "Grateful" to "🙏",
    "Hopeful" to "🌟",
    "Tired" to "😴",
    "Awkward" to "😅"
)

val commonlyUsedEmotions = listOf(
    "Confused" to "😕",
    "Excited" to "🤩",
    "Cool" to "😎",
    "Surprised" to "😲",
    "Peaceful" to "🕊️",
    "Amazed" to "😮"
)

val allReasons = listOf(
    "Family", "Work", "Hobbies", "Weather", "Love", "Sleep", "Breakup", "Social",
    "Food", "Party", "Self-esteem", "Wife", "Friends", "Health", "Career", "Exercise"
)

val commonlyUsedReasons = listOf(
    "Family", "Self-esteem", "Sleep", "Social"
)

fun calculateMoodScore(selectedEmotions: List<String>): Int {
    if (selectedEmotions.isEmpty()) return 50

    val emotionScores = mapOf(
        "Happy" to 90,
        "Sad" to 25,
        "Excited" to 80,
        "Calm" to 70,
        "Confused" to 40,
        "Surprised" to 50,
        "Amazed" to 85,
        "Peaceful" to 75,
        "Cool" to 60,
        "Stressed" to 30,
        "Angry" to 20,
        "Lonely" to 35,
        "Grateful" to 95,
        "Hopeful" to 80,
        "Tired" to 40,
        "Awkward" to 45
    )

    val totalScore = selectedEmotions.sumOf { emotionScores[it] ?: 50 }
    return totalScore / selectedEmotions.size
}




