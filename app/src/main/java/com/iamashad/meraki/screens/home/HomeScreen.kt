package com.iamashad.meraki.screens.home

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.R
import com.iamashad.meraki.navigation.Screens
import com.iamashad.meraki.screens.moodtracker.MoodTrackerViewModel
import com.iamashad.meraki.utils.LoadImageWithGlide
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.ProvideDimens
import com.iamashad.meraki.utils.daysOfWeek
import com.iamashad.meraki.utils.getMoodColor
import com.iamashad.meraki.utils.getMoodEmoji
import com.iamashad.meraki.utils.rememberWindowSizeClass
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeScreenViewModel = hiltViewModel(),
    moodTrackerViewModel: MoodTrackerViewModel = hiltViewModel()
) {
    val dimens = LocalDimens.current

    val user by homeViewModel.user.collectAsState()
    val photoUrl by homeViewModel.photoUrl.collectAsState()
    val lastMoods by moodTrackerViewModel.moodTrend.collectAsState()
    val isLoading by moodTrackerViewModel.loading.collectAsState()
    var streakCount by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user) {
        user?.uid?.let { userId ->
            try {
                homeViewModel.logDailyUsage(userId)
                streakCount = homeViewModel.calculateStreak(userId)
            } catch (e: Exception) {
                errorMessage = "Failed to calculate streak: ${e.message}"
            }
        }
    }

    val windowSize = rememberWindowSizeClass()
    ProvideDimens(windowSize) {
        val isLargeScreen = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded ||
                LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLargeScreen) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                MaterialTheme.colorScheme.onPrimaryContainer,
                                MaterialTheme.colorScheme.primaryContainer,
                            )
                        )
                    )
                    .padding(dimens.paddingMedium)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = dimens.paddingMedium),
                    verticalArrangement = Arrangement.spacedBy(dimens.paddingMedium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProfileCard(
                        photoUrl = photoUrl.toString(),
                        userName = user?.displayName ?: "User",
                        onProfileClick = { navController.navigate(Screens.SETTINGS.name) }
                    )

                    StreakMeterCard(streakCount = streakCount)

                    VerticalWeeklyCalendar(navController)
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(dimens.paddingMedium)
                ) {

                    item {
                        QuoteCardStack(homeViewModel)
                    }

                    item {
                        MoodPromptCard(navController)
                    }

                    item {
                        when {
                            errorMessage != null -> ErrorMessage(errorMessage!!)
                            isLoading -> LoadingIndicator()
                            lastMoods.isNotEmpty() -> MoodLogsCard(moodLogs = lastMoods.takeLast(7))
                            else -> EmptyMoodLogs()
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = dimens.paddingMedium),
                            contentAlignment = Alignment.Center
                        ) {
                            MeditateButton(navController)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                MaterialTheme.colorScheme.onPrimaryContainer,
                                MaterialTheme.colorScheme.primaryContainer,
                            )
                        )
                    )
                    .padding(dimens.paddingMedium),
                verticalArrangement = Arrangement.spacedBy(dimens.paddingMedium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileCard(
                            photoUrl = photoUrl.toString(),
                            userName = user?.displayName ?: "User",
                            onProfileClick = { navController.navigate(Screens.SETTINGS.name) }
                        )

                        StreakMeterCard(streakCount = streakCount)
                    }
                }

                item {
                    WeeklyCalendar(navController)
                }

                item {
                    QuoteCardStack(homeViewModel)
                }

                item {
                    MoodPromptCard(navController)
                }

                item {
                    when {
                        errorMessage != null -> ErrorMessage(errorMessage!!)
                        isLoading -> LoadingIndicator()
                        lastMoods.isNotEmpty() -> MoodLogsCard(moodLogs = lastMoods.takeLast(7))
                        else -> EmptyMoodLogs()
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = dimens.paddingMedium),
                        contentAlignment = Alignment.Center
                    ) {
                        MeditateButton(navController)
                    }
                }
            }
        }
    }
}


