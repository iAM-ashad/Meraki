package com.iamashad.meraki.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.iamashad.meraki.navigation.Screens

@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeScreenViewModel = hiltViewModel()
) {
    // Observe user data (FirebaseUser) using collectAsState for the StateFlow
    val user by homeViewModel.user.collectAsState()

    // Observe advice LiveData using observeAsState for the LiveData
    val advice by homeViewModel.advice.observeAsState("Loading advice...")

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Welcome ${user?.displayName ?: "User"}",
            style = MaterialTheme.typography.headlineLarge
        )

        // Optionally, display profile picture (if available)
        // Image(painter = rememberImagePainter(user?.photoUrl), contentDescription = null)

        // Display the advice
        Text("Here's a piece of advice: \"${advice}\"", style = MaterialTheme.typography.bodyMedium)

        Button(onClick = {
            homeViewModel.logout()
            navController.navigate(Screens.REGISTER.name)
        }) {
            Text("Log Out")
        }
    }
}
