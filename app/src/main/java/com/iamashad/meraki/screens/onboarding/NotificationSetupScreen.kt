package com.iamashad.meraki.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.iamashad.meraki.components.MerakiVideoLoader
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import com.iamashad.meraki.navigation.Home
import com.iamashad.meraki.navigation.NotificationSetup
import com.iamashad.meraki.utils.LocalDimens

/**
 * Phase 4: Smart Notification Setup Screen.
 *
 * Replaces the cold, context-free Android runtime permission dialog with an
 * explained, consensual opt-in that arrives during onboarding — when the user
 * is warmest and most open to engagement.
 *
 * Features:
 *  - Lottie animation + headline "Stay connected to yourself".
 *  - Two paragraphs explaining the daily check-in and weekly AI insight.
 *  - A free-text time input ("When do you usually wind down?") parsed by a
 *    lightweight, non-streaming Groq call in [OnboardingViewModel.parseAndSetCheckInTime].
 *    Fallback: 20:00 if parsing fails.
 *  - "Enable reminders" primary button — requests Android 13+ permission, sets
 *    dailyCheckInEnabled = true, saves the parsed time, navigates to [Home].
 *  - "Maybe later" secondary text button — navigates to [Home] with notifications off.
 *    No shame, no dark patterns. The user can re-enable in Settings at any time.
 */
@Composable
fun NotificationSetupScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val dimens = LocalDimens.current
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var timeInput by remember { mutableStateOf("") }
    // Tracks whether we're waiting for Groq to finish parsing before navigating.
    var pendingNavigation by remember { mutableStateOf(false) }

    // Navigate to Home only once Groq has finished parsing the time input.
    // Without this gate the user lands on Home before parsedCheckInTime is updated.
    LaunchedEffect(uiState.isParsingTime) {
        if (pendingNavigation && !uiState.isParsingTime) {
            pendingNavigation = false
            navController.navigate(Home) {
                popUpTo<NotificationSetup> { inclusive = true }
            }
        }
    }

    // Android 13+ notification permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Result ignored — permission granted or not, we proceed to Home */ }

    val lottieComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.mindful_nudges)
    )
    val lottieProgress by animateLottieCompositionAsState(
        lottieComposition, iterations = LottieConstants.IterateForever
    )

    Box(modifier = Modifier.fillMaxSize()) {
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
            modifier = Modifier.size(dimens.avatarSize / 2 + 24.dp)
        )

        Spacer(modifier = Modifier.height(dimens.paddingMedium))

        Text(
            text = "Stay connected\nto yourself",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(dimens.paddingMedium))

        // Notification type 1: Daily check-in
        NotificationTypeCard(
            icon = "🌙",
            title = "Daily check-in",
            description = "A gentle reminder at your preferred time to pause, check in with yourself, and log how you're feeling."
        )

        Spacer(modifier = Modifier.height(dimens.paddingSmall))

        // Notification type 2: Weekly AI insight
        NotificationTypeCard(
            icon = "✨",
            title = "Weekly AI insight",
            description = "Once a week, Meraki surfaces a personalised reflection based on what you've been working through."
        )

        Spacer(modifier = Modifier.height(dimens.paddingMedium))

        Text(
            text = "When do you usually wind down for the evening?",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(dimens.paddingSmall))

        // Natural language time input — Groq parses "9pm", "around 9", etc.
        OutlinedTextField(
            value = timeInput,
            onValueChange = { timeInput = it },
            placeholder = {
                Text(
                    text = "e.g. 9pm, around 9 at night, after dinner",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(dimens.cornerRadius),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.parsedCheckInTime != "20:00" || timeInput.isNotEmpty()) {
            Text(
                text = "I'll remind you at ${uiState.parsedCheckInTime} ✓",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(dimens.paddingMedium + dimens.paddingSmall))

        // Primary CTA: enable reminders
        Button(
            onClick = {
                focusManager.clearFocus()

                // Request Android 13+ notification permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }

                // Enable daily check-in flag in DataStore
                viewModel.enableNotifications()

                if (timeInput.isNotBlank()) {
                    // Parse the natural-language time via Groq and defer navigation until done.
                    pendingNavigation = true
                    viewModel.parseAndSetCheckInTime(timeInput)
                } else {
                    // No time input — navigate immediately (default 20:00 will be used)
                    navController.navigate(Home) {
                        popUpTo<NotificationSetup> { inclusive = true }
                    }
                }
            },
            enabled = !uiState.isParsingTime,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Enable reminders",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(dimens.paddingSmall))

        // Secondary: "Maybe later" — first-class skip, no penalty
        TextButton(
            onClick = {
                navController.navigate(Home) {
                    popUpTo<NotificationSetup> { inclusive = true }
                }
            }
        ) {
            Text(
                text = "Maybe later",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            )
        }

        Spacer(modifier = Modifier.height(dimens.paddingMedium))
    } // end Column

    // Video loader overlay while Groq parses the natural-language time input.
    // Interaction is implicitly blocked because the button is disabled while isParsingTime == true.
    if (uiState.isParsingTime) {
        MerakiVideoLoader(modifier = Modifier.fillMaxSize())
    }
    } // end Box
}

@Composable
private fun NotificationTypeCard(icon: String, title: String, description: String) {
    val dimens = LocalDimens.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            )
            .padding(dimens.paddingMedium)
    ) {
        Text(
            text = "$icon  $title",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}
