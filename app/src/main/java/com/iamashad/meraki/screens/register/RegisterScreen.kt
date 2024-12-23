package com.iamashad.meraki.screens.register

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.iamashad.meraki.components.showToast
import com.iamashad.meraki.navigation.Screens

@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val user by viewModel.user.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).result
        viewModel.firebaseAuthWithGoogle(account) { success ->
            if (!success) {
                showToast(context, "Signed In",)
                navController.navigate(Screens.HOME.name)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (user != null) {
            Text("Welcome, ${user?.displayName ?: "User"}")
            Button(onClick = {
                navController.navigate(Screens.HOME.name)
            }) {
                Text("Let's Start")
            }
        } else {
            Button(onClick = {
                val signInIntent = viewModel.getGoogleSignInIntent()
                launcher.launch(signInIntent)
            }) {
                Text("Sign in with Google")
            }
            errorMessage?.let { Text("Error: $it", color = Color.Red) }
        }
    }
}

