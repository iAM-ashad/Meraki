package com.iamashad.meraki.screens.home

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.window.core.layout.WindowWidthSizeClass
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.iamashad.meraki.R
import com.iamashad.meraki.model.MindfulNudge
import com.iamashad.meraki.model.NudgeType
import com.iamashad.meraki.navigation.Breathing
import com.iamashad.meraki.navigation.Chatbot
import com.iamashad.meraki.navigation.MoodTracker
import com.iamashad.meraki.navigation.Settings
import com.iamashad.meraki.screens.moodtracker.MoodTrackerViewModel
import com.iamashad.meraki.screens.settings.SettingsViewModel
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.ProvideDimens
import com.iamashad.meraki.utils.daysOfWeek
import com.iamashad.meraki.utils.getHomeMoodTint
import com.iamashad.meraki.utils.getMoodColor
import com.iamashad.meraki.utils.getMoodEmoji
import com.iamashad.meraki.utils.getMoodPromptSubtext
import com.iamashad.meraki.utils.rememberWindowAdaptiveInfo
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeScreenViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    moodTrackerViewModel: MoodTrackerViewModel = hiltViewModel()
) {
    val dimens = LocalDimens.current
    val user by settingsViewModel.user.collectAsState()
    val profilePicRes by settingsViewModel.profilePicRes.collectAsState()
    var profilePicState by remember { mutableIntStateOf(profilePicRes) }

    LaunchedEffect(profilePicRes) {
        profilePicState = profilePicRes
    }
    val lastMoods by moodTrackerViewModel.moodTrend.collectAsState()
    val isLoading by moodTrackerViewModel.loading.collectAsState()
    var streakCount by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Living Mood Card — collect AI insight + pattern alert state
    val weeklyInsight by homeViewModel.weeklyInsight.collectAsState()
    val isInsightLoading by homeViewModel.insightLoading.collectAsState()
    val patternAlert by homeViewModel.patternAlert.collectAsState()

    // Trigger insight + pattern generation whenever the mood list grows
    LaunchedEffect(lastMoods.size) {
        homeViewModel.generateWeeklyInsight(lastMoods)
    }

    // Mood-Aware UI: read the last session's dominant emotion and derive subtle tint colors.
    val dominantEmotion by homeViewModel.dominantEmotion.collectAsState()
    val moodTint = getHomeMoodTint(dominantEmotion)
    val animatedTopColor by animateColorAsState(
        targetValue = moodTint.first,
        animationSpec = tween(durationMillis = 900),
        label = "moodTopTint"
    )
    val animatedBottomColor by animateColorAsState(
        targetValue = moodTint.second,
        animationSpec = tween(durationMillis = 900),
        label = "moodBottomTint"
    )

    LaunchedEffect(user) {
        user?.uid?.let { userId ->
            try {
                homeViewModel.logDailyUsage(userId)
                streakCount = homeViewModel.calculateStreak(userId)
            } catch (e: Exception) {
                errorMessage = "Failed to calculate streak: ${e.message}"
            }
        }
    }

    val adaptiveInfo = rememberWindowAdaptiveInfo()
    ProvideDimens(adaptiveInfo) {
        val isLargeScreen =
            adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED ||
                    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLargeScreen) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                animatedTopColor,
                                MaterialTheme.colorScheme.onPrimaryContainer,
                                animatedBottomColor,
                            )
                        )
                    )
                    .padding(dimens.paddingMedium)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = dimens.paddingMedium),
                    verticalArrangement = Arrangement.spacedBy(dimens.paddingMedium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProfileCard(
                        profilePicRes = profilePicState,
                        userName = user?.displayName ?: "User",
                        onProfileClick = { navController.navigate(Settings) }
                    )

                    StreakMeterCard(streakCount = streakCount)

                    VerticalWeeklyCalendar(navController)
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(dimens.paddingMedium)
                ) {
                    item {
                        NudgeCardStack(homeViewModel)
                    }

                    item {
                        MoodPromptCard(navController, user?.displayName, dominantEmotion)
                    }

                    item {
                        when {
                            errorMessage != null -> ErrorMessage(errorMessage!!)
                            isLoading -> LoadingIndicator()
                            lastMoods.isNotEmpty() -> LivingMoodCard(
                                moodLogs = lastMoods.takeLast(7),
                                weeklyInsight = weeklyInsight,
                                isInsightLoading = isInsightLoading,
                                patternAlert = patternAlert,
                                navController = navController
                            )

                            else -> EmptyMoodLogs()
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = dimens.paddingMedium),
                            contentAlignment = Alignment.Center
                        ) {
                            MeditateButton(navController)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                animatedTopColor,
                                MaterialTheme.colorScheme.onPrimaryContainer,
                                animatedBottomColor,
                            )
                        )
                    )
                    .padding(dimens.paddingMedium),
                verticalArrangement = Arrangement.spacedBy(dimens.paddingMedium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileCard(
                            profilePicRes = profilePicRes, // Default avatar
                            userName = user?.displayName ?: "User",
                            onProfileClick = { navController.navigate(Settings) }
                        )

                        StreakMeterCard(streakCount = streakCount)
                    }
                }

                item {
                    WeeklyCalendar(navController)
                }

                item {
                    NudgeCardStack(homeViewModel)
                }

                item {
                    MoodPromptCard(navController, user?.displayName, dominantEmotion)
                }

                item {
                    when {
                        errorMessage != null -> ErrorMessage(errorMessage!!)
                        isLoading -> LoadingIndicator()
                        lastMoods.isNotEmpty() -> LivingMoodCard(
                            moodLogs = lastMoods.takeLast(7),
                            weeklyInsight = weeklyInsight,
                            isInsightLoading = isInsightLoading,
                            patternAlert = patternAlert,
                            navController = navController
                        )

                        else -> EmptyMoodLogs()
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = dimens.paddingMedium),
                        contentAlignment = Alignment.Center
                    ) {
                        MeditateButton(navController)
                    }
                }
            }
        }
    }
}


