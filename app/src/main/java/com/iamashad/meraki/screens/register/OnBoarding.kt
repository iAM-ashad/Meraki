package com.iamashad.meraki.screens.register

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.airbnb.lottie.compose.*
import com.google.accompanist.pager.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.iamashad.meraki.R
import com.iamashad.meraki.components.showToast
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnBoardingScreen(navController: NavController, viewModel: RegisterViewModel = hiltViewModel()) {
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val pages = listOf(
        OnboardingPage(
            title = "Welcome to Meraki",
            subtitle = "Your journey to emotional well-being starts here.",
            animation = R.raw.lottie_onb1,
            iterations = LottieConstants.IterateForever
        ),
        OnboardingPage(
            title = "Track Your Mood",
            subtitle = "Monitor your emotions and gain insights into your mental health.",
            animation = R.raw.lottie_onb2,
            iterations = 1
        ),
        OnboardingPage(
            title = "Make Journals",
            subtitle = "Record your thoughts and reflect on your day.",
            animation = R.raw.lottie_onb3,
            iterations = LottieConstants.IterateForever
        ),
        OnboardingPage(
            title = "Ready to Begin?",
            subtitle = "Take the first step towards a healthier, happier you.",
            animation = R.raw.lottie_onb4,
            iterations = LottieConstants.IterateForever
        )
    )

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).result
        viewModel.firebaseAuthWithGoogle(account) { success ->
            if (success) {
                navController.navigate("HOME") // Navigate to home screen upon successful sign-in
            } else {
                showToast(context, "Failed to sign in. Please try again.")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        HorizontalPager(
            count = pages.size,
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            OnboardingContent(pages[page], pages[page].iterations)
        }

        HorizontalPagerIndicator(
            pagerState = pagerState,
            activeColor = MaterialTheme.colorScheme.primary,
            inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.padding(16.dp)
        )

        Button(
            onClick = {
                scope.launch {
                    if (pagerState.currentPage == pages.size - 1) {
                        // Trigger Google Sign-In on the last page
                        val signInIntent = viewModel.getGoogleSignInIntent()
                        launcher.launch(signInIntent)
                    } else {
                        // Navigate to the next page
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            },
            modifier = Modifier
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (pagerState.currentPage == pages.size - 1) "I am ready to change my life!" else "Next ->",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun OnboardingContent(
    page: OnboardingPage, iterations: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(page.animation))
        val progress by animateLottieCompositionAsState(
            composition, iterations = iterations
        )

        Box(modifier = Modifier.size(250.dp)) {
            LottieAnimation(composition = composition, progress = { progress })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.title, style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = page.subtitle,
            style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

data class OnboardingPage(
    val title: String, val subtitle: String, val animation: Int, val iterations: Int
)
