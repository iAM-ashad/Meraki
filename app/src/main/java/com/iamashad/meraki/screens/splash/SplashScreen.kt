package com.iamashad.meraki.screens.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.navigation.Chatbot
import com.iamashad.meraki.navigation.Home
import com.iamashad.meraki.navigation.Onboarding
import com.iamashad.meraki.navigation.Register
import com.iamashad.meraki.navigation.Splash

/**
 * Invisible auth-gate composable shown at cold-start.
 *
 * Renders nothing — navigation fires as soon as DataStore emits the
 * [SplashViewModel.hasCompletedOnboarding] value, which is typically within one
 * frame. The Android 12+ system splash (dark background, no icon) bridges the gap
 * until Compose is ready, so the user never sees a blank screen.
 *
 * Routing rules:
 *  - Unauthenticated → [Register]
 *  - Authenticated, onboarding incomplete → [Onboarding]
 *  - Authenticated, onboarding complete, notification deep-link → [Chatbot]
 *  - Authenticated, onboarding complete → [Home]
 *
 * @param navController      App-level NavController.
 * @param navigateToChatbot  True when the launch originated from a notification tap.
 * @param viewModel          Hilt ViewModel that exposes the DataStore readiness gate.
 */
@Composable
fun SplashScreen(
    navController: NavController,
    navigateToChatbot: Boolean = false,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val isLoggedIn by remember {
        mutableStateOf(FirebaseAuth.getInstance().currentUser != null)
    }

    // Wait for DataStore to emit a real (non-null) value before navigating.
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsState()

    LaunchedEffect(hasCompletedOnboarding) {
        val onboardingDone = hasCompletedOnboarding ?: return@LaunchedEffect

        if (isLoggedIn) {
            if (!onboardingDone) {
                // Authenticated user who hasn't finished onboarding yet (e.g. fresh install
                // with existing Firebase account, or onboarding interrupted and app re-launched).
                navController.navigate(Onboarding) {
                    popUpTo<Splash> { inclusive = true }
                }
                return@LaunchedEffect
            }

            // Phase 5: honour the notification deep-link by going to Chatbot directly.
            val destination = if (navigateToChatbot) Chatbot() else Home
            navController.navigate(destination) {
                popUpTo<Splash> { inclusive = true }
            }
        } else {
            // Never bypass auth — unauthenticated users always land on Register.
            navController.navigate(Register) {
                popUpTo<Splash> { inclusive = true }
            }
        }
    }
}
