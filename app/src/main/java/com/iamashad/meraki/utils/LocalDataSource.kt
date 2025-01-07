package com.iamashad.meraki.utils

import androidx.compose.ui.graphics.Color
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

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

fun provideGenerativeModel(apiKey: String): GenerativeModel {
    return GenerativeModel(
        modelName = "gemini-2.0-flash-exp",
        apiKey = apiKey,
        systemInstruction = content {
            text(getSystemInstructions())
        }
    )
}

fun analyzeEmotion(input: String): String {
    for ((category, keywords) in emotionKeywords) {
        if (keywords.any { keyword -> input.contains(keyword, ignoreCase = true) }) {
            return category
        }
    }
    return "neutral"
}

fun getSystemInstructions(): String {
    return """
        You are a professional mental health assistant trained to provide empathetic, supportive, 
        and non-judgmental responses. Act as a compassionate therapist, focusing on the user's 
        emotions and concerns.
    """.trimIndent()
}

