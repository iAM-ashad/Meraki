package com.iamashad.meraki.screens.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.navigation.Home
import com.iamashad.meraki.navigation.Register
import com.iamashad.meraki.navigation.Splash

@Composable
fun SplashScreen(navController: NavController) {

    var isLoggedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }

    LaunchedEffect(key1 = true) {
        if (isLoggedIn) {
            navController.navigate(Home) {
                popUpTo<Splash> { inclusive = true }
            }
        } else {
            navController.navigate(Register) {
                popUpTo<Splash> { inclusive = true }
            }
        }
    }
}
