package com.iamashad.meraki.screens.register

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.iamashad.meraki.R
import com.iamashad.meraki.navigation.AvatarCelebration
import com.iamashad.meraki.navigation.WelcomeMeraki
import com.iamashad.meraki.screens.onboarding.OnboardingViewModel
import com.iamashad.meraki.utils.LocalDimens

/**
 * Phase 2: Step 2 of the sign-up flow — full-screen avatar picker.
 *
 * After [CreateUserScreen] creates the Firebase account, this screen lets the user
 * choose their Meraki companion from a centered grid (not a LazyRow).
 * The chosen avatar is saved to Firestore via [OnboardingViewModel.saveAvatar].
 *
 * UX details:
 *  - Selected avatar scales up with a bouncy spring and gains a glowing primary border.
 *  - A Lottie confetti animation plays briefly after any selection.
 *  - "Continue" saves the avatar and navigates to [WelcomeMeraki] (Phase 3).
 */
@Composable
fun AvatarCelebrationScreen(
    userId: String,
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val dimens = LocalDimens.current
    val uiState by viewModel.uiState.collectAsState()

    // Confetti Lottie — plays on avatar selection
    val confettiComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.lottie_confetti)
    )
    val confettiProgress by animateLottieCompositionAsState(
        composition = confettiComposition,
        isPlaying = uiState.selectedAvatar != null,
        iterations = 1,
        restartOnPlay = false
    )

    val avatars = listOf(
        R.drawable.avatar1, R.drawable.avatar2, R.drawable.avatar3,
        R.drawable.avatar4, R.drawable.avatar5, R.drawable.avatar6,
        R.drawable.avatar7, R.drawable.avatar8, R.drawable.avatar9, R.drawable.avatar10
    )

    // Listen for successful avatar save → navigate to WelcomeMeraki
    LaunchedEffect(uiState.avatarSaved) {
        if (uiState.avatarSaved) {
            navController.navigate(WelcomeMeraki) {
                popUpTo<AvatarCelebration> { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimens.paddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(dimens.paddingMedium))

            Text(
                text = "Choose your Meraki companion",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(dimens.paddingSmall))

            Text(
                text = "This is who walks with you.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            )

            Spacer(modifier = Modifier.height(dimens.paddingMedium))

            // Centered avatar grid (not a LazyRow — gives each avatar room to breathe)
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(dimens.paddingSmall),
                horizontalArrangement = Arrangement.spacedBy(dimens.paddingSmall),
                verticalArrangement = Arrangement.spacedBy(dimens.paddingSmall)
            ) {
                items(avatars) { avatarRes ->
                    val isSelected = uiState.selectedAvatar == avatarRes

                    // Spring-based scale animation for the selection pop effect
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.25f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "avatarScale_$avatarRes"
                    )

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(
                                if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                            .clickable { viewModel.selectAvatar(avatarRes) },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = avatarRes),
                            contentDescription = "Avatar option",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .padding(4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimens.paddingMedium))

            Button(
                onClick = {
                    viewModel.saveAvatar(
                        userId = userId,
                        avatarRes = uiState.selectedAvatar ?: R.drawable.avatar1
                    )
                },
                enabled = !uiState.isSavingAvatar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.isSavingAvatar) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Continue →",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimens.paddingMedium))
        }

        // Confetti overlay — positioned to cover the whole screen on top of content
        if (uiState.selectedAvatar != null) {
            LottieAnimation(
                composition = confettiComposition,
                progress = { confettiProgress },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.75f }
            )
        }
    }
}