@Composable
fun VerticalWeeklyCalendar(navController: NavController) {
    val dimens = LocalDimens.current
    val calendar = remember { java.util.Calendar.getInstance() }
    val currentDayIndex = remember {
        (calendar.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
    }
    val dates = remember {
        (0..6).map { offset ->
            java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                add(java.util.Calendar.DAY_OF_MONTH, offset)
            }.get(java.util.Calendar.DAY_OF_MONTH)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimens.paddingSmall),
        verticalArrangement = Arrangement.spacedBy(dimens.paddingSmall),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        daysOfWeek.forEachIndexed { index, day ->
            val isSelected = index == currentDayIndex
            val backgroundColor =
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

            Row(
                modifier = Modifier
                    .fillMaxWidth(.8f)
                    .clip(RoundedCornerShape(dimens.cornerRadius))
                    .background(backgroundColor)
                    .clickable { navController.navigate(MoodTracker) }
                    .padding(vertical = dimens.paddingSmall, horizontal = dimens.paddingMedium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                )
                Spacer(Modifier.width(dimens.paddingSmall))
                Text(
                    text = dates[index].toString(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor
                    )
                )
            }
        }
    }
}


@Composable
fun ErrorMessage(errorMessage: String) {
    val dimens = LocalDimens.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimens.paddingMedium),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                fontSize = dimens.fontMedium
            )
        )
    }
}

