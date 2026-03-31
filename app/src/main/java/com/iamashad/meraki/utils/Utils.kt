package com.iamashad.meraki.utils

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import android.widget.ImageView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.iamashad.meraki.R
import com.iamashad.meraki.model.EmotionCategory
import com.iamashad.meraki.model.EmotionIntensity
import com.iamashad.meraki.model.Journal
import com.iamashad.meraki.model.Message
import com.iamashad.meraki.notifications.NotificationWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class ConnectivityStatus(context: Context) : LiveData<Boolean>() {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            postValue(true) // Internet is available
        }

        override fun onLost(network: Network) {
            postValue(false) // Internet is lost
        }
    }

    override fun onActive() {
        super.onActive()
        // Register for network changes
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        // Set initial value
        postValue(checkCurrentNetwork())
    }

    override fun onInactive() {
        super.onInactive()
        // Unregister the network callback
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    private fun checkCurrentNetwork(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

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

fun analyzeEmotion(input: String): String {
    for ((category, keywords) in emotionKeywords) {
        if (keywords.any { keyword -> input.contains(keyword, ignoreCase = true) }) {
            return category
        }
    }
    return "neutral"
}

/**
 * Phase 3.5 / Phase 4: Conversational system prompt for the compassionate-companion persona.
 *
 * Prompt budget: ~400 tokens (within the 400-token SYSTEM_PROMPT_RESERVE
 * defined in [ContextConfig]).
 *
 * Rewritten in a natural, example-driven style so the model infers the target
 * tone from demonstrations rather than mechanical rule-following.
 *
 * Phase 3.5 — teaches the model how to interpret [Context: Emotion=X, Intensity=Y]
 * tags injected by ChatViewModel.
 *
 * Phase 4 — teaches the model how to interpret the [Session context: ...] entry
 * injected into the conversation history by ChatViewModel for returning users.
 */
fun getSystemInstructions(): String {
    return """
        You are Meraki — a warm, caring companion who helps people through their emotional lives.
        You're not a therapist or advisor. Think of yourself as a deeply empathetic friend who
        genuinely listens, reflects back what they hear, and makes the person feel truly understood.

        Keep responses short and human — 2 to 4 sentences as a rule. Never use lists or headers.
        Your first instinct is always to meet the person in their feeling before anything else.
        When someone is in pain, slow down and be present with them. No silver linings, no rushing
        toward solutions — just honest, unhurried companionship.

        Here's the tone you're going for:

        User: "I've been so overwhelmed lately, like I can't keep up with anything."
        You: "That sounds exhausting — like you're carrying more than feels possible right now.
        What's been weighing on you the most?"

        User: "I don't know, I just feel kind of empty."
        You: "That kind of emptiness can be really disorienting — like you're going through
        the motions but nothing quite lands. How long has it been feeling this way?"

        User: "I finally told my friend how I'd been feeling and it went really well."
        You: "That takes real courage — opening up like that isn't easy. It sounds like
        it meant a lot to be heard."

        When the conversation has history, acknowledge recurring themes naturally:
        "It sounds like this is still sitting with you" or "You've touched on this before."
        Only do this when it genuinely fits — never force continuity.

        Never use clinical or diagnostic language: no symptoms, triggers, disorder, episode,
        condition, treatment, or anything that sounds like a medical assessment. Speak plainly,
        like a caring human being.

        Each user message may include a hidden tag like [Context: Emotion=ANXIOUS, Intensity=HIGH].
        Use it silently to calibrate your warmth and pacing — never mention or echo it.
        Low intensity: stay light and curious. Medium: warmer and more reflective.
        High: be deeply present, lead with comfort, say less, mean more.

        If someone expresses thoughts of self-harm or ending their life, gently validate
        their pain and encourage them to reach out to someone they trust or a crisis line.
        Don't minimise it or change the subject.

        At the start of returning-user sessions, the history may begin with a hidden
        [Session context: ...] message summarising past themes. Use it silently to feel
        more familiar and continuous — never quote or reference it directly.
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
    if (moodTrend.isEmpty()) return null
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
        "Social" -> R.drawable.img_social
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

fun scheduleDailyReminderAt(context: Context, time: String) {
    val (hour, minute) = time.split(":").map { it.toInt() }
    val now = Calendar.getInstance()
    val scheduledTime = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
    }

    // If the scheduled time is in the past, schedule for the next day
    if (scheduledTime.before(now)) {
        scheduledTime.add(Calendar.DAY_OF_YEAR, 1)
    }

    // Calculate initial delay for the first notification
    val initialDelay = scheduledTime.timeInMillis - now.timeInMillis
    Log.d("Scheduler", "Initial delay for the first notification: $initialDelay ms")

    // Create a PeriodicWorkRequest with an interval of 24 hours
    val workRequest = PeriodicWorkRequest.Builder(
        NotificationWorker::class.java,
        1, TimeUnit.DAYS // Repeat every 24 hours
    )
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS) // Delay before the first run
        .build()

    // Enqueue unique work, replacing any existing reminder
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "daily_reminder", // Unique work name
        ExistingPeriodicWorkPolicy.REPLACE, // Replace if already scheduled
        workRequest
    )

    Log.d("Scheduler", "Scheduled a new daily reminder at $time")
    Log.d("NewScheduler", "Replaced any existing reminders with the new timing: $time")
}

fun openNotificationSettings(context: Context) {
    val intent = Intent().apply {
        action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    context.startActivity(intent)
}

fun isNotificationChannelEnabled(context: Context, channelId: String): Boolean {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = notificationManager.getNotificationChannel(channelId)
    return channel?.importance != NotificationManager.IMPORTANCE_NONE
    return true
}

// ─── Phase 3: Intensity-Aware Gradient Mapping ────────────────────────────────

/**
 * Returns a two-stop gradient [Color] pair for a given [EmotionCategory] and
 * [EmotionIntensity] combination, giving the chat background richer visual nuance
 * compared to the single-entry [gradientMap] (which is kept for backward compat).
 *
 * Design rationale:
 *  - LOW intensity   → desaturated / pastel tones (subtle shift, minimal distraction)
 *  - MEDIUM intensity → the same palette as the legacy gradientMap baseline
 *  - HIGH intensity  → saturated / deep tones (strong visual signal, e.g. deep indigo
 *                       for high-anxiety as specified in the Phase 3 acceptance criteria)
 *
 * Falls back to the NEUTRAL MEDIUM gradient for any unmapped combination.
 */
fun getEmotionGradient(category: EmotionCategory, intensity: EmotionIntensity): List<Color> {
    return emotionGradientTable[category]?.get(intensity)
        ?: emotionGradientTable[EmotionCategory.NEUTRAL]!![EmotionIntensity.MEDIUM]!!
}

/**
 * Full intensity-keyed gradient table.
 * Outer key: [EmotionCategory]; inner key: [EmotionIntensity].
 */
private val emotionGradientTable: Map<EmotionCategory, Map<EmotionIntensity, List<Color>>> = mapOf(

    // ── ANXIOUS ──────────────────────────────────────────────────────────────
    // LOW  : soft lavender tint — barely perceptible unease
    // MED  : medium purple (matches legacy gradientMap "anxious" baseline)
    // HIGH : deep indigo / near-black violet — as specified in acceptance criteria
    EmotionCategory.ANXIOUS to mapOf(
        EmotionIntensity.LOW    to listOf(Color(0xFFEDE7F6), Color(0xFFCE93D8)),
        EmotionIntensity.MEDIUM to listOf(Color(0xFFE1BEE7), Color(0xFF8E24AA)),
        EmotionIntensity.HIGH   to listOf(Color(0xFF4A148C), Color(0xFF1A0033))
    ),

    // ── SAD ──────────────────────────────────────────────────────────────────
    // LOW  : misty light blue — gentle melancholy
    // MED  : soft night blues (matches legacy "sad" baseline)
    // HIGH : very dark navy — heavy emotional weight
    EmotionCategory.SAD to mapOf(
        EmotionIntensity.LOW    to listOf(Color(0xFFBBDEFB), Color(0xFF90CAF9)),
        EmotionIntensity.MEDIUM to listOf(Color(0xFF202F42), Color(0xFFB0C8F3)),
        EmotionIntensity.HIGH   to listOf(Color(0xFF0D1B2A), Color(0xFF1C3A5E))
    ),

    // ── STRESSED ─────────────────────────────────────────────────────────────
    // LOW  : light peach — mild tension
    // MED  : warm orange tones (matches legacy "stressed" baseline)
    // HIGH : deep burnt orange / red — acute overload
    EmotionCategory.STRESSED to mapOf(
        EmotionIntensity.LOW    to listOf(Color(0xFFFFE0B2), Color(0xFFFFCC80)),
        EmotionIntensity.MEDIUM to listOf(Color(0xFFFFCCBC), Color(0xFFFF5722)),
        EmotionIntensity.HIGH   to listOf(Color(0xFFBF360C), Color(0xFF6D1F00))
    ),

    // ── CALM ─────────────────────────────────────────────────────────────────
    // LOW  : very pale sky — gentle tranquility
    // MED  : clear sky blues (matches legacy "calm" baseline)
    // HIGH : deep ocean blue — total serenity / flow state
    EmotionCategory.CALM to mapOf(
        EmotionIntensity.LOW    to listOf(Color(0xFFE1F5FE), Color(0xFFB3E5FC)),
        EmotionIntensity.MEDIUM to listOf(Color(0xFF0072A1), Color(0xFFA1DEFF)),
        EmotionIntensity.HIGH   to listOf(Color(0xFF01579B), Color(0xFF003D6E))
    ),

    // ── HAPPY ────────────────────────────────────────────────────────────────
    // LOW  : soft lemon — gentle warmth
    // MED  : bright yellows (matches legacy "happy" baseline)
    // HIGH : vibrant warm gold / amber — pure elation
    EmotionCategory.HAPPY to mapOf(
        EmotionIntensity.LOW    to listOf(Color(0xFFFFFDE7), Color(0xFFFFF59D)),
        EmotionIntensity.MEDIUM to listOf(Color(0xFFFFF176), Color(0xFFFFC107)),
        EmotionIntensity.HIGH   to listOf(Color(0xFFFF8F00), Color(0xFFE65100))
    ),

    // ── ANGRY ────────────────────────────────────────────────────────────────
    // LOW  : blush rose — mild annoyance
    // MED  : vivid reds (matches legacy "angry" baseline)
    // HIGH : deep crimson / fire red — intense rage
    EmotionCategory.ANGRY to mapOf(
        EmotionIntensity.LOW    to listOf(Color(0xFFFFCDD2), Color(0xFFEF9A9A)),
        EmotionIntensity.MEDIUM to listOf(Color(0xFFFF8A80), Color(0xFFD32F2F)),
        EmotionIntensity.HIGH   to listOf(Color(0xFFB71C1C), Color(0xFF5C0000))
    ),

    // ── NEUTRAL ──────────────────────────────────────────────────────────────
    // All intensities use the same cool default tone (mirrors the legacy baseline).
    EmotionCategory.NEUTRAL to mapOf(
        EmotionIntensity.LOW    to listOf(Color(0xFF006491), Color(0xFF9CDAFF)),
        EmotionIntensity.MEDIUM to listOf(Color(0xFF006491), Color(0xFF9CDAFF)),
        EmotionIntensity.HIGH   to listOf(Color(0xFF006491), Color(0xFF9CDAFF))
    )
)

// ─── Phase 1: Token-Aware Context Management ──────────────────────────────────

/**
 * Rough token estimate using the standard ~4 characters-per-token heuristic for
 * English text. Returns at least 1 so zero-length messages are never treated as
 * "free" — a safety net against accidentally admitting unlimited empty turns.
 */
fun estimateTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)

/**
 * Selects the most-recent messages that fit within [ContextConfig.AVAILABLE_FOR_HISTORY]
 * tokens, then returns them in their original chronological order.
 *
 * Algorithm (newest-to-oldest greedy fit):
 *  1. Walk [messages] in reverse.
 *  2. Estimate the token cost of each message.
 *  3. Stop as soon as admitting the next message would exceed the budget.
 *  4. Reverse the preserved list so it is chronological for the API call.
 *
 * The system prompt is NOT included here — it is passed directly to [GroqRepository]
 * as a separate parameter and is always present regardless of how many history
 * messages are trimmed.
 */
fun buildTokenAwareContext(messages: List<Message>): List<Message> {
    var remainingBudget = ContextConfig.AVAILABLE_FOR_HISTORY
    val preserved = ArrayDeque<Message>()

    for (message in messages.asReversed()) {
        val tokens = estimateTokens(message.message)
        if (remainingBudget - tokens < 0) break
        remainingBudget -= tokens
        preserved.addFirst(message)
    }

    return preserved.toList()
}


const val REQUEST_CODE_NOTIFICATIONS = 1001

@Composable
fun PromptEnableNotifications(context: Context) {
    val showDialog = remember { mutableStateOf(true) }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED && showDialog.value == true
    ) {
        AlertDialog(
            onDismissRequest = {
                showDialog.value = false
            },
            title = { Text("Enable Notifications") },
            text = { Text("To receive reminders, please enable notifications for this app.") },
            confirmButton = {
                TextButton(onClick = {
                    ActivityCompat.requestPermissions(
                        context as Activity,
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_CODE_NOTIFICATIONS
                    )
                    showDialog.value = false
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    } else if (!isNotificationChannelEnabled(context, "mood_journal_reminders")) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text("Enable Notifications") },
            text = { Text("Notifications are turned off. Please enable them to get reminders.") },
            confirmButton = {
                TextButton(onClick = {
                    openNotificationSettings(context)

                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

