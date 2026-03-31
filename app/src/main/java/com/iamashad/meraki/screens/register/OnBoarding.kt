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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.iamashad.meraki.R
import com.iamashad.meraki.navigation.CreateUser
import com.iamashad.meraki.utils.LocalDimens
import kotlinx.coroutines.launch

@Composable
fun OnBoardingScreen(navController: NavController) {
    val dimens = LocalDimens.current

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
        ),
        OnboardingPage(
            title = "Meraki Learns With You",
            subtitle = "After a few conversations, Meraki may send you a gentle evening nudge — something warm and tailored to what you've been carrying. You can turn this off any time in Settings.",
            animation = R.raw.lottie_onb2,
            iterations = LottieConstants.IterateForever
        )
    )

    // Foundation pager: pageCount is a lambda — evaluated lazily so safe to reference pages here.
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Foundation HorizontalPager: no `count` param — count is owned by pagerState.
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            // currentPageOffsetFraction replaces Accompanist's currentPageOffset.
            // Applying the same fraction to every rendered page preserves the original
            // uniform scale/fade effect during swipe transitions.
            val pageOffset = pagerState.currentPageOffsetFraction
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

        // Replaces Accompanist HorizontalPagerIndicator with a native Row of dot indicators.
        Row(
            modifier = Modifier.padding(dimens.paddingSmall),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(pages.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                )
            }
        }

        val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 500, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )

        Button(
            onClick = {
                scope.launch {
                    if (pagerState.currentPage == pages.size - 1) {
                        navController.navigate(CreateUser)
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
