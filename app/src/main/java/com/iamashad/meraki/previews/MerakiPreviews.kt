@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package com.iamashad.meraki.previews

// ─────────────────────────────────────────────────────────────────────────────
//  Meraki Wellness App — Jetpack Compose Preview Suite
//
//  Author  : Generated for com.iamashad.meraki
//  Purpose : Full-coverage @Preview functions for every primary screen.
//
//  Strategy
//  ────────
//  • Screens that use hiltViewModel() (HomeScreen, MoodTrackerScreen,
//    ChatbotScreen, JournalScreen) are previewed by composing their
//    individual *stateless* sub-composables into a preview-only content
//    shell. This avoids the "ViewModel not found" crash in the Preview tab
//    while still giving a pixel-accurate representation of each screen.
//
//  • BreathingScreen() takes no parameters and is called directly. Because
//    ExoPlayer side effects don't execute during preview rendering, the
//    composable's layout and visual state are still fully visible.
//
//  • All previews are wrapped in MerakiTheme(dynamicColor = false) so that
//    the Material You dynamic-colour path is bypassed and the app's own
//    design tokens are shown consistently.
//
//  • ProvideDimens is applied via rememberWindowAdaptiveInfo() which reads
//    from the Compose preview host's window size — so a PIXEL_7 preview
//    automatically receives CompactDimens and a TABLET preview receives
//    ExpandedDimens with no extra work.
//
//  MultiPreview Matrix (via @MerakiMultiPreview)
//  ─────────────────────────────────────────────
//   ┌──────────────┬────────────────┬──────────────────────┐
//   │ Device       │ Light          │ Dark                 │
//   ├──────────────┼────────────────┼──────────────────────┤
//   │ Pixel 7      │ Phone – Light  │ Phone – Dark         │
//   │ Tablet       │ Tablet – Light │ Tablet – Dark        │
//   └──────────────┴────────────────┴──────────────────────┘
// ─────────────────────────────────────────────────────────────────────────────

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.iamashad.meraki.components.JournalCard
import com.iamashad.meraki.components.MoodTrendGraph
import com.iamashad.meraki.model.ConfidenceScore
import com.iamashad.meraki.model.InsightTier
import com.iamashad.meraki.model.Journal
import com.iamashad.meraki.model.Message
import com.iamashad.meraki.model.MindfulNudge
import com.iamashad.meraki.model.NudgeType
import com.iamashad.meraki.screens.breathing.BreathingScreen
import com.iamashad.meraki.screens.chatbot.AnimatedAvatar
import com.iamashad.meraki.screens.chatbot.ChatHeader
import com.iamashad.meraki.screens.chatbot.ChatInputSection
import com.iamashad.meraki.screens.chatbot.ConfidentialityFooter
import com.iamashad.meraki.screens.chatbot.MessageList
import com.iamashad.meraki.screens.chatbot.MessageRow
import com.iamashad.meraki.screens.chatbot.StartConversationButton
import com.iamashad.meraki.screens.chatbot.TypingIndicator
import com.iamashad.meraki.screens.home.CelebrationDialog
import com.iamashad.meraki.screens.home.LivingInsightPage
import com.iamashad.meraki.screens.home.LivingMoodCard
import com.iamashad.meraki.screens.home.LivingPatternPage
import com.iamashad.meraki.screens.home.LivingSparklineChart
import com.iamashad.meraki.screens.home.NudgeCard
import com.iamashad.meraki.screens.home.PatternActionType
import com.iamashad.meraki.screens.home.PatternAlert
import com.iamashad.meraki.screens.home.StreakMeterCard
import com.iamashad.meraki.screens.journal.EmptyJournalList
import com.iamashad.meraki.screens.journal.HeaderCard
import com.iamashad.meraki.screens.moodtracker.CircularMoodSelector
import com.iamashad.meraki.screens.moodtracker.ToggleButtonBar
import com.iamashad.meraki.ui.theme.MerakiTheme
import com.iamashad.meraki.utils.ProvideDimens
import com.iamashad.meraki.utils.rememberWindowAdaptiveInfo

