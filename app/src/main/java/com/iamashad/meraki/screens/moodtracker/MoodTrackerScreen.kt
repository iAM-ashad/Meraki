package com.iamashad.meraki.screens.moodtracker

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iamashad.meraki.R
import com.iamashad.meraki.components.MoodTrendGraph
import com.iamashad.meraki.components.showToast
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.ProvideDimens
import com.iamashad.meraki.utils.calculateMoodChange
import com.iamashad.meraki.utils.getMoodLabel
import com.iamashad.meraki.utils.rememberWindowSizeClass
import kotlin.math.roundToInt

@Composable
fun MoodTrackerScreen(
    moodTrackerViewModel: MoodTrackerViewModel = hiltViewModel(),
    onMoodLogged: () -> Unit
) {
    val moodTrend by moodTrackerViewModel.moodTrend.collectAsState()
    var entryCount by remember { mutableIntStateOf(7) }
    var showInfoDialog by remember { mutableStateOf(false) }
    val windowSize = rememberWindowSizeClass()
    val dimens = LocalDimens.current

    ProvideDimens(windowSize) {
        val isLargeScreen = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded ||
                LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLargeScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.inversePrimary,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                        .padding(vertical = dimens.paddingLarge)
                        .align(Alignment.CenterVertically)
                ) {
                    if (moodTrend.isEmpty() || moodTrend.size < entryCount) {
                        EmptyMoodTrend(
                            modifier = Modifier
                                .fillMaxHeight()
                        )
                    } else {
                        val recentMoodTrend = moodTrend.takeLast(entryCount)
                        val moodLabel =
                            getMoodLabel(recentMoodTrend.map { it.second }.average().roundToInt())
                        val moodChange = calculateMoodChange(moodTrend, entryCount)
                        AnimatedContent(
                            targetState = entryCount,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(700)) +
                                        expandVertically(
                                            animationSpec = tween(700)
                                        ) { it } togetherWith
                                        fadeOut(animationSpec = tween(700)) +
                                        shrinkVertically(
                                            animationSpec = tween(700)
                                        ) { -it }
                            }
                        ) { targetEntryCount ->

                            val recentTrend = moodTrend.takeLast(targetEntryCount)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = dimens.paddingLarge)
                                ) {
                                    Text(
                                        text = moodLabel,
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.background
                                        )
                                    )

                                    Spacer(modifier = Modifier.width(dimens.paddingSmall))

                                    IconButton(
                                        onClick = { showInfoDialog = true },
                                        modifier = Modifier.size(dimens.avatarSize / 20)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_info),
                                            contentDescription = "Info",
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.padding(start = dimens.paddingLarge),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Average Mood: $moodChange%",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )

                                    if (moodChange != null) {
                                        Icon(
                                            painter = if (moodChange > 0) painterResource(R.drawable.ic_arrow_up)
                                            else painterResource(R.drawable.ic_arrow_down),
                                            contentDescription = if (moodChange > 0) "Positive Change"
                                            else "Negative Change",
                                            tint = if (moodChange > 0) Color.Green else Color.Red,
                                            modifier = Modifier.size(dimens.avatarSize / 15)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(dimens.paddingSmall / 2))

                                MoodTrendGraph(
                                    moodData = recentTrend,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(.8f)
                                        .padding(top = dimens.paddingSmall / 2)
                                )
                            }
                        }
                    }
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(
                        topStart = dimens.cornerRadius * 2,
                        bottomStart = dimens.cornerRadius * 2
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(dimens.paddingMedium),
                        contentPadding = PaddingValues(vertical = dimens.paddingMedium)
                    ) {
                        item {
                            MoodSelectionBar(
                                onMoodLogged = onMoodLogged,
                                viewModel = moodTrackerViewModel
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(dimens.paddingMedium))
                        }

                        item {
                            ToggleButtonBar(
                                options = listOf("Last 7", "Last 14"),
                                selectedOption = if (entryCount == 7) "Last 7" else "Last 14",
                                onOptionSelected = { selected ->
                                    entryCount = if (selected == "Last 7") 7 else 14
                                }
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.inversePrimary,
                                MaterialTheme.colorScheme.primary
                            )
                        )
                    )
            ) {
                if (moodTrend.isEmpty() || moodTrend.size < entryCount) {
                    EmptyMoodTrend(
                        modifier = Modifier
                            .fillMaxHeight(0.45f)
                    )
                } else {
                    val recentMoodTrend = moodTrend.takeLast(entryCount)
                    val moodLabel =
                        getMoodLabel(recentMoodTrend.map { it.second }.average().roundToInt())
                    val moodChange = calculateMoodChange(moodTrend, entryCount)

                    AnimatedContent(
                        targetState = entryCount,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(700)) +
                                    expandVertically(
                                        animationSpec = tween(700)
                                    ) { it } togetherWith
                                    fadeOut(animationSpec = tween(700)) +
                                    shrinkVertically(
                                        animationSpec = tween(700)
                                    ) { -it }
                        }
                    ) { targetEntryCount ->

                        val recentTrend = moodTrend.takeLast(targetEntryCount)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.45f),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = dimens.paddingLarge)
                            ) {
                                Text(
                                    text = moodLabel,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.background
                                    )
                                )

                                Spacer(modifier = Modifier.width(dimens.paddingSmall))

                                IconButton(
                                    onClick = { showInfoDialog = true },
                                    modifier = Modifier.size(dimens.avatarSize / 20)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_info),
                                        contentDescription = "Info",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.padding(start = dimens.paddingLarge),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Average Mood: $moodChange%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondary
                                )

                                if (moodChange != null) {
                                    Icon(
                                        painter = if (moodChange > 0) painterResource(R.drawable.ic_arrow_up)
                                        else painterResource(R.drawable.ic_arrow_down),
                                        contentDescription = if (moodChange > 0) "Positive Change"
                                        else "Negative Change",
                                        tint = if (moodChange > 0) Color.Green else Color.Red,
                                        modifier = Modifier.size(dimens.avatarSize / 15)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            MoodTrendGraph(
                                moodData = recentTrend,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(.8f)
                                    .padding(top = dimens.paddingSmall / 2)
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.55f)
                        .align(Alignment.BottomCenter),
                    shape = RoundedCornerShape(
                        topStart = dimens.cornerRadius * 2,
                        topEnd = dimens.cornerRadius * 2
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(dimens.paddingSmall / 2),
                        contentPadding = PaddingValues(vertical = dimens.paddingSmall)
                    ) {
                        item {
                            MoodSelectionBar(
                                onMoodLogged = onMoodLogged,
                                viewModel = moodTrackerViewModel
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(dimens.paddingMedium))
                        }

                        item {
                            ToggleButtonBar(
                                options = listOf("Last 7", "Last 14"),
                                selectedOption = if (entryCount == 7) "Last 7" else "Last 14",
                                onOptionSelected = { selected ->
                                    entryCount = if (selected == "Last 7") 7 else 14
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = {
                    Text(
                        text = "How Trends and Changes Are Calculated",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Text(
                        text = "Mood trends are displayed based on your last 7 or 14 logged moods. The average mood score is calculated from these entries, and mood change is determined by comparing the first and last entries in the selected period.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text("OK")
                    }
                },
                shape = RoundedCornerShape(dimens.cornerRadius),
                modifier = Modifier.padding(dimens.paddingMedium)
            )
        }
    }
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ToggleButtonBar(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    val dimens = LocalDimens.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.paddingMedium),
        horizontalArrangement = Arrangement.Center
    ) {
        options.forEach { option ->
            Button(
                onClick = { onOptionSelected(option) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedOption == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (selectedOption == option) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(dimens.cornerRadius),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = dimens.paddingSmall / 2)
            ) {
                AnimatedContent(
                    targetState = option == selectedOption,
                    transitionSpec = {
                        if (targetState) {
                            fadeIn(animationSpec = tween(300)) + slideInVertically { it } togetherWith
                                    fadeOut(animationSpec = tween(300)) + slideOutVertically { -it }
                        } else {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(
                                animationSpec = tween(
                                    300
                                )
                            )
                        }
                    }
                ) { isSelected ->
                    if (isSelected) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    } else {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun MoodSelectionBar(
    onMoodLogged: () -> Unit, viewModel: MoodTrackerViewModel
) {
    var moodScore by remember { mutableFloatStateOf(50f) }
    val context = LocalContext.current
    val dimens = LocalDimens.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimens.paddingMedium)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Rate your mood by rotating the slider",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(dimens.paddingSmall))

            CircularMoodSelector(moodScore = moodScore, onMoodScoreChanged = { moodScore = it })

            Spacer(modifier = Modifier.height(dimens.paddingSmall))

            Text(
                text = "Mood: ${getMoodLabel(moodScore.toInt())}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Spacer(modifier = Modifier.height(dimens.paddingLarge))

            Button(
                onClick = {
                    viewModel.logMood(moodScore.toInt())
                    onMoodLogged()
                    showToast(context = context, "Mood Logged")
                },
                modifier = Modifier
                    .padding(horizontal = dimens.paddingLarge / 2),
                shape = RoundedCornerShape(dimens.cornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Log Mood",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}


@Composable
fun CircularMoodSelector(
    moodScore: Float, onMoodScoreChanged: (Float) -> Unit
) {
    val gradientBrush = Brush.sweepGradient(
        colors = listOf(
            Color(224, 66, 23, 255),
            Color(224, 66, 23, 200),
            Color(139, 195, 74, 255),
            Color(76, 175, 80, 200),
            Color(151, 224, 82, 255),
            Color(13, 189, 17, 255),
            Color(136, 0, 0, 255),
            Color(218, 40, 23, 255),
        )
    )
    val animatedGlow by rememberInfiniteTransition(label = "").animateFloat(
        initialValue = 1f, targetValue = 1.38f, animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )
    val dimens = LocalDimens.current

    Box(contentAlignment = Alignment.Center,
        modifier = Modifier
            .size((dimens.avatarSize / 5) * 3)
            .padding(dimens.paddingMedium)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val canvasSize = size
                    val center = Offset(
                        x = canvasSize.width.toFloat() / 2, y = canvasSize.height.toFloat() / 2
                    )
                    val angle = Math.toDegrees(
                        kotlin.math.atan2(
                            change.position.y - center.y, change.position.x - center.x
                        ).toDouble()
                    ).toFloat() + 90

                    val adjustedAngle = if (angle < 0) angle + 360 else angle
                    val mood = (adjustedAngle / 360) * 100
                    onMoodScoreChanged(mood.coerceIn(0f, 100f))
                }
            }) {
        Canvas(modifier = Modifier.size((dimens.avatarSize / 3) * 2)) {
            drawCircle(
                brush = gradientBrush,
                radius = size.minDimension / 2,
                style = Stroke(width = dimens.paddingMedium.toPx())
            )
        }

        Canvas(modifier = Modifier.size((dimens.avatarSize / 3) * 2 * animatedGlow)) {
            drawCircle(
                color = Color.White, radius = size.minDimension / 2
            )
        }

        Canvas(modifier = Modifier.size((dimens.avatarSize / 3) * 2)) {
            val center = Offset(x = size.width / 2, y = size.height / 2)
            val angle = (moodScore / 100) * 360
            val radius = size.minDimension / 2 - dimens.paddingSmall.toPx()

            val x = center.x + radius * kotlin.math.cos(Math.toRadians(angle - 90.0).toFloat())
            val y = center.y + radius * kotlin.math.sin(Math.toRadians(angle - 90.0).toFloat())

            drawCircle(
                color = Color(0, 0, 0, 255),
                center = Offset(x, y),
                radius = dimens.paddingSmall.toPx()
            )
        }
    }
}

@Composable
fun EmptyMoodTrend(modifier: Modifier = Modifier) {
    val dimens = LocalDimens.current

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No mood trend available yet.",
            style = MaterialTheme.typography.headlineMedium.copy(
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.height(dimens.paddingSmall))
        Text(
            text = "Log your mood regularly to see insightful trends here.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(.4f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.img_empty_graph),
            contentDescription = "Empty Mood Graph",
            tint = MaterialTheme.colorScheme.surface.copy(alpha = .7f),
            modifier = Modifier
        )
    }
}




