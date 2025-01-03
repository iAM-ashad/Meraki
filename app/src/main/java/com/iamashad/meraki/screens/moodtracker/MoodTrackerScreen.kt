package com.iamashad.meraki.screens.moodtracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Composable
fun MoodTrackerScreen(
    navController: NavController,
    moodTrackerViewModel: MoodTrackerViewModel = hiltViewModel(),
    onMoodLogged: () -> Unit
) {
    val moodTrend by moodTrackerViewModel.moodTrend.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Mood Trends",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (moodTrend.isNotEmpty()) {
            MoodTrendGraph(moodData = moodTrend)
        } else {
            Text(
                text = "No mood data available. Log your mood to start tracking trends!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        MoodSelectionBar(
            onMoodLogged = onMoodLogged,
            viewModel = moodTrackerViewModel
        )
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

@Composable
fun MoodSelectionBar(
    onMoodLogged: () -> Unit,
    viewModel: MoodTrackerViewModel
) {
    var moodScore by remember { mutableFloatStateOf(50f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Rate Your Mood:",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = moodScore,
            onValueChange = { moodScore = it },
            valueRange = 0f..100f,
            steps = 9, // Break into 10 steps
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Mood: ${getMoodLabel(moodScore.toInt())}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.logMood(moodScore.toInt())
                onMoodLogged()
            }
        ) {
            Text(text = "Log Mood")
        }
    }
}


@Composable
fun MoodTrendGraph(
    moodData: List<Pair<String, Int>> // Date to mood score mapping
) {
    if (moodData.isEmpty()) return

    val maxMood = moodData.maxOfOrNull { it.second } ?: 100
    val minMood = moodData.minOfOrNull { it.second } ?: 0
    val moodRange = maxMood - minMood
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            Color.Blue,
            Color.Green
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Compute padding inside the Canvas context
            val verticalPadding = 16.dp.toPx()

            val width = size.width
            val height = size.height
            val xStep = width / (moodData.size - 1).coerceAtLeast(1)
            val yStep = (height - 2 * verticalPadding) / moodRange.coerceAtLeast(1)

            val path = Path().apply {
                moodData.forEachIndexed { index, (_, mood) ->
                    val x = index * xStep
                    val y = height - verticalPadding - (mood - minMood) * yStep
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }

            // Draw the path
            drawPath(
                path = path,
                brush = gradientBrush,
                style = Stroke(width = 4.dp.toPx())
            )

            // Draw points on the path
            moodData.forEachIndexed { index, (_, mood) ->
                val x = index * xStep
                val y = height - verticalPadding - (mood - minMood) * yStep
                drawCircle(
                    brush = gradientBrush,
                    radius = 6.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            // Draw grid lines for visual aid
            val horizontalStep = 5 // Divide graph into 5 rows
            for (i in 0..horizontalStep) {
                val y = verticalPadding + i * (height - 2 * verticalPadding) / horizontalStep
                drawLine(
                    color = Color.Gray.copy(alpha = 0.2f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }

    // Date Labels
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        moodData.forEach { (date, _) ->
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
