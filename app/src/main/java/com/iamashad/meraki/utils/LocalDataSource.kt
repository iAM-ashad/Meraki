package com.iamashad.meraki.utils

import androidx.compose.ui.graphics.Color

val emotionKeywords = mapOf(
    "calm" to listOf(
        "calm", "relaxed", "peaceful", "content", "serene", "chill", "at ease", "soothing"
    ), "stressed" to listOf(
        "stress",
        "stressful",
        "stressed",
        "overwhelmed",
        "pressure",
        "burnout",
        "exhausted",
        "tensed",
        "worried sick"
    ), "anxious" to listOf(
        "anxiety",
        "anxious",
        "nervous",
        "worried",
        "uneasy",
        "panicked",
        "restless",
        "on edge",
        "freaking out"
    ), "neutral" to listOf(
        "okay",
        "fine",
        "neutral",
        "indifferent",
        "alright",
        "meh",
        "so-so",
        "not much",
        "nothing special"
    ), "happy" to listOf(
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
    ), "sad" to listOf(
        "sad",
        "depressed",
        "upset",
        "down",
        "blue",
        "heartbroken",
        "low",
        "cry",
        "miserable",
        "hurt",
        "lonely",
        "crying",
        "bummed"
    ), "angry" to listOf(
        "angry",
        "mad",
        "frustrated",
        "annoyed",
        "irritated",
        "furious",
        "pissed",
        "outraged",
        "resentful",
        "hostile"
    )
)
val gradientMap = mapOf(
    "calm" to listOf(Color(0xFF0072A1), Color(0xFFA1DEFF)), // Calm blues
    "stressed" to listOf(Color(0xFFFFCCBC), Color(0xFFFF5722)), // Warm oranges
    "anxious" to listOf(Color(0xFFE1BEE7), Color(0xFF8E24AA)), // Gentle purples
    "neutral" to listOf(Color(0xFF006491), Color(0xFF9CDAFF)), // Default cool tones
    "happy" to listOf(Color(0xFFFFF176), Color(0xFFFFC107)), // Bright yellows
    "sad" to listOf(Color(0xFF202F42), Color(0xFFB0C8F3)), // Soft blues
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
    "Family",
    "Work",
    "Hobbies",
    "Weather",
    "Relationship",
    "Sleep",
    "Social",
    "Food",
    "Self-esteem",
    "Friends",
    "Health",
    "Career",
    "Exercise",
    "Finances",
    "Travel",
    "Academics",
    "Pets"
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
        "Calm" to 85,
        "Confused" to 40,
        "Surprised" to 60,
        "Amazed" to 85,
        "Peaceful" to 90,
        "Cool" to 75,
        "Stressed" to 20,
        "Angry" to 25,
        "Lonely" to 15,
        "Grateful" to 95,
        "Hopeful" to 80,
        "Tired" to 35,
        "Awkward" to 45
    )

    val totalScore = selectedEmotions.sumOf { emotionScores[it] ?: 50 }
    return totalScore / selectedEmotions.size
}

val moodTips = mapOf(
    "Happy" to "Celebrate your happiness by sharing it with a loved one!",
    "Sad" to "It's okay to feel sad. Try journaling your thoughts or listening to calming music.",
    "Excited" to "Channel your excitement into a creative project or plan something fun!",
    "Calm" to "Maintain your calmness by practicing mindfulness.",
    "Confused" to "Break the situation into smaller parts to understand it better.",
    "Surprised" to "Share your surprise with someone you trust, or reflect on what made the moment so unexpected.",
    "Amazed" to "Embrace your amazement by capturing the moment in a photo or writing about it.",
    "Peaceful" to "Extend your sense of peace by meditating or taking a gentle walk outside.",
    "Cool" to "Stay confident and enjoy the moment!",
    "Stressed" to "Take a deep breath, step away for a moment, and try a quick relaxation exercise.",
    "Angry" to "Channel your anger into something constructive like exercise or writing down your feelings.",
    "Lonely" to "Reach out to a friend or family member. Sometimes a small connection makes a big difference.",
    "Grateful" to "Write down what you're thankful for and share your gratitude with those around you.",
    "Hopeful" to "Fuel your hope by setting a small, achievable goal to take a step toward your dreams.",
    "Tired" to "Prioritize rest. A short nap or a good night's sleep can recharge your energy.",
    "Awkward" to "Laugh it off! Awkward moments are a part of life. Reflect on the humor and move forward."
)


fun getTipForMood(mood: String): String {
    return moodTips[mood] ?: "Take some time to care for yourself."
}

val daysOfWeek = listOf(
    "Mon",
    "Tue",
    "Wed",
    "Thu",
    "Fri",
    "Sat",
    "Sun"
)

/**
 * Mood-Aware UI: very soft pastel gradient stops for the Home screen background.
 *
 * These are deliberately desaturated and light — the goal is a gentle "color
 * temperature" shift rather than a bold change. Each pair maps to
 * [topColor, bottomColor] that replaces the fixed white/purple gradient on
 * HomeScreen when the user has a recorded dominant emotion from their last
 * chat session.
 *
 * Design rationale:
 *  - Negative emotions (anxious, sad, stressed, angry) → cool or muted tones that
 *    feel calm and contained — not alarming, just gently reflective.
 *  - Positive emotions (happy, calm) → warm or airy tones that feel open and energetic.
 *  - Neutral → the existing app default (white tint, unchanged feel).
 */
val homeMoodTintMap: Map<String, Pair<Color, Color>> = mapOf(
    "happy"   to (Color(0xFFFFFDE7) to Color(0xFFF3E5F5)), // soft lemon → lavender
    "calm"    to (Color(0xFFE1F5FE) to Color(0xFFEDE7F6)), // pale sky → pale lavender
    "anxious" to (Color(0xFFEDE7F6) to Color(0xFFE8EAF6)), // soft lavender → indigo tint
    "sad"     to (Color(0xFFE3F2FD) to Color(0xFFECEFF1)), // light blue → blue-grey
    "stressed" to (Color(0xFFFFF3E0) to Color(0xFFFCE4EC)), // pale peach → pale pink
    "angry"   to (Color(0xFFFCE4EC) to Color(0xFFE8EAF6)), // blush → cool lavender (calming contrast)
    "neutral" to (Color(0xFFFFFFFF) to Color(0xFFF3E5F5))  // white → default lavender
)

/**
 * Returns the mood-aware home tint color pair for [emotion], falling back to neutral.
 */
fun getHomeMoodTint(emotion: String): Pair<Color, Color> =
    homeMoodTintMap[emotion] ?: homeMoodTintMap["neutral"]!!

/**
 * Mood-Aware UI: subtext shown in the MoodPromptCard on the Home screen.
 *
 * For negative emotions the copy is softer and lower-pressure — "no rush,
 * just check in". For positive emotions it's lighter and more curious.
 */
fun getMoodPromptSubtext(emotion: String): String = when (emotion) {
    "sad", "anxious", "stressed" ->
        "No pressure — just a gentle check-in whenever you're ready."
    "angry" ->
        "Take a breath. Whenever you're ready, we're here."
    "happy", "calm" ->
        "You've been doing well — keep the momentum going!"
    else ->
        "Take a moment to reflect — it only takes a second."
}



