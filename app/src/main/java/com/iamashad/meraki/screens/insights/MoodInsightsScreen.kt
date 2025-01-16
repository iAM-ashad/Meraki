package com.iamashad.meraki.screens.insights

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iamashad.meraki.R
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.MoodInsightsAnalyzer
import com.iamashad.meraki.utils.getReasonIcon
import kotlin.math.roundToInt

@Composable
fun MoodInsightsScreen(
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val insights by viewModel.moodInsights.observeAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchMoodInsights()
    }

    insights?.let { data ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Highlights Section
            HighlightsSection(
                overallMood = data.overallAverageMood.roundToInt(),
                bestReason = data.reasonsAnalysis.maxByOrNull { it.value.deviation }?.key,
                worstReason = data.reasonsAnalysis.minByOrNull { it.value.deviation }?.key
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Reason Details Cards
            Text(
                text = "Reason-wise Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(data.reasonsAnalysis.entries.toList()) { (reason, deviation) ->
                    ReasonDetailsCard(reason, deviation)
                }
            }
        }
    } ?: Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ReasonDetailsCard(
    reason: String,
    deviation: MoodInsightsAnalyzer.MoodDeviation
) {
    val isPositive = deviation.deviation >= 0
    val dimens = LocalDimens.current

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(dimens.cornerRadius))
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.primary
                    )
                )
            )
    ) {
        Card(
            shape = RoundedCornerShape(dimens.cornerRadius),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.paddingSmall)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimens.paddingMedium),
                verticalArrangement = Arrangement.spacedBy(dimens.paddingSmall)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "'$reason' has a ${if (isPositive) "good" else "bad"} effect on your mood",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = dimens.fontMedium,
                            color = MaterialTheme.colorScheme.background,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Button",
                        tint = MaterialTheme.colorScheme.background,
                        modifier = Modifier.scale(.8f)
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    ImpactItem(reason, deviation.deviation.roundToInt())
                    Spacer(Modifier.padding(bottom = dimens.paddingMedium))
                    Text(
                        text = when (deviation.entriesCount) {
                            in 0..5 -> "We need more data for higher confidence."
                            in 6..15 -> "Moderate confidence based on ${deviation.entriesCount} entries."
                            else -> "High confidence based on ${deviation.entriesCount} entries."
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Suggestions",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Consider journaling about positive experiences after encountering '$reason'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun BadgeContent(text: String, backgroundColor: Color, textColor: Color) {
    val dimens = LocalDimens.current

    Box(
        modifier = Modifier
            .background(backgroundColor, shape = RoundedCornerShape(dimens.cornerRadius / 3))
            .padding(horizontal = dimens.paddingSmall, vertical = dimens.paddingSmall / 2),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
    }
}


@Composable
fun HighlightsSection(overallMood: Int, bestReason: String?, worstReason: String?) {
    val dimens = LocalDimens.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimens.paddingSmall),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Mood Insights",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Overall Mood Score: $overallMood",
            style = MaterialTheme.typography.bodyLarge
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            bestReason?.let {
                BadgeContent("Best Reason: $it", Color.Green, textColor = Color.Yellow)
            }
            worstReason?.let {
                BadgeContent("Worst Reason: $it", Color.Red, textColor = Color.Yellow)
            }
        }
    }
}

@Composable
fun ImpactItem(
    reason: String,
    moodImpact: Int
) {
    val dimens = LocalDimens.current
    Card(
        shape = RoundedCornerShape(dimens.cornerRadius),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onBackground
        ),
        modifier = Modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimens.paddingMedium),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(getReasonIcon(reason)),
                contentDescription = "Reason Icon",
                modifier = Modifier
                    .size(dimens.avatarSize / 6)
            )
            Spacer(Modifier.width(width = dimens.paddingSmall))
            Image(
                painter = if (moodImpact > 0) painterResource(
                    R.drawable.img_increase
                )
                else painterResource(
                    R.drawable.img_decrease
                ),
                contentDescription = if (moodImpact > 0) "Mood Improved" else "Mood Declined",
                modifier = Modifier
                    .size(dimens.avatarSize / 7)
            )
            Spacer(Modifier.width(width = dimens.paddingLarge))
            Column {
                Text(
                    text = "${moodImpact}%",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                Text(
                    text = "Impact on mood",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .8f)
                    ),
                )
            }
        }
    }
}