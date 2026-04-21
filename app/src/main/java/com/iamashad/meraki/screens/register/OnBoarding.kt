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
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Onboarding Flow Overhaul — Phase 1 & 3.
 *
 * Phase 1 changes:
 *  - Trimmed from 5 slides to 3: Safe Haven → Emotions & Journal → AI Companion.
 *  - Fixed duplicate Lottie on the old slide 5 (was reusing lottie_onb2 — now uses lottie_onb4).
 *  - Added a "Skip" TextButton in the top-right corner. No confirmation dialog — tap is final.
 *
 * Phase 3 change:
 *  - Slide 3 (AI Companion) shows a live typewriter animation of a canned sample
 *    AI message, making the AI feel alive before the user has typed a word.
 *
 * Navigation target after onboarding: [CreateUser] (account creation).
 */
@Composable
fun OnBoardingScreen(navController: NavController) {
    val dimens = LocalDimens.current

    // Phase 1: Trimmed to 3 slides.
    // Slide 2 merges "Emotions" + "Journal" from the old 5-slide version.
    // Slide 3 replaces the duplicate-Lottie slide with a proper AI Companion preview.
    val pages = listOf(
        OnboardingPage(
            title = "Your Safe Haven",
            subtitle = "This is a place where your thoughts are safe, your feelings are valid, and your journey to self-healing begins. Let's walk this path together.",
            animation = R.raw.lottie_onb1,
            iterations = LottieConstants.IterateForever,
            isAISlide = false
        ),
        OnboardingPage(
            title = "Understand & Express",
            subtitle = "Every feeling matters. Discover patterns in your emotions, untangle your thoughts through journaling, and learn to navigate life's ups and downs with clarity.",
            animation = R.raw.lottie_onb3,
            iterations = LottieConstants.IterateForever,
            isAISlide = false
        ),
        OnboardingPage(
            title = "Your AI Companion",
            subtitle = "Meraki learns who you are and grows with you",
            // Phase 1 fix: lottie_onb4 (distinct) instead of the old duplicate lottie_onb2
            animation = R.raw.lottie_onb4,
            iterations = LottieConstants.IterateForever,
            isAISlide = true
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top spacer reserves room for the overlaid Skip button
            Spacer(modifier = Modifier.height(48.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageOffset = pagerState.currentPageOffsetFraction
                val scale = 1f - 0.1f * kotlin.math.abs(pageOffset)
                val alpha = 1f - 0.3f * kotlin.math.abs(pageOffset)

                if (pages[page].isAISlide) {
                    // Phase 3: AI preview with typewriter animation
                    AIPreviewOnboardingContent(
                        page = pages[page],
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                    )
                } else {
                    OnboardingContent(
                        page = pages[page],
                        iterations = pages[page].iterations,
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                    )
                }
            }

            // Dot page indicators
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
                            // Navigate to MoodSeed (Phase 3) — not CreateUser directly
                            navController.navigate(CreateUser)
                        } else {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .padding(bottom = dimens.paddingMedium)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (pagerState.currentPage == pages.size - 1) "Let's Get Started!" else "Next →",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Phase 1: Skip button — top-right corner.
        // Respects user's time; no confirmation dialog gating this action.
        TextButton(
            onClick = { navController.navigate(CreateUser) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
        ) {
            Text(
                text = "Skip",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            )
        }
    }
}

/**
 * Standard onboarding slide: Lottie animation + title + subtitle.
 */
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

        Box(modifier = Modifier.size(dimens.avatarSize / 2)) {
            LottieAnimation(composition = composition, progress = { progress })
        }

        Spacer(modifier = Modifier.height(dimens.paddingMedium))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
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

/**
 * Phase 3: AI Companion slide with a live typewriter animation.
 *
 * Uses a canned 40-word string rendered via a simple character-emit coroutine — no API call.
 * The bubble is styled like the real ChatbotScreen to make the preview feel authentic.
 */
@Composable
fun AIPreviewOnboardingContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier
) {
    val dimens = LocalDimens.current

    val sampleAIMessage = "Hey, I noticed you've been carrying a lot lately. " +
            "How are you feeling right now? I'm here, and I'm not going anywhere. 💙"

    var displayedText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        displayedText = ""
        delay(600L)
        for (char in sampleAIMessage) {
            displayedText += char
            delay(35L)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimens.paddingMedium),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(page.animation))
        val progress by animateLottieCompositionAsState(
            composition, iterations = page.iterations
        )

        Box(modifier = Modifier.size(dimens.avatarSize / 2)) {
            LottieAnimation(composition = composition, progress = { progress })
        }

        Spacer(modifier = Modifier.height(dimens.paddingMedium))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(dimens.paddingSmall))

        Text(
            text = page.subtitle,
            style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(dimens.paddingMedium))

        // AI chat bubble — styled to match ChatbotScreen's assistant bubble
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(dimens.paddingMedium)
        ) {
            Text(
                text = if (displayedText.isNotEmpty()) displayedText else " ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val animation: Int,
    val iterations: Int,
    val isAISlide: Boolean = false
)