@Composable
fun VerticalWeeklyCalendar(navController: NavController) {
    val dimens = LocalDimens.current
    val calendar = remember { java.util.Calendar.getInstance() }
    val currentDayIndex = remember {
        (calendar.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
    }
    val dates = remember {
        (0..6).map { offset ->
            java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                add(java.util.Calendar.DAY_OF_MONTH, offset)
            }.get(java.util.Calendar.DAY_OF_MONTH)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimens.paddingSmall),
        verticalArrangement = Arrangement.spacedBy(dimens.paddingSmall),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        daysOfWeek.forEachIndexed { index, day ->
            val isSelected = index == currentDayIndex
            val backgroundColor =
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

            Row(
                modifier = Modifier
                    .fillMaxWidth(.8f)
                    .clip(RoundedCornerShape(dimens.cornerRadius))
                    .background(backgroundColor)
                    .clickable { navController.navigate(Screens.MOODTRACKER.name) }
                    .padding(vertical = dimens.paddingSmall, horizontal = dimens.paddingMedium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                )
                Spacer(Modifier.width(dimens.paddingSmall))
                Text(
                    text = dates[index].toString(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor
                    )
                )
            }
        }
    }
}


@Composable
fun ErrorMessage(errorMessage: String) {
    val dimens = LocalDimens.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimens.paddingMedium),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                fontSize = dimens.fontMedium
            )
        )
    }
}

@Composable
fun WeeklyCalendar(navController: NavController) {
    val dimens = LocalDimens.current
    val calendar = java.util.Calendar.getInstance()
    val currentDayIndex = remember {
        (calendar.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
    }
    val dates = remember {
        (0..6).map { offset ->
            java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                add(java.util.Calendar.DAY_OF_MONTH, offset)
            }.get(java.util.Calendar.DAY_OF_MONTH)
        }
    }

    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(horizontal = dimens.paddingSmall),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        daysOfWeek.forEachIndexed { index, day ->
            val isSelected = index == currentDayIndex
            val backgroundColor =
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

            Box(
                modifier = Modifier
                    .size(dimens.avatarSize / 5)
                    .clip(CircleShape)
                    .background(backgroundColor)
                    .clickable { navController.navigate(Screens.MOODTRACKER.name) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = textColor
                        )
                    )
                    Text(
                        text = dates[index].toString(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    )
                }
            }
        }
    }
}


@Composable
fun CelebrationDialog(
    onDismiss: () -> Unit,
    streakCount: Int
) {
    val dimens = LocalDimens.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(dimens.cornerRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .width((screenWidthDp.dp / 5) * 4)
                .height((screenHeightDp.dp / 5) * 2)
                .padding(dimens.paddingMedium)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.onBackground,
                                MaterialTheme.colorScheme.primary
                            )
                        ),
                    ), contentAlignment = Alignment.Center
            ) {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_fire))
                val progress by animateLottieCompositionAsState(
                    composition = composition, iterations = LottieConstants.IterateForever
                )

                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(dimens.avatarSize)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(dimens.paddingMedium)
                ) {
                    Spacer(modifier = Modifier.height(dimens.paddingMedium))

                    Text(
                        text = "🔥 $streakCount Day Streak! 🔥",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.background,
                            textAlign = TextAlign.Center
                        )
                    )
                    Text(
                        text = "You're doing great on your emotional journey. Keep moving forward!",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.surface,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(top = dimens.paddingSmall)
                    )

                    Spacer(modifier = Modifier.height(dimens.paddingLarge))

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close, contentDescription = null
                        )
                    }
                }

            }
        }
    }
}

