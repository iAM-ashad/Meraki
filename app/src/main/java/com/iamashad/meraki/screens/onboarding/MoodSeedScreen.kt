package com.iamashad.meraki.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.iamashad.meraki.R
import com.iamashad.meraki.navigation.CreateUser
import com.iamashad.meraki.navigation.MoodSeed
import com.iamashad.meraki.utils.LocalDimens

/**
 * Phase 3: Pre-account mood capture screen.
 *
 * Positioned between [OnBoardingScreen] and [CreateUserScreen].
 * Presents a single question — "Before we begin — how are you feeling right now?" —
 * with 5 emotion chips. The selection is stored in [OnboardingViewModel.selectMood]
 * and later persisted to Room via [OnboardingViewModel.persistMoodSeed] after
 * Firebase account creation in [CreateUserScreen].
 *
 * The mood is intentionally captured BEFORE sign-up so the AI has emotional context
 * from the very first interaction, even before the user has a UID.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodSeedScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val dimens = LocalDimens.current
    val uiState by viewModel.uiState.collectAsState()

    // 5 emotions as per the plan — kept simple and universally relatable
    val moods = listOf(
        MoodOption(label = "Happy", emoji = "😊"),
        MoodOption(label = "Calm", emoji = "😌"),
        MoodOption(label = "Anxious", emoji = "😟"),
        MoodOption(label = "Sad", emoji = "😢"),
        MoodOption(label = "Tired", emoji = "😴")
    )

    val lottieComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.lottie_heartcare)
    )
    val lottieProgress by animateLottieCompositionAsState(
        lottieComposition, iterations = LottieConstants.IterateForever
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(dimens.paddingMedium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Warm Lottie header
        LottieAnimation(
            composition = lottieComposition,
            progress = { lottieProgress },
            modifier = Modifier.size(dimens.avatarSize / 2)
        )

        Spacer(modifier = Modifier.height(dimens.paddingMedium))

        Text(
            text = "Before we begin —",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "how are you feeling\nright now?",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(dimens.paddingMedium + dimens.paddingSmall))

        // Mood chips in a wrapping flow row
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                space = dimens.paddingSmall,
                alignment = Alignment.CenterHorizontally
            ),
            verticalArrangement = Arrangement.spacedBy(dimens.paddingSmall)
        ) {
            moods.forEach { moodOption ->
                val isSelected = uiState.selectedMood == moodOption.label

                Box(
                    modifier = Modifier
                        .background(
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.extraLarge
                        )
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.extraLarge
                        )
                        .clickable { viewModel.selectMood(moodOption.label) }
                        .padding(horizontal = dimens.paddingMedium, vertical = dimens.paddingSmall),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${moodOption.emoji}  ${moodOption.label}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(dimens.paddingMedium + dimens.paddingSmall))

        Button(
            onClick = {
                // Navigate to CreateUser whether or not a mood was selected.
                // If no mood selected it defaults to "neutral" in the ViewModel.
                if (uiState.selectedMood == null) viewModel.selectMood("Calm")
                navController.navigate(CreateUser) {
                    popUpTo<MoodSeed> { inclusive = false }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Continue →",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

private data class MoodOption(val label: String, val emoji: String)
