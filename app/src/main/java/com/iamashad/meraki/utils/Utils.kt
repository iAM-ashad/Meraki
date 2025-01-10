package com.iamashad.meraki.utils

import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlin.math.roundToInt

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

fun getMoodEmoji(score: Int): String {
    return when (score) {
        in 0..10 -> "😡"
        in 11..20 -> "😞"
        in 21..30 -> "😔"
        in 31..40 -> "😟"
        in 41..50 -> "😐"
        in 51..60 -> "🙂"
        in 61..70 -> "😊"
        in 71..80 -> "😃"
        in 81..90 -> "😄"
        in 91..100 -> "😍"
        else -> "😶"
    }
}

fun getMoodColor(score: Int): Color {
    return when (score) {
        in 0..39 -> Color(227, 56, 0, 255)
        in 40..60 -> Color(222, 202, 43, 255)
        else -> Color(60, 187, 65, 255)
    }
}

fun provideGenerativeModel(apiKey: String): GenerativeModel {
    return GenerativeModel(
        modelName = "gemini-1.5-flash",
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

fun getMoodLabel(score: Int): String {
    return when (score) {
        in 0..10 -> "Abysmal"
        in 11..20 -> "Terrible"
        in 21..30 -> "Very Bad"
        in 31..40 -> "Bad"
        in 41..50 -> "Below Average"
        in 51..60 -> "Average"
        in 61..70 -> "Good"
        in 71..80 -> "Great"
        in 81..90 -> "Amazing"
        in 91..100 -> "Ecstatic"
        else -> "Unknown"
    }
}

fun calculateMoodChange(moodTrend: List<Pair<String, Int>>, entryCount: Int): Int? {
    if (moodTrend.size < entryCount) return null
    val recentMoodTrend = moodTrend.takeLast(entryCount)
    val firstMood = recentMoodTrend.first().second
    val lastMood = recentMoodTrend.last().second

    return if (firstMood == 0) {
        null
    } else {
        ((lastMood - firstMood).toDouble() / firstMood * 100).roundToInt()
    }
}
