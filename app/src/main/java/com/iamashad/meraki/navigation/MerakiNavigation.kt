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
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import com.iamashad.meraki.utils.rememberWindowSizeClass

/**
 * Sets up the app's main navigation using NavController and an adaptive layout.
 */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MerakiNavigation() {
    val navController = rememberNavController()
    val currentDestination by navController.currentBackStackEntryFlow.collectAsState(initial = null)
    val windowSize = rememberWindowSizeClass()

    Scaffold {
        AdaptiveScreen(
            navController = navController,
            currentDestination = currentDestination?.destination?.route,
            windowSize = windowSize
        ) {
            NavHost(
                navController = navController,
                startDestination = Screens.SPLASH.name,
                modifier = Modifier
            ) {
                addNavGraph(navController)
            }
        }
    }
}

/**
 * Defines all screen routes for navigation using NavGraphBuilder.
 */
fun NavGraphBuilder.addNavGraph(navController: NavController) {
    composable(Screens.SPLASH.name) { SplashScreen(navController) }
    composable(Screens.HOME.name) { HomeScreen(navController) }
    composable(Screens.REGISTER.name) { RegisterScreen(navController) }
    composable(Screens.INSIGHTS.name) {
        val viewModel = hiltViewModel<InsightsViewModel>()
        MoodInsightsScreen(viewModel, navController)
    }
    composable(Screens.ONBOARDING.name) { OnBoardingScreen(navController) }

    composable(
        route = "${Screens.CHATBOT.name}/{prompt}",
        arguments = listOf(navArgument("prompt") { defaultValue = "" })
    ) {
        val viewModel = hiltViewModel<ChatViewModel>()
        ChatbotScreen(viewModel, navController)
    }

    composable(Screens.MOODTRACKER.name) {
        val viewModel = hiltViewModel<MoodTrackerViewModel>()
        MoodTrackerScreen(onMoodLogged = { viewModel.fetchMoodTrend() })
    }

    composable(Screens.SETTINGS.name) {
        val chatVM = hiltViewModel<ChatViewModel>()
        val settingsVM = hiltViewModel<SettingsViewModel>()
        val registerVM = hiltViewModel<RegisterViewModel>()
        SettingsScreen(navController, settingsVM, registerVM, chatVM)
    }

    composable(Screens.BREATHING.name) { BreathingScreen() }

    composable(Screens.LOGIN.name) {
        val viewModel = hiltViewModel<RegisterViewModel>()
        LoginScreen(viewModel, navController) {
            navController.navigate(Screens.CREATEUSER.name)
        }
    }

    composable(Screens.CREATEUSER.name) {
        val viewModel = hiltViewModel<RegisterViewModel>()
        CreateUserScreen(viewModel, navController) {
            navController.navigate(Screens.LOGIN.name)
        }
    }

    composable(
        route = "${Screens.ADDJOURNAL.name}/{journalId}",
        arguments = listOf(navArgument("journalId") { defaultValue = "" })
    ) { backStackEntry ->
        val journalId = backStackEntry.arguments?.getString("journalId").orEmpty()
        val viewModel = hiltViewModel<JournalViewModel>()
        AddJournalScreen(
            viewModel = viewModel,
            userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
            journalId = journalId,
            onClose = { navController.popBackStack() },
            onSave = { navController.popBackStack() }
        )
    }

    composable(Screens.JOURNAL.name) {
        val viewModel = hiltViewModel<JournalViewModel>()
        JournalScreen(
            viewModel = viewModel,
            onAddJournalClick = {
                val newJournalId = FirebaseFirestore.getInstance().collection("journals").document().id
                navController.navigate("${Screens.ADDJOURNAL.name}/$newJournalId")
            },
            onViewJournalClick = { journal ->
                navController.navigate("${Screens.VIEWJOURNAL.name}/${journal.journalId}")
            }
        )
    }

    composable(
        route = "${Screens.VIEWJOURNAL.name}/{journalId}",
        arguments = listOf(navArgument("journalId") { defaultValue = "" })
    ) { backStackEntry ->
        val journalId = backStackEntry.arguments?.getString("journalId").orEmpty()
        val viewModel = hiltViewModel<JournalViewModel>()

        val journals by viewModel.journals.collectAsState()
        val searchResults by viewModel.searchResults.collectAsState()
        val isSearching by viewModel.isSearching.collectAsState()

        val displayedJournals = if (isSearching) searchResults else journals
        val journal = displayedJournals.find { it.journalId == journalId }

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
 */
@Composable
fun AdaptiveScreen(
    navController: NavController,
    currentDestination: String?,
    windowSize: WindowSizeClass,
    content: @Composable () -> Unit
) {
    val destination by navController.currentBackStackEntryFlow.collectAsState(initial = null)
    val showBottomBar = when (destination?.destination?.route) {
        Screens.HOME.name, Screens.MOODTRACKER.name, Screens.JOURNAL.name, Screens.INSIGHTS.name -> true
        else -> false
    }

    when (windowSize.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
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

        WindowWidthSizeClass.Medium, WindowWidthSizeClass.Expanded -> {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) {
                ScrollableNavigationRail(
                    navController = navController,
                    currentDestination = currentDestination,
                    windowSizeClass = windowSize
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
 * Animated vertical navigation rail with selection feedback for large screens.
 */
@Composable
fun AnimatedNavigationRail(
    navController: NavController,
    currentDestination: String?,
    windowSizeClass: WindowSizeClass
) {
    val dimens = LocalDimens.current

    val items = listOf(
        NavigationItem("Health", Screens.MOODTRACKER.name, R.drawable.ic_moodtracker),
        NavigationItem("Chatbot", "${Screens.CHATBOT.name}/Hi", R.drawable.ic_chat),
        NavigationItem("Home", Screens.HOME.name, R.drawable.home_123),
        NavigationItem("Journal", Screens.JOURNAL.name, R.drawable.ic_journal),
        NavigationItem("Insights", Screens.INSIGHTS.name, R.drawable.ic_insights)
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
            val isSelected = currentDestination == item.route
            val scale by animateFloatAsState(targetValue = if (isSelected) 1.2f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = 50f))
            val offset by animateDpAsState(targetValue = if (isSelected) (-10).dp else 0.dp, animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = 50f))
            val backgroundBrush = if (isSelected) {
                Brush.radialGradient(colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)))
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
                                popUpTo(Screens.HOME.name) { saveState = true }
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
    currentDestination: String?,
    windowSizeClass: WindowSizeClass
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        AnimatedNavigationRail(navController, currentDestination, windowSizeClass)
    }
}

/**
 * Bottom navigation bar for compact devices with animations and visual feedback.
 */
@Composable
fun BottomNavigationBar(
    navController: NavController,
    currentDestination: String?
) {
    val items = listOf(
        NavigationItem("Health", Screens.MOODTRACKER.name, R.drawable.ic_moodtracker),
        NavigationItem("Chatbot", "${Screens.CHATBOT.name}/Hi", R.drawable.ic_chat),
        NavigationItem("Home", Screens.HOME.name, R.drawable.home_123),
        NavigationItem("Journal", Screens.JOURNAL.name, R.drawable.ic_journal),
        NavigationItem("Insights", Screens.INSIGHTS.name, R.drawable.ic_insights)
    )

    val dimens = LocalDimens.current

    NavigationBar(
        containerColor = Color.Transparent,
        tonalElevation = dimens.elevation,
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { item ->
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            val isSelected = currentRoute == item.route

            val scale by animateFloatAsState(targetValue = if (isSelected) 1f else 0.7f, animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = 50f))
            val offset by animateDpAsState(targetValue = if (isSelected) (-5).dp else 0.dp, animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = 50f))

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
                            tint = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
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
                                popUpTo(Screens.HOME.name) { saveState = true }
                                launchSingleTop = true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
                modifier = Modifier.align(Alignment.Bottom)
            )
        }
    }
}

/**
 * Represents a single item in the navigation bar or rail.
 *
 * @property label Descriptive name of the screen
 * @property route Navigation route
 * @property icon Drawable resource ID for the screen's icon
 */
data class NavigationItem(
    val label: String,
    val route: String,
    val icon: Int
)
