package com.iamashad.meraki.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.R
import com.iamashad.meraki.navigation.NotificationSetup
import com.iamashad.meraki.navigation.WelcomeMeraki
import com.iamashad.meraki.notifications.Day3ReEngagementWorker
import com.iamashad.meraki.utils.LocalDimens
import kotlinx.coroutines.delay

/**
 * Phase 3: AI-powered personalised welcome screen.
 *
 * What happens here:
 *  1. Triggers [OnboardingViewModel.generateWelcome] with the user's name + mood seed.
 *  2. Renders the 2-sentence welcome with a typewriter animation pattern.
 *  3. Displays the first AI-generated journal prompt in a soft card ("Your first reflection").
 *  4. After the welcome renders, seeds [MemoryManager] (handled inside the ViewModel).
 *  5. "Start my journey" marks onboarding complete and navigates to [NotificationSetup].
 */
@Composable
fun WelcomeAIScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val dimens = LocalDimens.current
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val currentUser = FirebaseAuth.getInstance().currentUser
    val userName = currentUser?.displayName?.split(" ")?.firstOrNull() ?: "there"
    val userId = currentUser?.uid.orEmpty()

    // Trigger welcome generation on first composition
    LaunchedEffect(Unit) {
        viewModel.generateWelcome(userName = userName, userId = userId)
        // Persist the mood seed now that we have a userId
        if (userId.isNotEmpty()) {
            viewModel.persistMoodSeed(userId)
        }
        // Phase 5: Schedule the Day 3 re-engagement worker (72h from now).
        // Uses applicationContext to avoid leaking the Activity context into WorkManager.
        Day3ReEngagementWorker.schedule(context.applicationContext)
    }

    // Typewriter state for the welcome text
    var displayedWelcome by remember { mutableStateOf("") }
    var showPromptCard by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.welcomeText) {
        if (uiState.welcomeText.isNotEmpty()) {
            displayedWelcome = ""
            for (char in uiState.welcomeText) {
                displayedWelcome += char
                delay(28L)
            }
            // Show the journal prompt card after welcome finishes
            delay(400L)
            showPromptCard = true
        }
    }

    val lottieComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.lottie_chatbot)
    )
    val lottieProgress by animateLottieCompositionAsState(
        lottieComposition, iterations = LottieConstants.IterateForever
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(dimens.paddingMedium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(dimens.paddingMedium))

        LottieAnimation(
            composition = lottieComposition,
            progress = { lottieProgress },
            modifier = Modifier.size(dimens.avatarSize / 2)
        )

        Spacer(modifier = Modifier.height(dimens.paddingMedium))

        Text(
            text = "Hi $userName 👋",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(dimens.paddingSmall))

        // AI welcome message with typewriter render
        if (uiState.isGeneratingWelcome && displayedWelcome.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(28.dp)
                    .padding(top = dimens.paddingSmall),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(dimens.paddingMedium)
            ) {
                Text(
                    text = if (displayedWelcome.isNotEmpty()) displayedWelcome
                           else uiState.welcomeText.ifEmpty { "Welcome to Meraki, $userName. I'm really glad you're here. 💙" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(dimens.paddingMedium))

        // First journal prompt card — slides in after the welcome finishes
        AnimatedVisibility(
            visible = showPromptCard,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(dimens.paddingMedium)) {
                    Text(
                        text = "✨ Your first reflection",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    )
                    Spacer(modifier = Modifier.height(dimens.paddingSmall / 2))
                    Text(
                        text = uiState.firstJournalPrompt.ifEmpty {
                            "Tonight, what's one thing you're grateful for today?"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(dimens.paddingMedium + dimens.paddingSmall))

        Button(
            onClick = {
                // Mark onboarding complete and continue to notification setup
                viewModel.markOnboardingComplete()
                navController.navigate(NotificationSetup) {
                    popUpTo<WelcomeMeraki> { inclusive = true }
                }
            },
            enabled = !uiState.isGeneratingWelcome,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Start my journey →",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(dimens.paddingMedium))
    }
}
