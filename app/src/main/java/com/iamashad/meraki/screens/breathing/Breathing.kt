package com.iamashad.meraki.screens.breathing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.iamashad.meraki.R
import kotlinx.coroutines.delay

@Composable
fun BreathingScreen(navController: NavController) {
    // State variables
    var isSessionActive by remember { mutableStateOf(false) }
    var timerValue by remember { mutableStateOf(210) } // Default session time: 210 seconds
    val progress = remember(timerValue) { 1f * timerValue / 210 }

    // Context for audio
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val audioUri =
                "android.resource://${context.packageName}/${R.raw.guided_breathing}"
            val mediaItem = androidx.media3.common.MediaItem.fromUri(audioUri)
            setMediaItem(mediaItem)
            prepare()
        }
    }

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.colors))

    // Effect for starting/stopping the session
    LaunchedEffect(isSessionActive) {
        if (isSessionActive) {
            exoPlayer.play()
            while (timerValue > 0) {
                delay(1000L)
                timerValue--
            }
            exoPlayer.pause()
            timerValue = 210
            isSessionActive = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Meditating Image (Visible during session)

        Image(
            painter = painterResource(R.drawable.breathingimage), // Replace with your image
            contentDescription = "Meditating Person",
            modifier = Modifier
                .size(150.dp) // Adjust the size as needed
                .padding(8.dp)
        )

        // Instructional Text
        Text(
            text = if (!isSessionActive) {
                "Relax your mind and body. Click the button below to start a guided breathing session."
            } else {
                "Follow the audio instructions to breathe in, breathe out, and relax."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Circular Progress Bar with Lottie Animation
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(250.dp)
        ) {
            // Circular Progress Bar
            androidx.compose.material3.CircularProgressIndicator(
                progress = progress,
                strokeWidth = 10.dp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize()
            )

            // Lottie Animation (Radial Animation)
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Start Button
        if (!isSessionActive) {
            Button(
                onClick = { isSessionActive = true },
                shape = MaterialTheme.shapes.medium
            ) {
                Text(text = "Start Breathing Session")
            }
        }
    }

    // Cleanup the audio player when composable leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
}



