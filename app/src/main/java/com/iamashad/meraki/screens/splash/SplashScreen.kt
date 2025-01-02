package com.iamashad.meraki.screens.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.R
import com.iamashad.meraki.navigation.Screens
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {

    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color.Black
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = "Meraki Logo",
            modifier = Modifier
                .size(320.dp)
        )
    }

    LaunchedEffect(key1 = true) {
        delay(2000)
        if (isLoggedIn) {
            navController.navigate(Screens.HOME.name) {
                popUpTo(Screens.SPLASH.name) { inclusive = true }
            }
        } else {
            navController.navigate(Screens.REGISTER.name) {
                popUpTo(Screens.SPLASH.name) { inclusive = true }
            }
        }
    }
}
