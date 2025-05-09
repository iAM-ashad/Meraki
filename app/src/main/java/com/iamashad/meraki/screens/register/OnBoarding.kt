package com.iamashad.meraki.screens.register

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import com.iamashad.meraki.R
import com.iamashad.meraki.navigation.Screens
import com.iamashad.meraki.utils.LocalDimens
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnBoardingScreen(navController: NavController) {
    val dimens = LocalDimens.current
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()

    val pages = listOf(
        OnboardingPage(
            title = "Your Safe Haven",
            subtitle = "This is a place where your thoughts are safe, your feelings are valid, and your journey to self-healing begins. Let's walk this path together.",
            animation = R.raw.lottie_onb1,
            iterations = LottieConstants.IterateForever
        ),
        OnboardingPage(
            title = "Understand Your Emotions",
            subtitle = "Every feeling matters. Discover patterns in your emotions, gain clarity, and learn how to navigate life's ups and downs with ease.",
            animation = R.raw.lottie_onb2,
            iterations = 1
        ),
        OnboardingPage(
            title = "Write Your Heart Out",
            subtitle = "Your story is worth telling. Reflect on your days, untangle your thoughts, and find peace through the power of your own words.",
            animation = R.raw.lottie_onb3,
            iterations = LottieConstants.IterateForever
        ),
        OnboardingPage(
            title = "Your Journey Starts Today",
            subtitle = "Step into a world of self-discovery and transformation. You deserve a life filled with joy, clarity, and strength.",
            animation = R.raw.lottie_onb4,
            iterations = LottieConstants.IterateForever
        )
    )

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
            val pageOffset = pagerState.currentPageOffset
            val scale = 1f - 0.1f * kotlin.math.abs(pageOffset)
            val alpha = 1f - 0.3f * kotlin.math.abs(pageOffset)

            OnboardingContent(
                page = pages[page],
                iterations = pages[page].iterations,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            )
        }

        HorizontalPagerIndicator(
            pagerState = pagerState,
            activeColor = MaterialTheme.colorScheme.primary,
            inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.padding(dimens.paddingSmall)
        )

        val pulseScale by rememberInfiniteTransition().animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 500, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            )
        )

        Button(
            onClick = {
                scope.launch {
                    if (pagerState.currentPage == pages.size - 1) {
                        navController.navigate(Screens.CREATEUSER.name)
                    } else {
                        // Navigate to the next page
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            },
            modifier = Modifier
                .padding()
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (pagerState.currentPage == pages.size - 1) "Let's Get Started!" else "Next ->",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun OnboardingContent(
    page: OnboardingPage,
    iterations: Int,
    modifier: Modifier = Modifier
) {
    val dimens = LocalDimens.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimens.paddingMedium),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(page.animation))
        val progress by animateLottieCompositionAsState(
            composition, iterations = iterations
        )

        Box(modifier = Modifier.size((dimens.avatarSize / 2) * 1)) {
            LottieAnimation(composition = composition, progress = { progress })
        }

        Spacer(modifier = Modifier.height(dimens.paddingMedium))

        Text(
            text = page.title, style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.height(dimens.paddingMedium))
        Text(
            text = page.subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}


data class OnboardingPage(
    val title: String, val subtitle: String, val animation: Int, val iterations: Int
)