// ═════════════════════════════════════════════════════════════════════════════
//  MULTI-PREVIEW ANNOTATION
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Applies four simultaneous previews to any composable:
 *  1. Pixel 7  · Light mode   (Compact window class)
 *  2. Pixel 7  · Dark mode    (Compact window class)
 *  3. Tablet   · Light mode   (Expanded window class)
 *  4. Tablet   · Dark mode    (Expanded window class)
 *
 * Usage:
 * ```kotlin
 * @MerakiMultiPreview
 * @Composable
 * fun MyScreenPreview() { ... }
 * ```
 */
@Preview(
    name = "Phone – Light",
    group = "Phone",
    showBackground = true,
    showSystemUi = true,
    device = Devices.PIXEL_7
)
@Preview(
    name = "Phone – Dark",
    group = "Phone",
    showBackground = true,
    showSystemUi = true,
    device = Devices.PIXEL_7,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Preview(
    name = "Tablet – Light",
    group = "Tablet",
    showBackground = true,
    showSystemUi = true,
    device = Devices.TABLET
)
@Preview(
    name = "Tablet – Dark",
    group = "Tablet",
    showBackground = true,
    showSystemUi = true,
    device = Devices.TABLET,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
annotation class MerakiMultiPreview

// ═════════════════════════════════════════════════════════════════════════════
//  HIGH-QUALITY MOCK DATA
// ═════════════════════════════════════════════════════════════════════════════

private object MockData {

    // ── MindfulNudges ────────────────────────────────────────────────────────
    val nudges = listOf(
        MindfulNudge(
            text = "You are doing better than you think. Take a breath and trust the process.",
            type = NudgeType.AFFIRMATION,
            source = "Meraki AI"
        ),
        MindfulNudge(
            text = "What is one small act of kindness you can offer yourself today?",
            type = NudgeType.REFLECTION,
            source = "Meraki AI"
        ),
        MindfulNudge(
            text = "Your mood has been more stable over the past three days — keep it up!",
            type = NudgeType.INSIGHT,
            source = "Meraki AI"
        )
    )

    // ── 7-Day mood sparkline data (Mon → Sun, upward trend) ──────────────────
    val moodLogsWeek = listOf(
        Pair("Mon", 48),
        Pair("Tue", 55),
        Pair("Wed", 52),
        Pair("Thu", 63),
        Pair("Fri", 71),
        Pair("Sat", 78),
        Pair("Sun", 85)
    )

    // ── Upward trend (used for MoodTrendGraph) ───────────────────────────────
    val moodTrendUpward = listOf(
        Pair("Mon", 30),
        Pair("Tue", 38),
        Pair("Wed", 45),
        Pair("Thu", 52),
        Pair("Fri", 61),
        Pair("Sat", 72),
        Pair("Sun", 80)
    )

    // ── Confidence scores for insight tier previews ──────────────────────────
    /** Represents a brand-new user — FORMING tier, insight placeholder shown. */
    val confidenceForming = ConfidenceScore.EMPTY

    /** Represents a user with a handful of mood logs — LOW tier. */
    val confidenceLow = ConfidenceScore.compute(
        moodLogCount = 6,
        sessionCount = 1,
        avgEmotionConfidence = 0.55f,
        chatMessageCount = 5
    )

    /** Represents a regular user with sessions and moods — HIGH tier for most previews. */
    val confidenceHigh = ConfidenceScore.compute(
        moodLogCount = 18,
        sessionCount = 9,
        avgEmotionConfidence = 0.72f,
        chatMessageCount = 28
    )

    // ── Pattern alert ────────────────────────────────────────────────────────
    val patternAlert = PatternAlert(
        message = "Your mood has declined for 3 consecutive days. A short breathing session might help you reset.",
        actionType = PatternActionType.BREATHING
    )

    // ── Chat message bubbles ─────────────────────────────────────────────────
    val messages = listOf(
        Message(
            message = "Hi! I've been feeling quite overwhelmed lately and not sure why.",
            role = "user"
        ),
        Message(
            message = "I'm really glad you reached out. Feeling overwhelmed without a clear cause is more common than you think. Would you like to explore what might be underneath that feeling, or would a short breathing exercise help you settle first?",
            role = "model"
        ),
        Message(
            message = "Let's talk it through. I think work pressure is piling up.",
            role = "user"
        ),
        Message(
            message = "Work pressure can quietly accumulate until it feels unmanageable. What part of work weighs on you most — the volume, the expectations, or something in your environment?",
            role = "model"
        ),
        Message(
            message = "Mainly the volume. I never feel like I can catch up.",
            role = "user"
        ),
        Message(
            message = "That 'always behind' feeling is exhausting and demoralising. Let's look at one small, concrete thing you could do today that might give you a tiny sense of progress. Does that sound helpful?",
            role = "model"
        )
    )

    // ── Journal entries ──────────────────────────────────────────────────────
    private val now = System.currentTimeMillis()
    private val day = 86_400_000L

    val journals = listOf(
        Journal(
            journalId = "j_001",
            userId = "preview_user",
            title = "Happy",
            content = "Had a wonderful walk in the park this morning. The warm sunshine and birdsong felt genuinely healing. Grateful for these small moments.",
            moodScore = 84,
            reasons = listOf("Exercise", "Nature", "Gratitude"),
            date = now,
            imageUrl = null
        ),
        Journal(
            journalId = "j_002",
            userId = "preview_user",
            title = "Calm",
            content = "Spent a quiet afternoon reading my favourite novel with a cup of chamomile tea. No screens, no noise — just peace.",
            moodScore = 70,
            reasons = listOf("Reading", "Relaxation", "Self-care"),
            date = now - day * 1,
            imageUrl = null
        ),
        Journal(
            journalId = "j_003",
            userId = "preview_user",
            title = "Anxious",
            content = "Big presentation tomorrow. I've prepared well but my mind keeps running worst-case scenarios. Trying to stay grounded with deep breaths.",
            moodScore = 32,
            reasons = listOf("Work", "Deadline", "Overthinking"),
            date = now - day * 2,
            imageUrl = null
        ),
        Journal(
            journalId = "j_004",
            userId = "preview_user",
            title = "Grateful",
            content = "Called my family after weeks of silence. That single half-hour conversation lifted my spirits more than anything else this week.",
            moodScore = 91,
            reasons = listOf("Family", "Connection", "Belonging"),
            date = now - day * 3,
            imageUrl = null
        )
    )
}

// ═════════════════════════════════════════════════════════════════════════════
//  PREVIEW WRAPPER HELPER
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Wraps content in [MerakiTheme] + [ProvideDimens] using the preview host's
 * window adaptive info. Apply this to every full-screen preview so that
 * responsive layout breakpoints (Compact / Expanded) work automatically.
 */
@Composable
private fun MerakiPreviewSurface(content: @Composable () -> Unit) {
    MerakiTheme(dynamicColor = false) {
        val windowInfo = rememberWindowAdaptiveInfo()
        ProvideDimens(windowAdaptiveInfo = windowInfo) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
                content = content
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  1.  HOME SCREEN
// ═════════════════════════════════════════════════════════════════════════════
//
//  HomeScreen uses three hiltViewModel() injections. Rather than mocking
//  them, we compose the individual stateless sub-composables that HomeScreen
//  assembles at runtime. The preview is pixel-accurate for the scrollable
//  compact layout (Column) and the two-pane expanded layout (Row).
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen Home preview — compact (phone) scroll layout.
 * Shows: StreakMeter · NudgeCard · LivingMoodCard (with AI insight) ·
 *        MoodPromptCard
 */
@MerakiMultiPreview
@Composable
fun HomeScreenPreview() {
    MerakiPreviewSurface {
        val nav = rememberNavController()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Streak counter (5-day streak, no celebration overlay) ────────
            StreakMeterCard(streakCount = 5)

            // ── Nudge card stack (single card shown without swipe logic) ─────
            NudgeCard(nudge = MockData.nudges[0])

            // ── LivingMoodCard — Insight page (page 1 of 2) ─────────────────
            LivingMoodCard(
                weeklyInsight = "Your mood rose by 37 points this week — a clear upward trend driven by consistent rest and outdoor time. Keep it up!",
                isInsightLoading = false,
                patternAlert = null,
                insightTier = InsightTier.HIGH,
                confidenceScore = MockData.confidenceHigh,
                navController = nav
            )
        }
    }
}

/**
 * Home screen variant that shows the Pattern Alert page inside LivingMoodCard.
 * Demonstrates the third carousel page (page 2/2) with a Breathing CTA.
 */
@MerakiMultiPreview
@Composable
fun HomeScreenPatternAlertPreview() {
    MerakiPreviewSurface {
        val nav = rememberNavController()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StreakMeterCard(streakCount = 5)

            NudgeCard(nudge = MockData.nudges[2]) // INSIGHT type

            // LivingMoodCard — shows Pattern Alert as the active page
            LivingMoodCard(
                weeklyInsight = null,
                isInsightLoading = false,
                patternAlert = MockData.patternAlert,
                insightTier = InsightTier.HIGH,
                confidenceScore = MockData.confidenceHigh,
                navController = nav
            )
        }
    }
}

// ── Home sub-component previews ───────────────────────────────────────────────

@Preview(name = "StreakMeterCard · 5-Day Streak", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun StreakMeterCardPreview() {
    MerakiTheme(dynamicColor = false) {
        StreakMeterCard(streakCount = 5)
    }
}

@Preview(name = "CelebrationDialog · 7-Day Streak", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun CelebrationDialogPreview() {
    MerakiTheme(dynamicColor = false) {
        CelebrationDialog(onDismiss = {}, streakCount = 7)
    }
}

@Preview(name = "NudgeCard · Affirmation", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun NudgeCardAffirmationPreview() {
    MerakiTheme(dynamicColor = false) {
        NudgeCard(nudge = MockData.nudges[0])
    }
}

@Preview(name = "NudgeCard · Reflection", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun NudgeCardReflectionPreview() {
    MerakiTheme(dynamicColor = false) {
        NudgeCard(nudge = MockData.nudges[1])
    }
}

@Preview(name = "NudgeCard · Insight", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun NudgeCardInsightPreview() {
    MerakiTheme(dynamicColor = false) {
        NudgeCard(nudge = MockData.nudges[2])
    }
}

@Preview(
    name = "LivingSparklineChart · 7-Day Upward Trend",
    showBackground = true,
    device = Devices.PIXEL_7
)
@Composable
private fun LivingSparklineChartPreview() {
    MerakiTheme(dynamicColor = false) {
        LivingSparklineChart(
            moodLogs = MockData.moodLogsWeek,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Preview(name = "LivingMoodCard · AI Insight", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun LivingMoodCardInsightPreview() {
    MerakiTheme(dynamicColor = false) {
        LivingMoodCard(
            weeklyInsight = "Consistent rest and outdoor time are clearly lifting your mood. Your resilience is showing!",
            isInsightLoading = false,
            patternAlert = null,
            insightTier = InsightTier.HIGH,
            confidenceScore = MockData.confidenceHigh,
            navController = rememberNavController()
        )
    }
}

@Preview(
    name = "LivingMoodCard · Insight Loading",
    showBackground = true,
    device = Devices.PIXEL_7
)
@Composable
private fun LivingMoodCardLoadingPreview() {
    MerakiTheme(dynamicColor = false) {
        LivingMoodCard(
            weeklyInsight = null,
            isInsightLoading = true,
            patternAlert = null,
            insightTier = InsightTier.LOW,
            confidenceScore = MockData.confidenceLow,
            navController = rememberNavController()
        )
    }
}

@Preview(name = "LivingMoodCard · Pattern Alert", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun LivingMoodCardPatternPreview() {
    MerakiTheme(dynamicColor = false) {
        LivingMoodCard(
            weeklyInsight = null,
            isInsightLoading = false,
            patternAlert = MockData.patternAlert,
            insightTier = InsightTier.MODERATE,
            confidenceScore = MockData.confidenceHigh,
            navController = rememberNavController()
        )
    }
}

@Preview(
    name = "LivingInsightPage · Loaded · HIGH tier",
    showBackground = true,
    device = Devices.PIXEL_7
)
@Composable
private fun LivingInsightPageLoadedPreview() {
    MerakiTheme(dynamicColor = false) {
        LivingInsightPage(
            weeklyInsight = "A clear upward trend this week. Your evening wind-down routine seems to be making a real difference.",
            isLoading = false,
            insightTier = InsightTier.HIGH,
            confidenceScore = MockData.confidenceHigh
        )
    }
}

@Preview(
    name = "LivingInsightPage · FORMING placeholder",
    showBackground = true,
    device = Devices.PIXEL_7
)
@Composable
private fun LivingInsightPageFormingPreview() {
    MerakiTheme(dynamicColor = false) {
        LivingInsightPage(
            weeklyInsight = null,
            isLoading = false,
            insightTier = InsightTier.FORMING,
            confidenceScore = MockData.confidenceForming
        )
    }
}

@Preview(name = "LivingInsightPage · LOW badge", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun LivingInsightPageLowPreview() {
    MerakiTheme(dynamicColor = false) {
        LivingInsightPage(
            weeklyInsight = "You had a steady week overall — a couple of dips on Tuesday and Thursday, but you bounced back.",
            isLoading = false,
            insightTier = InsightTier.LOW,
            confidenceScore = MockData.confidenceLow
        )
    }
}

@Preview(
    name = "LivingPatternPage · Breathing CTA",
    showBackground = true,
    device = Devices.PIXEL_7
)
@Composable
private fun LivingPatternPagePreview() {
    MerakiTheme(dynamicColor = false) {
        LivingPatternPage(
            alert = MockData.patternAlert,
            navController = rememberNavController()
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  2.  MOOD TRACKER SCREEN
// ═════════════════════════════════════════════════════════════════════════════
//
//  MoodTrackerScreen depends on MoodTrackerViewModel for moodTrend,
//  moodPrompt, and suggestedMoodScore. We bypass the ViewModel by
//  wiring stateless sub-composables directly with hardcoded values.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen Mood Tracker — circular selector at neutral (50%) + upward trend.
 * Compact layout mirrors the single-column modal card behaviour.
 * Expanded layout mirrors the split-pane behaviour (graph left, dial right).
 */
@MerakiMultiPreview
@Composable
fun MoodTrackerScreenPreview() {
    MerakiPreviewSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Prompt header ────────────────────────────────────────────────
            Text(
                text = "How are you feeling right now?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            // ── Day-range toggle ─────────────────────────────────────────────
            ToggleButtonBar(
                options = listOf("Last 7 Days", "Last 14 Days"),
                selectedOption = "Last 7 Days",
                onOptionSelected = {}
            )

            // ── Circular mood dial — Neutral (50%) ───────────────────────────
            CircularMoodSelector(
                moodScore = 50f,
                onMoodScoreChanged = {}
            )

            // ── 7-day upward trend graph ─────────────────────────────────────
            MoodTrendGraph(
                moodData = MockData.moodTrendUpward,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

// ── Mood Tracker sub-component previews ──────────────────────────────────────

@Preview(
    name = "CircularMoodSelector · Neutral (50%)",
    showBackground = true,
    device = Devices.PIXEL_7
)
@Composable
private fun CircularMoodSelectorNeutralPreview() {
    MerakiTheme(dynamicColor = false) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularMoodSelector(
                moodScore = 50f,
                onMoodScoreChanged = {}
            )
        }
    }
}

@Preview(
    name = "CircularMoodSelector · Happy (85%)",
    showBackground = true,
    device = Devices.PIXEL_7
)
@Composable
private fun CircularMoodSelectorHappyPreview() {
    MerakiTheme(dynamicColor = false) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularMoodSelector(
                moodScore = 85f,
                onMoodScoreChanged = {}
            )
        }
    }
}

@Preview(
    name = "CircularMoodSelector · Low (15%)",
    showBackground = true,
    device = Devices.PIXEL_7
)
@Composable
private fun CircularMoodSelectorLowPreview() {
    MerakiTheme(dynamicColor = false) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularMoodSelector(
                moodScore = 15f,
                onMoodScoreChanged = {}
            )
        }
    }
}

@Preview(
    name = "MoodTrendGraph · 7-Day Upward Trend",
    showBackground = true,
    device = Devices.PIXEL_7
)
@Composable
private fun MoodTrendGraphUpwardPreview() {
    MerakiTheme(dynamicColor = false) {
        MoodTrendGraph(
            moodData = MockData.moodTrendUpward,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(16.dp)
        )
    }
}

@Preview(
    name = "ToggleButtonBar · 7 Days Selected",
    showBackground = true,
    device = Devices.PIXEL_7
)
@Composable
private fun ToggleButtonBarPreview() {
    MerakiTheme(dynamicColor = false) {
        ToggleButtonBar(
            options = listOf("Last 7 Days", "Last 14 Days"),
            selectedOption = "Last 7 Days",
            onOptionSelected = {}
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  3.  CHATBOT SCREEN  —  Two distinct state previews
// ═════════════════════════════════════════════════════════════════════════════

// ─── 3a. New Conversation ─────────────────────────────────────────────────────

/**
 * Chatbot — New Conversation state.
 * Shows the Lottie heart avatar, greeting text, Start button,
 * and the confidentiality footer, matching the NewConversationScreen layout.
 */
@MerakiMultiPreview
@Composable
fun ChatbotNewConversationPreview() {
    MerakiPreviewSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Lottie chat-header banner ────────────────────────────────────
            ChatHeader()

            Spacer(modifier = Modifier.weight(1f))

            // ── Animated Lottie heart avatar ─────────────────────────────────
            AnimatedAvatar(
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Welcome greeting text ────────────────────────────────────────
            Text(
                text = "Hello, Ashad! 👋\nI am here to listen and support you,\nwhenever you are ready.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // ── Start conversation CTA ───────────────────────────────────────
            StartConversationButton(
                onClick = {},
            )

            Spacer(modifier = Modifier.weight(1f))

            // ── Privacy assurance footer ─────────────────────────────────────
            ConfidentialityFooter()
        }
    }
}

// ─── 3b. Active Chat with Message Bubbles ────────────────────────────────────

/**
 * Chatbot — Active Chat state.
 * Shows a populated message list with alternating user (right) and
 * model (left) bubble styles, plus the input bar at the bottom.
 */
@MerakiMultiPreview
@Composable
fun ChatbotActiveChatPreview() {
    MerakiPreviewSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Sticky chat header ───────────────────────────────────────────
            ChatHeader()

            // ── Scrollable message list ──────────────────────────────────────
            MessageList(
                modifier = Modifier.weight(1f),
                messageList = MockData.messages
            )

            // ── Compose bar (ready to type) ──────────────────────────────────
            ChatInputSection(
                onMessageSend = {},
                onFinishConversation = {},
                isSending = false
            )
        }
    }
}

/**
 * Chatbot — Typing Indicator state.
 * Shows the "model is thinking" animation between a user message and
 * the next model response. The send button is disabled (isSending = true).
 */
@Preview(
    name = "Chatbot – AI Typing Indicator",
    showBackground = true,
    showSystemUi = true,
    device = Devices.PIXEL_7
)
@Composable
private fun ChatbotTypingIndicatorPreview() {
    MerakiPreviewSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatHeader()

            MessageList(
                modifier = Modifier.weight(1f),
                // Drop the last model message to simulate pending response
                messageList = MockData.messages.dropLast(1)
            )

            // Typing animation sits between message list and input bar
            TypingIndicator()

            ChatInputSection(
                onMessageSend = {},
                onFinishConversation = {},
                isSending = true // disables send button
            )
        }
    }
}

// ── Chatbot sub-component previews ───────────────────────────────────────────

@Preview(name = "MessageRow · User Bubble", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun MessageRowUserPreview() {
    MerakiTheme(dynamicColor = false) {
        MessageRow(
            message = Message(
                message = "Hi! I've been feeling quite overwhelmed lately and not sure why.",
                role = "user"
            )
        )
    }
}

@Preview(name = "MessageRow · Model Bubble", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun MessageRowModelPreview() {
    MerakiTheme(dynamicColor = false) {
        MessageRow(
            message = Message(
                message = "I'm really glad you reached out. Feeling overwhelmed without a clear cause is more common than you might think. Would you like to explore what might be underneath that feeling?",
                role = "model"
            )
        )
    }
}

@Preview(name = "MessageList · Full Conversation", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun MessageListFullPreview() {
    MerakiTheme(dynamicColor = false) {
        MessageList(messageList = MockData.messages)
    }
}

@Preview(name = "ChatInputSection · Idle", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun ChatInputSectionIdlePreview() {
    MerakiTheme(dynamicColor = false) {
        ChatInputSection(
            onMessageSend = {},
            onFinishConversation = {},
            isSending = false
        )
    }
}

@Preview(name = "ChatInputSection · Sending", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun ChatInputSectionSendingPreview() {
    MerakiTheme(dynamicColor = false) {
        ChatInputSection(
            onMessageSend = {},
            onFinishConversation = {},
            isSending = true
        )
    }
}

@Preview(name = "ConfidentialityFooter", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun ConfidentialityFooterPreview() {
    MerakiTheme(dynamicColor = false) {
        ConfidentialityFooter()
    }
}

@Preview(name = "AnimatedAvatar", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun AnimatedAvatarPreview() {
    MerakiTheme(dynamicColor = false) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedAvatar(modifier = Modifier.size(220.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  4.  BREATHING SCREEN
// ═════════════════════════════════════════════════════════════════════════════
//
//  BreathingScreen() accepts no parameters and can be previewed directly.
//  ExoPlayer & CountDownTimer side-effects are inert during preview
//  rendering, so the composable's layout is fully visible.
//
//  We also provide a stateless `BreathingProgressContent` shell that lets
//  us snapshot specific progress states (0 %, 50 %, 100 %) without any
//  runtime timer — useful for design review and Figma comparison.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen Breathing preview using the actual [BreathingScreen] composable.
 * Renders the initial (not-yet-started) layout with the Begin Session button.
 */
@MerakiMultiPreview
@Composable
fun BreathingScreenPreview() {
    MerakiPreviewSurface {
        BreathingScreen()
    }
}

/**
 * Stateless snapshot of the Breathing UI at exactly 50 % progress.
 * Bypasses ExoPlayer and the CountDownTimer — pure layout preview.
 */
@Preview(
    name = "Breathing – 50% Progress · Phone",
    showBackground = true,
    showSystemUi = true,
    device = Devices.PIXEL_7
)
@Composable
private fun BreathingHalfwayPhonePreview() {
    MerakiPreviewSurface {
        BreathingProgressContent(
            progressFraction = 0.5f,
            remainingSeconds = 105,
            instructionText = "Breathe out slowly…",
            isSessionActive = true
        )
    }
}

@Preview(
    name = "Breathing – 50% Progress · Tablet",
    showBackground = true,
    showSystemUi = true,
    device = Devices.TABLET
)
@Composable
private fun BreathingHalfwayTabletPreview() {
    MerakiPreviewSurface {
        // Tablet uses a side-by-side layout; the progress content fills
        // its allotted column so we centre it in a Row.
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BreathingProgressContent(
                progressFraction = 0.5f,
                remainingSeconds = 105,
                instructionText = "Breathe out slowly…",
                isSessionActive = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Preview(
    name = "Breathing – Not Started",
    showBackground = true,
    device = Devices.PIXEL_7
)
@Composable
private fun BreathingNotStartedPreview() {
    MerakiPreviewSurface {
        BreathingProgressContent(
            progressFraction = 0f,
            remainingSeconds = 210,
            instructionText = "Tap Begin to start your 3.5-minute guided breathing session.",
            isSessionActive = false
        )
    }
}

@Preview(
    name = "Breathing – Complete (100%)",
    showBackground = true,
    device = Devices.PIXEL_7
)
@Composable
private fun BreathingCompletePreview() {
    MerakiPreviewSurface {
        BreathingProgressContent(
            progressFraction = 1f,
            remainingSeconds = 0,
            instructionText = "Session complete. Well done!",
            isSessionActive = false
        )
    }
}

/**
 * Stateless composable that renders the Breathing screen's core visual
 * at an arbitrary [progressFraction] (0f–1f). Decouples the timer and
 * ExoPlayer logic from pure UI so that the layout is previewable at any
 * point in the session.
 *
 * @param progressFraction  0f = not started, 0.5f = halfway, 1f = complete.
 * @param remainingSeconds  Seconds remaining to display in the centre label.
 * @param instructionText   Guidance text shown below the progress ring.
 * @param isSessionActive   When false, a "Begin Session" button is shown.
 */
@Composable
private fun BreathingProgressContent(
    progressFraction: Float,
    remainingSeconds: Int,
    instructionText: String,
    isSessionActive: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // ── Screen title ─────────────────────────────────────────────────────
        Text(
            text = "Breathing Exercise",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        // ── Circular progress ring with centre label ─────────────────────────
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.size(220.dp),
                strokeWidth = 10.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val minutes = remainingSeconds / 60
                val seconds = remainingSeconds % 60
                Text(
                    text = "%d:%02d".format(minutes, seconds),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "remaining",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Dynamic instruction text ─────────────────────────────────────────
        Text(
            text = instructionText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // ── Begin button (only shown when session is not yet active) ─────────
        if (!isSessionActive) {
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(0.65f)
            ) {
                Text(
                    text = if (progressFraction == 1f) "Try Again" else "Begin Session",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  5.  JOURNAL SCREEN
// ═════════════════════════════════════════════════════════════════════════════
//
//  JournalScreen(viewModel, onAddJournalClick, onViewJournalClick) requires
//  a JournalViewModel. We extract its display into a stateless
//  JournalScreenContent composable that receives the journal list directly.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen Journal preview — 4 sample entries in a responsive grid.
 * On phones (Compact) the grid renders as a single column.
 * On tablets (Expanded) the grid renders as a two-column adaptive layout.
 */
@MerakiMultiPreview
@Composable
fun JournalScreenPreview() {
    MerakiPreviewSurface {
        JournalScreenContent(journals = MockData.journals)
    }
}

/**
 * Full-screen Journal preview — empty state with placeholder illustration.
 */
@MerakiMultiPreview
@Composable
fun JournalScreenEmptyPreview() {
    MerakiPreviewSurface {
        JournalScreenContent(journals = emptyList())
    }
}

/**
 * Stateless content shell for the Journal screen.
 * Assembles HeaderCard + adaptive grid + JournalCard list without a ViewModel.
 * Uses [GridCells.Adaptive] to replicate the screen's responsive column logic.
 *
 * @param journals The list of [Journal] entries to display.
 */
@Composable
private fun JournalScreenContent(journals: List<Journal>) {
    Column(modifier = Modifier.fillMaxSize()) {
        // ── "Capture Your Thoughts" header card ─────────────────────────────
        HeaderCard()

        if (journals.isEmpty()) {
            // ── Empty state illustration + CTA ──────────────────────────────
            EmptyJournalList()
        } else {
            // ── Responsive LazyVerticalGrid ──────────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 88.dp) // FAB clearance
            ) {
                items(journals, key = { it.journalId }) { journal ->
                    JournalCard(
                        journal = journal,
                        onEditClick = {},
                        onDeleteButtonClick = {}
                    )
                }
            }
        }
    }
}

// ── Journal sub-component previews ───────────────────────────────────────────

@Preview(name = "JournalCard · Happy Entry", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun JournalCardHappyPreview() {
    MerakiTheme(dynamicColor = false) {
        JournalCard(
            journal = MockData.journals[0],
            onEditClick = {},
            onDeleteButtonClick = {}
        )
    }
}

@Preview(name = "JournalCard · Calm Entry", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun JournalCardCalmPreview() {
    MerakiTheme(dynamicColor = false) {
        JournalCard(
            journal = MockData.journals[1],
            onEditClick = {},
            onDeleteButtonClick = {}
        )
    }
}

@Preview(name = "JournalCard · Anxious Entry", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun JournalCardAnxiousPreview() {
    MerakiTheme(dynamicColor = false) {
        JournalCard(
            journal = MockData.journals[2],
            onEditClick = {},
            onDeleteButtonClick = {}
        )
    }
}

@Preview(name = "JournalCard · Grateful Entry", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun JournalCardGratefulPreview() {
    MerakiTheme(dynamicColor = false) {
        JournalCard(
            journal = MockData.journals[3],
            onEditClick = {},
            onDeleteButtonClick = {}
        )
    }
}

@Preview(name = "Journal HeaderCard", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun JournalHeaderCardPreview() {
    MerakiTheme(dynamicColor = false) {
        HeaderCard()
    }
}

@Preview(name = "Journal Empty State", showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun JournalEmptyStatePreview() {
    MerakiTheme(dynamicColor = false) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            EmptyJournalList()
        }
    }
}

/**
 * 2×2 grid of JournalCards rendered without a LazyVerticalGrid container —
 * useful for confirming card-level spacing, typography, and colour pairing
 * in a single compact preview.
 */
@Preview(
    name = "JournalCard Grid · 4 Entries",
    showBackground = true,
    device = Devices.TABLET
)
@Composable
private fun JournalCardGridPreview() {
    MerakiTheme(dynamicColor = false) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(MockData.journals) { journal ->
                JournalCard(
                    journal = journal,
                    onEditClick = {},
                    onDeleteButtonClick = {}
                )
            }
        }
    }
}