@Composable
fun WeeklyCalendar(navController: NavController) {
    val dimens = LocalDimens.current
    val calendar = java.util.Calendar.getInstance()
    val currentDayIndex = remember {
        (calendar.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
    }
    val dates = remember {
        (0..6).map { offset ->
            java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                add(java.util.Calendar.DAY_OF_MONTH, offset)
            }.get(java.util.Calendar.DAY_OF_MONTH)
        }
    }

    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(horizontal = dimens.paddingSmall),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        daysOfWeek.forEachIndexed { index, day ->
            val isSelected = index == currentDayIndex
            val backgroundColor =
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

            Box(
                modifier = Modifier
                    .size(dimens.avatarSize / 5)
                    .clip(CircleShape)
                    .background(backgroundColor)
                    .clickable { navController.navigate(MoodTracker) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = textColor
                        )
                    )
                    Text(
                        text = dates[index].toString(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    )
                }
            }
        }
    }
}


@Composable
fun CelebrationDialog(
    onDismiss: () -> Unit,
    streakCount: Int
) {
    val dimens = LocalDimens.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(dimens.cornerRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .width((screenWidthDp.dp / 5) * 4)
                .height((screenHeightDp.dp / 5) * 2)
                .padding(dimens.paddingMedium)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.onBackground,
                                MaterialTheme.colorScheme.primary
                            )
                        ),
                    ), contentAlignment = Alignment.Center
            ) {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_fire))
                val progress by animateLottieCompositionAsState(
                    composition = composition, iterations = LottieConstants.IterateForever
                )

                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(dimens.avatarSize)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(dimens.paddingMedium)
                ) {
                    Spacer(modifier = Modifier.height(dimens.paddingMedium))

                    Text(
                        text = "🔥 $streakCount Day Streak! 🔥",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.background,
                            textAlign = TextAlign.Center
                        )
                    )
                    Text(
                        text = "You're doing great on your emotional journey. Keep moving forward!",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.surface,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(top = dimens.paddingSmall)
                    )

                    Spacer(modifier = Modifier.height(dimens.paddingLarge))

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        )
                    ) {
                        // Phase 6: functional icon inside IconButton requires meaningful description.
                        Icon(
                            imageVector = Icons.Default.Close, contentDescription = "Dismiss"
                        )
                    }
                }

            }
        }
    }
}

@Composable
fun MoodPromptCard(
    navController: NavController,
    userName: String?,
    dominantEmotion: String = "neutral"
) {
    val firstName = userName?.split(" ")?.firstOrNull()
    val dimens = LocalDimens.current

    // Mood-Aware UI: subtext softens for negative emotions, brightens for positive ones.
    val subtext = getMoodPromptSubtext(dominantEmotion)

    Card(
        shape = RoundedCornerShape(dimens.cornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(dimens.elevation),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimens.paddingMedium)
            .clickable {
                navController.navigate(MoodTracker)
            }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.paddingMedium)
        ) {
            Column {
                Text(
                    text = "Hey $firstName, how are you feeling today?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(dimens.avatarSize / 7)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🌟",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.Center
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(dimens.paddingSmall / 2)
                )
            }
        }
    }
}

