package com.iamashad.meraki.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.iamashad.meraki.navigation.Screens
import com.iamashad.meraki.screens.moodtracker.MoodTrackerViewModel
import com.iamashad.meraki.ui.theme.bodyFontFamily
import com.iamashad.meraki.ui.theme.displayFontFamily
import com.iamashad.meraki.utils.LoadImageWithGlide

@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeScreenViewModel = hiltViewModel(),
    moodTrackerViewModel: MoodTrackerViewModel = hiltViewModel()
) {
    val user by homeViewModel.user.collectAsState()
    val firstName = user?.displayName?.split(" ")?.firstOrNull() ?: "User"

    val advice by homeViewModel.advice.observeAsState("Loading advice...")

    val photoUrl by homeViewModel.photoUrl.collectAsState()

    val lastMoods by moodTrackerViewModel.moodTrend.collectAsState()

    val streakCount = remember { mutableIntStateOf(0) }

    // Log daily usage and calculate streak
    LaunchedEffect(user) {
        user?.uid?.let { userId ->
            homeViewModel.logDailyUsage(userId)
            streakCount.intValue = homeViewModel.calculateStreak(userId)
        }
    }

    // Make the screen scrollable
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
        // Profile and Streak Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileCard(photoUrl = photoUrl.toString(), userName = user?.displayName ?: "User")
            StreakMeterCard(streakCount = streakCount.intValue)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Mood Logs Section
        if (lastMoods.isNotEmpty()) {
            MoodLogsCard(moodLogs = lastMoods.takeLast(5))
        } else {
            Text(
                text = "No mood data available. Log your mood to see trends!",
                fontSize = 16.sp,
                fontFamily = bodyFontFamily,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Advice Card
        AdviceCard(advice)

        Spacer(modifier = Modifier.height(24.dp))

        // Welcome Message
        Text(
            text = "Welcome Back, $firstName!",
            fontSize = 25.sp,
            fontFamily = bodyFontFamily,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 8.dp)
        )

        Spacer(modifier = Modifier.height(15.dp))

        // Meditate Button
        MeditateButton(navController)
    }
}

@Composable
fun ProfileCard(photoUrl: String, userName: String) {
    Card(
        shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        ), elevation = CardDefaults.cardElevation(10.dp), modifier = Modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)
        ) {
            LoadImageWithGlide(imageUrl = photoUrl,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        // TODO: Navigate to Profile Screen
                    })
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = userName,
                fontSize = 16.sp,
                fontFamily = bodyFontFamily,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StreakMeterCard(streakCount: Int) {
    Card(
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(10.dp),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(text = "🔥", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$streakCount",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun MoodLogsCard(moodLogs: List<Pair<String, Int>>) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
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
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
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

// Function to get the appropriate emoji for a mood score
fun getMoodEmoji(score: Int): String {
    return when (score) {
        in 0..10 -> "😡" // Angry face
        in 11..20 -> "😞" // Sad face
        in 21..30 -> "😔" // Pensive face
        in 31..40 -> "😟" // Worried face
        in 41..50 -> "😐" // Neutral face
        in 51..60 -> "🙂" // Slightly smiling face
        in 61..70 -> "😊" // Smiling face
        in 71..80 -> "😃" // Big smile
        in 81..90 -> "😄" // Grinning face
        in 91..100 -> "😍" // Heart eyes
        else -> "😶" // Blank face
    }
}

// Function to get the appropriate color for a mood score
fun getMoodColor(score: Int): Color {
    return when (score) {
        in 0..39 -> Color(227, 56, 0, 255)
        in 40..60 -> Color(222, 202, 43, 255)
        else -> Color(60, 187, 65, 255)
    }
}

@Composable
fun AdviceCard(advice: String) {
    Card(
        shape = RoundedCornerShape(topStart = 25.dp, bottomEnd = 25.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        Text(
            text = advice,
            fontSize = 16.sp,
            fontFamily = displayFontFamily,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp)
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
                text = "Meditate?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
