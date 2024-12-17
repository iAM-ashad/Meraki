package com.iamashad.meraki.screens.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.iamashad.meraki.R
import com.iamashad.meraki.navigation.Screens
import com.iamashad.meraki.ui.theme.MerakiTheme
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController
) {

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.splash_animation))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.merakilogo),
            contentDescription = null,
        )

        Box {
            LottieAnimation(
                composition = composition,
                restartOnPlay = true,
                reverseOnRepeat = true,
                modifier = Modifier
                    .alpha(.2f)
                    .size(325.dp)
            )
        }
    }
    LaunchedEffect(key1 = true) {
        delay(2500)
        navController.navigate(Screens.HOME.name)
    }
}

@Preview
@Composable
fun ScreenPreview() {
    MerakiTheme {
        val navController = rememberNavController()
        SplashScreen(navController)
    }
}