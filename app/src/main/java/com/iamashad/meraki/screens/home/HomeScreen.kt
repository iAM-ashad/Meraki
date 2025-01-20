package com.iamashad.meraki.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.R
import com.iamashad.meraki.navigation.Screens
import com.iamashad.meraki.screens.moodtracker.MoodTrackerViewModel
import com.iamashad.meraki.utils.LoadImageWithGlide
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.ProvideDimens
import com.iamashad.meraki.utils.getMoodColor
import com.iamashad.meraki.utils.getMoodEmoji

@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeScreenViewModel = hiltViewModel(),
    moodTrackerViewModel: MoodTrackerViewModel = hiltViewModel()
) {
    val configuration = LocalConfiguration.current
    val dimens = LocalDimens.current
    val screenWidthDp = remember { configuration.screenWidthDp }
    val screenHeightDp = remember { configuration.screenHeightDp }

    val user by homeViewModel.user.collectAsState()
    val advice by homeViewModel.advice.collectAsState()
    val photoUrl by homeViewModel.photoUrl.collectAsState()
    val lastMoods by moodTrackerViewModel.moodTrend.collectAsState()
    val isLoading by moodTrackerViewModel.loading.collectAsState()
    var streakCount by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

    ProvideDimens(screenWidthDp, screenHeightDp) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            MaterialTheme.colorScheme.onPrimaryContainer,
                            MaterialTheme.colorScheme.primaryContainer,
                        )
                    )
                )
                .padding(dimens.paddingMedium)
                .verticalScroll(rememberScrollState())
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Profile and Streak Meter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = dimens.paddingMedium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileCard(
                        photoUrl = photoUrl.toString(),
                        userName = user?.displayName ?: "User",
                        onProfileClick = { navController.navigate(Screens.SETTINGS.name) }
                    )
                    StreakMeterCard(streakCount = streakCount)
                }

                Spacer(modifier = Modifier.height(dimens.paddingMedium))

                // Weekly Calendar
                WeeklyCalendar(navController)

                Spacer(modifier = Modifier.height(dimens.paddingMedium))

                // Mood Prompt Card
                MoodPromptCard(navController)

                Spacer(modifier = Modifier.height(dimens.paddingMedium))

                // Mood Logs or Loading/Error Indicator
                when {
                    errorMessage != null -> ErrorMessage(errorMessage!!)
                    isLoading -> LoadingIndicator()
                    lastMoods.isNotEmpty() -> MoodLogsCard(moodLogs = lastMoods.takeLast(7))
                    else -> EmptyMoodLogs()
                }

                Spacer(modifier = Modifier.height(dimens.paddingMedium))

                // Advice Card
                AdviceCard(advice)

                Spacer(modifier = Modifier.height(dimens.paddingMedium))

                // Meditate Button
                MeditateButton(navController)
            }
        }
    }
}

@Composable
fun ErrorMessage(errorMessage: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
fun WeeklyCalendar(navController: NavController) {
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val dimens = LocalDimens.current
    val calendar = remember { java.util.Calendar.getInstance() }
    val currentDayIndex = remember {
        (calendar.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
    }
    val dates = remember {
        (0..6).map { offset ->
            calendar.apply {
                set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY + offset)
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
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(backgroundColor)
                    .clickable { navController.navigate(Screens.MOODTRACKER.name) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodySmall.copy(color = textColor)
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
    onDismiss: () -> Unit, streakCount: Int
) {

    val dimens = LocalDimens.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(dimens.cornerRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(.9f)
                .fillMaxHeight(.35f)
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
                    modifier = Modifier.size(250.dp)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(dimens.paddingMedium)
                ) {
                    Spacer(modifier = Modifier.height(dimens.paddingMedium))

                    Text(
                        text = "🔥 $streakCount Day Streak! 🔥",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.background
                        ),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "You're doing great on your emotional journey. Keep moving forward!",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.surface,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center,
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
                        Icon(
                            imageVector = Icons.Default.Close, contentDescription = null
                        )
                    }
                }

            }
        }
    }
}

@Composable
fun MoodPromptCard(navController: NavController) {
    val userName = FirebaseAuth.getInstance().currentUser?.displayName
    val firstName = userName?.split(" ")?.firstOrNull()

    val dimens = LocalDimens.current
    Card(shape = RoundedCornerShape(dimens.cornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimens.paddingMedium)
            .clickable {
                navController.navigate(Screens.MOODTRACKER.name)
            }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(dimens.paddingMedium)
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
                    text = "Tap to log your mood and track your emotional journey!",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .size(50.dp)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🌟", style = MaterialTheme.typography.titleMedium.copy(
                        textAlign = TextAlign.Center, fontSize = dimens.fontMedium
                    ), color = Color.White, modifier = Modifier.padding(bottom = 4.dp)
                )

            }
        }
    }
}

@Composable
fun ProfileCard(photoUrl: String, userName: String, onProfileClick: () -> Unit) {
    val dimens = LocalDimens.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(10.dp),
        modifier = Modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(dimens.paddingSmall)
        ) {
            LoadImageWithGlide(
                imageUrl = photoUrl,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onProfileClick() }
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = userName,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = dimens.fontMedium)
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
    Card(shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(10.dp),
        modifier = Modifier
            .padding(start = dimens.paddingSmall)
            .clickable {
                showCelebrationDialog = true
            }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = dimens.paddingMedium,
                vertical = dimens.paddingSmall
            )
        ) {
            Text(text = "🔥", fontSize = dimens.fontLarge)
            Spacer(modifier = Modifier.width(dimens.paddingSmall))
            Text(
                text = "$streakCount", style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = dimens.fontMedium
                ), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground
            )
        }
    }

}


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
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Your last 7 mood entries",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(.6f)
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
                                    .width(30.dp)
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
                                        fontSize = dimens.fontLarge
                                    ),
                                    modifier = Modifier
                                        .padding(bottom = dimens.paddingSmall)
                                        .align(Alignment.BottomCenter)
                                        .offset(y = -(moodScore / 100f * 150.dp.toPx()).toDp() - 1.dp)
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
fun AdviceCard(advice: String) {

    val dimens = LocalDimens.current
    Card(
        shape = RoundedCornerShape(
            topStart = dimens.paddingLarge,
            bottomEnd = dimens.paddingLarge
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimens.paddingSmall)
    ) {
        Text(
            text = advice, style = MaterialTheme.typography.bodyLarge.copy(
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface
            ), modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.paddingSmall)
        )
    }

}

@Composable
fun MeditateButton(navController: NavController) {
    val dimens = LocalDimens.current

    Card(shape = CircleShape,
        elevation = CardDefaults.cardElevation(dimens.elevation),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .size(dimens.avatarSize / 3)
            .clickable {
                navController.navigate(Screens.BREATHING.name)
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
                text = "Meditate?", style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground
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
            contentDescription = "Make Journals"
        )
        Spacer(modifier = Modifier.height(dimens.paddingSmall))
        Text(
            text = "Your Emotional Journey Awaits!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.background,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimens.paddingSmall))

        Column {
            Text(
                text = """
                 • Track your emotional highs and lows.
             """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Text(
                text = """
                 • Gain insights into your mood patterns.
             """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Text(
                text = """
                • Celebrate progress and identify triggers.
             """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
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
            color = MaterialTheme.colorScheme.background,
            strokeWidth = 4.dp
        )
    }
}


