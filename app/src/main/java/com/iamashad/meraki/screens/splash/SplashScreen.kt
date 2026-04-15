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
 * Auth-gate screen shown at cold-start before the first real destination is known.
 *
 * Phase 5 addition: [navigateToChatbot] flag.
 * When a notification deep-link caused this fresh launch, [navigateToChatbot] is true.
 * Authenticated users are routed directly to [Chatbot] instead of [Home]; unauthenticated
 * users always go to [Register] regardless of the deep-link (security boundary).
 *
 * Onboarding Overhaul — Phase 1:
 * Reads [SplashViewModel.hasCompletedOnboarding] from DataStore.
 * Authenticated users who have NOT yet completed the new onboarding arc are routed
 * to [Onboarding] instead of [Home], ensuring the flow runs exactly once per account.
 *
 * Navigation waits until the DataStore value is available (non-null) to prevent a flash
 * where returning users briefly see the onboarding screen.
 *
 * @param navController      App-level NavController.
 * @param navigateToChatbot  True when the launch originated from a notification tap.
 * @param viewModel          Hilt ViewModel that exposes the onboarding gate flag.
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

    // null = DataStore not yet read; wait before navigating to avoid flicker
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsState()

    LaunchedEffect(hasCompletedOnboarding) {
        // Wait until DataStore emits a real value (not the null placeholder)
        val onboardingDone = hasCompletedOnboarding ?: return@LaunchedEffect

        if (isLoggedIn) {
            if (!onboardingDone) {
                // Authenticated user who hasn't finished onboarding yet (e.g. fresh install
                // with existing Firebase account, or onboarding interrupted and app re-launched)
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