@Composable
fun MoodPromptCard(navController: NavController) {
    val userName = FirebaseAuth.getInstance().currentUser?.displayName
    val firstName = userName?.split(" ")?.firstOrNull()
    val dimens = LocalDimens.current

    Card(shape = RoundedCornerShape(dimens.cornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(dimens.elevation),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimens.paddingMedium)
            .clickable {
                navController.navigate(Screens.MOODTRACKER.name)
            }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.paddingMedium)
        ) {
            Column {
                Text(
                    text = "Hey $firstName, how are you feeling today?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "Take a moment to reflect—it only takes a second.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(dimens.avatarSize / 7)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🌟",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.Center
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(dimens.paddingSmall / 2)
                )

            }
        }
    }
}

@Composable
fun ProfileCard(
    photoUrl: String,
    userName: String,
    onProfileClick: () -> Unit
) {
    val dimens = LocalDimens.current

    Card(
        shape = RoundedCornerShape(dimens.cornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(dimens.elevation),
        modifier = Modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(dimens.paddingSmall)
        ) {
            LoadImageWithGlide(
                imageUrl = photoUrl,
                modifier = Modifier
                    .size(dimens.avatarSize / 6)
                    .clip(CircleShape)
                    .clickable { onProfileClick() }
            )
            Spacer(modifier = Modifier.width(dimens.paddingSmall / 2))
            Text(
                text = userName,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun StreakMeterCard(streakCount: Int) {
    var showCelebrationDialog by remember { mutableStateOf(false) }

    if (showCelebrationDialog) {
        CelebrationDialog(
            onDismiss = { showCelebrationDialog = false }, streakCount = streakCount
        )
    }
    val dimens = LocalDimens.current

    Card(
        shape = RoundedCornerShape(dimens.cornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(dimens.elevation),
        modifier = Modifier
            .padding(start = dimens.paddingSmall / 2)
            .clickable {
                showCelebrationDialog = true
            }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = dimens.paddingMedium / 2,
                vertical = dimens.paddingSmall / 2
            )
        ) {
            Text(
                text = "🔥",
                fontSize = MaterialTheme.typography.titleLarge.fontSize
            )
            Spacer(modifier = Modifier.width(dimens.paddingSmall / 2))
            Text(
                text = "$streakCount", style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    }

}


@Composable
fun MoodLogsCard(moodLogs: List<Pair<String, Int>>) {

    val dimens = LocalDimens.current
    Card(
        shape = RoundedCornerShape(dimens.cornerRadius),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(dimens.elevation),
        modifier = Modifier
            .fillMaxWidth()
            .height(dimens.paddingLarge * 12)
    ) {
        val density = LocalDensity.current

        Column(
            modifier = Modifier.padding(dimens.paddingMedium)
        ) {
            Text(
                text = "Mood Chart",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Text(
                text = "Your last 7 mood entries",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(.6f)
                )
            )

            Spacer(modifier = Modifier.height(dimens.paddingMedium * 3))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                moodLogs.forEach { (_, moodScore) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width((dimens.paddingMedium * 2) - (dimens.paddingSmall / 2))
                    ) {
                        Box(
                            modifier = Modifier.width((dimens.paddingMedium * 2) - (dimens.paddingSmall / 2)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(dimens.paddingMedium * 2)
                                    .height(dimens.paddingMedium * 12)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(dimens.cornerRadius)
                                    ), contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(fraction = moodScore / 100f)
                                        .background(
                                            color = getMoodColor(moodScore),
                                            shape = RoundedCornerShape(dimens.cornerRadius)
                                        )
                                )
                            }
                            with(density) {
                                Text(
                                    text = getMoodEmoji(moodScore),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = dimens.fontMedium
                                    ),
                                    modifier = Modifier
                                        .padding(bottom = dimens.paddingSmall)
                                        .align(Alignment.BottomCenter)
                                        .offset(y = -(moodScore / 100f * (dimens.paddingMedium * 10).toPx()).toDp() + (dimens.paddingSmall / 2))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuoteCardStack(viewModel: HomeScreenViewModel) {
    val quotes by viewModel.quotes.collectAsState()
    var currentIndex by remember { mutableIntStateOf(0) }
    val screenWidth =
        with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Render cards from top to bottom
        quotes.forEachIndexed { index, quote ->
            if (index >= currentIndex) {
                val isTopCard = index == currentIndex
                val swipeOffset = remember { Animatable(0f) }
                val alpha = remember { Animatable(1f) }

                Box(
                    modifier = Modifier
                        .zIndex((quotes.size - index).toFloat())
                        .graphicsLayer(
                            scaleX = if (isTopCard) 1f else 0.95f - ((index - currentIndex) * 0.02f),
                            scaleY = if (isTopCard) 1f else 0.95f - ((index - currentIndex) * 0.02f),
                            translationY = if (isTopCard) 0f else (index - currentIndex) * 20f,
                            alpha = if (isTopCard) alpha.value else 1f
                        )
                ) {
                    if (isTopCard) {
                        SwipeableCard(
                            quote = quote.first,
                            author = quote.second,
                            swipeOffset = swipeOffset,
                            alpha = alpha,
                            screenWidth = screenWidth,
                            onSwipeComplete = {
                                scope.launch {
                                    swipeOffset.animateTo(if (swipeOffset.value > 0) screenWidth else -screenWidth)
                                    alpha.animateTo(0f)
                                    currentIndex++

                                    // Fetch a single new quote when reaching the end of the current stack
                                    if (currentIndex >= quotes.size - 1) {
                                        viewModel.fetchSingleQuote()
                                    }
                                }
                            }
                        )
                    } else {
                        QuoteCard(
                            quote = quote.first,
                            author = quote.second,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Stack indicators
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(quotes.size) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (index == currentIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SwipeableCard(
    quote: String,
    author: String,
    swipeOffset: Animatable<Float, AnimationVector1D>,
    alpha: Animatable<Float, AnimationVector1D>,
    screenWidth: Float,
    onSwipeComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    val dragState = rememberDraggableState { delta ->
        scope.launch {
            swipeOffset.snapTo(swipeOffset.value + delta)
        }
    }

    Box(
        modifier = modifier
            .offset { IntOffset(swipeOffset.value.roundToInt(), 0) }
            .draggable(
                orientation = Orientation.Horizontal,
                state = dragState,
                onDragStopped = {
                    if (swipeOffset.value > screenWidth / 2 || swipeOffset.value < -screenWidth / 2) {
                        onSwipeComplete() // Trigger swipe out
                    } else {
                        // Reset card position if not swiped far enough
                        scope.launch {
                            swipeOffset.animateTo(0f)
                        }
                    }
                }
            )
    ) {
        QuoteCard(
            quote = quote,
            author = author,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun QuoteCard(
    quote: String,
    author: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_quote),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\"$quote\"",
                style = MaterialTheme.typography.bodyLarge.copy(
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.width(50.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "- $author",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MeditateButton(navController: NavController) {
    val dimens = LocalDimens.current

    Card(
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(dimens.elevation),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .size((dimens.avatarSize / 20) * 9)
            .clickable {
                navController.navigate(Screens.BREATHING.name)
            }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(.8f),
                            MaterialTheme.colorScheme.inversePrimary,
                            MaterialTheme.colorScheme.primary
                        ), center = Offset.Unspecified, radius = 150f
                    )
                ), contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Meditate",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}


@Composable
fun EmptyMoodLogs() {
    val dimens = LocalDimens.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimens.paddingMedium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_moodtrack),
            contentDescription = "Make Journals",
            modifier = Modifier
                .size(dimens.avatarSize)
        )
        Spacer(modifier = Modifier.height(dimens.paddingSmall))
        Text(
            text = "Your Emotional Journey Awaits!",
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.background,
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.height(dimens.paddingSmall))

        Column {
            Text(
                text = """
                 • Track your emotional highs and lows.
             """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            )
            Text(
                text = """
                 • Gain insights into your mood patterns.
             """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            )
            Text(
                text = """
                • Celebrate progress and identify triggers.
             """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.background,
            strokeWidth = 4.dp
        )
    }
}


