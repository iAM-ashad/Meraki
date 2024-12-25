package com.iamashad.meraki.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iamashad.meraki.R
import com.iamashad.meraki.screens.chatbot.ChatViewModel
import com.iamashad.meraki.screens.chatbot.ChatbotScreen
import com.iamashad.meraki.screens.moodtracker.MoodTrackerScreen
import com.iamashad.meraki.screens.home.HomeScreen
import com.iamashad.meraki.screens.register.RegisterScreen
import com.iamashad.meraki.screens.splash.SplashScreen

@Composable
fun MerakiNavigation() {
    val navController = rememberNavController()

    // Get the current route to determine when to show the bottom bar
    val currentDestination = navController.currentBackStackEntryFlow.collectAsState(initial = null).value?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Show BottomNavigationBar only for specific screens
            if (shouldShowBottomBar(currentDestination)) {
                BottomNavigationBar(navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screens.SPLASH.name,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screens.SPLASH.name) {
                SplashScreen(navController)
            }
            composable(Screens.HOME.name) {
                HomeScreen(navController)
            }
            composable(Screens.REGISTER.name) {
                RegisterScreen(navController)
            }
            composable(Screens.CHATBOT.name) {
                val viewModel = ChatViewModel()
                ChatbotScreen(viewModel = viewModel)
            }
            composable(Screens.MOODTRACKER.name) {
                MoodTrackerScreen(navController)
            }
        }
    }
}

@Composable
fun shouldShowBottomBar(currentDestination: String?): Boolean {
    return currentDestination in listOf(
        Screens.HOME.name,
        Screens.CHATBOT.name,
        Screens.MOODTRACKER.name
    )
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    NavigationBar {
        NavigationBarItem(
            icon = {
                Icon(painter = painterResource(id = R.drawable.home_icon), contentDescription = null)
            },
            label = { Text("Home") },
            selected = navController.currentDestination?.route == Screens.HOME.name,
            onClick = {
                navController.navigate(Screens.HOME.name) {
                    popUpTo(Screens.HOME.name) { saveState = true }
                    launchSingleTop = true
                }
            }
        )

        NavigationBarItem(
            icon = {
                Icon(painter = painterResource(id = R.drawable.chat_icon), contentDescription = null)
            },
            label = { Text("Chatbot") },
            selected = navController.currentDestination?.route == Screens.CHATBOT.name,
            onClick = {
                navController.navigate(Screens.CHATBOT.name) {
                    popUpTo(Screens.CHATBOT.name) { saveState = true }
                    launchSingleTop = true
                }
            }
        )

        NavigationBarItem(
            icon = {
                Icon(painter = painterResource(id = R.drawable.metrics_icon), contentDescription = null)
            },
            label = { Text("Health") },
            selected = navController.currentDestination?.route == Screens.MOODTRACKER.name,
            onClick = {
                navController.navigate(Screens.MOODTRACKER.name) {
                    popUpTo(Screens.MOODTRACKER.name) { saveState = true }
                    launchSingleTop = true
                }
            }
        )
    }
}


