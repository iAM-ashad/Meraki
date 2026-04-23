package com.iamashad.meraki.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.window.core.layout.WindowWidthSizeClass
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.R
import com.iamashad.meraki.screens.breathing.BreathingScreen
import com.iamashad.meraki.screens.chatbot.ChatViewModel
import com.iamashad.meraki.screens.chatbot.ChatbotScreen
import com.iamashad.meraki.screens.createUser.CreateUserScreen
import com.iamashad.meraki.screens.home.HomeScreen
import com.iamashad.meraki.screens.insights.InsightsViewModel
import com.iamashad.meraki.screens.insights.MoodInsightsScreen
import com.iamashad.meraki.screens.journal.AddJournalScreen
import com.iamashad.meraki.screens.journal.JournalScreen
import com.iamashad.meraki.screens.journal.JournalViewModel
import com.iamashad.meraki.screens.journal.ViewJournalScreen
import com.iamashad.meraki.screens.login.LoginScreen
import com.iamashad.meraki.screens.moodtracker.MoodTrackerScreen
import com.iamashad.meraki.screens.moodtracker.MoodTrackerViewModel
import com.iamashad.meraki.screens.register.OnBoardingScreen
import com.iamashad.meraki.screens.register.RegisterScreen
import com.iamashad.meraki.screens.register.RegisterViewModel
import com.iamashad.meraki.screens.settings.SettingsScreen
import com.iamashad.meraki.screens.settings.SettingsViewModel
import com.iamashad.meraki.screens.splash.SplashScreen
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.rememberWindowAdaptiveInfo

/**
 * Holds the display label, type-safe route, and icon resource for a nav destination.
 * Used by both [AnimatedNavigationBar] and [AnimatedNavigationRail].
 */
data class NavigationItem(val label: String, val route: Any, val icon: Int)

/**
 * Sets up the app's main navigation using NavController and an adaptive layout.
 * Uses Navigation Compose 2.8+ type-safe routes (@Serializable objects/data classes).
 */
/**
 * Phase 5: [navigateToChatbot] is set to true when the user taps a Meraki notification.
 *
 * Two navigation paths handled here:
 *  1. **Fresh launch** — [SplashScreen] receives the flag and routes authenticated users
 *     to [Chatbot] instead of [Home].
 *  2. **App already running** — the [LaunchedEffect] below detects the flag change
 *     (delivered via [MainActivity.onNewIntent]) and navigates directly to [Chatbot]
 *     without going through Splash (which is already off the back-stack).
 */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MerakiNavigation(navigateToChatbot: Boolean = false) {
    val navController = rememberNavController()
    val currentDestination by navController.currentBackStackEntryFlow.collectAsState(initial = null)
    val adaptiveInfo = rememberWindowAdaptiveInfo()

    // Handle deep-link when the app is already running (onNewIntent path).
    // We only navigate here when the current destination is NOT Splash — Splash handles
    // the fresh-launch case itself via the navigateToChatbot parameter.
    LaunchedEffect(navigateToChatbot) {
        if (navigateToChatbot) {
            val isSplash = currentDestination?.destination
                ?.hasRoute<Splash>() == true
            if (!isSplash) {
                navController.navigate(Chatbot()) {
                    launchSingleTop = true
                }
            }
        }
    }

    Scaffold {
        AdaptiveScreen(
            navController = navController,
            currentDestination = currentDestination?.destination,
            windowSize = adaptiveInfo
        ) {
            NavHost(
                navController = navController,
                startDestination = Splash,
                modifier = Modifier
            ) {
                addNavGraph(navController, navigateToChatbot)
            }
        }
    }
}

/**
 * Defines all screen destinations using type-safe composable<T> registrations.
 * Arguments are extracted via backStackEntry.toRoute<T>() instead of navArgument().
 *
 * Phase 5: [navigateToChatbot] is forwarded to [SplashScreen] so fresh-launch
 * notification deep-links bypass [Home] and land directly on [Chatbot].
 */
