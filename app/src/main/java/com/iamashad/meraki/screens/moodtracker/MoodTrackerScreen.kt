package com.iamashad.meraki.screens.moodtracker

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iamashad.meraki.R
import kotlin.math.roundToInt

@Composable
fun MoodTrackerScreen(
    moodTrackerViewModel: MoodTrackerViewModel = hiltViewModel(),
    onMoodLogged: () -> Unit
) {
    val moodTrend by moodTrackerViewModel.moodTrend.collectAsState()
    var entryCount by remember { mutableIntStateOf(7) }
    var showInfoDialog by remember { mutableStateOf(false) }

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
        if (moodTrend.isNotEmpty()) {
            val recentMoodTrend = moodTrend.takeLast(entryCount)
            val averageMood = recentMoodTrend.map { it.second }.average().roundToInt()
            val moodLabel = getMoodLabel(averageMood)
            val moodChange = calculateMoodChange(moodTrend, entryCount)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 24.dp)
                ) {
                    Text(
                        text = moodLabel,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.background
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_info),
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                if (moodChange != null) {
                    Row(
                        modifier = Modifier
                            .padding(start = 28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Average Mood $moodChange%",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondary
                        )

                        Icon(
                            painter = if (moodChange > 0) painterResource(R.drawable.ic_arrow_up) else painterResource(
                                R.drawable.ic_arrow_down
                            ),
                            contentDescription = if (moodChange > 0) "Positive Change" else "Negative Change",
                            tint = if (moodChange > 0) Color.Green else Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                MoodTrendGraph(
                    moodData = recentMoodTrend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(.8f)
                        .padding(top = 4.dp)
                )
            }
        }

        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = {
                    Text(
                        text = "How Trends and Changes Are Calculated",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Text(
                        text = "Mood trends are displayed based on your last 7 or 14 logged moods. The average mood score is calculated from these entries, and mood change is determined by comparing the first and last entries in the selected period.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text("OK")
                    }
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .padding(16.dp)
            )
        }

        // Bottom Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MoodSelectionBar(
                    onMoodLogged = onMoodLogged,
                    viewModel = moodTrackerViewModel
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle Button Bar
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


@Composable
fun ToggleButtonBar(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        options.forEach { option ->
            Button(
                onClick = { onOptionSelected(option) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedOption == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (selectedOption == option) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text(text = option, style = MaterialTheme.typography.bodyMedium)
            }
        }
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
        in 71..80 -> "Very Good"
        in 81..90 -> "Great"
        in 91..100 -> "Amazing"
        else -> "Unknown"
    }
}

fun calculateMoodChange(moodTrend: List<Pair<String, Int>>, entryCount: Int): Int? {
    if (moodTrend.size < entryCount) return null
    val recentMoodTrend = moodTrend.takeLast(entryCount)
    val firstMood = recentMoodTrend.first().second
    val lastMood = recentMoodTrend.last().second

    // Avoid division by zero and handle edge cases
    return if (firstMood == 0) {
        null // Return null or handle as no change
    } else {
        ((lastMood - firstMood).toDouble() / firstMood * 100).roundToInt()
    }
}


@Composable
fun MoodTrendGraph(
    moodData: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    if (moodData.isEmpty()) return

    val maxMood = moodData.maxOfOrNull { it.second } ?: 100
    val minMood = moodData.minOfOrNull { it.second } ?: 0
    val moodRange = maxMood - minMood
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(220, 141, 243, 255),
            Color(53, 33, 59, 255)
        )
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
    ) {
        val width = size.width
        val height = size.height
        val xStep = width / (moodData.size - 1).coerceAtLeast(1)
        val verticalPadding = 16.dp.toPx()
        val yStep = (height - 2 * verticalPadding) / moodRange.coerceAtLeast(1)

        val path = Path().apply {
            moodData.forEachIndexed { index, (_, mood) ->
                val x = index * xStep
                val y = height - verticalPadding - (mood - minMood) * yStep
                if (index == 0) moveTo(x, y)
                else cubicTo(
                    x - xStep / 2,
                    getY(moodData, index - 1, minMood, yStep, height, verticalPadding),
                    x - xStep / 2,
                    y,
                    x,
                    y
                )
            }
        }

        // Draw the path
        drawPath(
            path = path,
            brush = gradientBrush,
            style = Stroke(width = 6.dp.toPx())
        )
    }
}

private fun getY(
    moodData: List<Pair<String, Int>>,
    index: Int,
    minMood: Int,
    yStep: Float,
    height: Float,
    verticalPadding: Float
): Float {
    return height - verticalPadding - (moodData[index].second - minMood) * yStep
}


@Composable
fun MoodSelectionBar(
    onMoodLogged: () -> Unit,
    viewModel: MoodTrackerViewModel
) {
    var moodScore by remember { mutableFloatStateOf(50f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Make the entire bar scrollable
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState()) // Enable scroll
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Rate Your Mood by rotating the slider",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Circular Mood Selector
            CircularMoodSelector(
                moodScore = moodScore,
                onMoodScoreChanged = { moodScore = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Display Current Mood
            Text(
                text = "Mood: ${getMoodLabel(moodScore.toInt())}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Log Mood Button
            Button(
                onClick = {
                    viewModel.logMood(moodScore.toInt())
                    onMoodLogged()
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(0.4f),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Log Mood",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun CircularMoodSelector(
    moodScore: Float,
    onMoodScoreChanged: (Float) -> Unit
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

    // Animation for glowing effect
    val animatedGlow by rememberInfiniteTransition(label = "").animateFloat(
        initialValue = 1f,
        targetValue = 1.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(175.dp)
            .padding(16.dp)
            .pointerInput(Unit) { // Attach pointer input directly to the Box
                detectDragGestures { change, _ ->
                    change.consume() // Consume the gesture event
                    val canvasSize = size // Access Canvas size dynamically
                    val center = Offset(
                        x = canvasSize.width.toFloat() / 2,
                        y = canvasSize.height.toFloat() / 2
                    )
                    val angle = Math
                        .toDegrees(
                            kotlin.math
                                .atan2(
                                    change.position.y - center.y,
                                    change.position.x - center.x
                                )
                                .toDouble()
                        )
                        .toFloat() + 90

                    val adjustedAngle = if (angle < 0) angle + 360 else angle
                    val mood = (adjustedAngle / 360) * 100
                    onMoodScoreChanged(mood.coerceIn(0f, 100f))
                }
            }
    ) {
        // Circular Slider Background
        Canvas(modifier = Modifier.size(200.dp)) {
            drawCircle(
                brush = gradientBrush,
                radius = size.minDimension / 2,
                style = Stroke(width = 16.dp.toPx())
            )
        }

        // Glow effect
        Canvas(modifier = Modifier.size(200.dp * animatedGlow)) {
            drawCircle(
                color = Color.White,
                radius = size.minDimension / 2
            )
        }

        // Circular slider handle
        Canvas(modifier = Modifier.size(200.dp)) {
            val center =
                Offset(x = size.width / 2, y = size.height / 2) // Correct center inside Canvas
            val angle = (moodScore / 100) * 360
            val radius = size.minDimension / 2 - 12.dp.toPx()

            val x = center.x + radius * kotlin.math.cos(Math.toRadians(angle - 90.0).toFloat())
            val y = center.y + radius * kotlin.math.sin(Math.toRadians(angle - 90.0).toFloat())

            drawCircle(
                color = Color(0, 0, 0, 255),
                center = Offset(x, y),
                radius = 7.dp.toPx()
            )
        }
    }
}


