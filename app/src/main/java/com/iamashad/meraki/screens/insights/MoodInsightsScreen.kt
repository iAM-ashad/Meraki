package com.iamashad.meraki.screens.insights

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.iamashad.meraki.R
import com.iamashad.meraki.navigation.Screens
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.MoodInsightsAnalyzer
import com.iamashad.meraki.utils.ProvideDimens
import com.iamashad.meraki.utils.getReasonIcon
import kotlin.math.roundToInt

@Composable
fun MoodInsightsScreen(
    viewModel: InsightsViewModel = hiltViewModel(),
    navController: NavController
) {
    val insights by viewModel.moodInsights.collectAsState()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    var searchQuery by remember { mutableStateOf("") }
    val filteredReasons = remember(searchQuery, insights) {
        insights?.reasonsAnalysis
            ?.filterKeys { it.contains(searchQuery, ignoreCase = true) }
            ?.toList() ?: emptyList()
    }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        viewModel.fetchMoodInsights()
    }

    ProvideDimens(screenWidth, screenHeight) {
        val dimens = LocalDimens.current

        if (insights == null || insights?.reasonsAnalysis.isNullOrEmpty()) {
            EmptyInsightsScreen { navController.navigate(Screens.JOURNAL.name) }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimens.paddingMedium),
                verticalArrangement = Arrangement.spacedBy(dimens.paddingMedium)
            ) {

                insights?.let { data ->
                    HighlightsSection(
                        overallMood = data.overallAverageMood.roundToInt()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(dimens.cornerRadius / 2)
                            )
                            .padding(horizontal = dimens.paddingSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(dimens.paddingSmall / 2))
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .clip(RoundedCornerShape(dimens.cornerRadius)),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent
                            ),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Done,
                                capitalization = KeyboardCapitalization.Words
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus() // Clear focus from TextField
                                }
                            )
                        )
                    }

                    Text(
                        text = "Key Insights",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = dimens.fontMedium
                        ),
                        modifier = Modifier.padding(bottom = dimens.paddingSmall)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(dimens.paddingMedium)
                    ) {
                        items(filteredReasons) { (reason, deviation) ->
                            ReasonDetailsCard(reason, deviation)
                        }
                    }
                } ?: Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}


@Composable
fun EmptyInsightsScreen(
    onActionClick: () -> Unit
) {
    val dimens = LocalDimens.current

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_insights),
            contentDescription = "No Insights",
            modifier = Modifier
                .size(dimens.avatarSize)
                .padding(bottom = dimens.paddingMedium)
        )

        Text(
            text = "No Insights Yet!",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = dimens.fontLarge
            )
        )

        Spacer(modifier = Modifier.height(dimens.paddingMedium))

        Text(
            text = "Begin journaling to consistently track your mood and uncover valuable insights into the patterns, triggers, and experiences that influence your emotional well-being",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                fontSize = dimens.fontSmall
            ),
            modifier = Modifier.padding(horizontal = dimens.paddingMedium)
        )

        Spacer(modifier = Modifier.height(dimens.paddingLarge * 2))

        Button(
            onClick = onActionClick,
            shape = RoundedCornerShape(dimens.cornerRadius),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .padding(horizontal = dimens.paddingMedium)
                .fillMaxWidth()
        ) {
            Text(
                text = "Start Journaling",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = dimens.fontSmall * 1.2,
                    fontWeight = FontWeight.Bold
                )
            )
        }
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
                            fontSize = dimens.fontMedium * .8,
                            color = MaterialTheme.colorScheme.onBackground,
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
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = dimens.fontSmall
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
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = dimens.fontMedium * .7
                        )
                    )
                    Text(
                        text = "Consider journaling about positive experiences after encountering '$reason'.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = dimens.fontSmall * .8
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun HighlightsSection(overallMood: Int) {
    val dimens = LocalDimens.current
    val progressColor = when {
        overallMood > 69 -> Color.Green
        overallMood > 50 -> Color.Yellow
        else -> Color.Red
    }

    Surface(
        shape = RoundedCornerShape(dimens.cornerRadius * 2),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(dimens.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(dimens.paddingSmall / 2),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.img_moodtrack),
                contentDescription = null,
                modifier = Modifier
                    .size(dimens.avatarSize / 3)
            )

            Spacer(Modifier.width(dimens.paddingSmall / 4))

            Text(
                text = "Mood Statistics",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = dimens.fontLarge
                )
            )

            Spacer(modifier = Modifier.height(dimens.paddingSmall / 2))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.paddingSmall),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Overall Mood Score: $overallMood",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = dimens.fontMedium * 0.8
                        )
                    )
                    LinearProgressIndicator(
                        progress = { overallMood / 100f },
                        color = progressColor,
                        strokeCap = StrokeCap.Butt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimens.paddingSmall)
                            .clip(RoundedCornerShape(dimens.cornerRadius / 6))
                    )
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                thickness = 1.dp
            )
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
                        color = MaterialTheme.colorScheme.primaryContainer,
                        fontSize = dimens.fontSmall * 1.3
                    )
                )
                Text(
                    text = "Impact on mood",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .8f),
                        fontSize = dimens.fontSmall
                    ),
                )
            }
        }
    }
}