fun NavGraphBuilder.addNavGraph(navController: NavController, navigateToChatbot: Boolean = false) {
    // Onboarding Overhaul — Phase 1: SplashScreen uses SplashViewModel (Hilt) to read the
    // hasCompletedOnboarding flag and gate authenticated users onto the onboarding flow.
    composable<Splash> { SplashScreen(navController, navigateToChatbot = navigateToChatbot) }
    composable<Home> { HomeScreen(navController) }
    composable<Register> { RegisterScreen(navController) }
    composable<Insights> {
        val viewModel = hiltViewModel<InsightsViewModel>()
        MoodInsightsScreen(viewModel, navController)
    }
    composable<Onboarding> { OnBoardingScreen(navController) }

    composable<Chatbot> {
        // prompt is available via backStackEntry.toRoute<Chatbot>().prompt if needed
        val viewModel = hiltViewModel<ChatViewModel>()
        ChatbotScreen(viewModel, navController)
    }

    composable<MoodTracker> {
        val viewModel = hiltViewModel<MoodTrackerViewModel>()
        MoodTrackerScreen(
            moodTrackerViewModel = viewModel,
            onMoodLogged = { viewModel.fetchMoodTrend() }
        )
    }

    composable<Settings> {
        val chatVM = hiltViewModel<ChatViewModel>()
        val settingsVM = hiltViewModel<SettingsViewModel>()
        val registerVM = hiltViewModel<RegisterViewModel>()
        SettingsScreen(navController, settingsVM, registerVM, chatVM)
    }

    composable<Breathing> { BreathingScreen() }

    composable<Login> {
        val viewModel = hiltViewModel<RegisterViewModel>()
        LoginScreen(viewModel, navController) {
            navController.navigate(CreateUser)
        }
    }

    composable<CreateUser> {
        val viewModel = hiltViewModel<RegisterViewModel>()
        CreateUserScreen(viewModel, navController) {
            navController.navigate(Login)
        }
    }

    // ── Onboarding Overhaul: 4 new destinations ──────────────────────────────

    // Phase 2: Full-screen avatar picker — step 2 of sign-up.
    // Kept outside OnboardingGraph: it's entered from CreateUser with a userId
    // arg and only needs its own ViewModel scope (selectedAvatar is independent).
    composable<AvatarCelebration> { backStackEntry ->
        val args = backStackEntry.toRoute<AvatarCelebration>()
        val viewModel = hiltViewModel<com.iamashad.meraki.screens.onboarding.OnboardingViewModel>()
        com.iamashad.meraki.screens.register.AvatarCelebrationScreen(
            userId = args.userId,
            navController = navController,
            viewModel = viewModel
        )
    }

    // Phase 3–4: MoodSeed → WelcomeMeraki → NotificationSetup are nested inside a
    // single named graph so that hiltViewModel() can be scoped to the graph's back
    // stack entry.  All three composables receive the *same* OnboardingViewModel
    // instance, which means the mood selected in MoodSeedScreen is visible in
    // WelcomeAIScreen without any extra argument passing.
    navigation<OnboardingGraph>(startDestination = MoodSeed) {

        // Phase 3: Pre-account mood capture — between AvatarCelebration and WelcomeAIScreen
        composable<MoodSeed> { backStackEntry ->
            val graphEntry = remember(backStackEntry) {
                navController.getBackStackEntry<OnboardingGraph>()
            }
            val viewModel =
                hiltViewModel<com.iamashad.meraki.screens.onboarding.OnboardingViewModel>(graphEntry)
            com.iamashad.meraki.screens.onboarding.MoodSeedScreen(navController, viewModel)
        }

        // Phase 3: AI-powered personalised welcome + first journal prompt
        composable<WelcomeMeraki> { backStackEntry ->
            val graphEntry = remember(backStackEntry) {
                navController.getBackStackEntry<OnboardingGraph>()
            }
            val viewModel =
                hiltViewModel<com.iamashad.meraki.screens.onboarding.OnboardingViewModel>(graphEntry)
            com.iamashad.meraki.screens.onboarding.WelcomeAIScreen(navController, viewModel)
        }

        // Phase 4: Explained notification opt-in with NL time input
        composable<NotificationSetup> { backStackEntry ->
            val graphEntry = remember(backStackEntry) {
                navController.getBackStackEntry<OnboardingGraph>()
            }
            val viewModel =
                hiltViewModel<com.iamashad.meraki.screens.onboarding.OnboardingViewModel>(graphEntry)
            com.iamashad.meraki.screens.onboarding.NotificationSetupScreen(navController, viewModel)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    composable<AddJournal> { backStackEntry ->
        val args = backStackEntry.toRoute<AddJournal>()
        val viewModel = hiltViewModel<JournalViewModel>()
        AddJournalScreen(
            viewModel = viewModel,
            userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
            journalId = args.journalId,
            onClose = { navController.popBackStack() },
            onSave = { emotions ->
                navController.popBackStack()
                if (emotions != null) {
                    navController.navigate(MoodTracker(preFilledEmotions = emotions))
                }
            }
        )
    }

    composable<Journal> {
        val viewModel = hiltViewModel<JournalViewModel>()
        JournalScreen(
            viewModel = viewModel,
            onAddJournalClick = {
                val newJournalId =
                    FirebaseFirestore.getInstance().collection("journals").document().id
                navController.navigate(AddJournal(journalId = newJournalId))
            },
            onViewJournalClick = { journal ->
                navController.navigate(ViewJournal(journalId = journal.journalId))
            }
        )
    }

    composable<ViewJournal> { backStackEntry ->
        val args = backStackEntry.toRoute<ViewJournal>()
        val viewModel = hiltViewModel<JournalViewModel>()

        val journals by viewModel.journals.collectAsState()
        val searchResults by viewModel.searchResults.collectAsState()
        val isSearching by viewModel.isSearching.collectAsState()

        val displayedJournals = if (isSearching) searchResults else journals
        val journal = displayedJournals.find { it.journalId == args.journalId }

        if (journal != null) {
            ViewJournalScreen(journal = journal)
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading or Journal not found.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

/**
 * Adjusts layout dynamically based on screen width size class (compact/medium/expanded).
 * [currentDestination] is a NavDestination used for hasRoute<T>() checks.
 */
@Composable
fun AdaptiveScreen(
    navController: NavController,
    currentDestination: androidx.navigation.NavDestination?,
    windowSize: WindowAdaptiveInfo,
    content: @Composable () -> Unit
) {
    val showBottomBar = currentDestination?.let { dest ->
        dest.hasRoute<Home>() || dest.hasRoute<MoodTracker>() ||
                dest.hasRoute<Journal>() || dest.hasRoute<Insights>()
    } ?: false

    when (windowSize.windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.COMPACT -> {
            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        BottomNavigationBar(navController, currentDestination)
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    content()
                }
            }
        }

        WindowWidthSizeClass.MEDIUM, WindowWidthSizeClass.EXPANDED -> {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) {
                ScrollableNavigationRail(
                    navController = navController,
                    windowSizeClass = windowSize,
                    currentDestination = currentDestination
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) {
                    content()
                }
            }
        }

        else -> {
            Scaffold(
                bottomBar = { BottomNavigationBar(navController, currentDestination) }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Animated vertical navigation rail for large screens.
 * Uses hasRoute(KClass) for selection detection, compatible with type-safe routes.
 */
@Composable
fun AnimatedNavigationRail(
    navController: NavController,
    windowSizeClass: WindowAdaptiveInfo,
    currentDestination: androidx.navigation.NavDestination?
) {
    val dimens = LocalDimens.current

    val items = listOf(
        NavigationItem("Health", MoodTracker(), R.drawable.ic_moodtracker),
        NavigationItem("Chatbot", Chatbot(), R.drawable.ic_chat),
        NavigationItem("Home", Home, R.drawable.home_123),
        NavigationItem("Journal", Journal, R.drawable.ic_journal),
        NavigationItem("Insights", Insights, R.drawable.ic_insights)
    )

    val railWidth = 100.dp
    val boxSize = 60.dp
    val iconSize = 36.dp
    val itemSpacing = 24.dp

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(railWidth)
            .padding(vertical = dimens.paddingMedium),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items.forEach { item ->
            val isSelected = currentDestination?.hasRoute(item.route::class) == true
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.2f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = 50f
                )
            )
            val offset by animateDpAsState(
                targetValue = if (isSelected) (-10).dp else 0.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = 50f
                )
            )
            val backgroundBrush = if (isSelected) {
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            } else {
                Brush.radialGradient(colors = listOf(Color.Transparent, Color.Transparent))
            }

            Box(
                modifier = Modifier
                    .padding(vertical = itemSpacing / 2)
                    .offset(y = offset)
                    .size(boxSize)
                    .clip(CircleShape)
                    .background(backgroundBrush)
                    .clickable {
                        if (!isSelected) {
                            navController.navigate(item.route) {
                                popUpTo<Home> { saveState = true }
                                launchSingleTop = true
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = item.icon),
                    contentDescription = item.label,
                    modifier = Modifier
                        .size(iconSize)
                        .scale(scale),
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Makes navigation rail scrollable if it overflows vertically.
 */
@Composable
fun ScrollableNavigationRail(
    navController: NavController,
    windowSizeClass: WindowAdaptiveInfo,
    currentDestination: androidx.navigation.NavDestination?
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        AnimatedNavigationRail(navController, windowSizeClass, currentDestination)
    }
}

/**
 * Bottom navigation bar for compact devices.
 * Selection is determined via hasRoute(KClass) — works with type-safe routes.
 */
@Composable
fun BottomNavigationBar(
    navController: NavController,
    currentDestination: androidx.navigation.NavDestination?
) {
    val items = listOf(
        NavigationItem("Health", MoodTracker(), R.drawable.ic_moodtracker),
        NavigationItem("Chatbot", Chatbot(), R.drawable.ic_chat),
        NavigationItem("Home", Home, R.drawable.home_123),
        NavigationItem("Journal", Journal, R.drawable.ic_journal),
        NavigationItem("Insights", Insights, R.drawable.ic_insights)
    )

    val dimens = LocalDimens.current

    NavigationBar(
        containerColor = Color.Transparent,
        tonalElevation = dimens.elevation,
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { item ->
            val isSelected = currentDestination?.hasRoute(item.route::class) == true

            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.7f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = 50f
                )
            )
            val offset by animateDpAsState(
                targetValue = if (isSelected) (-5).dp else 0.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = 50f
                )
            )

            NavigationBarItem(
                icon = {
                    Box(
                        modifier = Modifier
                            .padding(top = dimens.paddingSmall)
                            .offset(y = offset)
                            .clip(CircleShape)
                            .background(
                                brush = if (isSelected) {
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.onBackground,
                                            MaterialTheme.colorScheme.primary
                                        )
                                    )
                                } else {
                                    Brush.radialGradient(
                                        colors = listOf(Color.Transparent, Color.Transparent)
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = item.icon),
                            contentDescription = item.label,
                            tint = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground.copy(
                                alpha = 0.8f
                            ),
                            modifier = Modifier
                                .scale(scale)
                                .padding(dimens.paddingSmall + (dimens.paddingSmall / 2))
                        )
                    }
                },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        try {
                            navController.navigate(item.route) {
                                popUpTo<Home> { saveState = true }
                                launchSingleTop = true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
                modifier = Modifier
            )
        }
    }
}