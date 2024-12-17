package com.iamashad.meraki.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iamashad.meraki.screens.home.HomeScreen
import com.iamashad.meraki.screens.splash.SplashScreen

@Composable
fun MerakiNavigation() {
    val navController = rememberNavController()
    NavHost (
        navController = navController,
        startDestination = Screens.SPLASH.name
    ) {
        composable(Screens.SPLASH.name) {
            SplashScreen(navController)
        }
        composable(Screens.HOME.name) {
            HomeScreen()
        }
    }
}