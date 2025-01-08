package com.iamashad.meraki.utils

import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

@Composable
fun LoadImageWithGlide(
    imageScale: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP,
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    AndroidView(factory = { context ->
        ImageView(context).apply {
            scaleType = imageScale
        }
    }, modifier = modifier, update = { imageView ->
        // Use Glide to load the image
        Glide.with(imageView.context).load(imageUrl).into(imageView)
    })
}

fun getMoodScore(emotions: List<String>): Int {
    val emotionToScore = mapOf(
        "Happy" to 90,
        "Excited" to 80,
        "Calm" to 70,
        "Surprised" to 50,
        "Confused" to 40,
        "Sad" to 25
    )

    val scores = emotions.mapNotNull { emotionToScore[it] }
    return if (scores.isNotEmpty()) scores.average().toInt() else 50 // Default to neutral
}

fun getMoodEmoji(score: Int): String {
    return when (score) {
        in 0..10 -> "😡" // Angry face
        in 11..20 -> "😞" // Sad face
        in 21..30 -> "😔" // Pensive face
        in 31..40 -> "😟" // Worried face
        in 41..50 -> "😐" // Neutral face
        in 51..60 -> "🙂" // Slightly smiling face
        in 61..70 -> "😊" // Smiling face
        in 71..80 -> "😃" // Big smile
        in 81..90 -> "😄" // Grinning face
        in 91..100 -> "😍" // Heart eyes
        else -> "😶" // Blank face
    }
}

// Function to get the appropriate color for a mood score
fun getMoodColor(score: Int): Color {
    return when (score) {
        in 0..39 -> Color(227, 56, 0, 255)
        in 40..60 -> Color(222, 202, 43, 255)
        else -> Color(60, 187, 65, 255)
    }
}

fun provideGenerativeModel(apiKey: String): GenerativeModel {
    return GenerativeModel(modelName = "gemini-2.0-flash-exp",
        apiKey = apiKey,
        systemInstruction = content {
            text(getSystemInstructions())
        })
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

fun getMoodEmoji(title: String): String {
    return when {
        title.contains("Happy", true) -> "😊"
        title.contains("Sad", true) -> "😢"
        title.contains("Excited", true) -> "🤩"
        title.contains("Calm", true) -> "😌"
        title.contains("Confused", true) -> "😕"
        title.contains("Surprised", true) -> "😲"
        title.contains("Amazed", true) -> "😮"
        title.contains("Peaceful", true) -> "🕊️"
        title.contains("Cool", true) -> "😎"
        title.contains("Stressed", true) -> "😣"
        title.contains("Angry", true) -> "😡"
        title.contains("Lonely", true) -> "🥺"
        title.contains("Grateful", true) -> "🙏"
        title.contains("Hopeful", true) -> "🌟"
        title.contains("Tired", true) -> "😴"
        title.contains("Awkward", true) -> "😅"
        else -> "😶"
    }
}

fun getMoodLabelFromTitle(title: String): String {
    return when {
        title.contains("Happy", true) -> "Good"
        title.contains("Sad", true) -> "Bad"
        title.contains("Excited", true) -> "Excited"
        title.contains("Calm", true) -> "Calm"
        title.contains("Confused", true) -> "Confused"
        title.contains("Surprised", true) -> "Surprised"
        title.contains("Amazed", true) -> "Amazed"
        title.contains("Peaceful", true) -> "Peaceful"
        title.contains("Cool", true) -> "Cool"
        title.contains("Stressed", true) -> "Stressed"
        title.contains("Angry", true) -> "Angry"
        title.contains("Lonely", true) -> "Lonely"
        title.contains("Grateful", true) -> "Grateful"
        title.contains("Hopeful", true) -> "Hopeful"
        title.contains("Tired", true) -> "Tired"
        title.contains("Awkward", true) -> "Awkward"
        else -> "Unknown"
    }
}


