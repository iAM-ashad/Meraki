package com.iamashad.meraki.utils

import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.iamashad.meraki.R
import com.iamashad.meraki.model.Journal
import kotlin.math.roundToInt

@Composable
fun LoadImageWithGlide(
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                adjustViewBounds = true // Allow dynamic scaling
                scaleType = ImageView.ScaleType.FIT_CENTER // Keep the aspect ratio intact
            }
        },
        modifier = modifier,
        update = { imageView ->
            Glide.with(imageView.context)
                .load(imageUrl)
                .override(800, 600) // Limit size to prevent rendering issues
                .placeholder(android.R.drawable.ic_menu_gallery) // Placeholder image
                .error(android.R.drawable.stat_notify_error) // Fallback in case of an error
                .into(imageView)
        }
    )
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
    if (moodTrend.isEmpty()) return null // Handle empty trend
    val recentMoodTrend = moodTrend.takeLast(entryCount)
    if (recentMoodTrend.size < 2) return null // At least two entries are needed for calculation

    val firstMood = recentMoodTrend.first().second
    val lastMood = recentMoodTrend.last().second

    return if (firstMood == 0) {
        null
    } else {
        ((lastMood - firstMood).toDouble() / firstMood * 100).roundToInt()
    }
}

object MoodInsightsAnalyzer {

    fun calculateMoodTrends(journals: List<Journal>): MoodInsights {
        // Ensure only current journal entries are considered
        val validJournals =
            journals.filter { it.moodScore != 0 } // Filter out irrelevant entries if needed

        // Calculate the overall average mood from valid journals
        val overallAverageMood = validJournals.map { it.moodScore }.average()

        // Analyze reasons and their impact on mood
        val reasonsInsights = validJournals
            .flatMap { journal -> journal.reasons.map { reason -> reason to journal.moodScore } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, scores) ->
                val avgMood = scores.average()
                MoodDeviation(
                    averageMood = avgMood,
                    deviation = avgMood - overallAverageMood, // Calculate deviation from current average
                    entriesCount = scores.size
                )
            }

        // Analyze temporal trends (group by day)
        val temporalTrends = validJournals
            .groupBy { it.date.toDay() }
            .mapValues { (_, entries) -> entries.map { it.moodScore }.average() }

        // Calculate daily deviations based on temporal trends
        val dailyDeviations = calculateDailyDeviations(validJournals, temporalTrends)

        return MoodInsights(
            overallAverageMood = overallAverageMood,
            reasonsAnalysis = reasonsInsights,
            temporalTrends = temporalTrends,
            dailyDeviations = dailyDeviations
        )
    }

    private fun calculateDailyDeviations(
        journals: List<Journal>,
        temporalTrends: Map<String, Double>
    ): Map<String, DailyDeviation> {
        val sortedDates = temporalTrends.keys.sorted() // Ensure chronological order
        val dateToIndex = sortedDates.withIndex().associate { it.value to it.index }

        return temporalTrends.map { (date, avgMood) ->
            val index = dateToIndex[date] ?: return@map date to DailyDeviation()
            val dayBeforeMood = sortedDates.getOrNull(index - 1)?.let { temporalTrends[it] }
            val dayAfterMood = sortedDates.getOrNull(index + 1)?.let { temporalTrends[it] }

            date to DailyDeviation(
                dayBefore = dayBeforeMood?.let { (it - avgMood).toInt() },
                sameDay = avgMood.toInt(),
                dayAfter = dayAfterMood?.let { (it - avgMood).toInt() }
            )
        }.toMap()
    }

    data class MoodInsights(
        val overallAverageMood: Double,
        val reasonsAnalysis: Map<String, MoodDeviation>,
        val temporalTrends: Map<String, Double>,
        val dailyDeviations: Map<String, DailyDeviation> // Added field
    )

    data class MoodDeviation(
        val averageMood: Double,
        val deviation: Double,
        val entriesCount: Int // Added field
    )

    data class DailyDeviation(
        val dayBefore: Int? = null,
        val sameDay: Int? = null,
        val dayAfter: Int? = null
    )

    private fun Long.toDay(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(this))
    }
}


fun getReasonIcon(reason: String): Int {
    return when (reason) {
        "Family" -> R.drawable.img_family
        "Work" -> R.drawable.img_work
        "Hobbies" -> R.drawable.img_hobbies
        "Weather" -> R.drawable.img_weather
        "Relationship" -> R.drawable.img_relationships
        "Sleep" -> R.drawable.img_sleep
        "Social Life" -> R.drawable.img_social
        "Food" -> R.drawable.img_food
        "Self-esteem" -> R.drawable.img_selfesteem
        "Friends" -> R.drawable.img_friends
        "Health" -> R.drawable.img_health
        "Career" -> R.drawable.img_career
        "Exercise" -> R.drawable.img_exercise
        "Finances" -> R.drawable.img_finances
        "Travel" -> R.drawable.img_travel
        "Academics" -> R.drawable.img_academics
        "Pets" -> R.drawable.img_pets
        else -> R.drawable.img_decrease
    }
}

