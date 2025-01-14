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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.iamashad.meraki.utils.getMoodColor
import com.iamashad.meraki.utils.getMoodEmoji

@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeScreenViewModel = hiltViewModel(),
    moodTrackerViewModel: MoodTrackerViewModel = hiltViewModel()
) {
    val user by homeViewModel.user.collectAsState()
    val advice by homeViewModel.advice.observeAsState("Loading advice...")
    val photoUrl by homeViewModel.photoUrl.collectAsState()
    val lastMoods by moodTrackerViewModel.moodTrend.collectAsState()
    val isLoading by moodTrackerViewModel.loading.collectAsState() // Observe loading state
    val streakCount = remember { mutableIntStateOf(0) }

    LaunchedEffect(user) {
        user?.uid?.let { userId ->
            homeViewModel.logDailyUsage(userId)
            streakCount.intValue = homeViewModel.calculateStreak(userId)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        MaterialTheme.colorScheme.onPrimaryContainer,
                        MaterialTheme.colorScheme.primaryContainer,
                    )
                )
            )
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileCard(
                photoUrl = photoUrl.toString(), userName = user?.displayName ?: "User"
            ) {
                navController.navigate(Screens.SETTINGS.name)
            }
            StreakMeterCard(streakCount = streakCount.intValue)
        }

        Spacer(modifier = Modifier.height(16.dp))

        WeeklyCalendar(navController)

        Spacer(modifier = Modifier.height(16.dp))

        MoodPromptCard(navController)

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                LoadingIndicator()
            }

            lastMoods.isNotEmpty() -> {
                MoodLogsCard(moodLogs = lastMoods.takeLast(5))
            }

            else -> {
                EmptyMoodLogs()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AdviceCard(advice)

        Spacer(modifier = Modifier.height(24.dp))

        MeditateButton(navController)
    }
}


@Composable
fun CelebrationDialog(
    onDismiss: () -> Unit, streakCount: Int
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(.9f)
                .fillMaxHeight(.35f)
                .padding(16.dp)
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
                    modifier = Modifier.padding(24.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Streak Info Text
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
                            color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

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
fun WeeklyCalendar(navController: NavController) {
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val calendar = java.util.Calendar.getInstance()
    val currentDayIndex =
        (calendar.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 // Adjusting index for Monday start
    val dates = (0..6).map { offset ->
        calendar.apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY + offset)
        }.get(java.util.Calendar.DAY_OF_MONTH)
    }

    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        daysOfWeek.forEachIndexed { index, day ->
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentDayIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable {
                        navController.navigate(Screens.MOODTRACKER.name)
                    }, contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = day, style = MaterialTheme.typography.bodySmall.copy(
                            color = if (index == currentDayIndex) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = dates[index].toString(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (index == currentDayIndex) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun MoodPromptCard(navController: NavController) {
    val userName = FirebaseAuth.getInstance().currentUser?.displayName
    val firstName = userName?.split(" ")?.firstOrNull()
    Card(shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .clickable {
                navController.navigate(Screens.MOODTRACKER.name)
            }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Hey $firstName, how are you feeling today?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface
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
                        textAlign = TextAlign.Center, fontSize = 18.sp
                    ), color = Color.White, modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}


@Composable
fun ProfileCard(
    photoUrl: String, userName: String, onProfileClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        ), elevation = CardDefaults.cardElevation(10.dp), modifier = Modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)
        ) {
            LoadImageWithGlide(
                imageUrl = photoUrl,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        onProfileClick.invoke()
                    })
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = userName, style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp
                )
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

    Card(shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(10.dp),
        modifier = Modifier
            .padding(start = 8.dp)
            .clickable {
                showCelebrationDialog = true
            }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(text = "🔥", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$streakCount", style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 20.sp
                ), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}


@Composable
fun MoodLogsCard(moodLogs: List<Pair<String, Int>>) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        val density = LocalDensity.current

        Column(
            modifier = Modifier.padding(16.dp)
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

            Spacer(modifier = Modifier.height(80.dp))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                moodLogs.forEach { (_, moodScore) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(50.dp)
                    ) {
                        Box(
                            modifier = Modifier.width(30.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Bar container
                            Box(
                                modifier = Modifier
                                    .width(30.dp)
                                    .height(170.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(16.dp)
                                    ), contentAlignment = Alignment.BottomCenter
                            ) {
                                // Filled part of the bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(fraction = moodScore / 100f)
                                        .background(
                                            color = getMoodColor(moodScore),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                )
                            }

                            // Larger emoji above the bar
                            with(density) {
                                Text(
                                    text = getMoodEmoji(moodScore),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 25.sp),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .offset(y = -(moodScore / 100f * 150.dp.toPx()).toDp() - 1.dp) // Increased offset for larger emoji
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
    Card(
        shape = RoundedCornerShape(topStart = 25.dp, bottomEnd = 25.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        Text(
            text = advice, style = MaterialTheme.typography.bodyLarge.copy(
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface
            ), modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}

@Composable
fun MeditateButton(navController: NavController) {
    Card(shape = CircleShape,
        elevation = CardDefaults.cardElevation(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .size(100.dp)
            .clickable {
                navController.navigate(Screens.BREATHING.name)
            }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.inversePrimary
                        ), center = Offset.Zero, radius = 150f
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_moodtrack),
            contentDescription = "Make Journals"
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your Emotional Journey Awaits!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.background,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))

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


