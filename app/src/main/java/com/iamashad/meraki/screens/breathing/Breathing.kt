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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.iamashad.meraki.R
import kotlinx.coroutines.delay

/**
 * A guided breathing session screen using audio, animation, and countdown.
 * Helps users relax through a calming routine.
 */
@Composable
fun BreathingScreen() {
    // State for session status and timer
    var isSessionActive by remember { mutableStateOf(false) }
    var timerValue by remember { mutableIntStateOf(210) } // 3.5 minutes session
    val progress = remember(timerValue) { 1f * timerValue / 210 }

    // Set up ExoPlayer to play local audio
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val audioUri = "android.resource://${context.packageName}/${R.raw.guided_breathing}"
            val mediaItem = androidx.media3.common.MediaItem.fromUri(audioUri)
            setMediaItem(mediaItem)
            prepare()
        }
    }

    // Load Lottie animation (e.g., a pulsing circle or relaxing visual)
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.colors))

    /**
     * Starts session: countdown + audio
     * Auto-resets when finished
     */
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

    // UI Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Calm image (optional branding or visual)
        Image(
            painter = painterResource(R.drawable.breathingimage),
            contentDescription = "Meditating Person",
            modifier = Modifier
                .size(150.dp)
                .padding(8.dp)
        )

        // Instructional text based on session state
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

        // Circular breathing progress indicator with Lottie visual
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(250.dp)
        ) {
            CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 10.dp
            )

            // Lottie animation inside the progress circle
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

        // Start button (only visible when session is not running)
        if (!isSessionActive) {
            Button(
                onClick = { isSessionActive = true },
                shape = MaterialTheme.shapes.medium
            ) {
                Text(text = "Start Breathing Session")
            }
        }
    }

    // Release the audio player when the screen is dismissed
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
}