@Composable
fun ProfileCard(
    profilePicRes: Int,
    userName: String,
    onProfileClick: () -> Unit
) {
    val dimens = LocalDimens.current

    Card(
        shape = RoundedCornerShape(dimens.cornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(dimens.elevation),
        modifier = Modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(dimens.paddingSmall)
        ) {
            Image(
                painter = painterResource(id = profilePicRes),
                contentDescription = "User Avatar",
                modifier = Modifier
                    .size(dimens.avatarSize / 6)
                    .clip(CircleShape)
                    .clickable { onProfileClick() }
            )
            Spacer(modifier = Modifier.width(dimens.paddingSmall / 2))
            Text(
                text = userName,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun StreakMeterCard(streakCount: Int) {
    var showCelebrationDialog by remember { mutableStateOf(false) }

    if (showCelebrationDialog) {
        CelebrationDialog(
            onDismiss = { showCelebrationDialog = false }, streakCount = streakCount
        )
    }
    val dimens = LocalDimens.current

    Card(
        shape = RoundedCornerShape(dimens.cornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(dimens.elevation),
        modifier = Modifier
            .padding(start = dimens.paddingSmall / 2)
            .clickable {
                showCelebrationDialog = true
            }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = dimens.paddingMedium / 2,
                vertical = dimens.paddingSmall / 2
            )
        ) {
            Text(
                text = "🔥",
                fontSize = MaterialTheme.typography.titleLarge.fontSize
            )
            Spacer(modifier = Modifier.width(dimens.paddingSmall / 2))
            Text(
                text = "$streakCount", style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    }

}


// =============================================================================
// Living Mood Card — replaces MoodLogsCard with a 3-state swipeable mini-dashboard
// =============================================================================

/**
 * A card that cycles between three states via horizontal swipe:
 *  0 — Curve      : a smooth sparkline of the last 7 days with tinted gradient fill
 *  1 — AI Insight : a single AI-generated sentence summarising the week
 *  2 — Pattern    : a warm, actionable pattern alert (or empty if none detected)
 */
@Composable
fun LivingMoodCard(
    moodLogs: List<Pair<String, Int>>,
    weeklyInsight: String?,
    isInsightLoading: Boolean,
    patternAlert: PatternAlert?,
    navController: NavController
) {
    val dimens = LocalDimens.current
    val scope = rememberCoroutineScope()

    // Current page: 0 = Curve, 1 = Insight, 2 = Pattern
    var page by remember { mutableIntStateOf(0) }
    val pageCount = if (patternAlert != null) 3 else 2

    // Horizontal swipe tracking
    val swipeDelta = remember { Animatable(0f) }
    val dragState = rememberDraggableState { delta ->
        scope.launch { swipeDelta.snapTo(swipeDelta.value + delta) }
    }

    Card(
        shape = RoundedCornerShape(dimens.cornerRadius),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(dimens.elevation),
        modifier = Modifier
            .fillMaxWidth()
            .height(dimens.paddingLarge * 13)
            .draggable(
                orientation = Orientation.Horizontal,
                state = dragState,
                onDragStopped = { velocity ->
                    scope.launch {
                        val threshold = 80f
                        when {
                            swipeDelta.value < -threshold && page < pageCount - 1 -> page++
                            swipeDelta.value > threshold && page > 0 -> page--
                        }
                        swipeDelta.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                    }
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimens.paddingMedium)
        ) {
            // ---- Header row ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val title = when (page) {
                    0 -> "7-Day Curve"
                    1 -> "Weekly Insight"
                    else -> "Pattern Detected"
                }
                val subtitle = when (page) {
                    0 -> "Swipe to see your AI insight →"
                    1 -> "← Curve   Pattern →"
                    else -> "← Back"
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    )
                }
                // Dot indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pageCount) { idx ->
                        val dotSize by animateFloatAsState(
                            targetValue = if (idx == page) 8f else 5f,
                            animationSpec = spring(),
                            label = "dotSize$idx"
                        )
                        Box(
                            modifier = Modifier
                                .size(dotSize.dp)
                                .background(
                                    color = if (idx == page) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimens.paddingSmall))

            // ---- Page content ----
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally { width -> direction * width } + fadeIn()) togetherWith
                            (slideOutHorizontally { width -> -direction * width } + fadeOut())
                },
                label = "livingMoodPage",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { currentPage ->
                when (currentPage) {
                    0 -> LivingSparklineChart(
                        moodLogs = moodLogs.takeLast(7),
                        modifier = Modifier.fillMaxSize()
                    )

                    1 -> LivingInsightPage(
                        weeklyInsight = weeklyInsight,
                        isLoading = isInsightLoading,
                        modifier = Modifier.fillMaxSize()
                    )

                    else -> patternAlert?.let {
                        LivingPatternPage(
                            alert = it,
                            navController = navController,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// State 0 — The Curve: sparkline with dynamic gradient fill
// ---------------------------------------------------------------------------

@Composable
fun LivingSparklineChart(
    moodLogs: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    if (moodLogs.isEmpty()) return

    val dimens = LocalDimens.current

    // Which point the user has tapped (null = none)
    var tappedIndex by remember { mutableStateOf<Int?>(null) }
    // Store computed point positions for hit-testing
    val pointPositions = remember { mutableListOf<Offset>() }

    // Derive gradient colours from average mood
    val avg = moodLogs.map { it.second }.average()
    val lineColor = when {
        avg >= 65 -> Color(0xFF66BB6A)   // warm green
        avg <= 40 -> Color(0xFF42A5F5)   // cool blue
        else -> Color(0xFFDC8DF3)        // signature purple (neutral)
    }
    val fillTop = lineColor.copy(alpha = 0.35f)
    val fillBottom = lineColor.copy(alpha = 0.02f)

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(moodLogs) {
                    detectTapGestures { position ->
                        // Find the nearest data-point within 44px — dismiss if none
                        val hit = pointPositions.indexOfFirst {
                            abs(it.x - position.x) < 44f && abs(it.y - position.y) < 44f
                        }
                        tappedIndex = if (hit != -1) hit else null
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val vPad = 20.dp.toPx()
            val scores = moodLogs.map { it.second }
            val maxS = scores.max()
            val minS = scores.min()
            val range = (maxS - minS).coerceAtLeast(1)
            val xStep = if (moodLogs.size > 1) w / (moodLogs.size - 1).toFloat() else w

            fun xOf(i: Int) = i * xStep
            fun yOf(s: Int) = h - vPad - (s - minS) / range.toFloat() * (h - 2 * vPad)

            // Build bezier path
            val linePath = Path()
            scores.forEachIndexed { i, s ->
                val x = xOf(i)
                val y = yOf(s)
                if (i == 0) linePath.moveTo(x, y) else {
                    val cx = x - xStep / 2f
                    val prevY = yOf(scores[i - 1])
                    linePath.cubicTo(cx, prevY, cx, y, x, y)
                }
            }

            // Closed fill path (line + baseline)
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(xOf(scores.lastIndex), h)
                lineTo(xOf(0), h)
                close()
            }

            // Draw gradient fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(fillTop, fillBottom),
                    startY = 0f,
                    endY = h
                )
            )

            // Draw the line
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(
                    width = 5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Collect point positions for hit-testing and draw dots
            pointPositions.clear()
            scores.forEachIndexed { i, s ->
                val cx = xOf(i)
                val cy = yOf(s)
                pointPositions.add(Offset(cx, cy))
                drawCircle(
                    color = lineColor,
                    radius = 5.dp.toPx(),
                    center = Offset(cx, cy)
                )
                // Highlighted dot
                if (tappedIndex == i) {
                    drawCircle(
                        color = Color.White,
                        radius = 7.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        // Tap tooltip
        tappedIndex?.let { idx ->
            val (date, score) = moodLogs[idx]
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = dimens.paddingSmall, top = dimens.paddingSmall / 2)
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = getMoodEmoji(score),
                            fontSize = 16.sp
                        )
                        Text(
                            text = "$score / 100",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        Text(
                            text = date,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// State 1 — The AI Insight
// ---------------------------------------------------------------------------

@Composable
fun LivingInsightPage(
    weeklyInsight: String?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )

            weeklyInsight != null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "✨",
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = weeklyInsight,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                )
            }

            else -> Text(
                text = "Log a few moods to unlock your weekly insight.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// State 2 — Pattern Alert
// ---------------------------------------------------------------------------

@Composable
fun LivingPatternPage(
    alert: PatternAlert,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🔍",
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = alert.message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                when (alert.actionType) {
                    PatternActionType.BREATHING -> navController.navigate(Breathing)
                    PatternActionType.CHATBOT -> navController.navigate(
                        Chatbot(prompt = "I've been noticing some patterns in my mood. Can we talk about it?")
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = when (alert.actionType) {
                    PatternActionType.BREATHING -> "Try breathing exercise"
                    PatternActionType.CHATBOT -> "Talk it through"
                },
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// =============================================================================
// Legacy MoodLogsCard — kept for reference; replaced by LivingMoodCard above
// =============================================================================

@Composable
fun MoodLogsCard(moodLogs: List<Pair<String, Int>>) {

    val dimens = LocalDimens.current
    Card(
        shape = RoundedCornerShape(dimens.cornerRadius),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(dimens.elevation),
        modifier = Modifier
            .fillMaxWidth()
            .height(dimens.paddingLarge * 12)
    ) {
        val density = LocalDensity.current

        Column(
            modifier = Modifier.padding(dimens.paddingMedium)
        ) {
            Text(
                text = "Mood Chart",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Text(
                text = "Your last 7 mood entries",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(.6f)
                )
            )

            Spacer(modifier = Modifier.height(dimens.paddingMedium * 3))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                moodLogs.forEach { (_, moodScore) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width((dimens.paddingMedium * 2) - (dimens.paddingSmall / 2))
                    ) {
                        Box(
                            modifier = Modifier.width((dimens.paddingMedium * 2) - (dimens.paddingSmall / 2)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(dimens.paddingMedium * 2)
                                    .height(dimens.paddingMedium * 12)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(dimens.cornerRadius)
                                    ), contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(fraction = moodScore / 100f)
                                        .background(
                                            color = getMoodColor(moodScore),
                                            shape = RoundedCornerShape(dimens.cornerRadius)
                                        )
                                )
                            }
                            with(density) {
                                Text(
                                    text = getMoodEmoji(moodScore),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = dimens.fontMedium
                                    ),
                                    modifier = Modifier
                                        .padding(bottom = dimens.paddingSmall)
                                        .align(Alignment.BottomCenter)
                                        .offset(y = -(moodScore / 100f * (dimens.paddingMedium * 10).toPx()).toDp() + (dimens.paddingSmall / 2))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NudgeCardStack(viewModel: HomeScreenViewModel) {
    val nudges by viewModel.nudges.collectAsState()
    var currentIndex by remember { mutableIntStateOf(0) }
    val screenWidth =
        with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val scope = rememberCoroutineScope()
    val dimens = LocalDimens.current

    if (nudges.isEmpty()) {
        LoadingIndicator()
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Render only the top card and one background card
            nudges.forEachIndexed { index, nudge ->
                if (index == currentIndex || index == currentIndex + 1) {
                    val isTopCard = index == currentIndex
                    val swipeOffset = remember { Animatable(0f) }
                    val alpha = remember { Animatable(1f) }

                    Box(
                        modifier = Modifier
                            .zIndex((nudges.size - index).toFloat())
                            .graphicsLayer(
                                scaleX = if (isTopCard) 1f else 0.95f,
                                scaleY = if (isTopCard) 1f else 0.95f,
                                translationY = if (isTopCard) 0f else 20f,
                                alpha = if (isTopCard) alpha.value else 0.8f
                            )
                    ) {
                        if (isTopCard) {
                            SwipeableNudgeCard(
                                nudge = nudge,
                                swipeOffset = swipeOffset,
                                alpha = alpha,
                                screenWidth = screenWidth,
                                onSwipeComplete = {
                                    scope.launch {
                                        swipeOffset.animateTo(if (swipeOffset.value > 0) screenWidth else -screenWidth)
                                        alpha.animateTo(0f)
                                        currentIndex++

                                        // Fetch a single new nudge when reaching the end of the current stack
                                        if (currentIndex >= nudges.size - 1) {
                                            viewModel.fetchNextNudge()
                                        }
                                    }
                                }
                            )
                        } else {
                            NudgeCard(
                                nudge = nudge,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Stack indicators
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(dimens.paddingMedium),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(nudges.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(dimens.paddingSmall)
                            .background(
                                if (index == currentIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .padding(horizontal = dimens.paddingSmall / 2)
                    )
                }
            }
        }
    }
}

@Composable
fun SwipeableNudgeCard(
    nudge: MindfulNudge,
    swipeOffset: Animatable<Float, AnimationVector1D>,
    alpha: Animatable<Float, AnimationVector1D>,
    screenWidth: Float,
    onSwipeComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    val dragState = rememberDraggableState { delta ->
        scope.launch {
            swipeOffset.snapTo(swipeOffset.value + delta)
        }
    }

    Box(
        modifier = modifier
            .offset { IntOffset(swipeOffset.value.roundToInt(), 0) }
            .draggable(
                orientation = Orientation.Horizontal,
                state = dragState,
                onDragStopped = {
                    if (swipeOffset.value > screenWidth / 2 || swipeOffset.value < -screenWidth / 2) {
                        onSwipeComplete() // Trigger swipe out
                    } else {
                        // Reset card position if not swiped far enough
                        scope.launch {
                            swipeOffset.animateTo(0f)
                        }
                    }
                }
            )
    ) {
        NudgeCard(
            nudge = nudge,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun NudgeCard(
    nudge: MindfulNudge,
    modifier: Modifier = Modifier
) {
    val (iconRes, backgroundColor, label) = when (nudge.type) {
        NudgeType.AFFIRMATION -> Triple(
            R.drawable.ic_quote,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            "Affirmation"
        )

        NudgeType.REFLECTION -> Triple(
            R.drawable.ic_journal,
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
            "Reflection"
        )

        NudgeType.INSIGHT -> Triple(
            R.drawable.ic_insights,
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
            "AI Insight"
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = when (nudge.type) {
                        NudgeType.AFFIRMATION -> MaterialTheme.colorScheme.primary
                        NudgeType.REFLECTION -> MaterialTheme.colorScheme.secondary
                        NudgeType.INSIGHT -> MaterialTheme.colorScheme.tertiary
                    },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = when (nudge.type) {
                            NudgeType.AFFIRMATION -> MaterialTheme.colorScheme.primary
                            NudgeType.REFLECTION -> MaterialTheme.colorScheme.secondary
                            NudgeType.INSIGHT -> MaterialTheme.colorScheme.tertiary
                        }
                    )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = nudge.text ?: "Take a moment for yourself.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    textAlign = TextAlign.Center,
                    fontStyle = if (nudge.type == NudgeType.AFFIRMATION) FontStyle.Italic else FontStyle.Normal
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                modifier = Modifier.width(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = nudge.source ?: "Meraki AI",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Light),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MeditateButton(navController: NavController) {
    val dimens = LocalDimens.current

    Card(
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(dimens.elevation),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .size((dimens.avatarSize / 20) * 9)
            .clickable {
                navController.navigate(Breathing)
            }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(.8f),
                            MaterialTheme.colorScheme.inversePrimary,
                            MaterialTheme.colorScheme.primary
                        ), center = Offset.Unspecified, radius = 150f
                    )
                ), contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Meditate",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}


@Composable
fun EmptyMoodLogs() {
    val dimens = LocalDimens.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimens.paddingMedium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_moodtrack),
            contentDescription = "No mood entries yet",
            modifier = Modifier
                .size(dimens.avatarSize)
        )
        Spacer(modifier = Modifier.height(dimens.paddingSmall))
        Text(
            text = "Your Emotional Journey Awaits!",
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.background,
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.height(dimens.paddingSmall))

        Column {
            Text(
                text = """
                 • Track your emotional highs and lows.
             """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            )
            Text(
                text = """
                 • Gain insights into your mood patterns.
             """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            )
            Text(
                text = """
                • Celebrate progress and identify triggers.
             """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp),
            strokeWidth = 3.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}