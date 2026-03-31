package com.iamashad.meraki.screens.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.navigation.Chatbot
import com.iamashad.meraki.navigation.Home
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
 * @param navController  App-level NavController.
 * @param navigateToChatbot  True when the launch originated from a notification tap.
 */
@Composable
fun SplashScreen(
    navController: NavController,
    navigateToChatbot: Boolean = false
) {
    val isLoggedIn by remember {
        mutableStateOf(FirebaseAuth.getInstance().currentUser != null)
    }

    LaunchedEffect(key1 = true) {
        if (isLoggedIn) {
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
