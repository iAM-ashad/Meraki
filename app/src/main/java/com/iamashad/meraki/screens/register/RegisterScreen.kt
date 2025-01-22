package com.iamashad.meraki.screens.register

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.iamashad.meraki.R
import com.iamashad.meraki.components.ConnectivityObserver
import com.iamashad.meraki.components.showToast
import com.iamashad.meraki.navigation.Screens
import com.iamashad.meraki.utils.ConnectivityStatus
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.ProvideDimens

@Composable
fun RegisterScreen(navController: NavController, viewModel: RegisterViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val isConnected = ConnectivityStatus(context)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).result
        viewModel.firebaseAuthWithGoogle(account) { success ->
            if (success) {
                navController.navigate(Screens.HOME.name)
            } else {
                showToast(context, "Failed to sign in.")
            }
        }
    }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    ConnectivityObserver(connectivityStatus = isConnected) {
        ProvideDimens(screenWidth, screenHeight) {
            val dimens = LocalDimens.current

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.surface
                    ), contentAlignment = Alignment.Center
            ) {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_care))
                val progress by animateLottieCompositionAsState(
                    composition = composition, iterations = LottieConstants.IterateForever
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Welcome to Meraki",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = dimens.fontLarge * 1.5
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = dimens.paddingMedium * 2)
                    )
                    Text(
                        text = """"Essence of Yourself"""",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(.8f),
                            fontSize = dimens.fontSmall * 1.5
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = dimens.paddingSmall / 2)
                    )

                    Spacer(modifier = Modifier.height(dimens.paddingMedium * 2))

                    Box(
                        modifier = Modifier, contentAlignment = Alignment.Center
                    ) {
                        LottieAnimation(
                            composition = composition,
                            progress = { progress },
                            modifier = Modifier.size((dimens.avatarSize / 3) * 2)
                        )
                    }

                    Spacer(modifier = Modifier.height(dimens.paddingLarge * 2))

                    Text(
                        text = "Your journey to emotional well-being starts here.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(dimens.paddingMedium * 2))

                    Button(
                        onClick = { navController.navigate(Screens.ONBOARDING.name) },
                        modifier = Modifier
                            .padding(dimens.paddingSmall),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Get Started  \uD83D\uDC95",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(dimens.paddingMedium))

                    TextButton(onClick = {
                        val signInIntent = viewModel.getGoogleSignInIntent()
                        launcher.launch(signInIntent)
                    }) {
                        Text(
                            text = "Already have an account? Log In",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.secondary)
                        )
                    }
                }
            }
        }
    }
}
