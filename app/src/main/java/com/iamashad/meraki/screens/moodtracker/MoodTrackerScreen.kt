package com.iamashad.meraki.screens.moodtracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.iamashad.meraki.model.Mood
import com.iamashad.meraki.navigation.Screens
import com.iamashad.meraki.screens.home.HomeScreenViewModel

@Composable
fun MoodTrackerScreen(
    navController: NavController,
    moodTrackerViewModel: MoodTrackerViewModel = hiltViewModel(),
    homeViewModel: HomeScreenViewModel = hiltViewModel()
) {
    val user by homeViewModel.user.collectAsState()
    val firstName = user?.displayName?.split(" ")?.firstOrNull() ?: "User"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Greeting Section
        GreetingCard(firstName)

        // Spacer
        Spacer(modifier = Modifier.padding(vertical = 16.dp))

        // Mood Row
        Text(
            text = "Select your mood:",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 8.dp, bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            items(moodTrackerViewModel.moods) { mood ->
                MoodCard(mood, navController)
            }
        }
    }
}

@Composable
fun GreetingCard(userName: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(12.dp),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Hey $userName!",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "How are you feeling today?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun MoodCard(mood: Mood, navController: NavController) {
    Card(
        modifier = Modifier
            .size(120.dp)
            .clickable {
                when (mood.label) {
                    "Happy" -> navController.navigate(Screens.CELEBRATION.name)
                    "Sad" -> navController.navigate("${Screens.CHATBOT.name}/I feel sad. Please tell me a joke.")
                    "Anxious" -> navController.navigate(Screens.BREATHING.name)
                    "Calm" -> navController.navigate(Screens.MINDFULNESS.name)
                    "Excited" -> navController.navigate(Screens.JOURNAL.name)
                }
            },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Text(
                text = mood.emoji,
                style = MaterialTheme.typography.headlineLarge
            )

            Text(
                text = mood.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
