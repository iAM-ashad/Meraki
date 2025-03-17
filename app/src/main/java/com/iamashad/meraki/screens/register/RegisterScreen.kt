package com.iamashad.meraki.screens.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.iamashad.meraki.R
import com.iamashad.meraki.components.ConnectivityObserver
import com.iamashad.meraki.navigation.Screens
import com.iamashad.meraki.utils.ConnectivityStatus
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.ProvideDimens
import com.iamashad.meraki.utils.rememberWindowSizeClass

@Composable
fun RegisterScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val isConnected = ConnectivityStatus(context)
    val windowSize = rememberWindowSizeClass()

    ConnectivityObserver(connectivityStatus = isConnected) {
        ProvideDimens(windowSize) {
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
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = dimens.paddingMedium * 2)
                    )
                    Text(
                        text = """"Essence of Yourself"""",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(.8f)
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
                            modifier = Modifier.size((dimens.avatarSize))
                        )
                    }

                    Spacer(modifier = Modifier.height(dimens.paddingLarge * 2))

                    Text(
                        text = "Your journey to emotional well-being starts here.",
                        style = MaterialTheme.typography.bodyMedium.copy(
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
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(dimens.paddingMedium))

                    TextButton(onClick = {
                        navController.navigate(Screens.LOGIN.name)
                    }) {
                        Text(
                            text = "Already have an account? Log In",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.secondary)
                        )
                    }
                }
            }
        }
    }
}
